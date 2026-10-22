package com.fongmi.chaquo;

import androidx.annotation.Keep;

import com.github.catvod.net.Net;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Keep
public final class StreamReader extends InputStream {

    private static final Gson GSON = new Gson();

    private final Net.StreamResult response;
    private final Streams.Scope scope;
    private final AtomicBoolean reading = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean eof;
    private byte[] chunk;

    StreamReader(Net.StreamResult response, Streams.Scope scope) {
        this.response = response;
        this.scope = scope;
    }

    public String metadata() {
        return GSON.toJson(Map.of("code", response.code, "headers", response.headers, "contentType", response.contentType));
    }

    public byte[] readChunk(int size) throws IOException {
        if (size < 1 || size > 1048576) throw new IllegalArgumentException("Chunk size must be 1..1048576 bytes");
        if (!beginRead()) return null;
        try {
            if (chunk == null || chunk.length < size) chunk = new byte[size];
            int count = readResponse(chunk, 0, size);
            return count == -1 ? null : Arrays.copyOf(chunk, count);
        } finally {
            reading.set(false);
        }
    }

    @Override
    public int read() throws IOException {
        byte[] bytes = new byte[1];
        return read(bytes, 0, 1) == -1 ? -1 : bytes[0] & 255;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int capacity = bytes.length;
        if (offset < 0 || length < 0 || length > capacity - offset) throw new IndexOutOfBoundsException();
        if (length == 0) return 0;
        if (!beginRead()) return -1;
        try {
            return readResponse(bytes, offset, length);
        } finally {
            reading.set(false);
        }
    }

    private boolean beginRead() throws IOException {
        if (eof) return false;
        if (closed.get()) throw new IOException("Stream reader closed");
        if (!reading.compareAndSet(false, true)) throw new IOException("Stream read already pending");
        return true;
    }

    private int readResponse(byte[] bytes, int offset, int length) throws IOException {
        try {
            int count = response.read(bytes, offset, length);
            if (count == -1) {
                eof = true;
                close();
            }
            return count;
        } catch (IOException | RuntimeException | Error error) {
            try {
                close();
            } catch (RuntimeException | Error closing) {
                if (closing != error) error.addSuppressed(closing);
            }
            throw error;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            response.close();
        } finally {
            if (scope != null) scope.release(this);
        }
    }
}
