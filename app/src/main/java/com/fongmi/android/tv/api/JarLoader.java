package com.fongmi.android.tv.api;

import android.content.Context;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

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
    private String jar;

    public JarLoader() {
        this.loaders = new ConcurrentHashMap<>();
        this.methods = new ConcurrentHashMap<>();
        this.spiders = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.loaders.clear();
        this.methods.clear();
        this.spiders.clear();
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    private void load(String key, File file) throws Throwable {
        DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), FileUtil.getCachePath(), null, App.get().getClassLoader());
        Class<?> classInit = loader.loadClass("com.github.catvod.spider.Init");
        Method method = classInit.getMethod("init", Context.class);
        method.invoke(classInit, App.get());
        loaders.put(key, loader);
        putProxy(key);
    }

    private void putProxy(String key) {
        try {
            Class<?> classProxy = loaders.get(key).loadClass("com.github.catvod.spider.Proxy");
            methods.put(key, classProxy.getMethod("proxy", Map.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File download(String jar) {
        try {
            return FileUtil.write(FileUtil.getJar(jar), OkHttp.newCall(jar).execute().body().bytes());
        } catch (Exception e) {
            return FileUtil.getJar(jar);
        }
    }

    public void parseJar(String key, String jar) throws Throwable {
        String[] texts = jar.split(";md5;");
        String md5 = !jar.startsWith("file") && texts.length > 1 ? texts[1].trim() : "";
        jar = texts[0];
        if (jar.startsWith("img+")) {
            load(key, Decoder.getSpider(jar, md5));
        } else if (md5.length() > 0 && FileUtil.equals(jar, md5)) {
            load(key, FileUtil.getJar(jar));
        } else if (jar.startsWith("http")) {
            load(key, download(jar));
        } else if (jar.startsWith("file")) {
            load(key, FileUtil.getLocal(jar));
        } else if (!jar.isEmpty()) {
            parseJar(key, Utils.convert(ApiConfig.getUrl(), jar));
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            String jaKey = Utils.getMd5(jar);
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

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Exception {
        Class<?> clz = loaders.get("").loadClass("com.github.catvod.parser.Json" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class);
        return (JSONObject) method.invoke(null, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Exception {
        Class<?> clz = loaders.get("").loadClass("com.github.catvod.parser.Mix" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
        return (JSONObject) method.invoke(null, jxs, name, flag, url);
    }

    public Object[] proxyInvoke(Map<?, ?> params) {
        try {
            Method method = methods.get(Utils.getMd5(jar));
            if (method != null) return (Object[]) method.invoke(null, params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
