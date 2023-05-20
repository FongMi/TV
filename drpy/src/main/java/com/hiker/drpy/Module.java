package com.hiker.drpy;

import android.content.Context;

import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Headers;

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
        if (name.startsWith("http")) cache.put(name, getModule(name));
        if (name.startsWith("assets")) cache.put(name, getAssets(context, name));
        return cache.get(name);
    }

    private String getAssets(Context context, String name) {
        try {
            InputStream is = context.getAssets().open(name.substring(9));
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            return new String(data, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getModule(String url) {
        try {
            return OkHttp.newCall(url, Headers.of("User-Agent", "Mozilla/5.0")).execute().body().string();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static boolean isRemote(String name) {
        return name.startsWith("http://") || name.startsWith("https://") || name.startsWith("assets://");
    }

    public static String convertModuleName(String moduleBaseName, String moduleName) {
        if (moduleName == null || moduleName.length() == 0 || isRemote(moduleName)) {
            return moduleName;
        }
        moduleName = moduleName.replace("//", "/");
        if (moduleName.startsWith("./")) {
            moduleName = moduleName.substring(2);
        }
        if (moduleName.charAt(0) == '/') {
            return moduleName;
        }
        if (moduleBaseName == null || moduleBaseName.length() == 0) {
            return moduleName;
        }
        if (!isRemote(moduleBaseName)) {
            moduleBaseName = moduleBaseName.replace("//", "/");
        }
        if (moduleBaseName.startsWith("./")) {
            moduleBaseName = moduleBaseName.substring(2);
        }
        if (moduleBaseName.equals("/")) {
            return "/" + moduleName;
        }
        if (moduleBaseName.endsWith("/")) {
            return moduleBaseName + moduleName;
        }
        String[] parentSplit = moduleBaseName.split("/");
        String[] pathSplit = moduleName.split("/");
        List<String> parentStack = new ArrayList<>();
        List<String> pathStack = new ArrayList<>();
        Collections.addAll(parentStack, parentSplit);
        Collections.addAll(pathStack, pathSplit);
        while (!pathStack.isEmpty()) {
            String tmp = pathStack.get(0);
            if (tmp.equals("..")) {
                pathStack.remove(0);
                parentStack.remove(parentStack.size() - 1);
            } else {
                break;
            }
        }
        if (!parentStack.isEmpty()) {
            parentStack.remove(parentStack.size() - 1);
        }
        StringBuilder builder = new StringBuilder();
        if (moduleBaseName.startsWith("/")) {
            builder.append("/");
        }
        for (String it : parentStack) {
            builder.append(it).append("/");
        }
        for (String it : pathStack) {
            builder.append(it).append("/");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
