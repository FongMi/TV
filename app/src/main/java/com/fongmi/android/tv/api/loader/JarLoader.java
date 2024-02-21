package com.fongmi.android.tv.api.loader;

import android.content.Context;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

public class JarLoader {

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Method> methods;
    private final ConcurrentHashMap<String, Spider> spiders;
    private String recent;

    public JarLoader() {
        loaders = new ConcurrentHashMap<>();
        methods = new ConcurrentHashMap<>();
        spiders = new ConcurrentHashMap<>();
    }

    public void clear() {
        for (Spider spider : spiders.values()) spider.destroy();
        loaders.clear();
        methods.clear();
        spiders.clear();
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private void load(String key, File file) {
        loaders.put(key, new DexClassLoader(file.getAbsolutePath(), Path.jar().getAbsolutePath(), null, App.get().getClassLoader()));
        invokeInit(key);
        putProxy(key);
    }

    private void invokeInit(String key) {
        try {
            Class<?> clz = loaders.get(key).loadClass("com.github.catvod.spider.Init");
            Method method = clz.getMethod("init", Context.class);
            method.invoke(clz, App.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void putProxy(String key) {
        try {
            Class<?> clz = loaders.get(key).loadClass("com.github.catvod.spider.Proxy");
            Method method = clz.getMethod("proxy", Map.class);
            methods.put(key, method);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private File download(String url) {
        try {
            return Path.write(Path.jar(url), OkHttp.newCall(url).execute().body().bytes());
        } catch (Exception e) {
            return Path.jar(url);
        }
    }

    public void parseJar(String key, String jar) {
        if (loaders.containsKey(key)) return;
        String[] texts = jar.split(";md5;");
        String md5 = texts.length > 1 ? texts[1].trim() : "";
        jar = texts[0];
        if (md5.length() > 0 && Util.equals(jar, md5)) {
            load(key, Path.jar(jar));
        } else if (jar.startsWith("img+")) {
            load(key, Decoder.getSpider(jar));
        } else if (jar.startsWith("http")) {
            load(key, download(jar));
        } else if (jar.startsWith("file")) {
            load(key, Path.local(jar));
        } else if (jar.startsWith("assets")) {
            parseJar(key, UrlUtil.convert(jar));
        }
    }

    public DexClassLoader getLoader(String key, String jar) {
        if (!loaders.containsKey(key)) parseJar(key, jar);
        return loaders.get(key);
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            String jaKey = Util.md5(jar);
            String spKey = jaKey + key;
            if (spiders.containsKey(spKey)) return spiders.get(spKey);
            if (!loaders.containsKey(jaKey)) parseJar(jaKey, jar);
            Spider spider = (Spider) loaders.get(jaKey).loadClass("com.github.catvod.spider." + api.split("csp_")[1]).newInstance();
            spider.init(App.get(), ext);
            spiders.put(spKey, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        Class<?> clz = loaders.get("").loadClass("com.github.catvod.parser.Json" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class);
        return (JSONObject) method.invoke(null, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        Class<?> clz = loaders.get("").loadClass("com.github.catvod.parser.Mix" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
        return (JSONObject) method.invoke(null, jxs, name, flag, url);
    }

    public Object[] proxyInvoke(Map<String, String> params) {
        try {
            Method method = methods.get(Util.md5(recent));
            if (method == null) return null;
            return (Object[]) method.invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
