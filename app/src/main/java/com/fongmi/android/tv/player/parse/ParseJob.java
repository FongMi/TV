package com.fongmi.android.tv.player.parse;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;

public class ParseJob implements ParseCallback {

    private final List<CustomWebView> webViews;
    private ExecutorService executor;
    private ExecutorService infinite;
    private ParseCallback callback;
    private Parse parse;

    public static ParseJob create(ParseCallback callback) {
        return new ParseJob(callback);
    }

    public ParseJob(ParseCallback callback) {
        this.executor = Executors.newFixedThreadPool(2);
        this.infinite = Executors.newCachedThreadPool();
        this.webViews = new ArrayList<>();
        this.callback = callback;
    }

    public ParseJob start(Result result, boolean useParse) {
        setParse(result, useParse);
        execute(result);
        return this;
    }

    private void setParse(Result result, boolean useParse) {
        if (useParse) parse = ApiConfig.get().getParse();
        if (result.getPlayUrl().startsWith("json:")) parse = Parse.get(1, result.getPlayUrl().substring(5));
        if (result.getPlayUrl().startsWith("parse:")) parse = ApiConfig.get().getParse(result.getPlayUrl().substring(6));
        if (parse == null) parse = Parse.get(0, result.getPlayUrl(), result.getHeader());
    }

    private void execute(Result result) {
        executor.execute(() -> {
            try {
                executor.submit(getTask(result)).get(Constant.TIMEOUT_PARSE_DEF, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                onParseError();
            }
        });
    }

    private Runnable getTask(Result result) {
        return () -> {
            try {
                doInBackground(result.getKey(), result.getUrl(), result.getFlag());
            } catch (Exception e) {
                onParseError();
            }
        };
    }

    private void doInBackground(String key, String webUrl, String flag) throws Exception {
        switch (parse.getType()) {
            case 0: //嗅探
                startWeb(key, parse, webUrl);
                break;
            case 1: //Json
                jsonParse(parse, webUrl, true);
                break;
            case 2: //Json擴展
                jsonExtend(webUrl);
                break;
            case 3: //Json聚合
                jsonMix(webUrl, flag);
                break;
            case 4: //上帝模式
                godParse(webUrl, flag);
                break;
        }
    }

    private void jsonParse(Parse item, String webUrl, boolean error) throws Exception {
        String body = OkHttp.newCall(item.getUrl() + webUrl, Headers.of(item.getHeaders())).execute().body().string();
        JsonObject object = JsonParser.parseString(body).getAsJsonObject();
        object = object.has("data") ? object.getAsJsonObject("data") : object;
        boolean illegal = body.contains("不存在") || body.contains("已过期");
        String url = illegal ? "" : Json.safeString(object, "url");
        checkResult(getHeader(object), url, item.getName(), error);
    }

    private void jsonExtend(String webUrl) throws Exception {
        LinkedHashMap<String, String> jxs = new LinkedHashMap<>();
        for (Parse item : ApiConfig.get().getParses()) if (item.getType() == 1) jxs.put(item.getName(), item.extUrl());
        checkResult(Result.fromObject(ApiConfig.get().jsonExt(parse.getUrl(), jxs, webUrl)));
    }

    private void jsonMix(String webUrl, String flag) throws Exception {
        LinkedHashMap<String, HashMap<String, String>> jxs = new LinkedHashMap<>();
        for (Parse item : ApiConfig.get().getParses()) jxs.put(item.getName(), item.mixMap());
        checkResult(Result.fromObject(ApiConfig.get().jsonExtMix(flag + "@", parse.getUrl(), parse.getName(), jxs, webUrl)));
    }

    private void godParse(String webUrl, String flag) throws Exception {
        List<Parse> json = ApiConfig.get().getParses(1, flag);
        List<Parse> webs = ApiConfig.get().getParses(0, flag);
        CountDownLatch latch = new CountDownLatch(json.size());
        for (Parse item : json) infinite.execute(() -> jsonParse(latch, item, webUrl));
        latch.await();
        if (webs.isEmpty()) onParseError();
        for (Parse item : webs) startWeb(item, webUrl);
    }

    private void jsonParse(CountDownLatch latch, Parse item, String webUrl) {
        try {
            jsonParse(item, webUrl, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    private void checkResult(Map<String, String> headers, String url, String from, boolean error) {
        if (isPass(headers, url)) {
            onParseSuccess(headers, url, from);
        } else if (error) {
            onParseError();
        }
    }

    private void checkResult(Result result) {
        if (result.getUrl().isEmpty()) onParseError();
        else if (result.getParse() == 1) startWeb(result.getHeaders(), Utils.checkProxy(result.getUrl()));
        else onParseSuccess(result.getHeaders(), result.getUrl(), result.getJxFrom());
    }

    private boolean isPass(Map<String, String> headers, String url) {
        try {
            int code = OkHttp.newCall(url, Headers.of(headers)).execute().code();
            return code == 200 && url.length() >= 40;
        } catch (Exception e) {
            return false;
        }
    }

    private void startWeb(Parse item, String webUrl) {
        startWeb("", item, webUrl);
    }

    private void startWeb(String key, Parse item, String webUrl) {
        startWeb(key, item.getName(), item.getHeaders(), item.getUrl() + webUrl);
    }

    private void startWeb(Map<String, String> headers, String url) {
        startWeb("", "", headers, url);
    }

    private void startWeb(String key, String form, Map<String, String> headers, String url) {
        App.post(() -> webViews.add(CustomWebView.create(App.get()).start(key, form, headers, url, this)));
    }

    private Map<String, String> getHeader(JsonObject object) {
        Map<String, String> headers = new HashMap<>();
        for (String key : object.keySet()) if (key.equalsIgnoreCase("user-agent") || key.equalsIgnoreCase("referer")) headers.put(key, object.get(key).getAsString());
        return headers;
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        App.post(() -> {
            if (callback != null) callback.onParseSuccess(headers, url, from);
            stop();
        });
    }

    @Override
    public void onParseError() {
        App.post(() -> {
            if (callback != null) callback.onParseError();
            stop();
        });
    }

    private void stopWeb() {
        for (CustomWebView webView : webViews) webView.stop(false);
        webViews.clear();
    }

    public void stop() {
        if (executor != null) executor.shutdownNow();
        if (infinite != null) infinite.shutdownNow();
        infinite = null;
        executor = null;
        callback = null;
        stopWeb();
    }
}