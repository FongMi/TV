package com.fongmi.android.tv.api;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.net.OkHttp;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

public class JsLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private String recent;

    public JsLoader() {
        spiders = new ConcurrentHashMap<>();
    }

    public void clear() {
        for (Spider spider : spiders.values()) spider.destroy();
        this.spiders.clear();
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private File download(String jar) {
        try {
            return FileUtil.write(FileUtil.getJar(jar), OkHttp.newCall(jar).execute().body().bytes());
        } catch (Exception e) {
            return FileUtil.getJar(jar);
        }
    }

    private File getFile(String jar) {
        String[] texts = jar.split(";md5;");
        String md5 = !jar.startsWith("file") && texts.length > 1 ? texts[1].trim() : "";
        jar = texts[0];
        if (jar.startsWith("img+")) {
            return Decoder.getSpider(jar, md5);
        } else if (md5.length() > 0 && FileUtil.equals(jar, md5)) {
            return FileUtil.getJar(jar);
        } else if (jar.startsWith("http")) {
            return download(jar);
        } else if (jar.startsWith("file")) {
            return FileUtil.getLocal(jar);
        } else if (!jar.isEmpty()) {
            return getFile(Utils.convert(ApiConfig.getUrl(), jar));
        } else {
            return null;
        }
    }

    private DexClassLoader dex(String jar) {
        try {
            return jar.isEmpty() ? null : new DexClassLoader(getFile(jar).getAbsolutePath(), FileUtil.getCachePath(), null, App.get().getClassLoader());
        } catch (Exception e) {
            return null;
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            if (spiders.containsKey(key)) return spiders.get(key);
            Spider spider = new com.hiker.drpy.Spider(api, dex(jar));
            spider.init(App.get(), ext);
            spiders.put(key, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    public Object[] proxyInvoke(Map<?, ?> params) {
        try {
            Spider spider = spiders.get(recent);
            if (spider != null) return spider.proxyLocal(params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
