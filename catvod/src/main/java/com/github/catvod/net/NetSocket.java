package com.github.catvod.net;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Keep
public final class NetSocket implements AutoCloseable {

    @Keep
    @FunctionalInterface
    public interface Listener {
        void accept(NetSocket socket, Event event);
    }

    private final Net owner;
    private final NetOptions options;
    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private Listener listener;
    private volatile WebSocket socket;
    private ScheduledFuture<?> deadline;
    private boolean opened;
    private boolean dispatching;
    private volatile boolean closing;
    private volatile boolean finished;

    NetSocket(Net owner, NetOptions options, Listener listener) {
        this.owner = owner;
        this.options = options;
        this.listener = listener;
    }

    synchronized void start(OkHttpClient client, Request request) {
        if (finished) return;
        deadline = Net.DEADLINES.schedule(() -> client.dispatcher().executorService().execute(this::timeout), options.getTimeout(), TimeUnit.MILLISECONDS);
        socket = client.newWebSocket(request, new Callback());
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
                    abort(1001, "Canceled", null);
                } else {
                    closing = true;
                    socket.close(1000, "");
                }
            }
        } finally {
            dispatch();
        }
    }

    public void cancel() {
        try {
            abort(1001, "Canceled", null);
        } finally {
            dispatch();
        }
    }

    private synchronized void open(WebSocket socket, Response response) {
        if (finished) {
            socket.cancel();
            return;
        }
        this.socket = socket;
        opened = true;
        deadline.cancel(false);
        try {
            owner.saveCookies(response, options);
            String message = options.message();
            if (message != null && !socket.send(message)) throw new IOException("WebSocket send failed");
            events.add(new Event("open", null, 101, null, null));
        } catch (Exception e) {
            abort(1006, "", Net.Result.failure(e).error);
        }
    }

    private synchronized void message(Object content) {
        if (!closing && !finished) events.add(new Event("message", content, 0, null, null));
    }

    private synchronized void closing(WebSocket socket, int code, String reason) {
        closing = true;
        socket.close(code, reason);
    }

    private synchronized void failed(Throwable error, Response response) {
        if (finished) {
            if (response != null) response.close();
            return;
        }
        String message = Net.Result.failure(error).error;
        if (response != null) {
            try (response) {
                owner.saveCookies(response, options);
                message = "WebSocket handshake failed (HTTP " + response.code() + ")";
            } catch (Exception e) {
                message = Net.Result.failure(e).error;
            }
        }
        finish(1006, "", message);
    }

    private synchronized void abort(int code, String reason, String error) {
        if (finished) return;
        try {
            if (socket != null) socket.cancel();
        } finally {
            finish(code, reason, error);
        }
    }

    private void timeout() {
        try {
            synchronized (this) {
                if (!opened) abort(1006, "", Net.Result.failure(new SocketTimeoutException()).error);
            }
        } finally {
            dispatch();
        }
    }

    private synchronized void finish(int code, String reason, String error) {
        if (finished) return;
        finished = true;
        if (deadline != null) deadline.cancel(false);
        owner.remove(this);
        if (error != null) events.add(new Event("error", null, 0, null, error));
        events.add(new Event("close", null, code, reason, null));
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
                abort(1006, "", Net.Result.failure(e).error);
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
        @Override
        public void onOpen(@NonNull WebSocket socket, @NonNull Response response) {
            try {
                open(socket, response);
            } finally {
                dispatch();
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket socket, @NonNull String text) {
            message(text);
            dispatch();
        }

        @Override
        public void onMessage(@NonNull WebSocket socket, @NonNull ByteString bytes) {
            message(bytes.toByteArray());
            dispatch();
        }

        @Override
        public void onClosing(@NonNull WebSocket socket, int code, @NonNull String reason) {
            closing(socket, code, reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket socket, int code, @NonNull String reason) {
            finish(code, reason, null);
            dispatch();
        }

        @Override
        public void onFailure(@NonNull WebSocket socket, @NonNull Throwable error, @Nullable Response response) {
            failed(error, response);
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
            if (type.equals("open") || type.equals("close")) metadata.addProperty("code", code);
            if (reason != null) metadata.addProperty("reason", reason);
            if (error != null) metadata.addProperty("error", error);
            return new Object[]{metadata.toString(), content};
        }
    }
}
