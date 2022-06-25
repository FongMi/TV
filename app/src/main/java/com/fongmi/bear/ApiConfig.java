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
    private final List<Parse> parses;
    private final List<Live> lives;
    private final List<Site> sites;
    private final Handler handler;
    private String spider;
    private Parse parse;
    private Site home;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public ApiConfig() {
        this.home = new Site();
        this.sites = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.lives = new ArrayList<>();
        this.jarLoader = new JarLoader();
        this.handler = new Handler(Looper.getMainLooper());
    }

    private void clear() {
        this.sites.clear();
        this.parses.clear();
        this.lives.clear();
        this.home = new Site();
    }

    public void loadConfig(Callback callback) {
        OKHttp.get().client().newCall(new Request.Builder().url(Prefers.getUrl()).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    clear();
                    parseJson(response.body().string());
                    loadJar();
                    handler.post(callback::success);
                } catch (Exception e) {
                    handler.post(() -> callback.error("解析配置失敗"));
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> callback.error("取得配置失敗"));
            }
        });
    }

    private void parseJson(String json) {
        JsonObject object = new Gson().fromJson(json, JsonObject.class);
        spider = Json.safeString(object, "spider", "");
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
            sites.add(site);
        }
        if (sites.size() > 0) {
            setHome(sites.get(0));
        }
    }

    private void loadJar() throws IOException {
        Request request = new Request.Builder().url(spider).build();
        Response response = OKHttp.get().client().newCall(request).execute();
        jarLoader.load(response.body().bytes());
    }

    public Spider getCSP(Site site) {
        return jarLoader.getSpider(site.getApi(), site.getExt());
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
        for (Site item : sites) if (item.getKey().equals(key)) return item;
        return new Site();
    }

    public List<Site> getSites() {
        return sites;
    }

    public Site getHome() {
        return home;
    }

    public void setHome(Site home) {
        this.home = home;
    }
}