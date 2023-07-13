package com.hiker.drpy;

import android.content.Context;

import com.hiker.drpy.method.Global;
import com.hiker.drpy.method.Local;
import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
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

    public Spider(String api, DexClassLoader dex) {
        this.key = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
        this.executor = Executors.newSingleThreadExecutor();
        this.api = api;
        this.dex = dex;
    }

    private void submit(Runnable runnable) {
        executor.submit(runnable);
    }

    private <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    private Object call(String func, Object... args) throws Exception {
        return submit(() -> jsObject.getJSFunction(func).call(args)).get();
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        submit(() -> initJS(context, extend));
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
        JSObject obj = submit(() -> convert(extend)).get();
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
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        JSArray array = submit(() -> convert(vipFlags)).get();
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
    public Object[] proxyLocal(Map<?, ?> params) throws Exception {
        return submit(() -> {
            JSObject obj = ctx.createNewJSObject();
            for (Object key : params.keySet()) obj.setProperty((String) key, (String) params.get(key));
            JSONArray array = new JSONArray(((JSArray) jsObject.getJSFunction("proxy").call(obj)).stringify());
            Object[] result = new Object[3];
            result[0] = array.opt(0);
            result[1] = array.opt(1);
            result[2] = getStream(array.opt(2));
            return result;
        }).get();
    }

    @Override
    public void destroy() {
        submit(() -> {
            executor.shutdownNow();
            ctx.destroy();
        });
    }

    private void initJS(Context context, String extend) {
        if (ctx == null) createCtx();
        if (dex != null) createDex();
        ctx.evaluateModule(getContent(context), api);
        jsObject = (JSObject) ctx.getProperty(ctx.getGlobalObject(), key);
        jsObject.getJSFunction("init").call(extend);
    }

    private void createCtx() {
        ctx = QuickJSContext.create();
        QuickJSLoader.initConsoleLog(ctx);
        Global.create(ctx, executor).setProperty();
        ctx.getGlobalObject().setProperty("local", Local.class);
    }

    private void createDex() {
        try {
            Class<?> apiClz = dex.loadClass("com.github.catvod.js.Method");
            JSObject apiObj = ctx.getGlobalObject().getJSObject("jsapi");
            injectApi(apiClz, apiObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void injectApi(Class<?> apiClz, JSObject apiObj) throws Exception {
        for (Class<?> clz : apiClz.getDeclaredClasses()) {
            Object javaObj = clz.getDeclaredConstructor(apiClz).newInstance(apiClz.getDeclaredConstructor(QuickJSContext.class).newInstance(ctx));
            JSObject clzObj = ctx.createNewJSObject();
            for (Method method : clz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(JSMethod.class)) continue;
                clzObj.setProperty(method.getName(), args -> {
                    try {
                        return method.invoke(javaObj, args);
                    } catch (Exception e) {
                        return null;
                    }
                });
            }
            apiObj.setProperty(clz.getSimpleName(), clzObj);
        }
    }

    private String getContent(Context context) {
        return Module.get().load(context, api)
                .replace("export default{", "globalThis." + key + " ={")
                .replace("export default {", "globalThis." + key + " ={")
                .replace("__JS_SPIDER__", "globalThis." + key);
    }

    private JSObject convert(HashMap<String, String> map) {
        JSObject obj = ctx.createNewJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) obj.setProperty(s, map.get(s));
        return obj;
    }

    private JSArray convert(List<String> items) {
        JSArray array = ctx.createNewJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) array.set(items.get(i), i);
        return array;
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
