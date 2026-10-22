package com.fongmi.quickjs.crawler;

import android.content.Context;

import com.fongmi.quickjs.Console;
import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.QuickJSContext;
import com.fongmi.quickjs.QuickJSLoader;
import com.fongmi.quickjs.bean.Res;
import com.fongmi.quickjs.host.Host;
import com.fongmi.quickjs.method.Global;
import com.fongmi.quickjs.method.Invocation;
import com.fongmi.quickjs.utils.Module;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import dalvik.system.DexClassLoader;

public class Spider extends com.github.catvod.crawler.Spider {

    private static final long DESTROY_TIMEOUT = 5000;
    private final DexClassLoader dex;
    private final String api;
    private final Host host;
    private final Module module;

    private QuickJS ctx;
    private volatile Global global;
    private volatile boolean destroyed;
    private boolean cat;

    public Spider(String api, DexClassLoader dex) {
        this(api, dex, Host.NONE);
    }

    public Spider(String api, DexClassLoader dex, Host host) {
        this(api, dex, host, new Module());
    }

    public Spider(String api, DexClassLoader dex, Host host, Module module) {
        this.api = api;
        this.dex = dex;
        this.host = host;
        this.module = module;
    }

    private void await(CompletableFuture<Object> future) throws Exception {
        if (destroyed) future.cancel(false);
        try {
            future.get();
        } catch (InterruptedException error) {
            future.cancel(false);
            Thread.currentThread().interrupt();
            throw error;
        }
    }

    private Object call(String func, Object... args) throws Exception {
        try (Invocation current = new Invocation()) {
            return call(current, func, args);
        }
    }

    private Object call(Invocation current, String func, Object... args) throws Exception {
        CompletableFuture<Object> result = ctx.invokeTimed("destroy".equals(func) || "stopDanmaku".equals(func) ? DESTROY_TIMEOUT : host.getCallTimeout(), "__JS_SPIDER__", func,
                future -> beginCall(current, func, future), global::endCall, args);
        try {
            return "destroy".equals(func) ? result.join() : result.get();
        } catch (InterruptedException error) {
            result.cancel(false);
            current.cancel();
            Thread.currentThread().interrupt();
            throw error;
        } finally {
            logMetrics(current, func);
        }
    }

    private void beginCall(Invocation current, String func, CompletableFuture<?> future) {
        if (destroyed && !"destroy".equals(func)) throw new IllegalStateException("Spider destroyed");
        current.bind(future);
        if (destroyed && !"destroy".equals(func)) future.cancel(false);
        global.beginCall(current);
    }

    private void logMetrics(Invocation current, String func) {
        Invocation.Metrics metrics = current.metrics();
        SpiderDebug.log("quickjs", "%s total=%.2fms queue=%.2fms owner=%.2fms net=%.2fms html=%.2fms netOps=%d pending=%d streams=%d",
                func, metrics.elapsedNanos() / 1e6, metrics.queuedNanos() / 1e6, metrics.ownerNanos() / 1e6,
                metrics.networkNanos() / 1e6, metrics.htmlNanos() / 1e6, metrics.networkOperations(), metrics.pending(), metrics.streams());
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        try {
            initializeJS();
            call("init", getExt(extend));
        } catch (Exception | Error error) {
            failInitialization(error);
            throw error;
        }
    }

    private void failInitialization(Throwable error) {
        destroyed = true;
        try {
            releaseJS();
        } catch (Exception failure) {
            error.addSuppressed(failure);
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        return (String) call("home", filter);
    }

    @Override
    public String homeVideoContent() throws Exception {
        return (String) call("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return (String) call("category", tid, pg, filter, new QuickJS.Json(new JSONObject(extend).toString()));
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return (String) call("detail", ids.get(0));
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return (String) call("search", key, quick);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return (String) call("search", key, quick, pg);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return (String) call("play", flag, id, vipFlags.toArray());
    }

    @Override
    public String liveContent(String url) throws Exception {
        return (String) call("live", url);
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        return (Boolean) call("sniffer");
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        return (Boolean) call("isVideo", url);
    }

    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        return "catvod".equals(params.get("from")) ? proxy2(params) : proxy1(params);
    }

    @Override
    public String action(String action) throws Exception {
        return (String) call("action", action);
    }

    @Override
    public AutoCloseable danmaku(String id, Consumer<JSONObject> emit) throws Exception {
        int token = global.addDanmaku(emit);
        try {
            call("startDanmaku", id, token);
        } catch (Exception | Error error) {
            global.removeDanmaku(token);
            throw error;
        }
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (!closed.compareAndSet(false, true)) return;
            global.removeDanmaku(token);
            if (!destroyed) call("stopDanmaku", token);
        };
    }

    @Override
    public synchronized void destroy() {
        if (destroyed) return;
        destroyed = true;
        try {
            cancelRequests();
            destroyScript();
        } finally {
            closeContext();
        }
    }

