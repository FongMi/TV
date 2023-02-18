package com.hiker.drpy;

import android.content.Context;

import com.hiker.drpy.method.Global;
import com.hiker.drpy.method.Local;
import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Spider extends com.github.catvod.crawler.Spider {

    private ExecutorService executor;
    private String key;
    private String api;
    private QuickJSContext ctx;
    private JSObject jsObject;

    public Spider() {
    }

    public Spider(String api) {
        this.executor = Executors.newSingleThreadExecutor();
        this.key = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
        this.api = api;
    }

    private void submit(Runnable runnable) {
        executor.submit(runnable);
    }

    private <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    private String post(String func, Object... args) throws ExecutionException, InterruptedException {
        return submit(() -> (String) jsObject.getJSFunction(func).call(args)).get();
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        submit(() -> initJS(context, extend));
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        return post("home", filter);
    }

    @Override
    public String homeVideoContent() throws Exception {
        return post("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        JSObject obj = submit(() -> convert(extend)).get();
        return post("category", tid, pg, filter, obj);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return post("detail", ids.get(0));
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return post("search", key, quick);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        JSArray array = submit(() -> convert(vipFlags)).get();
        return post("play", flag, id, array);
    }

    public void destroy() {
        submit(() -> {
            executor.shutdownNow();
            ctx.destroy();
        });
    }

    private void initJS(Context context, String extend) {
        if (ctx == null) createCtx();
        ctx.evaluateModule(getContent(context), api);
        jsObject = (JSObject) ctx.getProperty(ctx.getGlobalObject(), key);
        jsObject.getJSFunction("init").call(extend);
    }

    private void createCtx() {
        ctx = QuickJSContext.create();
        Global.create(ctx).setProperty();
        QuickJSLoader.initConsoleLog(ctx);
        ctx.getGlobalObject().setProperty("local", Local.class);
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
}
