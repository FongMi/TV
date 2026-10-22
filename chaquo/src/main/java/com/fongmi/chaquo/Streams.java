package com.fongmi.chaquo;

import androidx.annotation.Keep;

import com.chaquo.python.PyObject;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Keep
public final class Streams implements AutoCloseable {

    private final Set<ProxyBody> bodies = new HashSet<>();
    private final ThreadLocal<Scope> current = new ThreadLocal<>();
    private boolean closed;

    public StreamReader open(Net net, String url, String options) throws IOException {
        Scope scope = current.get();
        synchronized (bodies) {
            if (closed) throw new IOException("Stream owner closed");
        }
        if (scope != null) scope.checkOpen();
        CompletableFuture<Net.StreamResult> pending = net.requestStreamAsync(url, NetOptions.from(options));
        if (scope != null) scope.track(pending);
        try {
            StreamReader reader = new StreamReader(pending.get(), scope);
            if (scope != null) scope.accept(reader);
            return reader;
        } catch (InterruptedException error) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            InterruptedIOException failure = new InterruptedIOException("Stream request interrupted");
            failure.initCause(error);
            throw failure;
        } catch (ExecutionException error) {
            throw new IOException("Stream request failed", error.getCause());
        } finally {
            if (scope != null) scope.release(pending);
        }
    }

    public InputStream body(PyObject source) {
        synchronized (bodies) {
            if (closed) throw new IllegalStateException("Stream owner closed");
            ProxyBody body = new ProxyBody(this, source, new Scope());
            bodies.add(body);
            return body;
        }
    }

    PyObject call(Scope scope, PyObject source, String method, Object... args) {
        Scope previous = current.get();
        current.set(scope);
        try {
            return source.callAttr(method, args);
        } finally {
            if (previous == null) current.remove();
            else current.set(previous);
        }
    }

    void release(ProxyBody body) {
        synchronized (bodies) {
            bodies.remove(body);
        }
    }

    @Override
    public void close() {
        ArrayList<ProxyBody> pending;
        synchronized (bodies) {
            if (closed) return;
            closed = true;
            pending = new ArrayList<>(bodies);
        }
        Throwable failure = null;
        for (ProxyBody body : pending) {
            try {
                body.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    static final class Scope {

        private final Set<CompletableFuture<?>> pending = new HashSet<>();
        private final Set<StreamReader> readers = new HashSet<>();
        private boolean closed;

        synchronized void checkOpen() throws IOException {
            if (closed) throw new IOException("Proxy stream closed");
        }

        synchronized void track(CompletableFuture<?> request) {
            if (closed) request.cancel(true);
            else pending.add(request);
        }

        synchronized void release(CompletableFuture<?> request) {
            pending.remove(request);
        }

        void accept(StreamReader reader) throws IOException {
            synchronized (this) {
                if (!closed) {
                    readers.add(reader);
                    return;
                }
            }
            reader.close();
            throw new IOException("Proxy stream closed");
        }

        synchronized void release(StreamReader reader) {
            readers.remove(reader);
        }

        void close() {
            ArrayList<CompletableFuture<?>> requests;
            ArrayList<StreamReader> streaming;
            synchronized (this) {
                if (closed) return;
                closed = true;
                requests = new ArrayList<>(pending);
                streaming = new ArrayList<>(readers);
                pending.clear();
                readers.clear();
            }
            for (CompletableFuture<?> request : requests) request.cancel(true);
            Throwable failure = null;
            for (StreamReader reader : streaming) {
                try {
                    reader.close();
                } catch (RuntimeException | Error error) {
                    if (failure == null) failure = error;
                    else if (failure != error) failure.addSuppressed(error);
                }
            }
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }
    }
}