    private void cancelRequests() {
        try {
            if (global != null) global.cancelRequests();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void destroyScript() {
        try {
            if (ctx != null) ctx.cancelPending();
            call("destroy");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void closeContext() {
        try {
            releaseJS();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void releaseJS() {
        if (ctx == null) return;
        ctx.close();
    }

    private void initializeJS() throws Exception {
        ctx = new QuickJS(QuickJSLoader::init);
        ctx.submit(this::bindGlobals).get();
        ctx.setModuleLoader(module).get();
        await(ctx.evaluate(RuntimeModule.QUICKJS.readAsset("js/lib/net.js"), "lib/net.js"));
        ctx.submit(() -> {
            createFun();
            return null;
        }).get();
        createObj();
    }

    private Object bindGlobals() {
        ctx.setUnhandledRejectionListener(SpiderDebug::log).join();
        Console.install(ctx).join();
        global = Global.create(ctx, host, siteKey, api);
        return null;
    }

    private void createFun() {
        if (dex == null) return;
        try {
            Class<?> clz = dex.loadClass("com.github.catvod.js.Function");
            clz.getDeclaredConstructor(QuickJSContext.class).newInstance(new QuickJSContext(ctx));
        } catch (ClassNotFoundException ignored) {
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot initialize JavaScript JAR functions", error);
        }
    }

    private void createObj() throws Exception {
        String spider = "__JS_SPIDER__";
        String global = "globalThis." + spider;
        String content = module.load(api);
        cat = content.contains("__jsEvalReturn");
        await(ctx.evaluateModule(content.replace(spider, global), api));
        await(ctx.evaluateModule(String.format(RuntimeModule.QUICKJS.readAsset("js/lib/spider.js"), JSONObject.quote(api), JSONObject.quote(siteKey)), api + "#spider"));
    }

    private Object getExt(String ext) throws Exception {
        if (!cat) return Json.isObj(ext) ? new QuickJS.Json(ext) : ext;
        JSONObject object = new JSONObject().put("stype", 3).put("skey", siteKey);
        object.put("ext", Json.isObj(ext) ? new JSONObject(ext) : ext);
        return new QuickJS.Json(object.toString());
    }

    private Object[] proxy1(Map<String, String> params) throws Exception {
        try (Invocation current = new Invocation(true)) {
            Object[] array = (Object[]) call(current, "proxy", new QuickJS.Json(new JSONObject(params).toString()));
            return proxyResponse(current, array);
        }
    }

    private Object[] proxyResponse(Invocation current, Object[] array) throws Exception {
        if (array.length < 3) throw new IllegalArgumentException("Invalid proxy response");
        Object body = jsonValue(array[2]);
        boolean streaming = body instanceof JSONObject handle && handle.length() == 1 && handle.has("$tvStream");
        Object header = array.length > 3 ? array[3] : null;
        Map<String, String> headers = proxyHeaders(header, streaming);
        JSONArray metadata = new JSONArray().put(array[0]).put(array[1]).put(array.length > 4 ? array[4] : null);
        if (streaming) {
            if (metadata.optInt(2) == 1) throw new IllegalArgumentException("Streams cannot be base64 encoded");
            int code = metadata.optInt(0);
            if (code < 100 || code > 599) throw new IllegalArgumentException("Invalid stream response status");
            return new Object[]{code, metadata.optString(1), current.takeStream(((JSONObject) body).getString("$tvStream")), headers};
        }
        return new Object[]{metadata.optInt(0), metadata.optString(1), getStream(body, metadata.optInt(2) == 1), headers};
    }

    private Map<String, String> proxyHeaders(Object header, boolean streaming) throws JSONException {
        if (streaming) return streamHeaders(header);
        return header == null ? null : Json.toMap(header instanceof QuickJS.Json(String text) ? text : String.valueOf(header));
    }

    private Map<String, String> streamHeaders(Object header) throws JSONException {
        if (header == null) return null;
        JSONObject object = new JSONObject(header instanceof QuickJS.Json(String text) ? text : String.valueOf(header));
        Map<String, String> headers = new LinkedHashMap<>();
        for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
            String key = keys.next();
            headers.put(key, streamHeaderValue(object.get(key)));
        }
        return headers;
    }

    private String streamHeaderValue(Object value) throws JSONException {
        if (value instanceof JSONArray values) {
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < values.length(); i++) {
                if (!(values.get(i) instanceof String part)) throw new IllegalArgumentException("Invalid stream response header");
                if (i > 0) joined.append(", ");
                joined.append(part);
            }
            value = joined.toString();
        }
        if (!(value instanceof String text)) throw new IllegalArgumentException("Invalid stream response header");
        return text;
    }

    private Object[] proxy2(Map<String, String> params) throws Exception {
        String url = params.get("url");
        String header = params.get("header");
        String proxy = (String) call("proxy", (Object) url.split("/"), new QuickJS.Json(header));
        Res res = Res.objectFrom(proxy);
        return new Object[]{res.getCode(), res.getContentType(), res.getStream()};
    }

    private ByteArrayInputStream getStream(Object o, boolean base64) {
        if (o instanceof byte[] bytes) return new ByteArrayInputStream(bytes);
        String content = String.valueOf(o);
        if (base64 && content.contains("base64,")) content = content.split("base64,")[1];
        return new ByteArrayInputStream(base64 ? Util.decode(content) : content.getBytes());
    }

    private Object jsonValue(Object value) throws JSONException {
        if (value instanceof QuickJS.Json(String text)) return new JSONTokener(text).nextValue();
        if (!(value instanceof Object[] items)) return value;
        JSONArray array = new JSONArray();
        for (Object item : items) array.put(jsonValue(item));
        return array;
    }
}
