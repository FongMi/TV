package com.github.catvod.crawler;

import android.content.Context;

import com.fongmi.bear.App;
import com.fongmi.bear.utils.FileUtil;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

public class JarLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private DexClassLoader classLoader;
    private Method proxyFun;

    public JarLoader() {
        this.spiders = new ConcurrentHashMap<>();
    }

    public void writeJar(byte[] jarData) {
        try {
            FileOutputStream fos = new FileOutputStream(FileUtil.getJar());
            fos.write(jarData);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(byte[] jarData) {
        try {
            spiders.clear();
            proxyFun = null;
            writeJar(jarData);
            classLoader = new DexClassLoader(FileUtil.getJarPath(), FileUtil.getCachePath(), null, App.get().getClassLoader());
            int count = 0;
            do {
                try {
                    Class<?> classInit = classLoader.loadClass("com.github.catvod.spider.Init");
                    if (classInit != null) {
                        Method method = classInit.getMethod("init", Context.class);
                        method.invoke(classInit, App.get());
                        SpiderDebug.log("自定義爬蟲代碼加載成功！");
                        try {
                            Class<?> proxy = classLoader.loadClass("com.github.catvod.spider.Proxy");
                            proxyFun = proxy.getMethod("proxy", Map.class);
                        } catch (Exception e) {

                        }
                        break;
                    }
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                count++;
            } while (count < 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Spider getSpider(String key, String api, String ext) {
        try {
            api = api.replace("csp_", "");
            if (spiders.containsKey(key)) return spiders.get(key);
            if (classLoader == null) return new SpiderNull();
            Spider spider = (Spider) classLoader.loadClass("com.github.catvod.spider." + api).newInstance();
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
            Class<?> jsonParserCls = classLoader.loadClass(hotClass);
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
            Class<?> jsonParserCls = classLoader.loadClass(hotClass);
            Method mth = jsonParserCls.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
            return (JSONObject) mth.invoke(null, jxs, name, flag, url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object[] proxyInvoke(Map params) {
        try {
            if (proxyFun != null) return (Object[]) proxyFun.invoke(null, params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
