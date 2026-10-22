package com.fongmi.chaquo;

import com.chaquo.python.PyObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProxyBody extends InputStream {

    private static final int CHUNK_SIZE = 65536;

    private final Streams streams;
    private final PyObject source;
    private final Streams.Scope scope;
    private final Object readLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean reading = new AtomicBoolean();
    private final AtomicBoolean released = new AtomicBoolean();
    private byte[] bytes;
    private int position;

    ProxyBody(Streams streams, PyObject source, Streams.Scope scope) {
        this.streams = streams;
        this.source = source;
        this.scope = scope;
    }

    @Override
    public int read() throws IOException {
        byte[] value = new byte[1];
        return read(value, 0, 1) == -1 ? -1 : value[0] & 255;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        int capacity = target.length;
        if (offset < 0 || length < 0 || length > capacity - offset) throw new IndexOutOfBoundsException();
        if (length == 0) return 0;
        synchronized (readLock) {
            reading.set(true);
            Throwable failure = null;
            try {
                if (closed.get()) return -1;
                return readChunk(target, offset, length);
            } catch (IOException | RuntimeException | Error error) {
                failure = error;
                closeAfterFailure(error);
                throw error;
            } finally {
                reading.set(false);
                releaseSource(failure);
            }
        }
    }

    private int readChunk(byte[] target, int offset, int length) throws IOException {
        if (bytes == null || position == bytes.length) {
            bytes = nextChunk();
            position = 0;
            if (bytes == null) return -1;
        }
        int count = Math.min(length, bytes.length - position);
        System.arraycopy(bytes, position, target, offset, count);
        position += count;
        return count;
    }

    private byte[] nextChunk() throws IOException {
        try {
            while (!closed.get()) {
                PyObject value = streams.call(scope, source, "pull", CHUNK_SIZE);
                if (value == null) {
                    close();
                    return null;
                }
                byte[] chunk = value.toJava(byte[].class);
                if (chunk.length > CHUNK_SIZE) throw new IOException("Invalid proxy stream chunk");
                if (chunk.length != 0) return closed.get() ? null : chunk;
            }
            return null;
        } catch (RuntimeException error) {
            throw new IOException("Proxy stream failed", error);
        }
    }

    private void closeAfterFailure(Throwable failure) {
        try {
            close();
        } catch (RuntimeException | Error closing) {
            if (closing != failure) failure.addSuppressed(closing);
        }
    }

    private void releaseSource(Throwable failure) {
        try {
            if (closed.get() && !reading.get() && released.compareAndSet(false, true)) {
                bytes = null;
                streams.call(scope, source, "close");
            }
        } catch (RuntimeException | Error closing) {
            if (failure == null) throw closing;
            if (closing != failure) failure.addSuppressed(closing);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        streams.release(this);
        Throwable failure = null;
        try {
            scope.close();
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            releaseSource(failure);
        }
    }
}
