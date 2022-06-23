package com.fongmi.bear.utils;

import androidx.annotation.NonNull;

import com.fongmi.bear.App;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.net.OKHttp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class FileUtil {

    public static File getCacheDir() {
        return App.get().getExternalCacheDir();
    }

    public static String getCachePath() {
        return getCacheDir().getAbsolutePath();
    }

    public static File getCacheFile(String fileName) {
        return new File(getCacheDir(), fileName);
    }

    public static File getJar() {
        return getCacheFile("spider.jar");
    }

    public static String getJarPath() {
        return getJar().getAbsolutePath();
    }

    public static void download(String url, Callback callback) {
        Request request = new Request.Builder().url(url).build();
        OKHttp.get().client().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                callback.onResponse(writeFile(response));
            }
        });
    }

    private static File writeFile(Response response) throws IOException {
        FileOutputStream fos = new FileOutputStream(getJar());
        fos.write(response.body().bytes());
        fos.close();
        return getJar();
    }
}
