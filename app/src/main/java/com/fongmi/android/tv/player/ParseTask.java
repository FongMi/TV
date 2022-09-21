package com.fongmi.android.tv.player;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.fongmi.android.tv.utils.Json;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Headers;
import okhttp3.Response;

public class ParseTask {

    private final Handler handler;
    private CustomWebView webView;
    private ExecutorService executor;
    private Callback callback;
    private Parse parse;

    public static ParseTask create(Callback callback) {
        return new ParseTask(callback);
    }

    public ParseTask(Callback callback) {
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.webView = new CustomWebView(App.get());
        this.callback = callback;
    }

    public ParseTask run(Result result, boolean useParse) {
        setParse(result.getPlayUrl(), useParse);
        executor.submit(() -> doInBackground(result.getUrl(), result.getFlag()));
        return this;
    }

    private void setParse(String url, boolean useParse) {
        if (useParse) parse = ApiConfig.get().getParse();
        else if (url.startsWith("json:")) parse = Parse.get(1, url.substring(5));
        else if (url.startsWith("parse:")) parse = ApiConfig.get().getParse(url.substring(6));
        if (parse == null) parse = Parse.get(0, url);
    }

    private void doInBackground(String webUrl, String flag) {
        if (webUrl.startsWith("magnet:")) {
            onParseError();
            return;
        }
        switch (parse.getType()) {
            case 0: //嗅探
                handler.post(() -> webView.start(parse.getUrl() + webUrl, callback));
                break;
            case 1: //Json
                jsonParse(webUrl);
                break;
            case 2: //Json 擴展
                jsonExtend(webUrl);
                break;
            case 3: //聚合
                jsonMix(webUrl, flag);
                break;
        }
    }

    private void jsonParse(String webUrl) {
        try {
            Response response = OKHttp.newCall(parse.getUrl() + webUrl, Headers.of(Json.toMap(parse.getHeader()))).execute();
            JsonObject object = JsonParser.parseString(response.body().string()).getAsJsonObject();
            HashMap<String, String> headers = new HashMap<>();
            for (String key : object.keySet()) if (key.equalsIgnoreCase("user-agent") || key.equalsIgnoreCase("referer")) headers.put(key, object.get(key).getAsString());
            onParseSuccess(headers, object.get("url").getAsString(), "");
        } catch (Exception e) {
            e.printStackTrace();
            onParseError();
        }
    }

    private void jsonExtend(String webUrl) {
        LinkedHashMap<String, String> jxs = new LinkedHashMap<>();
        for (Parse item : ApiConfig.get().getParses()) if (item.getType() == 1) jxs.put(item.getName(), item.extUrl());
        checkResult(Result.fromObject(ApiConfig.get().jsonExt(parse.getUrl(), jxs, webUrl)));
    }

    private void jsonMix(String webUrl, String flag) {
        LinkedHashMap<String, HashMap<String, String>> jxs = new LinkedHashMap<>();
        for (Parse item : ApiConfig.get().getParses()) jxs.put(item.getName(), item.mixMap());
        checkResult(Result.fromObject(ApiConfig.get().jsonExtMix(flag + "@", parse.getUrl(), parse.getName(), jxs, webUrl)));
    }

    private void checkResult(Result result) {
        if (result.getUrl().isEmpty()) {
            onParseError();
        } else if (result.getParse(0) == 1) {
            handler.post(() -> webView.start(result.getUrl(), callback));
        } else {
            onParseSuccess(result.getHeaders(), result.getUrl(), result.getJxFrom());
        }
    }

    private void onParseSuccess(Map<String, String> headers, String url, String from) {
        handler.post(() -> {
            if (callback != null) callback.onParseSuccess(headers, url, from);
        });
    }

    private void onParseError() {
        handler.post(() -> {
            if (callback != null) callback.onParseError();
        });
    }

    public void cancel() {
        if (executor != null) executor.shutdownNow();
        webView.stop(false);
        executor = null;
        callback = null;
        webView = null;
    }

    public interface Callback {

        void onParseSuccess(Map<String, String> headers, String url, String from);

        void onParseError();
    }
}