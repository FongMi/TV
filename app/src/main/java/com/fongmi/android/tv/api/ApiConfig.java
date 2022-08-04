package com.fongmi.android.tv.api;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiConfig {

    private List<String> ads;
    private List<String> flags;
    private List<Parse> parses;
    private List<Live> lives;
    private List<Site> sites;
    private JarLoader loader;
    private Handler handler;
    private Parse parse;
    private Site home;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public static String getHomeName() {
        return get().getHome().getName();
    }

    public static String getSiteName(String key) {
        return get().getSite(key).getName();
    }

    public ApiConfig init() {
        this.ads = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.lives = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.loader = new JarLoader();
        this.handler = new Handler(Looper.getMainLooper());
        return this;
    }

    public void loadConfig(Callback callback) {
        new Thread(() -> {
            String url = Prefers.getUrl();
            if (url.startsWith("http")) getWebConfig(url, callback);
            else if (url.startsWith("file")) getFileConfig(url, callback);
            else handler.post(() -> callback.error(0));
        }).start();
    }

    private void getFileConfig(String url, Callback callback) {
        try {
            JsonReader reader = new JsonReader(new FileReader(FileUtil.getLocal(url)));
            parseConfig(new Gson().fromJson(reader, JsonObject.class), callback);
        } catch (Exception e) {
            handler.post(() -> callback.error(R.string.error_config_get));
        }
    }

    private void getWebConfig(String url, Callback callback) {
        try {
            parseConfig(new Gson().fromJson(OKHttp.newCall(url).execute().body().string(), JsonObject.class), callback);
        } catch (Exception e) {
            handler.post(() -> callback.error(R.string.error_config_get));
        }
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            String spider = Json.safeString(object, "spider", "");
            parseJson(object);
            parseJar(spider);
            handler.post(callback::success);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private void parseJson(JsonObject object) {
        for (JsonElement element : object.get("sites").getAsJsonArray()) {
            Site site = Site.objectFrom(element);
            site.setExt(parseExt(site.getExt()));
            if (site.getKey().equals(Prefers.getHome())) setHome(site);
            sites.add(site);
        }
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        flags.addAll(Json.safeList(object, "flags"));
        ads.addAll(Json.safeList(object, "ads"));
    }

    private String parseExt(String ext) {
        if (ext.startsWith("http")) return ext;
        else if (ext.startsWith("file")) return FileUtil.read(ext);
        else if (ext.endsWith(".json")) return parseExt(Utils.convert(ext));
        return ext;
    }

    private void parseJar(String spider) throws Exception {
        if (spider.contains(";md5")) spider = spider.split(";md5")[0];
        if (spider.startsWith("http")) {
            FileUtil.write(FileUtil.getJar(), OKHttp.newCall(spider).execute().body().bytes());
            loader.load(FileUtil.getJar());
        } else if (spider.startsWith("file")) {
            loader.load(FileUtil.getLocal(spider));
        } else if (!spider.isEmpty()) {
            parseJar(Utils.convert(spider));
        }
    }

    public Spider getCSP(Site site) {
        return loader.getSpider(site.getKey(), site.getApi(), site.getExt());
    }

    public Object[] proxyLocal(Map<?, ?> param) {
        return loader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return loader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return loader.jsonExtMix(flag, key, name, jxs, url);
    }

    public Site getSite(String key) {
        int index = sites.indexOf(Site.get(key));
        return index == -1 ? new Site() : sites.get(index);
    }

    public List<Site> getSites() {
        return sites;
    }

    public String getAds() {
        return ads.toString();
    }

    public List<String> getFlags() {
        return flags;
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public void setHome(Site home) {
        this.home = home;
        this.home.setHome(true);
        Prefers.putHome(home.getKey());
    }

    public ApiConfig clear() {
        this.ads.clear();
        this.sites.clear();
        this.lives.clear();
        this.flags.clear();
        this.parses.clear();
        this.home = null;
        return this;
    }

    public void release() {
        this.ads = null;
        this.home = null;
        this.sites = null;
        this.lives = null;
        this.flags = null;
        this.parses = null;
        this.loader = null;
        this.handler = null;
    }
}