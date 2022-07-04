package com.fongmi.bear;

import android.os.Handler;
import android.os.Looper;

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

    private final JarLoader jarLoader;
    private final List<String> flags;
    private final List<Parse> parses;
    private final List<Live> lives;
    private final List<Site> sites;
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
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.lives = new ArrayList<>();
        this.jarLoader = new JarLoader();
        this.handler = new Handler(Looper.getMainLooper());
    }

    private void clear() {
        this.sites.clear();
        this.flags.clear();
        this.parses.clear();
        this.lives.clear();
        this.home = null;
    }

    public void loadConfig(Callback callback) {
        if (Prefers.getUrl().isEmpty()) {
            handler.post(() -> callback.error(""));
            return;
        }
        OKHttp.get().client().newCall(new Request.Builder().url(Prefers.getUrl()).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    clear();
                    JsonObject object = new Gson().fromJson(response.body().string(), JsonObject.class);
                    String spider = Json.safeString(object, "spider", "");
                    parseJson(object);
                    loadJar(spider);
                    handler.post(callback::success);
                } catch (Exception e) {
                    handler.post(() -> callback.error("配置解析失敗"));
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.error("配置取得失敗"));
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
        if (home == null) {
            setHome(sites.isEmpty() ? new Site() : sites.get(0));
        }
        flags.addAll(Json.safeList(object, "flags"));
    }

    private void loadJar(String spider) throws IOException {
        Request request = new Request.Builder().url(spider).build();
        Response response = OKHttp.get().client().newCall(request).execute();
        jarLoader.load(response.body().bytes());
    }

    public Spider getCSP(Site site) {
        return jarLoader.getSpider(site.getKey(), site.getApi(), site.getExt());
    }

    public Object[] proxyLocal(Map<?, ?> param) {
        return jarLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public Site getSite(String key) {
        int index = sites.indexOf(Site.get(key));
        return index == -1 ? new Site() : sites.get(index);
    }

    public List<Site> getSites() {
        return sites;
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