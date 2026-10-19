package com.fongmi.android.tv.player.parse;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.ui.custom.ParseWebView;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.WebViewUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Task;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Response;

public class ParseJob implements ParseCallback {

    private final AtomicBoolean done = new AtomicBoolean();
    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private final List<ParseWebView> webViews;
    private ScheduledFuture<?> timeout;
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
        FutureTask<?> task = new FutureTask<>(() -> parseResult(result), null);
        timeout = Task.scheduler().schedule(() -> {
            if (task.cancel(true)) onParseError();
        }, Constant.TIMEOUT_PARSE_DEF, TimeUnit.MILLISECONDS);
        executor.execute(task);
    }

    private void parseResult(Result result) {
        try {
            doInBackground(result.getKey(), result.getUrl().v(), result.getFlag());
        } catch (Throwable e) {
            onParseError();
        }
    }

    private void doInBackground(String key, String webUrl, String flag) throws Throwable {
        switch (parse.getType()) {
            case 0 -> startWeb(key, parse, webUrl, this);
            case 1 -> jsonParse(parse, webUrl, true);
            case 2 -> jsonExtend(webUrl);
            case 3 -> jsonMix(webUrl, flag);
            case 4 -> superParse(webUrl, flag);
        }
    }

    private void jsonParse(Parse item, String webUrl, boolean fatal) throws Exception {
        Map<String, String> headers = getHeaders(item);
        Call call = OkHttp.newCall(item.getUrl() + webUrl, headers);
        calls.add(call);
        try {
            if (done.get()) return;
            try (Response res = call.execute()) {
                readJsonResult(res, headers, item, fatal);
            }
        } catch (IOException e) {
            if (!call.isCanceled()) throw e;
        } finally {
            calls.remove(call);
        }
    }

    private void readJsonResult(Response response, Map<String, String> headers, Parse item, boolean fatal) throws IOException {
        JsonObject object = Json.parse(response.body().string()).getAsJsonObject();
        String url = Json.safeString(object, "url");
        JsonObject data = object.getAsJsonObject("data");
        if (url.isEmpty()) url = Json.safeString(data, "url");
        checkResult(getHeader(object, headers), url, item.getName(), fatal);
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
        CountDownLatch latch = new CountDownLatch(json.size() + webs.size());
        for (Parse item : json) infinite.execute(() -> jsonParse(latch, item, webUrl));
        ParseCallback webCallback = new ParseCallback() {
            @Override
            public void onParseSuccess(Map<String, String> headers, String url, String from) {
                ParseJob.this.onParseSuccess(headers, url, from);
            }

            @Override
            public void onParseError() {
                latch.countDown();
            }
        };
        for (Parse item : webs) startWeb("", item, webUrl, webCallback);
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
        if (!url.isEmpty()) onParseSuccess(headers, url, from);
        else if (fatal) onParseError();
    }

    private void checkResult(Result result) {
        result.setHeader(new HashMap<>(parseHeaders));
        if (result.getUrl().isEmpty()) onParseError();
        else if (result.needParse()) startWeb(result.getHeader(), UrlUtil.convert(result.getUrl().v()));
        else onParseSuccess(result.getHeader(), result.getUrl().v(), result.getJxFrom());
    }

    private void startWeb(String key, Parse item, String webUrl, ParseCallback callback) {
        startWeb(key, item.getName(), getHeaders(item), item.getUrl() + webUrl, item == parse ? parseClick : item.getClick(), callback);
    }

    private void startWeb(Map<String, String> headers, String url) {
        startWeb("", "", headers, url, "", this);
    }

    private void startWeb(String key, String from, Map<String, String> headers, String url, String click, ParseCallback callback) {
        if (!WebViewUtil.support()) {
            callback.onParseError();
            return;
        }
        App.post(() -> {
            if (done.get()) return;
            webViews.add(ParseWebView.create(App.get()).start(key, from, headers, url, click, callback, !url.contains("player/?url=")));
        });
    }

    private Map<String, String> getHeaders(Parse item) {
        return item == parse ? parseHeaders : new HashMap<>(item.getHeader());
    }

    private Map<String, String> getHeader(JsonObject object, Map<String, String> fallback) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String name = entry.getKey();
            if (!entry.getValue().isJsonNull() && (name.equalsIgnoreCase(HttpHeaders.USER_AGENT) || name.equalsIgnoreCase(HttpHeaders.REFERER) || name.equalsIgnoreCase(HttpHeaders.COOKIE) || name.equalsIgnoreCase("ua"))) headers.put(UrlUtil.fixHeader(name), entry.getValue().getAsString());
        }
        return headers.isEmpty() ? fallback : headers;
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        complete(callback -> callback.onParseSuccess(headers, url, from));
    }

    @Override
    public void onParseError() {
        complete(ParseCallback::onParseError);
    }

    private void complete(Consumer<ParseCallback> result) {
        if (!done.compareAndSet(false, true)) return;
        App.post(() -> deliverResult(result));
    }

    private void deliverResult(Consumer<ParseCallback> result) {
        try {
            if (callback != null) result.accept(callback);
        } finally {
            stop();
        }
    }

    public void stop() {
        done.set(true);
        callback = null;
        if (timeout != null) timeout.cancel(false);
        for (Call call : calls) call.cancel();
        calls.clear();
        if (executor != null) executor.shutdownNow();
        if (infinite != null) infinite.shutdownNow();
        infinite = null;
        executor = null;
        for (ParseWebView webView : webViews) webView.release();
        webViews.clear();
    }
}
