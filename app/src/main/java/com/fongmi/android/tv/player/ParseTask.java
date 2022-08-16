package com.fongmi.android.tv.player;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.Json;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Headers;
import okhttp3.Response;

public class ParseTask {

    private final Handler handler;
    private ExecutorService executor;
    private Callback callback;
    private Parse parse;

    public static ParseTask create(Callback callback) {
        return new ParseTask(callback);
    }

    public ParseTask(Callback callback) {
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.callback = callback;
    }

    public void run(Result result) {
        boolean useParse = (result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx().equals("1");
        setParse(result.getPlayUrl() + result.getUrl(), useParse);
        executor.submit(this::doInBackground);
    }

    private void setParse(String url, boolean useParse) {
        if (useParse) parse = ApiConfig.get().getParse();
        if (url.startsWith("json:")) parse = Parse.get(1, url.substring(5));
        if (url.startsWith("parse:")) parse = ApiConfig.get().getParse(url.substring(6));
        if (parse == null) parse = Parse.get(0, url);
    }

    private void doInBackground() {
        switch (parse.getType()) {
            case 0: //嗅探
                handler.post(() -> Players.get().web().start(parse.getUrl(), callback));
                break;
            case 1: //Json
                jsonParse();
                break;
            case 2: //Json 擴展

                break;
            case 3: //聚合

                break;
        }
    }

    private void jsonParse() {
        try {
            Headers headers = new Headers.Builder().build();
            if (parse.hasHeader()) headers = Headers.of(Json.toMap(parse.getHeader()));
            Response response = OKHttp.newCall(parse.getUrl(), headers).execute();
            JsonObject object = JsonParser.parseString(response.body().string()).getAsJsonObject();
            HashMap<String, String> header = new HashMap<>();
            for (String key : object.keySet()) if (key.equalsIgnoreCase("user-agent") || key.equalsIgnoreCase("referer")) header.put(key, object.get(key).getAsString());
            onParseSuccess(header, object.get("url").getAsString());
        } catch (Exception e) {
            e.printStackTrace();
            onParseError();
        }
    }

    private void onParseSuccess(Map<String, String> headers, String url) {
        handler.post(() -> {
            if (callback != null) callback.onParseSuccess(headers, url);
        });
    }

    private void onParseError() {
        handler.post(() -> {
            if (callback != null) callback.onParseError();
        });
    }

    public void cancel() {
        if (executor != null) executor.shutdownNow();
        executor = null;
        callback = null;
    }

    public interface Callback {

        void onParseSuccess(Map<String, String> headers, String url);

        void onParseError();
    }
}