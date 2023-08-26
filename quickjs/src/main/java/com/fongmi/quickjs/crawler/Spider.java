package com.fongmi.quickjs.crawler;

import android.content.Context;
import android.util.Base64;

import com.fongmi.quickjs.bean.Res;
import com.fongmi.quickjs.method.Function;
import com.fongmi.quickjs.method.Global;
import com.fongmi.quickjs.method.Local;
import com.fongmi.quickjs.utils.JSUtil;
import com.fongmi.quickjs.utils.Module;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;
import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dalvik.system.DexClassLoader;

public class Spider extends com.github.catvod.crawler.Spider {

    private final ExecutorService executor;
    private final DexClassLoader dex;
    private QuickJSContext ctx;
    private JSObject jsObject;
    private final String key;
    private final String api;

    public Spider(String key, String api, DexClassLoader dex) throws Exception {
        this.key = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
        this.executor = Executors.newSingleThreadExecutor();
        this.api = api;
        this.dex = dex;
        initializeJS();
    }

    private void submit(Runnable runnable) {
        executor.submit(runnable);
    }

    private <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    private Object call(String func, Object... args) throws Exception {
        return executor.submit((Function.call(jsObject, func, args))).get();
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        call("init", Json.valid(extend) ? ctx.parse(extend) : extend);
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
        JSObject obj = submit(() -> JSUtil.toObj(ctx, extend)).get();
        return (String) call("category", tid, pg, filter, obj);
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
        JSArray array = submit(() -> JSUtil.toArray(ctx, vipFlags)).get();
        return (String) call("play", flag, id, array);
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
    public Object[] proxyLocal(Map<String, String> params) throws Exception {
        if ("catvod".equals(params.get("from"))) return proxy2(params);
        else return submit(() -> proxy1(params)).get();
    }

    @Override
    public void destroy() {
        submit(() -> {
            executor.shutdownNow();
            ctx.destroy();
        });
    }

    private void initializeJS() throws Exception {
        submit(() -> {
            if (ctx == null) createCtx();
            if (dex != null) createDex();
            ctx.evaluateModule(getContent(), api);
            jsObject = (JSObject) ctx.getProperty(ctx.getGlobalObject(), key);
            return null;
        }).get();
    }

    private void createCtx() {
        ctx = QuickJSContext.create();
        QuickJSLoader.initConsoleLog(ctx);
        Global.create(ctx, executor).setProperty();
        ctx.getGlobalObject().setProperty("local", Local.class);
        ctx.getGlobalObject().getContext().evaluate(Path.asset("js/lib/http.js"));
    }

    private void createDex() {
        try {
            JSObject obj = ctx.createNewJSObject();
            Class<?> clz = dex.loadClass("com.github.catvod.js.Method");
            Class<?>[] classes = clz.getDeclaredClasses();
            ctx.getGlobalObject().setProperty("jsapi", obj);
            if (classes.length == 0) invokeSingle(clz, obj);
            if (classes.length >= 1) invokeMultiple(clz, obj);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void invokeSingle(Class<?> clz, JSObject jsObj) throws Throwable {
        invoke(clz, jsObj, clz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx));
    }

    private void invokeMultiple(Class<?> clz, JSObject jsObj) throws Throwable {
        for (Class<?> subClz : clz.getDeclaredClasses()) {
            Object javaObj = subClz.getDeclaredConstructor(clz).newInstance(clz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx));
            JSObject subObj = ctx.createNewJSObject();
            invoke(subClz, subObj, javaObj);
            jsObj.setProperty(subClz.getSimpleName(), subObj);
        }
    }

    private void invoke(Class<?> clz, JSObject jsObj, Object javaObj) {
        for (Method method : clz.getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            invoke(jsObj, method, javaObj);
        }
    }

    private void invoke(JSObject jsObj, Method method, Object javaObj) {
        jsObj.setProperty(method.getName(), args -> {
            try {
                return method.invoke(javaObj, args);
            } catch (Throwable e) {
                return null;
            }
        });
    }

    private String getContent() {
        String global = "globalThis." + key;
        String content = Module.get().load(api);
        if (content.contains("__jsEvalReturn")) {
            ctx.evaluate("req = http");
            return content.concat(global).concat(" = __jsEvalReturn()");
        } else if (content.contains("__JS_SPIDER__")) {
            return content.replace("__JS_SPIDER__", global);
        } else {
            return content.replaceAll("export default.*?[{]", global + " = {");
        }
    }

    private Object[] proxy1(Map<String, String> params) throws Exception {
        JSObject object = JSUtil.toObj(ctx, params);
        JSONArray array = new JSONArray(((JSArray) jsObject.getJSFunction("proxy").call(object)).stringify());
        Object[] result = new Object[3];
        result[0] = array.opt(0);
        result[1] = array.opt(1);
        result[2] = getStream(array.opt(2));
        return result;
    }

    private Object[] proxy2(Map<String, String> params) throws Exception {
        String url = params.get("url");
        String header = params.get("header");
        JSArray array = submit(() -> JSUtil.toArray(ctx, Arrays.asList(url.split("/")))).get();
        Object object = submit(() -> ctx.parse(header)).get();
        String json = (String) call("proxy", array, object);
        Res res = Res.objectFrom(json);
        Object[] result = new Object[3];
        result[0] = 200;
        result[1] = "application/octet-stream";
        result[2] = new ByteArrayInputStream(Base64.decode(res.getContent(), Base64.DEFAULT));
        return result;
    }

    private ByteArrayInputStream getStream(Object o) {
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            byte[] bytes = new byte[a.length()];
            for (int i = 0; i < a.length(); i++) bytes[i] = (byte) a.optInt(i);
            return new ByteArrayInputStream(bytes);
        } else {
            return new ByteArrayInputStream(o.toString().getBytes());
        }
    }
}

