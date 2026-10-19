package com.github.catvod.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class NetExchange extends WebSocketListener {

    private final Net owner;
    private final NetOptions options;
    private final Consumer<Net.Result> complete;
    private final AtomicBoolean done = new AtomicBoolean();
    private volatile WebSocket socket;
    private volatile ScheduledFuture<?> deadline;
    private volatile Headers headers = new Headers.Builder().build();
    private volatile boolean closing;

    NetExchange(Net owner, NetOptions options, Consumer<Net.Result> complete) {
        this.owner = owner;
        this.options = options;
        this.complete = complete;
    }

    void start(OkHttpClient client, Request request) {
        if (done.get()) return;
        deadline = Net.DEADLINES.schedule(() -> client.dispatcher().executorService().execute(this::onTimeout), options.getTimeout(), TimeUnit.MILLISECONDS);
        if (done.get()) {
            deadline.cancel(false);
            return;
        }
        WebSocket created = client.newWebSocket(request, this);
        socket = created;
        if (done.get() && !closing) created.cancel();
    }

    private void onTimeout() {
        finish(Net.Result.failure(new SocketTimeoutException()));
        cancel();
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        socket = webSocket;
        headers = response.headers();
        String message;
        try {
            owner.saveCookies(response, options);
            if (done.get()) {
                webSocket.cancel();
                return;
            }
            message = options.message();
        } catch (Exception e) {
            finish(Net.Result.failure(e));
            return;
        }
        if (message != null && !webSocket.send(message)) finish(Net.Result.failure("IOException: WebSocket send failed"));
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        finish(Net.Result.success(101, headers, text.getBytes(StandardCharsets.UTF_8), false));
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        finish(Net.Result.success(101, headers, bytes.toByteArray(), false));
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        finish(Net.Result.failure("IOException: WebSocket closed before a message"));
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        finish(Net.Result.failure("IOException: WebSocket closed before a message"));
        release();
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable error, @Nullable Response response) {
        finish(failure(error, response));
        release();
    }

    private Net.Result failure(Throwable error, Response response) {
        if (response == null) return Net.Result.failure(error);
        try (response) {
            owner.saveCookies(response, options);
            return Net.Result.handshakeFailure(response);
        } catch (Exception e) {
            return Net.Result.failure(e);
        }
    }

    void cancel() {
        Throwable failure = null;
        try {
            finish(Net.Result.failure(new InterruptedIOException()));
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        failure = cancelSocket(failure);
        release();
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable cancelSocket(Throwable failure) {
        WebSocket current = socket;
        try {
            if (current != null) current.cancel();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    void finish(Net.Result result) {
        if (!done.compareAndSet(false, true)) return;
        closing = result.error == null;
        Throwable failure = closeSocket();
        if (!closing || failure != null) release();
        failure = completeResult(result, failure);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable closeSocket() {
        WebSocket current = socket;
        try {
            if (current != null) {
                if (closing) current.close(1000, null);
                else current.cancel();
            }
            return null;
        } catch (RuntimeException | Error error) {
            return error;
        }
    }

    private Throwable completeResult(Net.Result result, Throwable failure) {
        try {
            complete.accept(result);
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private void release() {
        ScheduledFuture<?> timer = deadline;
        if (timer != null) timer.cancel(false);
        owner.remove(this);
    }
}
