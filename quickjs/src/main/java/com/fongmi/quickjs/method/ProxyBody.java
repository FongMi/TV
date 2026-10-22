package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.crawler.SpiderDebug;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProxyBody extends InputStream {

    private static final int CHUNK_SIZE = 65536;

    private final Global global;
    private final QuickJS.Callback pull;
    private final QuickJS.Callback cancel;
    private final Runnable release;
    private final Invocation lifetime = new Invocation();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object readLock = new Object();
    private final long timeout;
    private byte[] bytes;
    private int position;

    ProxyBody(Global global, QuickJS.Callback pull, QuickJS.Callback cancel, Runnable release, long timeout) {
        this.global = global;
        this.pull = pull;
        this.cancel = cancel;
        this.release = release;
        this.timeout = timeout;
    }

    @Override
    public int read() throws IOException {
        byte[] value = new byte[1];
        return read(value, 0, 1) == -1 ? -1 : value[0] & 255;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, target.length);
        if (length == 0) return 0;
        synchronized (readLock) {
            if (closed.get()) return -1;
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
    }

    private byte[] nextChunk() throws IOException {
        CompletableFuture<Object> pending = global.invoke(lifetime, pull, CHUNK_SIZE);
        try {
            return awaitChunk(pending);
        } catch (InterruptedException error) {
            pending.cancel(false);
            Thread.currentThread().interrupt();
            throw fail(error);
        } catch (ExecutionException error) {
            throw fail(error.getCause());
        } catch (TimeoutException error) {
            pending.cancel(false);
            throw fail(error);
        } catch (RuntimeException | IOException error) {
            throw fail(error);
        }
    }

    private byte[] awaitChunk(CompletableFuture<Object> pending) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Object value = timeout == 0 ? pending.get() : pending.get(timeout, TimeUnit.MILLISECONDS);
        if (value == null) {
            close();
            return null;
        }
        if (!(value instanceof byte[] chunk) || chunk.length == 0 || chunk.length > CHUNK_SIZE) throw new IOException("Invalid proxy stream chunk");
        return closed.get() ? null : chunk;
    }

    private IOException fail(Throwable error) {
        IOException failure = error instanceof IOException value ? value : new IOException("Proxy stream failed", error);
        try {
            close();
        } catch (RuntimeException closing) {
            failure.addSuppressed(closing);
        }
        return failure;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        release.run();
        try {
            lifetime.cancel();
        } finally {
            global.invoke(lifetime, cancel).whenComplete(this::releaseCallbacks);
        }
    }

    private void releaseCallbacks(Object value, Throwable error) {
        pull.close();
        cancel.close();
        if (error != null && !global.isDestroyed()) SpiderDebug.log(error);
    }
}
