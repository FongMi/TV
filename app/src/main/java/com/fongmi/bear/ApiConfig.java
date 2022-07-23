package com.fongmi.bear;

import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import com.fongmi.bear.bean.Live;
import com.fongmi.bear.bean.Parse;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.net.OKHttp;
import com.fongmi.bear.utils.FileUtil;
import com.fongmi.bear.utils.Json;
import com.fongmi.bear.utils.Prefers;
import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class ApiConfig {

    private final List<String> ads;
    private final List<String> flags;
    private final List<Parse> parses;
    private final List<Live> lives;
    private final List<Site> sites;
    private final JarLoader loader;
    private final Handler handler;
    private Parse parse;
    private Site home;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public ApiConfig() {
        this.ads = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.lives = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.loader = new JarLoader();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void clear() {
        this.ads.clear();
        this.flags.clear();
        this.parses.clear();
        this.lives.clear();
        this.sites.clear();
        this.home = null;
    }

    public void loadConfig(Callback callback) {
        new Thread(() -> {
            String url = Prefers.getUrl();
            if (url.startsWith("file://")) getFileConfig(url, callback);
            else if (Patterns.WEB_URL.matcher(url).matches()) getWebConfig(url, callback);
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
            Response response = OKHttp.newCall(url).execute();
            parseConfig(new Gson().fromJson(response.body().string(), JsonObject.class), callback);
        } catch (IOException e) {
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
        for (JsonElement opt : object.get("sites").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            Site site = new Site();
            site.setKey(obj.get("key").getAsString().trim());
            site.setName(obj.get("name").getAsString().trim());
            site.setType(obj.get("type").getAsInt());
            site.setApi(obj.get("api").getAsString().trim());
            site.setSearchable(Json.safeInt(obj, "searchable", 1));
            site.setSearchable(Json.safeInt(obj, "quickSearch", 1));
            site.setFilterable(Json.safeInt(obj, "filterable", 1));
            site.setExt(Json.safeString(obj, "ext", ""));
            if (site.getExt().startsWith("file://")) site.setExt(FileUtil.read(site.getExt()));
            if (site.getKey().equals(Prefers.getHome())) setHome(site);
            sites.add(site);
        }
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        flags.addAll(Json.safeList(object, "flags"));
        ads.addAll(Json.safeList(object, "ads"));
    }

    private void parseJar(String spider) throws Exception {
        if (spider.startsWith("file://")) {
            loader.load(FileUtil.getLocal(spider));
        } else if (Patterns.WEB_URL.matcher(spider).matches()) {
            Response response = OKHttp.newCall(spider).execute();
            FileUtil.write(FileUtil.getJar(), response.body().bytes());
            loader.load(FileUtil.getJar());
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
}