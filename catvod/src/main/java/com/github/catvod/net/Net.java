package com.github.catvod.net;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.github.catvod.Runtime;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.ByteString;

@Keep
public final class Net implements AutoCloseable {

    private static final int TEXT_TIMEOUT = 15000;

    @Keep
    public interface Cookies {
        String getCookie(String url);

        boolean setCookie(String url, String cookie);
    }

    @Keep
    public interface Mapper<T, R> {
        R apply(T item, int index) throws Exception;
    }

    static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Spider WebSocket timeout");
        thread.setDaemon(true);
        return thread;
    });

    private final Cookies cookies;
    private final OkHttpClient client;
    private final Net parent;
    private final NetTrace trace;
    private final Object lock = new Object();
    private final Set<Call> calls = new HashSet<>();
    private final Set<StreamResult> streams = new HashSet<>();
    private final Set<NetExchange> sockets = new HashSet<>();
    private final Set<NetSocket> connections = new HashSet<>();
    private final Set<Net> children = new HashSet<>();
    private final Set<NetMap<?, ?>> maps = new HashSet<>();
    private final NetCache cache;
    private boolean closed;

    public Net() {
        this(null);
    }

    public Net(Cookies cookies) {
        this(OkHttp.client(), cookies);
    }

    public Net(Cookies cookies, String siteKey) {
        this(OkHttp.client(), cookies, null, new NetStore(siteKey), NetTrace.find(siteKey));
    }

    Net(OkHttpClient client, Cookies cookies) {
        this(client, cookies, null);
    }

    Net(OkHttpClient client, Cookies cookies, NetTrace trace) {
        this(client, cookies, null, null, trace);
    }

    private Net(OkHttpClient client, Cookies cookies, Net parent, NetStore store, NetTrace trace) {
        this.client = client;
        this.cookies = cookies;
        this.parent = parent;
        this.trace = trace;
        this.cache = store == null ? new NetCache() : new NetCache(store);
    }

    public Net session() {
        synchronized (lock) {
            checkOpen();
            Net child = new Net(client, new SessionCookies(), this, null, trace);
            children.add(child);
            return child;
        }
    }

    public String getCookie(String url) {
        checkOpen();
        HttpUrl target = url == null ? null : HttpUrl.parse(url);
        if (cookies == null || target == null) return "";
        String value = cookies.getCookie(target.toString());
        return value == null ? "" : value;
    }

    public boolean setCookie(String url, String value) {
        checkOpen();
        HttpUrl target = url == null ? null : HttpUrl.parse(url);
        if (cookies == null || target == null || value == null || Cookie.parse(target, value) == null) return false;
        return cookies.setCookie(target.toString(), value);
    }

    private void checkOpen() {
        synchronized (lock) {
            if (closed) throw new IllegalStateException("Network owner closed");
        }
    }

    public void sleep(long milliseconds) throws InterruptedIOException {
        if (milliseconds < 0) throw new IllegalArgumentException("Sleep duration must not be negative");
        long started = System.nanoTime();
        synchronized (lock) {
            while (true) {
                if (closed) throw new InterruptedIOException("Network owner closed");
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Thread interrupted");
                long remaining = milliseconds - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                if (remaining <= 0) return;
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    throw interrupted(e);
                }
            }
        }
    }

    public String cached(String key, String options, Callable<String> loader) throws Exception {
        if (loader == null) throw new IllegalArgumentException("Cache loader is required");
        CompletableFuture<String> result = cachedAsync(key, options, () -> load(loader));
        try {
            return result.get();
        } catch (InterruptedException e) {
            throw interrupted(e);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            if (error.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException(error.getCause());
        }
    }

    private static InterruptedIOException interrupted(InterruptedException cause) {
        Thread.currentThread().interrupt();
        InterruptedIOException error = new InterruptedIOException("Thread interrupted");
        error.initCause(cause);
        return error;
    }

    static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    private CompletableFuture<String> load(Callable<String> loader) {
        CompletableFuture<String> loading = new CompletableFuture<>();
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                loading.complete(loader.call());
            } catch (Throwable error) {
                if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                loading.completeExceptionally(error);
            }
            return null;
        });
        loading.whenComplete((value, error) -> {
            if (loading.isCancelled()) task.cancel(true);
        });
        client.dispatcher().executorService().execute(task);
        return loading;
    }

    public CompletableFuture<String> cachedAsync(String key, String options, Supplier<CompletableFuture<String>> loader) {
        synchronized (lock) {
            if (closed) return failedFuture(new InterruptedIOException("Network owner closed"));
        }
        return cache.cached(key, CacheOptions.from(options), loader);
    }

    public String peek(String key) {
        checkOpen();
        return cache.peek(key);
    }

    public String cachedValue(String key, String options) {
        checkOpen();
        return cache.value(key, CacheOptions.from(options));
    }

    public void refresh(String key, String options, Callable<String> loader) {
        if (loader == null) throw new IllegalArgumentException("Cache loader is required");
        CacheOptions config = CacheOptions.from(options);
        synchronized (lock) {
            if (closed) {
                SpiderDebug.log(new InterruptedIOException("Network owner closed"));
                return;
            }
        }
        cache.cached(key, new CacheOptions(config.ttl, false, config.persist), () -> load(loader)).whenComplete((value, error) -> {
            if (error != null) SpiderDebug.log(error);
        });
    }

    public void clearCache(String key) {
        cache.clear(key);
    }

    public void clearCache() {
        cache.clear(null);
    }

    public String json(String url, String options) {
        String text = jsonText(url, options);
        Json.strict(text);
        return text;
    }

    public String jsonText(String url, String options) {
        NetOptions request = NetOptions.from(options);
        if (request.getBuffer() != 0) throw new IllegalArgumentException("JSON requests require text content");
        Result result = request(url, request);
        if (result.error != null) throw new IllegalStateException(result.error);
        int code = (Integer) result.code;
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return result.text(request);
    }

    public String req(String url, String json) {
        return json(url, json, false);
    }

    public String get(String url) {
        return get(url, null);
    }

    public String get(String url, Map<String, String> headers) {
        return text(url, NetOptions.text("GET", null, headers, TEXT_TIMEOUT));
    }

    public String get(String url, int timeout) {
        return text(url, NetOptions.text("GET", null, null, timeout));
    }

    public String post(String url, String json) {
        return post(url, json, null);
    }

    public String post(String url, String json, Map<String, String> headers) {
        return text(url, NetOptions.text("POST", json, headers, TEXT_TIMEOUT));
    }

    private String text(String url, NetOptions options) {
        Result result = request(url, options);
        if (result.error != null) throw new IllegalStateException(result.error);
        return result.text(options);
    }

    public String ws(String url, String json) {
        return json(url, json, true);
    }

    private String json(String url, String json, boolean webSocket) {
        NetOptions options = new NetOptions();
        try {
            options = NetOptions.from(json);
            return (webSocket ? socket(url, options) : request(url, options)).json(options);
        } catch (Exception e) {
            return Result.failure(e).json(options);
        }
    }

    public Object[] reqParts(String url, String json) {
        return parts(url, json, false);
    }

    public Object[] wsParts(String url, String json) {
        return parts(url, json, true);
    }

    private Object[] parts(String url, String json, boolean webSocket) {
        NetOptions options = new NetOptions();
        try {
            options = NetOptions.from(json);
            return (webSocket ? socket(url, options) : request(url, options)).parts(options);
        } catch (Exception e) {
            return Result.failure(e).parts(options);
        }
    }

    private Result socket(String url, NetOptions options) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Result> result = new AtomicReference<>();
        NetExchange exchange = openSocket(url, options, value -> {
            result.set(value);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            if (exchange != null) exchange.cancel();
            Thread.currentThread().interrupt();
            return Result.failure(e);
        }
        return result.get();
    }

    public Result request(String url, NetOptions options) {
        CompletableFuture<Result> future = requestAsync(url, options);
        try {
            return future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            return Result.failure(interrupted(e));
        } catch (ExecutionException e) {
            return Result.failure(e.getCause());
        }
    }

    public <T, R> List<R> map(List<T> items, Mapper<T, R> mapper) throws Exception {
        return map(items, mapper, 4);
    }

    public <T, R> List<R> map(List<T> items, Mapper<T, R> mapper, int limit) throws Exception {
        if (items == null || mapper == null || limit < 1) throw new IllegalArgumentException("Invalid map options");
        NetMap<T, R> mapping;
        synchronized (lock) {
            checkOpen();
            if (items.isEmpty()) return new ArrayList<>();
            mapping = new NetMap<>(items, mapper, limit, this::removeMap);
            maps.add(mapping);
        }
        try {
            return mapping.start().get();
        } catch (InterruptedException error) {
            mapping.cancel();
            throw interrupted(error);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            if (error.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException(error.getCause());
        }
    }

    private void removeMap(NetMap<?, ?> mapping) {
        synchronized (lock) {
            maps.remove(mapping);
        }
    }

    public Result[] batch(String requests) throws IOException {
        return batch(requests, 4);
    }

    public Result[] batch(String requests, int limit) throws IOException {
        return batch(Json.strict(requests).getAsJsonArray(), limit);
    }

    private Result[] batch(JsonArray requests, int limit) throws IOException {
        validateBatch(requests, limit);
        Result[] results = new Result[requests.size()];
        List<CompletableFuture<Result>> active = new ArrayList<>();
        try {
            for (int start = 0; start < requests.size(); start += active.size()) {
                active.clear();
                enqueueBatch(requests, start, limit, active);
                awaitBatch(active, results, start);
            }
            return results;
        } catch (InterruptedException e) {
            throw interrupted(e);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } finally {
            for (CompletableFuture<Result> request : active) if (!request.isDone()) request.cancel(true);
        }
    }

    private void enqueueBatch(JsonArray requests, int start, int limit, List<CompletableFuture<Result>> active) {
        for (int index = start; index < requests.size() && active.size() < limit; index++) {
            JsonObject item = requests.get(index).getAsJsonObject();
            String url = item.get("url").getAsString();
            active.add(requestAsync(url, NetOptions.from(item.get("options") == null ? null : item.get("options").toString())));
        }
    }

    private void validateBatch(JsonArray requests, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Batch limit must be positive");
        for (JsonElement value : requests) validateBatchRequest(value);
    }

    private void validateBatchRequest(JsonElement value) {
        if (!value.isJsonObject()) throw new IllegalArgumentException("Invalid batch request");
        JsonObject item = value.getAsJsonObject();
        JsonElement url = item.get("url"), options = item.get("options");
        if (url == null || !url.isJsonPrimitive() || !url.getAsJsonPrimitive().isString() || options != null && !options.isJsonNull() && !options.isJsonObject()) throw new IllegalArgumentException("Invalid batch request");
    }

    private void awaitBatch(List<CompletableFuture<Result>> active, Result[] results, int start) throws InterruptedException, ExecutionException {
        for (int index = 0; index < active.size(); index++) results[start + index] = active.get(index).get();
    }

    public Object[][] batchParts(String requests, int limit) throws IOException {
        JsonArray items = Json.strict(requests).getAsJsonArray();
        Result[] results = batch(items, limit);
        Object[][] parts = new Object[results.length][];
        for (int i = 0; i < parts.length; i++) {
            JsonElement options = items.get(i).getAsJsonObject().get("options");
            parts[i] = results[i].parts(NetOptions.from(options == null ? null : options.toString()));
        }
        return parts;
    }

    public void download(String url, String json, File file) throws IOException {
        CompletableFuture<String> future = downloadAsync(url, json, file);
        try {
            future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            throw interrupted(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException error) throw error;
            throw new IOException(e.getCause());
        }
    }

    public CompletableFuture<String> downloadAsync(String url, String json, File file) {
        File target = file.isAbsolute() ? file : new File(Runtime.context().getCacheDir(), file.getPath());
        CompletableFuture<String> future = new CompletableFuture<>();
        NetOptions options;
        Download download;
        Call call;
        try {
            options = NetOptions.from(json);
            download = createDownload(url, target, options);
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
        try {
            call = newDownloadCall(download, options);
        } catch (Exception e) {
            closeDownload(download, e);
            future.completeExceptionally(e);
            return future;
        }
        future.whenComplete((result, error) -> {
            if (future.isCancelled()) cancelDownload(call, download);
        });
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                failDownload(call, e, download, future);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                finishDownload(call, response, download, target, future);
            }
        });
        return future;
    }

    private Download createDownload(String url, File target, NetOptions options) {
        Download download = new Download(NetRequest.build(url, options, false), target)
                .maxBytes(options.getMaxBytes())
                .threads(options.getThreads())
                .resume(!usesCookies(options))
                .strictStatus(false)
                .allowEmpty(true);
        if (options.getSize() > 0) download.size(options.getSize());
        return download;
    }

    private Call newDownloadCall(Download download, NetOptions options) throws IOException {
        return newCall(download.request(request -> {
            try {
                return newCall(request, options);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }), options);
    }

    private void failDownload(Call call, IOException e, Download download, CompletableFuture<String> future) {
        closeDownload(download, e);
        remove(call);
        future.completeExceptionally(e);
    }

    private void closeDownload(Download download, Throwable failure) {
        try {
            download.close();
        } catch (IOException cleanup) {
            failure.addSuppressed(cleanup);
        }
    }

    private void finishDownload(Call call, Response response, Download download, File target, CompletableFuture<String> future) {
        try (download) {
            download.write(call, response, null);
        } catch (Exception e) {
            future.completeExceptionally(response.isSuccessful() ? e : new IOException("HTTP " + response.code(), e));
            return;
        } finally {
            download.releaseCalls(this::remove);
            remove(call);
        }
        future.complete(target.getAbsolutePath());
    }

    private void cancelDownload(Call call, Download download) {
        Throwable failure = null;
        try {
            call.cancel();
        } catch (RuntimeException | Error closing) {
            failure = closing;
        }
        try {
            download.cancel();
        } catch (RuntimeException | Error closing) {
            if (failure == null) failure = closing;
            else if (failure != closing) failure.addSuppressed(closing);
        }
        if (failure instanceof RuntimeException closing) throw closing;
        if (failure instanceof Error closing) throw closing;
    }

    public void enqueue(String url, NetOptions options, Consumer<Result> complete) {
        requestAsync(url, options).whenComplete((result, error) -> complete.accept(error == null ? result : Result.failure(error)));
    }

    public CompletableFuture<Result> requestAsync(String url, NetOptions options) {
        CompletableFuture<Result> future = new CompletableFuture<>();
        Call call;
        try {
            call = newCall(url, options);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Result.failure(e));
        }
        future.whenComplete((result, error) -> {
            if (future.isCancelled()) call.cancel();
        });
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                remove(call);
                future.complete(Result.failure(call, e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                finishRequest(call, response, future);
            }
        });
        return future;
    }

    private void finishRequest(Call call, Response response, CompletableFuture<Result> future) {
        Result result;
        try (response) {
            result = Result.read(response);
        } catch (Exception e) {
            result = Result.failure(call, e);
        } finally {
            remove(call);
        }
        future.complete(result);
    }

    public CompletableFuture<StreamResult> requestStreamAsync(String url, NetOptions options) {
        CompletableFuture<StreamResult> future = new CompletableFuture<>();
        Call call;
        try {
            call = newCall(url, options);
        } catch (Exception error) {
            return failedFuture(error);
        }
        future.whenComplete((result, error) -> {
            if (future.isCancelled()) call.cancel();
        });
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException error) {
                remove(call);
                future.completeExceptionally(error);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                acceptStream(call, response, future);
            }
        });
        return future;
    }

    private void acceptStream(Call call, Response response, CompletableFuture<StreamResult> future) {
        StreamResult result = new StreamResult(call, response);
        boolean accepted;
        synchronized (lock) {
            accepted = !closed && !call.isCanceled();
            if (accepted) streams.add(result);
        }
        if (!accepted) {
            InterruptedIOException error = new InterruptedIOException("Network request canceled");
            try {
                result.close();
            } catch (RuntimeException | Error closing) {
                error.addSuppressed(closing);
            }
            future.completeExceptionally(error);
        } else if (!future.complete(result)) {
            result.close();
        }
    }

    public void webSocket(String url, NetOptions options, Consumer<Result> complete) {
        openSocket(url, options, complete);
    }

    public CompletableFuture<Result> socketAsync(String url, NetOptions options) {
        CompletableFuture<Result> future = new CompletableFuture<>();
        NetExchange exchange = openSocket(url, options, future::complete);
        future.whenComplete((result, error) -> {
            if (future.isCancelled() && exchange != null) exchange.cancel();
        });
        return future;
    }

    private Call newCall(String url, NetOptions options) throws IOException {
        return newCall(NetRequest.build(url, options, false), options);
    }

    private Call newCall(Request request, NetOptions options) throws IOException {
        OkHttpClient http = builder(options).addNetworkInterceptor(chain -> interceptCookies(chain, request, options)).build();
        Call call = http.newCall(request.newBuilder().tag(NetOptions.class, options).build());
        synchronized (lock) {
            if (closed) throw new InterruptedIOException("Network owner closed");
            calls.add(call);
        }
        return call;
    }

    private Response interceptCookies(Interceptor.Chain chain, Request request, NetOptions options) throws IOException {
        Request next = withCookies(chain.request(), request.url(), request.header("Cookie"), options);
        Response response = chain.proceed(next);
        try {
            saveCookies(response, options);
            return response;
        } catch (IOException | RuntimeException e) {
            response.close();
            throw e;
        }
    }

    private OkHttpClient.Builder builder(NetOptions options) {
        OkHttpClient.Builder builder = client.newBuilder()
                .connectTimeout(options.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(options.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(options.getTimeout(), TimeUnit.MILLISECONDS)
                .callTimeout(options.getCallTimeout(), TimeUnit.MILLISECONDS)
                .followRedirects(options.isRedirect())
                .followSslRedirects(options.isRedirect());
        if (trace != null) builder.eventListenerFactory(trace::listener);
        return builder;
    }

    private Request withCookies(Request request, HttpUrl origin, String explicit, NetOptions options) throws IOException {
        if (explicit == null && !usesCookies(options)) return request;
        Request.Builder builder = request.newBuilder().removeHeader("Cookie");
        String value = null;
        if (explicit != null && sameOrigin(origin, request.url())) {
            value = explicit;
        } else if (usesCookies(options)) {
            try {
                value = getCookie(request.url().toString());
            } catch (RuntimeException e) {
                throw cookieFailure(e);
            }
        }
        if (value != null && !value.isEmpty()) builder.header("Cookie", value);
        return builder.build();
    }

    void saveCookies(Response response, NetOptions options) throws IOException {
        if (!usesCookies(options)) return;
        HttpUrl url = response.request().url();
        try {
            for (String value : response.headers("Set-Cookie")) setCookie(url.toString(), value);
        } catch (RuntimeException e) {
            throw cookieFailure(e);
        }
    }

    private IOException cookieFailure(RuntimeException cause) {
        synchronized (lock) {
            IOException error = closed ? new InterruptedIOException("Network owner closed") : new IOException("Cookie provider failed");
            error.initCause(cause);
            return error;
        }
    }

    private boolean usesCookies(NetOptions options) {
        return parent != null || options.isCookie();
    }

    private boolean sameOrigin(HttpUrl first, HttpUrl second) {
        return first.scheme().equals(second.scheme()) && first.host().equals(second.host()) && first.port() == second.port();
    }

    private NetExchange openSocket(String url, NetOptions options, Consumer<Result> complete) {
        NetExchange exchange = null;
        try {
            if (options.getTimeout() <= 0) throw new IllegalArgumentException("WebSocket requires a deadline");
            Request request = socketRequest(url, options);
            OkHttpClient socket = socketClient(options);
            exchange = new NetExchange(this, options, complete);
            registerExchange(exchange);
            exchange.start(socket, request);
            return exchange;
        } catch (Exception e) {
            if (exchange != null) exchange.finish(Result.failure(e));
            else complete.accept(Result.failure(e));
            return null;
        }
    }

    Request socketRequest(String url, NetOptions options) throws IOException {
        Request request = NetRequest.build(url, options, true);
        return withCookies(request, request.url(), request.header("Cookie"), options);
    }

    private void registerExchange(NetExchange exchange) throws InterruptedIOException {
        synchronized (lock) {
            if (closed) throw new InterruptedIOException("Network owner closed");
            sockets.add(exchange);
        }
    }

    public NetSocket connect(String url, String json, NetSocket.Listener listener) throws IOException {
        NetOptions options = NetOptions.from(json);
        if (listener == null) throw new IllegalArgumentException("WebSocket requires a listener");
        if (options.getTimeout() <= 0) throw new IllegalArgumentException("WebSocket requires a connect deadline");
        Request request = socketRequest(url, options);
        OkHttpClient client = socketClient(options).newBuilder().pingInterval(options.getPing(), TimeUnit.MILLISECONDS).build();
        NetSocket socket = new NetSocket(this, url, options, listener);
        synchronized (lock) {
            checkOpen();
            connections.add(socket);
        }
        try {
            socket.start(client, request);
            return socket;
        } catch (RuntimeException e) {
            socket.cancel();
            throw e;
        }
    }

    void remove(NetExchange exchange) {
        synchronized (lock) {
            sockets.remove(exchange);
        }
    }

    void remove(NetSocket socket) {
        synchronized (lock) {
            connections.remove(socket);
        }
    }

    private OkHttpClient socketClient(NetOptions options) {
        OkHttpClient strict = Strict.CLIENT;
        return builder(options)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .sslSocketFactory(strict.sslSocketFactory(), strict.x509TrustManager())
                .hostnameVerifier(strict.hostnameVerifier())
                .webSocketCloseTimeout(Math.min(options.getTimeout(), 1000), TimeUnit.MILLISECONDS)
                .build();
    }

    private void remove(Call call) {
        synchronized (lock) {
            calls.remove(call);
        }
    }

    @Override
    public void close() {
        List<Call> pending;
        List<StreamResult> streaming;
        List<NetExchange> active;
        List<NetSocket> connected;
        List<Net> sessions;
        List<NetMap<?, ?>> mappings;
        synchronized (lock) {
            if (closed) return;
            closed = true;
            lock.notifyAll();
            pending = new ArrayList<>(calls);
            streaming = new ArrayList<>(streams);
            active = new ArrayList<>(sockets);
            connected = new ArrayList<>(connections);
            sessions = new ArrayList<>(children);
            mappings = new ArrayList<>(maps);
            maps.clear();
            children.clear();
        }
        cache.close();
        for (NetMap<?, ?> mapping : mappings) mapping.cancel();
        if (cookies instanceof SessionCookies local) local.close();
        Throwable failure = null;
        failure = cancelCalls(pending, failure);
        failure = closeStreams(streaming, failure);
        failure = cancelExchanges(active, failure);
        failure = cancelSockets(connected, failure);
        failure = closeSessions(sessions, failure);
        if (parent != null) {
            synchronized (parent.lock) {
                parent.children.remove(this);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable cancelCalls(List<Call> pending, Throwable failure) {
        for (Call call : pending) {
            try {
                call.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        return failure;
    }

    private Throwable closeStreams(List<StreamResult> streaming, Throwable failure) {
        for (StreamResult stream : streaming) {
            try {
                stream.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        return failure;
    }

    private Throwable cancelExchanges(List<NetExchange> active, Throwable failure) {
        for (NetExchange exchange : active) {
            try {
                exchange.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        return failure;
    }

    private Throwable cancelSockets(List<NetSocket> connected, Throwable failure) {
        for (NetSocket socket : connected) {
            try {
                socket.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        return failure;
    }

    private Throwable closeSessions(List<Net> sessions, Throwable failure) {
        for (Net session : sessions) {
            try {
                session.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        return failure;
    }

    @Keep
    public final class StreamResult extends InputStream {
        public final int code;
        public final Map<String, Object> headers;
        public final String contentType;
        private final Call call;
        private final Response response;
        private final InputStream input;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean reading = new AtomicBoolean();
        private final AtomicBoolean bodyClosed = new AtomicBoolean();

        private StreamResult(Call call, Response response) {
            this.call = call;
            this.response = response;
            this.input = response.body().byteStream();
            this.code = response.code();
            this.headers = Result.headers(response.headers());
            this.contentType = response.header("Content-Type", "application/octet-stream");
        }

        @Override
        public int read() throws IOException {
            beginRead();
            try {
                return finishRead(input.read());
            } catch (IOException | RuntimeException | Error error) {
                failRead(error);
                throw error;
            }
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int capacity = bytes.length;
            if (offset < 0 || length < 0 || length > capacity - offset) throw new IndexOutOfBoundsException();
            if (finished.get()) throw new IOException("Response stream closed");
            if (length == 0) return 0;
            beginRead();
            try {
                return finishRead(input.read(bytes, offset, length));
            } catch (IOException | RuntimeException | Error error) {
                failRead(error);
                throw error;
            }
        }

        @Override
        public void close() {
            finish(true);
        }

        public CompletableFuture<Integer> readAsync(ByteBuffer bytes) {
            if (bytes == null || bytes.isReadOnly() || !bytes.hasRemaining() || bytes.remaining() > 1024 * 1024) throw new IllegalArgumentException("Read requires a writable range of 1..1048576 bytes");
            if (!reading.compareAndSet(false, true)) return failedFuture(new IllegalStateException("Stream read already pending"));
            if (finished.get()) {
                endRead();
                return failedFuture(new IOException("Response stream closed"));
            }
            CompletableFuture<Integer> result = new CompletableFuture<>();
            result.whenComplete((value, error) -> {
                if (result.isCancelled()) close();
            });
            try {
                client.dispatcher().executorService().execute(() -> readInto(bytes, result));
            } catch (RuntimeException error) {
                failRead(error);
                result.completeExceptionally(error);
            }
            return result;
        }

        private void readInto(ByteBuffer bytes, CompletableFuture<Integer> result) {
            try {
                if (finished.get()) throw new IOException("Response stream closed");
                int size = response.body().source().read(bytes);
                if (finished.get()) throw new IOException("Response stream closed");
                result.complete(finishRead(size));
            } catch (Throwable error) {
                failRead(error);
                result.completeExceptionally(error);
            }
        }

        private void beginRead() throws IOException {
            if (!reading.compareAndSet(false, true)) throw new IOException("Stream read already pending");
            if (finished.get()) {
                endRead();
                throw new IOException("Response stream closed");
            }
        }

        private void endRead() {
            reading.set(false);
            closeBody();
        }

        private int finishRead(int size) {
            if (size == -1) finish(false);
            endRead();
            return size;
        }

        private void failRead(Throwable error) {
            try {
                close();
            } catch (Throwable closing) {
                if (closing != error) error.addSuppressed(closing);
            } finally {
                endFailedRead(error);
            }
        }

        private void endFailedRead(Throwable error) {
            try {
                endRead();
            } catch (Throwable closing) {
                if (closing != error) error.addSuppressed(closing);
            }
        }

        private void closeBody() {
            if (finished.get() && !reading.get() && bodyClosed.compareAndSet(false, true)) response.close();
        }

        private void finish(boolean cancel) {
            if (!finished.compareAndSet(false, true)) return;
            Throwable failure = null;
            try {
                if (cancel) call.cancel();
            } catch (RuntimeException | Error error) {
                failure = error;
            }
            synchronized (lock) {
                streams.remove(this);
                calls.remove(call);
            }
            try {
                closeBody();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }
    }

    public static final class Result {
        private static final Gson GSON = new Gson();

        public final Object code;
        public final Map<String, Object> headers;
        public final byte[] body;
        public final String error;
        private final boolean http;

        private Result(Object code, Map<String, Object> headers, byte[] body, String error, boolean http) {
            this.code = code;
            this.headers = headers;
            this.body = body;
            this.error = error;
            this.http = http;
        }

        public static Result failure(Throwable error) {
            String message = "Network request failed";
            if (error instanceof SSLException) message = "TLS verification failed";
            else if (isTimeout(error)) message = "Timed out";
            else if (error instanceof InterruptedIOException || error instanceof InterruptedException || error instanceof CancellationException) message = "Canceled";
            else if (error instanceof UnknownHostException) message = "Host resolution failed";
            else if (error instanceof IllegalArgumentException) message = "Invalid request options";
            return failure(error.getClass().getSimpleName() + ": " + message);
        }

        private static Result failure(Call call, Throwable error) {
            return failure(call != null && call.isCanceled() && !isTimeout(error) ? new InterruptedIOException() : error);
        }

        private static boolean isTimeout(Throwable error) {
            return error instanceof SocketTimeoutException || error instanceof InterruptedIOException && "timeout".equals(error.getMessage());
        }

        static Result failure(String error) {
            return new Result("", new LinkedHashMap<>(), new byte[0], error, false);
        }

        private static Result read(Response response) throws IOException {
            return success(response.code(), response.headers(), response.body().bytes(), true);
        }

        static Result success(int code, Headers headers, byte[] body, boolean http) {
            return new Result(code, headers(headers), body, null, http);
        }

        private static Map<String, Object> headers(Headers headers) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : headers.toMultimap().entrySet()) values.put(entry.getKey(), entry.getValue().size() == 1 ? entry.getValue().get(0) : entry.getValue());
            return values;
        }

        static Result handshakeFailure(Response response) {
            return new Result(response.code(), headers(response.headers()), new byte[0],
                    "WebSocket handshake failed (HTTP " + response.code() + ")", false);
        }

        public String json(NetOptions options) {
            Map<String, Object> result = metadata();
            result.put("content", content(options));
            return GSON.toJson(result);
        }

        public Object[] parts(NetOptions options) {
            return new Object[]{GSON.toJson(metadata()), content(options)};
        }

        private Map<String, Object> metadata() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", code);
            result.put("headers", headers);
            if (error != null) result.put("error", error);
            return result;
        }

        private Object content(NetOptions options) {
            if (error != null) return "";
            return switch (options.getBuffer()) {
                case 0 -> text(options);
                case 1, 3 -> body;
                case 2 -> ByteString.of(body).base64();
                default -> "";
            };
        }

        public String text() {
            return text(new NetOptions());
        }

        private String text(NetOptions options) {
            if (!http) return new String(body, Charset.forName(options.getCharset()));
            try (ResponseBody response = ResponseBody.create(body, contentType())) {
                return response.string();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private MediaType contentType() {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("Content-Type")) continue;
                Object value = entry.getValue();
                if (value instanceof List<?> values) value = values.get(values.size() - 1);
                return MediaType.parse(String.valueOf(value));
            }
            return null;
        }
    }

    private static final class SessionCookies implements Cookies {
        private final List<Cookie> values = new ArrayList<>();
        private boolean closed;

        @Override
        public synchronized String getCookie(String url) {
            checkOpen();
            HttpUrl target = HttpUrl.get(url);
            values.removeIf(cookie -> cookie.expiresAt() <= System.currentTimeMillis());
            return values.stream().filter(cookie -> cookie.matches(target))
                    .sorted(Comparator.comparingInt((Cookie cookie) -> cookie.path().length()).reversed())
                    .map(cookie -> cookie.name() + "=" + cookie.value()).collect(Collectors.joining("; "));
        }

        @Override
        public synchronized boolean setCookie(String url, String value) {
            checkOpen();
            Cookie cookie = Cookie.parse(HttpUrl.get(url), value);
            if (cookie == null) return false;
            for (int i = 0; i < values.size(); i++) {
                Cookie previous = values.get(i);
                if (!previous.name().equals(cookie.name()) || !previous.domain().equals(cookie.domain()) || !previous.path().equals(cookie.path())) continue;
                if (cookie.expiresAt() <= System.currentTimeMillis()) values.remove(i);
                else values.set(i, cookie);
                return true;
            }
            if (cookie.expiresAt() > System.currentTimeMillis()) values.add(cookie);
            return true;
        }

        private synchronized void close() {
            closed = true;
            values.clear();
        }

        private void checkOpen() {
            if (closed) throw new IllegalStateException("Network owner closed");
        }
    }

    private static class Strict {
        private static final OkHttpClient CLIENT = new OkHttpClient();
    }
}
