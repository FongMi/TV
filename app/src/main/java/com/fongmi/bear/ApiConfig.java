package com.fongmi.bear;

import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import androidx.annotation.NonNull;

import com.fongmi.bear.bean.Live;
import com.fongmi.bear.bean.Parse;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.net.OKHttp;
import com.fongmi.bear.utils.Json;
import com.fongmi.bear.utils.Prefers;
import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;
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
        if (Prefers.getUrl().isEmpty() || !Patterns.WEB_URL.matcher(Prefers.getUrl()).matches()) {
            handler.post(() -> callback.error(0));
            return;
        }
        OKHttp.get().client().newCall(new Request.Builder().url(Prefers.getUrl()).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JsonObject object = new Gson().fromJson(response.body().string(), JsonObject.class);
                    String spider = Json.safeString(object, "spider", "");
                    parseJson(object);
                    loadJar(spider);
                    handler.post(callback::success);
                } catch (Exception e) {
                    handler.post(() -> callback.error(R.string.error_config_parse));
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.error(R.string.error_config_get));
            }
        });
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
            if (site.getKey().equals(Prefers.getHome())) setHome(site);
            sites.add(site);
        }
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        flags.addAll(Json.safeList(object, "flags"));
        ads.addAll(Json.safeList(object, "ads"));
    }

    private void loadJar(String spider) throws Exception {
        Request request = new Request.Builder().url(spider).build();
        Response response = OKHttp.get().client().newCall(request).execute();
        loader.load(response.body().bytes());
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