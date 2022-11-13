package com.fongmi.android.tv.api;

import android.content.Context;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OKHttp;
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
    private String current;

    public JarLoader() {
        this.loaders = new ConcurrentHashMap<>();
        this.methods = new ConcurrentHashMap<>();
        this.spiders = new ConcurrentHashMap<>();
        this.current = "";
    }

    public void clear() {
        this.loaders.clear();
        this.methods.clear();
        this.spiders.clear();
        this.current = "";
    }

    public void load(String key, File file) throws Exception {
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
            return FileUtil.write(FileUtil.getJar(jar), OKHttp.newCall(jar).execute().body().bytes());
        } catch (Exception e) {
            return FileUtil.getJar(jar);
        }
    }

    public void parseJar(String key, String jar) throws Exception {
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
            String spKey = (current = Utils.getMD5(jar)) + key;
            if (spiders.containsKey(spKey)) return spiders.get(spKey);
            if (!loaders.containsKey(current)) parseJar(current, jar);
            Spider spider = (Spider) loaders.get(current).loadClass("com.github.catvod.spider." + api.split("csp_")[1]).newInstance();
            spider.init(App.get(), ext);
            spiders.put(spKey, spider);
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
}
