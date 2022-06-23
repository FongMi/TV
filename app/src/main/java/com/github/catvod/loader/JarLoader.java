package com.github.catvod.loader;

import com.fongmi.bear.App;
import com.fongmi.bear.utils.FileUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.io.FileOutputStream;

import dalvik.system.DexClassLoader;

public class JarLoader {

    private DexClassLoader classLoader;

    private static class Loader {
        static volatile JarLoader INSTANCE = new JarLoader();
    }

    public static JarLoader get() {
        return Loader.INSTANCE;
    }

    public void load(byte[] jarData) {
        try {
            FileOutputStream fos = new FileOutputStream(FileUtil.getCacheFile("spider.jar"));
            fos.write(jarData);
            fos.flush();
            fos.close();
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            classLoader = new DexClassLoader(FileUtil.getJarPath(), FileUtil.getCachePath(), null, App.get().getClassLoader());
            int count = 0;
            do {
                try {
                    if (classLoader.loadClass("com.github.catvod.spider.Init") != null) {
                        break;
                    }
                    Thread.sleep(200);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
                count++;
            } while (count < 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Spider getSpider(String key) {
        if (classLoader == null) return new SpiderNull();
        try {
            return (Spider) classLoader.loadClass("com.github.catvod.spider." + key).newInstance();
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return new SpiderNull();
    }
}
