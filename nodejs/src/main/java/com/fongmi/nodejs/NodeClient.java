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
import com.github.catvod.utils.Json;
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

public final class NodeClient {

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

    private final Callback callback;
    private final Context context;
    private final Set<Call> calls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, String> initializations = new LinkedHashMap<>();
    private volatile AtomicBoolean loadCancellation;
    private volatile long session;
    private volatile CountDownLatch startLatch;
    private volatile String startError;
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
    private volatile String lastEndpoint = "";
    private volatile String lastError = "";
    private volatile NodeConnection connection;
    private NodeBridge bridge;

    public NodeClient(Context context, Callback callback) {
        Context application = context.getApplicationContext();
        this.context = application == null ? context : application;
        this.callback = callback;
    }

    public synchronized String load(String source) throws Exception {
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
        } catch (Exception e) {
            clear();
            throw e;
        } finally {
            loadCancellation = null;
        }
    }

    public synchronized LoadedConfig loadConfig(String source) throws Exception {
        return new LoadedConfig(load(source), session);
    }

    static boolean isInterrupted(Throwable error) {
        boolean timedOut = hasCause(error, SocketTimeoutException.class);
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException) return true;
            if (current instanceof InterruptedIOException && !timedOut) return true;
        }
        return Thread.currentThread().isInterrupted();
    }

    private boolean canReuse(NodeBundle prepared) {
        return bundle != null && bundle.isSameVersion(prepared) && bridge != null && bridge.isAlive() && !TextUtils.isEmpty(address);
    }

    private void onServiceLost(String message) {
        address = null;
        lastError = message;
        if (TextUtils.isEmpty(startError)) startError = message;
        CountDownLatch latch = startLatch;
        if (latch != null) latch.countDown();
    }

    public synchronized void clear() {
        session++;
        cancelRequests();
        initializations.clear();
        stopService();
        if (bridge != null) {
            bridge.setToken(null);
            bridge.stop();
        }
        bridge = null;
        bundle = null;
        address = null;
        token = null;
        startLatch = null;
        startError = null;
        generation++;
        startedAt = 0;
        nodePid = 0;
        nodeVersion = "";
        nodeArch = "";
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
        AtomicBoolean cancellation = loadCancellation;
        if (cancellation != null) {
            cancellation.set(true);
            for (Call call : calls) if (call.request().tag(AtomicBoolean.class) == cancellation) call.cancel();
        }
        NodeBundle.cancelLoad();
    }

    private void checkLoadCanceled() throws InterruptedIOException {
        AtomicBoolean cancellation = loadCancellation;
        if (Thread.currentThread().isInterrupted() || cancellation != null && cancellation.get()) throw new InterruptedIOException("Node config load canceled");
    }

    private void start(NodeBundle prepared) throws Exception {
        checkLoadCanceled();
        cancelRequests();
        stopService();
        generation++;
        token = UUID.randomUUID().toString();
        if (bridge == null || !bridge.isAlive()) {
            bridge = new NodeBridge(this::onMessage);
            bridge.setToken(token);
            bridge.open();
        } else {
            bridge.setToken(token);
        }
        bundle = prepared;
        address = null;
        startError = null;
        startLatch = new CountDownLatch(1);
        File data = prepared.getData();
        Intent intent = new Intent(context, NodeService.class).setAction(NodeService.ACTION_START)
                .putExtra(NodeService.EXTRA_INDEX, prepared.getIndex().getAbsolutePath())
                .putExtra(NodeService.EXTRA_CONFIG, prepared.getConfig().getAbsolutePath())
                .putExtra(NodeService.EXTRA_DATA, data.getAbsolutePath())
                .putExtra(NodeService.EXTRA_BRIDGE_PORT, bridge.getListeningPort())
                .putExtra(NodeService.EXTRA_TOKEN, token);
        NodeConnection next = new NodeConnection(generation);
        connection = next;
        if (context.startService(intent) == null) throw new IOException("Unable to start Node service");
        next.bound = context.bindService(intent, next, Context.BIND_AUTO_CREATE);
        if (!next.bound) throw new IOException("Unable to bind Node service");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_TIMEOUT_SECONDS);
        while (!startLatch.await(100, TimeUnit.MILLISECONDS)) {
            checkLoadCanceled();
            if (System.nanoTime() >= deadline) throw new IOException("Node startup timed out");
        }
        checkLoadCanceled();
        if (!TextUtils.isEmpty(startError)) throw new IOException(startError);
        if (TextUtils.isEmpty(address)) throw new IOException("Node server did not report an address");
        startedAt = System.currentTimeMillis();
        SpiderDebug.log("NodeRuntime", "Started %s", diagnostics());
    }

    private void stopService() {
        NodeConnection current = connection;
        connection = null;
        if (current != null) {
            current.stop();
            return;
        }
        try {
            context.stopService(new Intent(context, NodeService.class));
        } catch (Exception ignored) {
        }
    }

    String onMessage(String text) {
        try {
            JsonObject request = Json.parse(text).getAsJsonObject();
            String action = Json.safeString(request, "action");
            JsonObject opt = request.has("opt") && request.get("opt").isJsonObject() ? request.getAsJsonObject("opt") : new JsonObject();
            return switch (action) {
                case "serverStarted" -> onStarted(opt);
                case "nodeError" -> onError(opt);
                case "diagnostics" -> diagnostics();
                default -> callback == null ? failure() : callback.onMessage(action, opt);
            };
        } catch (Exception e) {
            SpiderDebug.log(e);
            return failure();
        }
    }

    private String onStarted(JsonObject opt) {
        if (!tokenMatches(opt)) return success();
        String value = Json.safeString(opt, "address");
        if (value.startsWith("http://127.0.0.1:") || value.startsWith("http://localhost:")) {
            address = value.replace("http://localhost:", "http://127.0.0.1:");
            nodePid = opt.has("pid") && opt.get("pid").isJsonPrimitive() ? opt.get("pid").getAsInt() : 0;
            nodeVersion = Json.safeString(opt, "version");
            nodeArch = Json.safeString(opt, "arch");
            CountDownLatch latch = startLatch;
            if (latch != null) latch.countDown();
        }
        return success();
    }

    private String onError(JsonObject opt) {
        if (!tokenMatches(opt)) return success();
        startError = Json.safeString(opt, "message");
        CountDownLatch latch = startLatch;
        if (latch != null) latch.countDown();
        return success();
    }

    public static String normalizeInternalUrl(String value, String address) {
        if (value == null || address == null || value.isEmpty() || address.isEmpty()) return value;
        HttpUrl target = HttpUrl.parse(value);
        HttpUrl runtime = HttpUrl.parse(address);
        if (target == null || runtime == null || !"http".equals(target.scheme()) || target.port() != runtime.port()) return value;
        return target.newBuilder().scheme(runtime.scheme()).host(runtime.host()).port(runtime.port()).build().toString();
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public void setPlayInfo(String flag, String id) {
        if (callback != null) callback.onPlayInfo(flag, id);
    }

    public File getProfileFile() {
        NodeBundle current = bundle;
        return current == null ? null : new File(current.getRoot().getParentFile(), "profile.json");
    }

    private boolean tokenMatches(JsonObject opt) {
        return !TextUtils.isEmpty(token) && token.equals(Json.safeString(opt, "token"));
    }

    public String post(String api, JsonObject body) throws Exception {
        return request(api, state -> executePost(state, api, body));
    }

    private <T> T request(String endpoint, RequestOperation<T> operation) throws Exception {
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
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful()) throw new NodeHttpException(response.code());
            if (responseBody == null) throw new IOException("Empty Node response");
            String result = rewriteLoopback(readResponse(responseBody, MAX_JSON_RESPONSE_BYTES), base);
            checkCurrent(state);
            return result;
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
        Call call = registerCall(state, PLAY_CLIENT, request.build());
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

    private String get(String route) throws IOException {
        RequestState state = snapshot();
        String base = state.address();
        if (TextUtils.isEmpty(base)) throw new IOException("Node runtime is not running");
        Request request = new Request.Builder().url(base + route).get().build();
        Call call = registerCall(state, CLIENT, request);
        try (Response response = call.execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) throw new NodeHttpException(response.code());
            if (body == null) throw new IOException("Empty Node response");
            String result = rewriteLoopback(readResponse(body, MAX_CONFIG_RESPONSE_BYTES), base);
            checkCurrent(state);
            return result;
        } finally {
            calls.remove(call);
        }
    }

    private synchronized RequestState recover(RequestState observed) throws Exception {
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
        if (Thread.currentThread().isInterrupted() || state.cancellation() != null && state.cancellation().get()) throw new InterruptedIOException("Node request canceled");
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

    static String rewriteLoopback(String value, String address) {
        if (value == null || address == null || address.isEmpty()) return value;
        return LEGACY_LOOPBACK.matcher(value).replaceAll(Matcher.quoteReplacement(address));
    }

    static String readResponse(ResponseBody body, long limit) throws IOException {
        long length = body.contentLength();
        if (length > limit) throw new NodeResponseException("Node response is too large");
        try (InputStream input = body.byteStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > limit) throw new NodeResponseException("Node response is too large");
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void cancelRequests() {
        for (Call call : calls) call.cancel();
        calls.clear();
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
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            try {
                service.linkToDeath(this, 0);
            } catch (RemoteException e) {
                stopped.countDown();
                notifyLost("Node service died while binding");
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
        public void binderDied() {
            stopped.countDown();
            notifyLost("Node service process died");
        }

        private void notifyLost(String message) {
            if (connection == this && generation == session) onServiceLost(message);
        }

        private void stop() {
            if (bound && binder == null) {
                try {
                    connected.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            IBinder target = binder;
            if (bound) {
                try {
                    context.unbindService(this);
                } catch (Exception ignored) {
                }
                bound = false;
            }
            try {
                context.stopService(new Intent(context, NodeService.class));
            } catch (Exception ignored) {
            }
            if (target != null && target.isBinderAlive()) {
                try {
                    if (!stopped.await(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) SpiderDebug.log("NodeRuntime", "Timed out waiting for Node service process to stop");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (target != null) {
                try {
                    target.unlinkToDeath(this, 0);
                } catch (Exception ignored) {
                }
            }
            binder = null;
        }
    }

    private static String success() {
        return "{\"success\":true,\"code\":0}";
    }

    private static String failure() {
        return "{\"success\":false,\"code\":-1}";
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

    private interface RequestOperation<T> {
        T execute(RequestState state) throws Exception;
    }

    public record LoadedConfig(String json, long session) {
    }

    public interface Callback {
        String onMessage(String action, JsonObject options);

        void onPlayInfo(String flag, String id);
    }
}
