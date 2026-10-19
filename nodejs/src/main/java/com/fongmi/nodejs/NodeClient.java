package com.fongmi.nodejs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.runtime.NodeRuntimeApi;
import com.github.catvod.runtime.NodeRuntimeApi.Callback;
import com.github.catvod.runtime.NodeRuntimeApi.LoadedConfig;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class NodeClient implements NodeRuntimeApi.Client {

    private static final String SUCCESS_RESPONSE = "{\"success\":true,\"code\":0}";
    private static final String FAILURE_RESPONSE = "{\"success\":false,\"code\":-1}";
    private static final long START_TIMEOUT_SECONDS = 45;
    private static final long STOP_TIMEOUT_SECONDS = 5;
    private static final long BIND_TIMEOUT_SECONDS = 1;
    private static final long FAST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);
    private static final long REQUEST_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(45);
    private static final long PLAY_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60);
    private static final long MAX_CONFIG_RESPONSE_BYTES = 4L * 1024 * 1024;
    private static final long MAX_JSON_RESPONSE_BYTES = 16L * 1024 * 1024;
    private static final long CIRCUIT_OPEN_MILLIS = TimeUnit.SECONDS.toMillis(15);
    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Pattern LEGACY_LOOPBACK = Pattern.compile("http://(?:127\\.0\\.0\\.1|localhost):9978(?=[/?#|$\\s\"']|$)");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = client(REQUEST_TIMEOUT_MILLIS);
    private static final OkHttpClient FAST_CLIENT = client(FAST_TIMEOUT_MILLIS);
    private static final OkHttpClient PLAY_CLIENT = client(PLAY_TIMEOUT_MILLIS);
    private static final OkHttpClient HEALTH_CLIENT = client(TimeUnit.SECONDS.toMillis(5)).newBuilder().callTimeout(5, TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();

    private final Callback callback;
    private final Context context;
    private final Object profileLock = new Object();
    private final Set<Call> calls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, String> initializations = new LinkedHashMap<>();
    private final AtomicInteger pendingClears = new AtomicInteger();
    private volatile AtomicBoolean loadCancellation;
    private volatile long session;
    private volatile CountDownLatch startLatch;
    private volatile Exception startError;
    private volatile String address;
    private volatile String token;
    private volatile NodeBundle bundle;
    private volatile long generation;
    private volatile long startedAt;
    private volatile long lastDuration;
    private volatile long circuitUntil;
    private volatile int consecutiveFailures;
    private volatile int restartCount;
    private volatile int nodePid;
    private volatile String nodeVersion = "";
    private volatile String nodeArch = "";
    private volatile String nativePath = "";
    private volatile String lastEndpoint = "";
    private volatile String lastError = "";
    private volatile NodeConnection connection;
    private NodeBridge bridge;

    public NodeClient(Context context, Callback callback) {
        Context application = context.getApplicationContext();
        this.context = application == null ? context : application;
        this.callback = callback;
    }

    static boolean isInterrupted(Throwable error) {
        boolean timedOut = hasCause(error, SocketTimeoutException.class);
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException) return true;
            if (current instanceof InterruptedIOException && !timedOut) return true;
        }
        return Thread.currentThread().isInterrupted();
    }

    static String rewriteLoopback(String value, String address) {
        if (value == null || address == null || address.isEmpty()) return value;
        return LEGACY_LOOPBACK.matcher(value).replaceAll(Matcher.quoteReplacement(address));
    }

    static String readResponse(ResponseBody body, long limit) throws IOException {
        long length = body.contentLength();
        if (length > limit) throw new NodeResponseException("Node response is too large");
        try (InputStream input = body.byteStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyResponse(input, output, limit);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void copyResponse(InputStream input, ByteArrayOutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > limit) throw new NodeResponseException("Node response is too large");
            output.write(buffer, 0, count);
        }
    }

    private static OkHttpClient client(long timeout) {
        return new OkHttpClient.Builder().proxy(Proxy.NO_PROXY).connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    private static OkHttpClient clientFor(String route) {
        int query = route.indexOf('?');
        String path = query < 0 ? route : route.substring(0, query);
        if (path.endsWith("/init") || path.endsWith("/support") || path.endsWith("/action")) return FAST_CLIENT;
        if (path.endsWith("/play") || path.endsWith("/proxy")) return PLAY_CLIENT;
        return CLIENT;
    }

    static boolean isTransportFailure(Exception error) {
        return !(error instanceof NodeHttpException) && !(error instanceof NodeResponseException) && hasCause(error, IOException.class);
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) if (type.isInstance(current)) return true;
        return false;
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static boolean isProxyControl(String key) {
        return "siteKey".equalsIgnoreCase(key) || "path".equalsIgnoreCase(key) || "remote-addr".equalsIgnoreCase(key) || "range".equalsIgnoreCase(key) || "user-agent".equalsIgnoreCase(key) || "referer".equalsIgnoreCase(key) || "origin".equalsIgnoreCase(key);
    }

    private static void copyProxyHeader(Map<String, String> params, Request.Builder request, String name) {
        for (Map.Entry<String, String> entry : params.entrySet()) if (name.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) request.header(name, entry.getValue());
    }

    public synchronized String load(String source) throws Exception {
        checkLoadCanceled();
        RuntimeModule.NODEJS.require();
        session++;
        cancelRequests();
        initializations.clear();
        loadCancellation = new AtomicBoolean();
        try {
            NodeBundle prepared = NodeBundle.prepare(context, source);
            checkLoadCanceled();
            if (!canReuse(prepared)) start(prepared);
            String result = NodeConfigMapper.transform(get("/config"));
            checkLoadCanceled();
            recordSuccess(session, "/config", 0);
            return result;
        } catch (Exception | Error error) {
            clearPreservingFailure(error);
            throw error;
        } finally {
            loadCancellation = null;
        }
    }

    public synchronized LoadedConfig loadConfig(String source) throws Exception {
        return new LoadedConfig(load(source), session);
    }

    public synchronized void check() throws Exception {
        checkLoadCanceled();
        RuntimeModule.NODEJS.require();
        if (bundle != null) {
            checkHealth();
            return;
        }
        if (connection != null || !TextUtils.isEmpty(address)) throw new IOException("Node runtime is already starting");
        loadCancellation = new AtomicBoolean();
        Throwable failure = null;
        try {
            NodeBundle probe = NodeBundle.prepareHealth(context);
            start(probe);
            checkHealth();
        } catch (Exception | Error error) {
            failure = error;
            throw error;
        } finally {
            clearPreservingFailure(failure);
        }
    }

    private void clearPreservingFailure(Throwable failure) {
        try {
            clear();
        } catch (RuntimeException | Error error) {
            if (failure == null) throw error;
            if (failure != error) failure.addSuppressed(error);
        }
    }

    private void checkHealth() throws IOException {
        RequestState state = snapshot();
        Request request = healthRequest(state);
        Call call = registerCall(state, HEALTH_CLIENT, request);
        try (Response response = call.execute()) {
            verifyHealth(response);
            checkCurrent(state);
        } catch (RuntimeException e) {
            throw new IOException("Invalid Node health response", e);
        } finally {
            calls.remove(call);
        }
    }

    private Request healthRequest(RequestState state) throws IOException {
        if (TextUtils.isEmpty(state.address())) throw new IOException("Node runtime is not running");
        HttpUrl address = HttpUrl.parse(state.address());
        if (address == null || !"http".equals(address.scheme()) || !"127.0.0.1".equals(address.host())) throw new IOException("Invalid Node health address");
        return new Request.Builder().url(address.newBuilder().encodedPath("/__catvod_health").query(null).fragment(null).build()).header("X-CatVod-Token", token).get().build();
    }

    private void verifyHealth(Response response) throws IOException {
        ResponseBody body = response.body();
        if (!response.isSuccessful()) throw new NodeHttpException(response.code());
        if (body == null) throw new IOException("Empty Node health response");
        JsonObject proof = JsonParser.parseString(readResponse(body, 1024)).getAsJsonObject();
        if (proof.get("value").getAsInt() != 42 || nodePid <= 0 || proof.get("pid").getAsInt() != nodePid || nodeVersion.isEmpty() || !nodeVersion.equals(Json.safeString(proof, "version"))) throw new IOException("Node runtime health check failed");
    }

    private boolean canReuse(NodeBundle prepared) {
        return bundle != null && bundle.isSameVersion(prepared) && bridge != null && bridge.isAlive() && !TextUtils.isEmpty(address);
    }

    private void onServiceLost(String message) {
        address = null;
        lastError = message;
        if (startError == null) startError = new IOException(message);
        CountDownLatch latch = startLatch;
        if (latch != null) latch.countDown();
    }

    public void clear() {
        pendingClears.incrementAndGet();
        Throwable failure = null;
        try {
            failure = cancelActiveRequests();
            try {
                clearRuntime();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        } finally {
            pendingClears.decrementAndGet();
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable cancelActiveRequests() {
        Throwable failure = null;
        try {
            cancelLoad();
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        return cancelRequests(failure);
    }

    private Throwable cancelRequests(Throwable failure) {
        try {
            cancelRequests();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private synchronized void clearRuntime() {
        session++;
        Throwable failure = cancelRequests(null);
        initializations.clear();
        failure = stopRuntime(failure);
        resetRuntimeState();
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable stopRuntime(Throwable failure) {
        failure = stopService(failure);
        return stopBridge(failure);
    }

    private Throwable stopService(Throwable failure) {
        try {
            stopService();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private Throwable stopBridge(Throwable failure) {
        try {
            if (bridge != null) {
                bridge.setToken(null);
                bridge.stop();
            }
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private void resetRuntimeState() {
        bridge = null;
        bundle = null;
        address = null;
        token = null;
        loadCancellation = null;
        startLatch = null;
        startError = null;
        generation++;
        startedAt = 0;
        nodePid = 0;
        nodeVersion = "";
        nodeArch = "";
        nativePath = "";
        consecutiveFailures = 0;
        restartCount = 0;
        circuitUntil = 0;
        lastEndpoint = "";
        lastDuration = 0;
        lastError = "";
    }

    public synchronized void accept(LoadedConfig loaded) throws IOException {
        if (loaded == null || loaded.session() != session) throw new IOException("Node config load was superseded");
        NodeBundle current = bundle;
        if (current != null) current.accept();
    }

    public synchronized void prune(LoadedConfig loaded, Iterable<String> sources, String currentSource) throws IOException {
        if (loaded != null && loaded.session() != session) throw new IOException("Node config load was superseded");
        NodeBundle.prune(context, sources, currentSource);
    }

    public synchronized boolean fail(LoadedConfig loaded) {
        if (loaded == null || loaded.session() != session) return false;
        clear();
        return true;
    }

    public void cancelLoad() {
        Throwable failure = cancelLoadRequests();
        try {
            NodeBundle.cancelLoad();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable cancelLoadRequests() {
        Throwable failure = null;
        AtomicBoolean cancellation = loadCancellation;
        if (cancellation != null) {
            cancellation.set(true);
            for (Call call : calls) {
                if (call.request().tag(AtomicBoolean.class) == cancellation) {
                    try {
                        call.cancel();
                    } catch (RuntimeException | Error error) {
                        if (failure == null) failure = error;
                        else if (failure != error) failure.addSuppressed(error);
                    }
                }
            }
        }
        return failure;
    }

    private void checkLoadCanceled() throws InterruptedIOException {
        AtomicBoolean cancellation = loadCancellation;
        if (pendingClears.get() > 0 || Thread.currentThread().isInterrupted() || cancellation != null && cancellation.get()) throw new InterruptedIOException("Node config load canceled");
    }

    private void start(NodeBundle prepared) throws Exception {
        RuntimeModule.NODEJS.require();
        if (RuntimeModule.NODEJS.isRestartRequired()) throw new IOException("Node.js runtime changed; restart the app");
        checkLoadCanceled();
        cancelRequests();
        stopService();
        generation++;
        token = UUID.randomUUID().toString();
        openBridge();
        bindProcess(prepared);
        awaitStarted();
    }

    private void openBridge() throws IOException {
        if (bridge == null || !bridge.isAlive()) {
            bridge = new NodeBridge(this::onMessage);
            bridge.setToken(token);
            bridge.open();
        } else {
            bridge.setToken(token);
        }
    }

    private void bindProcess(NodeBundle prepared) throws IOException {
        bundle = prepared;
        address = null;
        startError = null;
        startLatch = new CountDownLatch(1);
        File data = prepared.getData();
        Intent intent = new Intent().setClassName(context.getPackageName(), NodeRuntimeApi.SERVICE_CLASS).setAction(NodeService.ACTION_START)
                .putExtra(NodeService.EXTRA_INDEX, prepared.getIndex().getAbsolutePath())
                .putExtra(NodeService.EXTRA_CONFIG, prepared.getConfig().getAbsolutePath())
                .putExtra(NodeService.EXTRA_DATA, data.getAbsolutePath())
                .putExtra(NodeService.EXTRA_BRIDGE_PORT, bridge.getListeningPort())
                .putExtra(NodeService.EXTRA_TOKEN, token);
        NodeConnection next = new NodeConnection(generation);
        connection = next;
        next.bound = context.bindService(intent, next, Context.BIND_AUTO_CREATE);
        if (!next.bound) throw new IOException("Unable to bind Node service");
    }

    private void awaitStarted() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_TIMEOUT_SECONDS);
        while (!startLatch.await(100, TimeUnit.MILLISECONDS)) {
            checkLoadCanceled();
            if (System.nanoTime() >= deadline) throw new IOException("Node startup timed out");
        }
        checkLoadCanceled();
        Exception error = startError;
        if (error != null) throw error;
        if (TextUtils.isEmpty(address)) throw new IOException("Node server did not report an address");
        startedAt = System.currentTimeMillis();
        SpiderDebug.log("NodeRuntime", "Started %s", diagnostics());
    }

    private void stopService() {
        NodeConnection current = connection;
        if (current != null) {
            synchronized (current) {
                connection = null;
            }
            if (current.stop()) RuntimeModule.NODEJS.recordStopped(current);
            return;
        }
        try {
            context.stopService(new Intent().setClassName(context.getPackageName(), NodeRuntimeApi.SERVICE_CLASS));
        } catch (Exception ignored) {
        }
    }

    String onMessage(String text) {
        try {
            JsonObject request = Json.parse(text).getAsJsonObject();
            String action = Json.safeString(request, "action");
            return dispatchMessage(action, messageOptions(request, action));
        } catch (Exception e) {
            SpiderDebug.log(e);
            return FAILURE_RESPONSE;
        }
    }

    private JsonObject messageOptions(JsonObject request, String action) {
        JsonObject opt = request.has("opt") && request.get("opt").isJsonObject() ? request.getAsJsonObject("opt") : new JsonObject();
        return switch (action) {
            case "openWeb", "openInternalWebview", "getCookie", "setCookie" -> normalizeWeb(opt);
            default -> opt;
        };
    }

    private String dispatchMessage(String action, JsonObject opt) {
        return switch (action) {
            case "serverStarted" -> onStarted(opt);
            case "nodeError" -> onError(opt);
            case "diagnostics" -> diagnostics();
            case "queryProfile" -> readProfile().toString();
            case "saveProfile" -> saveProfile(opt);
            default -> callback == null ? FAILURE_RESPONSE : callback.onMessage(action, opt);
        };
    }

    private String onStarted(JsonObject opt) {
        NodeConnection current = connection;
        if (current == null || !tokenMatches(opt)) return SUCCESS_RESPONSE;
        synchronized (current) {
            if (connection != current || current.stopped.getCount() == 0 || !tokenMatches(opt)) return SUCCESS_RESPONSE;
            String value = Json.safeString(opt, "address");
            if (value.startsWith("http://127.0.0.1:") || value.startsWith("http://localhost:")) {
                String path = Json.safeString(opt, "runtimePath");
                try {
                    if (path.isEmpty()) throw new IOException("Node service did not report its runtime library");
                    RuntimeModule.NODEJS.recordLoaded(new File(path), current);
                } catch (IOException e) {
                    opt.addProperty("message", message(e));
                    return onError(opt);
                }
                address = value.replace("http://localhost:", "http://127.0.0.1:");
                nodePid = opt.has("pid") && opt.get("pid").isJsonPrimitive() ? opt.get("pid").getAsInt() : 0;
                nodeVersion = Json.safeString(opt, "version");
                nodeArch = Json.safeString(opt, "arch");
                nativePath = path;
                CountDownLatch latch = startLatch;
                if (latch != null) latch.countDown();
            }
        }
        return SUCCESS_RESPONSE;
    }

    private String onError(JsonObject opt) {
        if (!tokenMatches(opt)) return SUCCESS_RESPONSE;
        String message = Json.safeString(opt, "message");
        startError = RuntimeModule.NODEJS.id.equals(Json.safeString(opt, "missingModule")) ? new RuntimeModule.MissingException(RuntimeModule.NODEJS) : TextUtils.isEmpty(message) ? null : new IOException(message);
        CountDownLatch latch = startLatch;
        if (latch != null) latch.countDown();
        return SUCCESS_RESPONSE;
    }

    private String normalizeInternalUrl(String value, String address) {
        if (value == null || address == null || value.isEmpty() || address.isEmpty()) return value;
        HttpUrl target = HttpUrl.parse(value);
        HttpUrl runtime = HttpUrl.parse(address);
        if (target == null || runtime == null || !"http".equals(target.scheme()) || target.port() != runtime.port()) return value;
        return target.newBuilder().scheme(runtime.scheme()).host(runtime.host()).port(runtime.port()).build().toString();
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    JsonObject normalizeWeb(JsonObject options) {
        String address = this.address;
        JsonObject result = options.deepCopy();
        normalizeUrl(result, address);
        JsonElement cookie = result.get("cookie");
        if (cookie != null && cookie.isJsonArray()) for (JsonElement group : cookie.getAsJsonArray()) if (group.isJsonObject()) normalizeUrl(group.getAsJsonObject(), address);
        return result;
    }

    private void normalizeUrl(JsonObject options, String address) {
        JsonElement url = options.get("url");
        if (url != null && url.isJsonPrimitive() && url.getAsJsonPrimitive().isString()) options.addProperty("url", normalizeInternalUrl(url.getAsString(), address));
    }

    public void setPlayInfo(String flag, String id) {
        if (callback != null) callback.onPlayInfo(flag, id);
    }

    public File getProfileFile() {
        NodeBundle current = bundle;
        return current == null ? null : new File(current.getRoot().getParentFile(), "profile.json");
    }

    private JsonElement readProfile() {
        synchronized (profileLock) {
            File file = getProfileFile();
            if (file == null || !file.isFile()) return new JsonObject();
            try {
                return JsonParser.parseString(Path.read(file));
            } catch (Exception e) {
                return new JsonObject();
            }
        }
    }

    private String saveProfile(JsonObject profile) {
        synchronized (profileLock) {
            File file = getProfileFile();
            if (file == null) return FAILURE_RESPONSE;
            try {
                Path.writeAtomically(file, profile.toString().getBytes(StandardCharsets.UTF_8));
                return SUCCESS_RESPONSE;
            } catch (IOException e) {
                SpiderDebug.log(e);
                return FAILURE_RESPONSE;
            }
        }
    }

    private boolean tokenMatches(JsonObject opt) {
        return !TextUtils.isEmpty(token) && token.equals(Json.safeString(opt, "token"));
    }

    public String post(String api, JsonObject body) throws Exception {
        return request(api, state -> executePost(state, api, body));
    }

    private <T> T request(String endpoint, RequestOperation<T> operation) throws Exception {
        RuntimeModule.NODEJS.require();
        checkCircuit();
        RequestState state = snapshot();
        long start = System.currentTimeMillis();
        try {
            T result = operation.execute(state);
            recordSuccess(state.session(), endpoint, System.currentTimeMillis() - start);
            return result;
        } catch (Exception first) {
            checkSession(state);
            if (!isTransportFailure(first) || isInterrupted(first)) {
                recordApplicationError(state.session(), endpoint, first, System.currentTimeMillis() - start);
                throw first;
            }
            if (hasCause(first, SocketTimeoutException.class)) {
                invalidate(state, first);
                recordTransportFailure(state.session(), endpoint, first, System.currentTimeMillis() - start);
                throw first;
            }
            return recoverRequest(endpoint, operation, state, start, first);
        }
    }

    private <T> T recoverRequest(String endpoint, RequestOperation<T> operation, RequestState state, long start, Exception first) throws Exception {
        try {
            state = recover(state);
            T result = operation.execute(state);
            recordSuccess(state.session(), endpoint, System.currentTimeMillis() - start);
            return result;
        } catch (Exception second) {
            checkSession(state);
            second.addSuppressed(first);
            if (isInterrupted(second) || !isTransportFailure(second)) recordApplicationError(state.session(), endpoint, second, System.currentTimeMillis() - start);
            else recordTransportFailure(state.session(), endpoint, second, System.currentTimeMillis() - start);
            throw second;
        }
    }

    public synchronized void initialize(String api, JsonObject body) throws Exception {
        post(NodeRoute.append(api, "/init"), body);
        initializations.put(api, body.toString());
    }

    private String executePost(RequestState state, String api, JsonObject body) throws Exception {
        String route = NodeRoute.path(api);
        String base = state.address();
        if (TextUtils.isEmpty(base)) throw new IOException("Node runtime is not running");
        Request request = new Request.Builder().url(base + route).post(RequestBody.create(body.toString().getBytes(StandardCharsets.UTF_8), JSON)).build();
        Call call = registerCall(state, clientFor(route), request);
        try (Response response = call.execute()) {
            return readResponse(response, state, MAX_JSON_RESPONSE_BYTES);
        } finally {
            calls.remove(call);
        }
    }

    public String postOptional(String api, JsonObject body) throws Exception {
        String result = postOrNull(api, body);
        return result == null ? "" : result;
    }

    @Nullable
    String postOrNull(String api, JsonObject body) throws Exception {
        try {
            return post(api, body);
        } catch (NodeHttpException e) {
            if (e.code == 404) return null;
            throw e;
        }
    }

    public Object[] proxy(String api, Map<String, String> params) throws Exception {
        return request(String.valueOf(params.getOrDefault("path", "/proxy")), state -> executeProxy(state, api, params));
    }

    private Object[] executeProxy(RequestState state, String api, Map<String, String> params) throws Exception {
        String base = state.address();
        if (TextUtils.isEmpty(base)) throw new IOException("Node runtime is not running");
        Call call = registerCall(state, PLAY_CLIENT, proxyRequest(api, params, base));
        Response response = null;
        try {
            response = call.execute();
            checkCurrent(state);
        } catch (Exception e) {
            if (response != null) response.close();
            calls.remove(call);
            throw e;
        }
        ResponseBody body = response.body();
        if (body == null) {
            calls.remove(call);
            response.close();
            throw new IOException("Empty Node proxy response");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.headers().names()) headers.put(name, response.header(name, ""));
        String type = response.header("Content-Type", "application/octet-stream");
        return new Object[]{response.code(), type, new ResponseStream(body.byteStream(), response, call), headers};
    }

    private Request proxyRequest(String api, Map<String, String> params, String base) throws IOException {
        String path = params.get("path");
        String target = TextUtils.isEmpty(path) ? NodeRoute.append(api, "/proxy") : path.startsWith("/") ? "node:" + path : NodeRoute.append(api, path);
        HttpUrl parsed = HttpUrl.parse(base + NodeRoute.path(target));
        if (parsed == null) throw new IOException("Invalid Node proxy route");
        HttpUrl.Builder url = parsed.newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (isProxyControl(key) || entry.getValue() == null) continue;
            url.addQueryParameter(key, entry.getValue());
        }
        Request.Builder request = new Request.Builder().url(url.build());
        copyProxyHeader(params, request, "range");
        copyProxyHeader(params, request, "user-agent");
        copyProxyHeader(params, request, "referer");
        copyProxyHeader(params, request, "origin");
        return request.build();
    }

    private String get(String route) throws IOException {
        RequestState state = snapshot();
        String base = state.address();
        if (TextUtils.isEmpty(base)) throw new IOException("Node runtime is not running");
        Request request = new Request.Builder().url(base + route).get().build();
        Call call = registerCall(state, CLIENT, request);
        try (Response response = call.execute()) {
            return readResponse(response, state, MAX_CONFIG_RESPONSE_BYTES);
        } finally {
            calls.remove(call);
        }
    }

    private String readResponse(Response response, RequestState state, long limit) throws IOException {
        ResponseBody body = response.body();
        if (!response.isSuccessful()) throw new NodeHttpException(response.code());
        if (body == null) throw new IOException("Empty Node response");
        String result = rewriteLoopback(readResponse(body, limit), state.address());
        checkCurrent(state);
        return result;
    }

    private synchronized RequestState recover(RequestState observed) throws Exception {
        RuntimeModule.NODEJS.require();
        checkSession(observed);
        checkCircuit();
        if (generation != observed.generation() && !TextUtils.isEmpty(address)) return snapshot();
        NodeBundle current = bundle;
        if (current == null) throw new IOException("Node runtime has no bundle to restart");
        try {
            start(current);
            replayInitializations();
        } catch (Exception e) {
            invalidate(snapshot(), e);
            throw e;
        }
        restartCount++;
        return snapshot();
    }

    private void replayInitializations() throws Exception {
        for (Map.Entry<String, String> entry : initializations.entrySet()) {
            JsonObject body = JsonParser.parseString(entry.getValue()).getAsJsonObject();
            executePost(snapshot(), NodeRoute.append(entry.getKey(), "/init"), body);
        }
    }

    private synchronized void invalidate(RequestState observed, Exception error) {
        if (session != observed.session() || generation != observed.generation()) return;
        cancelRequests();
        stopService();
        address = null;
        generation++;
        lastError = message(error);
    }

    private synchronized RequestState snapshot() {
        return new RequestState(session, generation, address, loadCancellation);
    }

    private synchronized void checkSession(RequestState state) throws InterruptedIOException {
        if (session != state.session()) throw new InterruptedIOException("Node config load was superseded");
        if (pendingClears.get() > 0 || Thread.currentThread().isInterrupted() || state.cancellation() != null && state.cancellation().get()) throw new InterruptedIOException("Node request canceled");
    }

    private synchronized void checkCurrent(RequestState state) throws InterruptedIOException {
        checkSession(state);
        if (generation != state.generation()) throw new InterruptedIOException("Node runtime was restarted");
    }

    private synchronized Call registerCall(RequestState state, OkHttpClient client, Request request) throws IOException {
        checkCurrent(state);
        Call call = client.newCall(request.newBuilder().tag(AtomicBoolean.class, state.cancellation()).build());
        calls.add(call);
        try {
            checkCurrent(state);
            return call;
        } catch (IOException e) {
            call.cancel();
            calls.remove(call);
            throw e;
        }
    }

    private synchronized void checkCircuit() throws IOException {
        long now = System.currentTimeMillis();
        if (circuitUntil > now) throw new IOException("Node runtime circuit is open for " + (circuitUntil - now) + " ms");
        if (circuitUntil != 0) {
            circuitUntil = 0;
            consecutiveFailures = 0;
        }
    }

    private synchronized void recordSuccess(long observedSession, String endpoint, long duration) {
        if (session != observedSession) return;
        consecutiveFailures = 0;
        circuitUntil = 0;
        lastEndpoint = endpoint;
        lastDuration = duration;
        lastError = "";
    }

    private synchronized void recordApplicationError(long observedSession, String endpoint, Exception error, long duration) {
        if (session != observedSession) return;
        lastEndpoint = endpoint;
        lastDuration = duration;
        lastError = message(error);
    }

    private synchronized void recordTransportFailure(long observedSession, String endpoint, Exception error, long duration) {
        if (session != observedSession) return;
        lastEndpoint = endpoint;
        lastDuration = duration;
        lastError = message(error);
        consecutiveFailures++;
        if (consecutiveFailures >= CIRCUIT_FAILURE_THRESHOLD) circuitUntil = System.currentTimeMillis() + CIRCUIT_OPEN_MILLIS;
        SpiderDebug.log("NodeRuntime", "Request failed %s", diagnostics());
    }

    public String diagnostics() {
        JsonObject result = new JsonObject();
        long now = System.currentTimeMillis();
        result.addProperty("state", circuitUntil > now ? "circuit-open" : TextUtils.isEmpty(address) ? "stopped" : "running");
        result.addProperty("address", TextUtils.isEmpty(address) ? "" : address);
        result.addProperty("pid", nodePid);
        result.addProperty("node", nodeVersion);
        result.addProperty("arch", nodeArch);
        result.addProperty("nativePath", nativePath);
        result.addProperty("uptimeMs", startedAt == 0 ? 0 : Math.max(0, now - startedAt));
        result.addProperty("restarts", restartCount);
        result.addProperty("failures", consecutiveFailures);
        result.addProperty("circuitRemainingMs", Math.max(0, circuitUntil - now));
        result.addProperty("lastEndpoint", lastEndpoint);
        result.addProperty("lastDurationMs", lastDuration);
        result.addProperty("lastError", lastError);
        NodeBundle current = bundle;
        if (current != null) {
            result.addProperty("indexMd5", current.getIndexMd5());
            result.addProperty("configMd5", current.getConfigMd5());
            result.addProperty("signingKey", current.getSigningKey());
            result.addProperty("pending", current.isPending());
        }
        return result.toString();
    }

    private void cancelRequests() {
        Throwable failure = null;
        for (Call call : calls) {
            try {
                call.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        calls.clear();
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    @Override
    public com.github.catvod.crawler.Spider spider(String api) {
        return new NodeSpider(this, api);
    }

    private interface RequestOperation<T> {
        T execute(RequestState state) throws Exception;
    }

    private static class NodeHttpException extends IOException {
        private final int code;

        private NodeHttpException(int code) {
            super("Node HTTP " + code);
            this.code = code;
        }
    }

    private static class NodeResponseException extends IOException {
        private NodeResponseException(String message) {
            super(message);
        }
    }

    private record RequestState(long session, long generation, String address, AtomicBoolean cancellation) {
    }

    private final class ResponseStream extends FilterInputStream {

        private final Response response;
        private final Call call;

        private ResponseStream(InputStream input, Response response, Call call) {
            super(input);
            this.response = response;
            this.call = call;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                response.close();
                calls.remove(call);
            }
        }
    }

    private final class NodeConnection implements ServiceConnection, IBinder.DeathRecipient {

        private final CountDownLatch connected = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
        private final long session;
        private volatile IBinder binder;
        private volatile boolean bound;

        private NodeConnection(long session) {
            this.session = session;
        }

        @Override
        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            try {
                service.linkToDeath(this, 0);
            } catch (RemoteException e) {
                stopped.countDown();
                notifyLost("Node service died while binding");
                RuntimeModule.NODEJS.recordStopped(this);
            } finally {
                connected.countDown();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected.countDown();
            stopped.countDown();
            notifyLost("Node service disconnected");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            connected.countDown();
            stopped.countDown();
            notifyLost("Node service binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            connected.countDown();
            stopped.countDown();
            notifyLost("Node service returned no binder");
        }

        @Override
        public synchronized void binderDied() {
            stopped.countDown();
            notifyLost("Node service process died");
            RuntimeModule.NODEJS.recordStopped(this);
        }

        private void notifyLost(String message) {
            if (connection != this || generation != session) return;
            unbind();
            onServiceLost(message);
        }

        private synchronized void unbind() {
            if (!bound) return;
            bound = false;
            try {
                context.unbindService(this);
            } catch (Exception ignored) {
            }
        }

        private boolean stop() {
            awaitConnection();
            IBinder target = binder;
            unbind();
            requestStop();
            awaitStopped(target);
            unlinkStoppedBinder(target);
            binder = null;
            return target != null && !target.isBinderAlive();
        }

        private void requestStop() {
            try {
                context.stopService(new Intent().setClassName(context.getPackageName(), NodeRuntimeApi.SERVICE_CLASS));
            } catch (Exception ignored) {
            }
        }

        private void unlinkStoppedBinder(IBinder target) {
            if (target != null && !target.isBinderAlive()) {
                try {
                    target.unlinkToDeath(this, 0);
                } catch (Exception ignored) {
                }
            }
        }

        private void awaitConnection() {
            if (bound && binder == null) {
                try {
                    connected.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void awaitStopped(IBinder target) {
            if (target != null && target.isBinderAlive()) {
                try {
                    if (!stopped.await(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) SpiderDebug.log("NodeRuntime", "Timed out waiting for Node service process to stop");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
