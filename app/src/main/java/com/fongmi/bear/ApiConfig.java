package com.fongmi.bear;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class ApiConfig {

    private List<Site> sites;
    private List<Parse> parses;
    private List<Live> channels;
    private Parse defaultParse;
    private JarLoader jarLoader;
    private String spider;
    private Site home;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public ApiConfig() {
        this.sites = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.channels = new ArrayList<>();
        this.jarLoader = new JarLoader();
    }

    public void loadConfig(Callback callback) {
        OKHttp.get().client().newCall(new Request.Builder().url(Prefers.getUrl()).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    parseJson(response.body().string());
                    loadJar();
                    callback.success();
                } catch (Exception e) {
                    callback.error("解析配置失敗");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.error("無法取得配置");
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
        if (sites.size() > 0) home = sites.get(0);
    }

    private void loadJar() throws IOException {
        Request request = new Request.Builder().url(spider).build();
        Response response = OKHttp.get().client().newCall(request).execute();
        jarLoader.load(response.body().bytes());
    }

    public Spider getCSP(Site site) {
        return jarLoader.getSpider(site.getApi(), site.getExt());
    }

    public Site getHome() {
        return home;
    }
}