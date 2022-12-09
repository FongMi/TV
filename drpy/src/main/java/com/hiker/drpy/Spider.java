package com.hiker.drpy;

import android.content.Context;

import com.github.tvbox.quickjs.JSObject;
import com.github.tvbox.quickjs.QuickJSContext;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Spider extends com.github.catvod.crawler.Spider {

    private QuickJSContext ctx;
    private JSObject jsObject;
    private String api;
    private String ext;
    private String key;

    public Spider(QuickJSContext ctx, String api, String ext) {
        this.key = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
        this.ctx = ctx;
        this.api = api;
        this.ext = ext;
    }

    private String getContent() {
        return Module.get().load(api)
                .replace("export default{", "globalThis." + key + " ={")
                .replace("export default {", "globalThis." + key + " ={")
                .replace("__JS_SPIDER__", "globalThis." + key);
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        Worker.submit(() -> {
            ctx.evaluateModule(getContent(), api);
            jsObject = (JSObject) ctx.getProperty(ctx.getGlobalObject(), key);
            jsObject.getJSFunction("init").call(ext);
        });
    }

    private String post(String func, Object... args) throws ExecutionException, InterruptedException {
        return Worker.submit(() -> (String) jsObject.getJSFunction(func).call(args)).get();
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
        //TODO extend
        return post("category", tid, pg, filter, null);
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
        //TODO vipFlags
        return post("play", flag, id, null);
    }
}
