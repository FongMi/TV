package com.fongmi.android.tv.player.parse;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.custom.ParseWebView;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.WebViewUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Response;

public class ParseJob implements ParseCallback {

    private final AtomicBoolean done = new AtomicBoolean();
    private final List<ParseWebView> webViews;
    private ExecutorService executor;
    private ExecutorService infinite;
    private ParseCallback callback;
    private Parse parse;
    private Map<String, String> parseHeaders;
    private String parseClick;

    private ParseJob(ParseCallback callback) {
        this.executor = Executors.newSingleThreadExecutor();
        this.infinite = Executors.newCachedThreadPool();
        this.webViews = new ArrayList<>();
        this.callback = callback;
    }

    public static ParseJob create(ParseCallback callback) {
        return new ParseJob(callback);
    }

    public ParseJob start(Result result, boolean useParse) {
        setParse(result, useParse);
        execute(result);
        return this;
    }

    private void setParse(Result result, boolean useParse) {
        if (useParse) parse = VodConfig.get().getParse();
        if (result.getPlayUrl().startsWith("json:")) parse = Parse.get(1, result.getPlayUrl().substring(5));
        if (result.getPlayUrl().startsWith("parse:")) parse = VodConfig.get().getParse(result.getPlayUrl().substring(6));
        if (parse == null || parse.isEmpty()) parse = Parse.get(0, result.getPlayUrl());
        parseHeaders = resolveHeaders(parse, result);
        String click = VodConfig.get().getSite(result.getKey()).getClick();
        parseClick = TextUtils.isEmpty(click) ? result.getClick() : click;
    }

    static Map<String, String> resolveHeaders(Parse parse, Result result) {
        Map<String, String> configured = parse.getHeader();
        return new HashMap<>(configured.isEmpty() ? result.getHeader() : configured);
    }

    private void execute(Result result) {
        Future<?> task = executor.submit(() -> {
            try {
                doInBackground(result.getKey(), result.getUrl().v(), result.getFlag());
            } catch (Throwable e) {
                onParseError();
            }
        });
        Task.schedule(() -> {
            if (task.cancel(true)) onParseError();
        }, Constant.TIMEOUT_PARSE_DEF, TimeUnit.MILLISECONDS);
    }

    private void doInBackground(String key, String webUrl, String flag) throws Throwable {
        switch (parse.getType()) {
            case 0 -> startWeb(key, parse, webUrl);
            case 1 -> jsonParse(parse, webUrl, true);
            case 2 -> jsonExtend(webUrl);
            case 3 -> jsonMix(webUrl, flag);
            case 4 -> superParse(webUrl, flag);
        }
    }

    private void jsonParse(Parse item, String webUrl, boolean fatal) throws Exception {
        Map<String, String> headers = getHeaders(item);
        try (Response res = OkHttp.newCall(item.getUrl() + webUrl, headers).execute()) {
            JsonObject object = Json.parse(res.body().string()).getAsJsonObject();
            String url = Json.safeString(object, "url");
            JsonObject data = object.getAsJsonObject("data");
            if (url.isEmpty()) url = Json.safeString(data, "url");
            checkResult(getHeader(object, headers), url, item.getName(), fatal);
        }
    }

    private void jsonExtend(String webUrl) throws Throwable {
        LinkedHashMap<String, String> jxs = new LinkedHashMap<>();
        for (Parse item : VodConfig.get().getParses()) if (item.getType() == 1) jxs.put(item.getName(), item.extUrl());
        checkResult(Result.fromObject(BaseLoader.get().jsonExt(parse.getUrl(), jxs, webUrl)));
    }

    private void jsonMix(String webUrl, String flag) throws Throwable {
        LinkedHashMap<String, HashMap<String, String>> jxs = new LinkedHashMap<>();
        for (Parse item : VodConfig.get().getParses()) jxs.put(item.getName(), item.mixMap());
        checkResult(Result.fromObject(BaseLoader.get().jsonExtMix(flag, parse.getUrl(), parse.getName(), jxs, webUrl)));
    }

    private void superParse(String webUrl, String flag) throws Exception {
        List<Parse> json = VodConfig.get().getParses(1, flag);
        List<Parse> webs = VodConfig.get().getParses(0, flag);
        int count = json.size() + (webs.isEmpty() ? 0 : 1);
        CountDownLatch latch = new CountDownLatch(count);
        for (Parse item : json) infinite.execute(() -> jsonParse(latch, item, webUrl));
        if (!webs.isEmpty()) startWeb(webs, webUrl);
        latch.await();
        onParseError();
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

    private void checkResult(Map<String, String> headers, String url, String from, boolean fatal) {
        if (url.length() > 40) onParseSuccess(headers, url, from);
        else if (fatal) onParseError();
    }

    private void checkResult(Result result) {
        result.setHeader(new HashMap<>(parseHeaders));
        if (result.getUrl().isEmpty()) onParseError();
        else if (result.needParse()) startWeb(result.getHeader(), UrlUtil.convert(result.getUrl().v()));
        else onParseSuccess(result.getHeader(), result.getUrl().v(), result.getJxFrom());
    }

    private void startWeb(List<Parse> items, String webUrl) {
        StringBuilder sb = new StringBuilder();
        for (Parse item : items) sb.append(item.getUrl()).append(";");
        startWeb(new HashMap<>(), Server.get().getAddress("/parse?jxs=" + Util.substring(sb.toString()) + "&url=" + webUrl));
    }

    private void startWeb(String key, Parse item, String webUrl) {
        startWeb(key, item.getName(), getHeaders(item), item.getUrl() + webUrl, item == parse ? parseClick : item.getClick());
    }

    private void startWeb(Map<String, String> headers, String url) {
        startWeb("", "", headers, url, "");
    }

    private void startWeb(String key, String from, Map<String, String> headers, String url, String click) {
        if (!WebViewUtil.support()) {
            onParseError();
            return;
        }
        App.post(() -> {
            if (done.get()) return;
            webViews.add(ParseWebView.create(App.get()).start(key, from, headers, url, click, this, !url.contains("player/?url=")));
        });
    }

    private Map<String, String> getHeaders(Parse item) {
        return item == parse ? parseHeaders : new HashMap<>(item.getHeader());
    }

    private Map<String, String> getHeader(JsonObject object, Map<String, String> fallback) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) if (!entry.getValue().isJsonNull() && (entry.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT) || entry.getKey().equalsIgnoreCase(HttpHeaders.REFERER) || entry.getKey().equalsIgnoreCase(HttpHeaders.COOKIE) || entry.getKey().equalsIgnoreCase("ua"))) headers.put(UrlUtil.fixHeader(entry.getKey()), entry.getValue().getAsString());
        return headers.isEmpty() ? fallback : headers;
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!done.compareAndSet(false, true)) return;
        App.post(() -> {
            if (callback != null) callback.onParseSuccess(headers, url, from);
            stop();
        });
    }

    @Override
    public void onParseError() {
        if (!done.compareAndSet(false, true)) return;
        App.post(() -> {
            if (callback != null) callback.onParseError();
            stop();
        });
    }

    public void stop() {
        if (executor != null) executor.shutdownNow();
        if (infinite != null) infinite.shutdownNow();
        infinite = null;
        executor = null;
        callback = null;
        done.set(true);
        for (ParseWebView webView : webViews) webView.release();
        webViews.clear();
    }
}
