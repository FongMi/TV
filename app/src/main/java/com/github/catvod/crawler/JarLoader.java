package com.github.catvod.crawler;

import android.content.Context;

import com.fongmi.bear.App;
import com.fongmi.bear.utils.FileUtil;
import com.orhanobut.logger.Logger;

import java.io.FileOutputStream;
import java.lang.reflect.Method;
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
                        Logger.d("自定義爬蟲代碼加載成功！");
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

    public Spider getSpider(String key, String ext) {
        String clsKey = key.replace("csp_", "");
        if (spiders.contains(clsKey)) return spiders.get(clsKey);
        if (classLoader == null) return new SpiderNull();
        try {
            Spider spider = (Spider) classLoader.loadClass("com.github.catvod.spider." + clsKey).newInstance();
            spider.init(App.get(), ext);
            spiders.put(clsKey, spider);
            return spider;
        } catch (Exception e) {
            return new SpiderNull();
        }
    }

    public Object[] proxyInvoke(Map params) {
        try {
            if (proxyFun != null) return (Object[]) proxyFun.invoke(null, params);
        } catch (Exception ignored) {

        }
        return null;
    }
}
