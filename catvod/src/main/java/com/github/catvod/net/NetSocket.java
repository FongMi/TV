package com.github.catvod.net;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.ArrayDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Keep
public final class NetSocket implements AutoCloseable {

    private static final long RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 30000;

    @Keep
    @FunctionalInterface
    public interface Listener {
        void accept(NetSocket socket, Event event);
    }

    private final Net owner;
    private final String url;
    private final NetOptions options;
    private final long idleTimeoutNs;
    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private Listener listener;
    private OkHttpClient client;
    private volatile WebSocket socket;
    private ScheduledFuture<?> deadline;
    private ScheduledFuture<?> retry;
    private long retryDelayMs = RETRY_DELAY_MS;
    private long openedAtNs;
    private long receivedAtNs;
    private int generation;
    private boolean opened;
    private boolean dispatching;
    private volatile boolean closing;
    private volatile boolean finished;

    NetSocket(Net owner, String url, NetOptions options, Listener listener) {
        this.owner = owner;
        this.url = url;
        this.options = options;
        this.idleTimeoutNs = TimeUnit.MILLISECONDS.toNanos(options.getIdle());
        this.listener = listener;
    }

    synchronized void start(OkHttpClient client, Request request) {
        if (finished) return;
        this.client = client;
        connect(request);
    }

    private void connect(Request request) {
        int token = ++generation;
        scheduleTimeout(token, TimeUnit.MILLISECONDS.toNanos(options.getTimeout()));
        socket = client.newWebSocket(request, new Callback(token));
    }

    private void scheduleTimeout(int token, long delayNs) {
        if (deadline != null) deadline.cancel(false);
        deadline = Net.DEADLINES.schedule(() -> client.dispatcher().executorService().execute(() -> timeout(token)), delayNs, TimeUnit.NANOSECONDS);
    }

    private void reconnect() {
        try {
            synchronized (this) {
                if (closing || finished) return;
                retry = null;
                try {
                    connect(owner.socketRequest(url, options));
                } catch (IOException | RuntimeException error) {
                    finish(1006, "", Net.Result.failure(error).error);
                }
            }
        } finally {
            dispatch();
        }
    }

    private boolean isCurrent(int token) {
        return !finished && generation == token;
    }

    public boolean send(String text) {
        if (text == null) throw new IllegalArgumentException("WebSocket text is null");
        WebSocket current = socket;
        return !closing && !finished && current != null && current.send(text);
    }

    public boolean send(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("WebSocket bytes are null");
        WebSocket current = socket;
        return !closing && !finished && current != null && current.send(ByteString.of(bytes));
    }

    @Override
    public void close() {
        try {
            synchronized (this) {
                if (closing || finished) return;
                if (!opened) {
                    finish(1001, "Canceled", null);
                } else {
                    closing = true;
                    deadline.cancel(false);
                    socket.close(1000, "");
                }
            }
        } finally {
            dispatch();
        }
    }

    public void cancel() {
        try {
            finish(1001, "Canceled", null);
        } finally {
            dispatch();
        }
    }

    private synchronized void open(int token, WebSocket socket, Response response) {
        if (!isCurrent(token)) {
            socket.cancel();
            return;
        }
        this.socket = socket;
        opened = true;
        openedAtNs = System.nanoTime();
        receivedAtNs = openedAtNs;
        deadline.cancel(false);
        if (idleTimeoutNs > 0) scheduleTimeout(token, idleTimeoutNs);
        try {
            owner.saveCookies(response, options);
            String message = options.message();
            if (message != null && !socket.send(message)) throw new IOException("WebSocket send failed");
            events.add(new Event("open", null, 101, null, null));
        } catch (Exception e) {
            finish(1006, "", Net.Result.failure(e).error);
        }
    }

    private synchronized void message(int token, Object content) {
        if (closing || !isCurrent(token)) return;
        if (idleTimeoutNs > 0) receivedAtNs = System.nanoTime();
        events.add(new Event("message", content, 0, null, null));
    }

    private synchronized void closing(int token, WebSocket socket, int code, String reason) {
        if (isCurrent(token)) socket.close(code, reason);
    }

    private synchronized void failed(int token, Throwable error, Response response) {
        if (!isCurrent(token)) {
            if (response != null) response.close();
            return;
        }
        String message = Net.Result.failure(error).error;
        boolean recoverable = isRecoverable(error);
        if (response != null) {
            try (response) {
                owner.saveCookies(response, options);
                message = "WebSocket handshake failed (HTTP " + response.code() + ")";
                recoverable = response.code() == 408 || response.code() == 425 || response.code() == 429 || response.code() >= 500 && response.code() <= 599;
            } catch (Exception e) {
                message = Net.Result.failure(e).error;
                recoverable = false;
            }
        }
        disconnected(token, 1006, "", message, recoverable);
    }

    private boolean isRecoverable(Throwable error) {
        if (!(error instanceof IOException) || error instanceof ProtocolException || error instanceof SSLPeerUnverifiedException) return false;
        return !(error instanceof SSLHandshakeException) || !(error.getCause() instanceof CertificateException);
    }

