package com.fongmi.android.tv.api;

import android.content.Context;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

public class JarLoader {

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Spider> spiders;
    private final ConcurrentHashMap<String, Method> methods;
    private String current;

    public JarLoader() {
        this.loaders = new ConcurrentHashMap<>();
        this.spiders = new ConcurrentHashMap<>();
        this.methods = new ConcurrentHashMap<>();
    }

    public void load(File file) {
        loaders.clear();
        spiders.clear();
        methods.clear();
        load("", file);
    }

    public void load(String key, File file) {
        try {
            DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), FileUtil.getCachePath(), null, App.get().getClassLoader());
            Class<?> classInit = loader.loadClass("com.github.catvod.spider.Init");
            Method method = classInit.getMethod("init", Context.class);
            method.invoke(classInit, App.get());
            loaders.put(key, loader);
            Class<?> classProxy = loader.loadClass("com.github.catvod.spider.Proxy");
            methods.put(key, classProxy.getMethod("proxy", Map.class));
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private void parseJar(String key, String jar) throws Exception {
        if (jar.startsWith("http")) {
            load(key, FileUtil.write(FileUtil.getJar(MD5(jar)), OKHttp.newCall(jar).execute().body().bytes()));
        } else if (jar.startsWith("file")) {
            load(key, FileUtil.getLocal(jar));
        } else if (!jar.isEmpty()) {
            parseJar(key, FileUtil.convert(jar));
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            current = MD5(jar);
            api = api.replace("csp_", "");
            if (spiders.containsKey(key)) return spiders.get(key);
            if (!loaders.containsKey(current)) parseJar(current, jar);
            Spider spider = (Spider) loaders.get(current).loadClass("com.github.catvod.spider." + api).newInstance();
            spider.init(App.get(), ext);
            spiders.put(key, spider);
            return spider;
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        try {
            String clsKey = "Json" + key;
            String hotClass = "com.github.catvod.parser." + clsKey;
            Class<?> jsonParserCls = loaders.get("").loadClass(hotClass);
            Method mth = jsonParserCls.getMethod("parse", LinkedHashMap.class, String.class);
            return (JSONObject) mth.invoke(null, jxs, url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        try {
            String clsKey = "Mix" + key;
            String hotClass = "com.github.catvod.parser." + clsKey;
            Class<?> jsonParserCls = loaders.get("").loadClass(hotClass);
            Method mth = jsonParserCls.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
            return (JSONObject) mth.invoke(null, jxs, name, flag, url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object[] proxyInvoke(Map<?, ?> params) {
        try {
            Method proxyFun = methods.get(current);
            if (proxyFun != null) return (Object[]) proxyFun.invoke(null, params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String MD5(String src) {
        try {
            if (TextUtils.isEmpty(src)) return "";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(src.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder sb = new StringBuilder(no.toString(16));
            while (sb.length() < 32) sb.insert(0, "0");
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
