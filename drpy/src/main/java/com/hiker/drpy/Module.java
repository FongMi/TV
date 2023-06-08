package com.hiker.drpy;

import android.content.Context;
import android.net.Uri;

import com.github.catvod.net.OkHttp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Headers;
import okhttp3.Response;

public class Module {

    private final ConcurrentHashMap<String, String> cache;

    private static class Loader {
        static volatile Module INSTANCE = new Module();
    }

    public static Module get() {
        return Loader.INSTANCE;
    }

    public Module() {
        this.cache = new ConcurrentHashMap<>();
    }

    public String load(Context context, String name) {
        if (cache.contains(name)) return cache.get(name);
        if (name.startsWith("http")) cache.put(name, getModule(context, name));
        if (name.startsWith("assets")) cache.put(name, getAssets(context, name));
        return cache.get(name);
    }

    private String getAssets(Context context, String name) {
        try {
            InputStream is = context.getAssets().open(name.substring(9));
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getModule(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            File file = getFile(context, uri);
            if (file.exists()) return read(file);
            Response response = OkHttp.newCall(url, Headers.of("User-Agent", "Mozilla/5.0")).execute();
            if (response.code() != 200) return "";
            byte[] data = response.body().bytes();
            boolean cache = !uri.getHost().equals("127.0.0.1");
            if (cache) new Thread(() -> write(file, data)).start();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private File getFile(Context context, Uri uri) {
        return new File(context.getCacheDir(), uri.getLastPathSegment());
    }

    private void write(File file, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
            chmod(file);
        } catch (Exception e) {
            e.printStackTrace();
            file.delete();
        }
    }

    private String read(File file) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) sb.append(text).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void chmod(File file) {
        try {
            Process process = Runtime.getRuntime().exec("chmod 777 " + file);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