    private synchronized void disconnected(int token, int code, String reason, String error, boolean recoverable) {
        if (!isCurrent(token)) return;
        if (!closing && options.isReconnect() && recoverable) scheduleReconnect(code, reason, error);
        else finish(code, reason, error);
    }

    private void scheduleReconnect(int code, String reason, String error) {
        if (opened && System.nanoTime() - openedAtNs >= TimeUnit.MILLISECONDS.toNanos(MAX_RETRY_DELAY_MS)) retryDelayMs = RETRY_DELAY_MS;
        generation++;
        opened = false;
        if (deadline != null) deadline.cancel(false);
        WebSocket previous = socket;
        socket = null;
        if (previous != null) previous.cancel();
        retry = Net.DEADLINES.schedule(() -> client.dispatcher().executorService().execute(this::reconnect), retryDelayMs, TimeUnit.MILLISECONDS);
        retryDelayMs = Math.min(retryDelayMs * 2, MAX_RETRY_DELAY_MS);
        events.add(new Event("retry", null, code, reason, error));
    }

    private void timeout(int token) {
        try {
            synchronized (this) {
                if (closing || !isCurrent(token)) return;
                if (opened) {
                    if (idleTimeoutNs == 0) return;
                    long remainingNs = idleTimeoutNs - (System.nanoTime() - receivedAtNs);
                    if (remainingNs > 0) {
                        scheduleTimeout(token, remainingNs);
                        return;
                    }
                }
                String error = opened ? "WebSocket receive timeout" : Net.Result.failure(new SocketTimeoutException()).error;
                disconnected(token, 1006, "", error, true);
            }
        } finally {
            dispatch();
        }
    }

    private synchronized void finish(int code, String reason, String error) {
        if (finished) return;
        finished = true;
        generation++;
        if (deadline != null) deadline.cancel(false);
        if (retry != null) retry.cancel(false);
        owner.remove(this);
        if (error != null) events.add(new Event("error", null, 0, null, error));
        events.add(new Event("close", null, code, reason, null));
        WebSocket previous = socket;
        socket = null;
        if (previous != null) previous.cancel();
    }

    private void dispatch() {
        synchronized (this) {
            if (dispatching) return;
            dispatching = true;
        }
        Throwable failure = null;
        while (true) {
            Event event;
            Listener callback;
            synchronized (this) {
                event = events.poll();
                if (event == null) {
                    dispatching = false;
                    break;
                }
                callback = listener;
            }
            failure = deliverEvent(callback, event, failure);
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable deliverEvent(Listener callback, Event event, Throwable failure) {
        try {
            callback.accept(this, event);
        } catch (Throwable e) {
            SpiderDebug.log(e);
            try {
                finish(1006, "", Net.Result.failure(e).error);
            } catch (RuntimeException | Error closing) {
                if (failure == null) failure = closing;
                else if (failure != closing) failure.addSuppressed(closing);
            }
        } finally {
            if (event.type.equals("close")) {
                synchronized (this) {
                    listener = null;
                }
            }
        }
        return failure;
    }

    private final class Callback extends WebSocketListener {

        private final int token;

        private Callback(int token) {
            this.token = token;
        }

        @Override
        public void onOpen(@NonNull WebSocket socket, @NonNull Response response) {
            try {
                open(token, socket, response);
            } finally {
                dispatch();
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket socket, @NonNull String text) {
            message(token, text);
            dispatch();
        }

        @Override
        public void onMessage(@NonNull WebSocket socket, @NonNull ByteString bytes) {
            message(token, bytes.toByteArray());
            dispatch();
        }

        @Override
        public void onClosing(@NonNull WebSocket socket, int code, @NonNull String reason) {
            closing(token, socket, code, reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket socket, int code, @NonNull String reason) {
            disconnected(token, code, reason, null, code == 1000 || code == 1001 || code == 1011 || code == 1012 || code == 1013);
            dispatch();
        }

        @Override
        public void onFailure(@NonNull WebSocket socket, @NonNull Throwable error, @Nullable Response response) {
            failed(token, error, response);
            dispatch();
        }
    }

    @Keep
    public static final class Event {
        public final String type;
        public final Object content;
        public final boolean binary;
        public final int code;
        public final String reason;
        public final String error;

        private Event(String type, Object content, int code, String reason, String error) {
            this.type = type;
            this.content = content;
            this.binary = content instanceof byte[];
            this.code = code;
            this.reason = reason;
            this.error = error;
        }

        public Object[] parts() {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("type", type);
            if (type.equals("message")) metadata.addProperty("binary", binary);
            if (type.equals("open") || type.equals("close") || type.equals("retry")) metadata.addProperty("code", code);
            if (reason != null) metadata.addProperty("reason", reason);
            if (error != null) metadata.addProperty("error", error);
            return new Object[]{metadata.toString(), content};
        }
    }
}
