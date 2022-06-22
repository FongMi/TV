package com.fongmi.bear.net;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

public class Task {

    private final ExecutorService executor;
    private final Callback callback;
    private final Handler handler;

    public static Task create(Callback callback) {
        return new Task(callback);
    }

    public Task(Callback callback) {
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.callback = callback;
    }

    public void run(String url) {
        executor.submit(() -> doInBackground(url));
    }

    private void doInBackground(String url) {
        try {
            Response res = OKHttp.get().client().newCall(new Request.Builder().url(url).build()).execute();
            onPostExecute(res.body().string());
        } catch (Exception e) {
            onPostExecute("");
        }
    }

    private void onPostExecute(String result) {
        handler.post(() -> {
            if (callback != null) callback.onResponse(result);
        });
    }
}
