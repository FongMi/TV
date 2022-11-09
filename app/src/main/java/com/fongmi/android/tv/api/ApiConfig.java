package com.fongmi.android.tv.api;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiConfig {

    private List<String> ads;
    private List<String> flags;
    private List<Parse> parses;
    private List<Site> sites;
    private JarLoader jLoader;
    private PyLoader pLoader;
    private Config config;
    private String wall;
    private Parse parse;
    private Site home;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public static int getCid() {
        return get().getConfig().getId();
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static int getHomeIndex() {
        return get().getSites().indexOf(get().getHome());
    }

    public static String getSiteName(String key) {
        return get().getSite(key).getName();
    }

    public ApiConfig init() {
        this.home = null;
        this.wall = null;
        this.config = Config.vod();
        this.ads = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.jLoader = new JarLoader();
        this.pLoader = new PyLoader();
        return this;
    }

    public ApiConfig config(Config config) {
        this.config = config;
        return this;
    }

    public ApiConfig clear() {
        this.home = null;
        this.wall = null;
        this.ads.clear();
        this.sites.clear();
        this.flags.clear();
        this.parses.clear();
        this.jLoader.clear();
        this.pLoader.clear();
        return this;
    }

    public void load(Callback callback) {
        load(false, callback);
    }

    public void load(boolean cache, Callback callback) {
        new Thread(() -> {
            if (cache) loadCache(callback);
            else loadConfig(callback);
        }).start();
    }

    private void loadConfig(Callback callback) {
        try {
            parseConfig(JsonParser.parseString(Decoder.getJson(config.getUrl())).getAsJsonObject(), callback);
        } catch (Exception e) {
            if (config.getUrl().isEmpty()) App.post(() -> callback.error(0));
            else loadCache(callback);
            LiveConfig.get().load();
            e.printStackTrace();
        }
    }

    private void loadCache(Callback callback) {
        if (!TextUtils.isEmpty(config.getJson())) parseConfig(JsonParser.parseString(config.getJson()).getAsJsonObject(), callback);
        else App.post(() -> callback.error(R.string.error_config_get));
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            parseJson(object);
            parseLive(object);
            jLoader.parseJar("", Json.safeString(object, "spider"));
            config.json(object.toString()).update();
            App.post(callback::success);
        } catch (Exception e) {
            e.printStackTrace();
            App.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private void parseJson(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            Site site = Site.objectFrom(element).sync();
            site.setExt(parseExt(site.getExt()));
            if (site.getKey().equals(config.getHome())) setHome(site);
            if (!sites.contains(site)) sites.add(site);
        }
        for (JsonElement element : Json.safeListElement(object, "parses")) {
            Parse parse = Parse.objectFrom(element);
            if (parse.getName().equals(Prefers.getParse())) setParse(parse);
            if (!parses.contains(parse)) parses.add(parse);
        }
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        if (parse == null) setParse(parses.isEmpty() ? new Parse() : parses.get(0));
        flags.addAll(Json.safeListString(object, "flags"));
        ads.addAll(Json.safeListString(object, "ads"));
        setWall(Json.safeString(object, "wallpaper"));
    }

    private void parseLive(JsonObject object) {
        boolean hasLive = object.has("lives");
        if (hasLive) Config.create(config.getUrl(), 1);
        boolean loadApi = hasLive && LiveConfig.get().isSame(config.getUrl());
        if (loadApi) LiveConfig.get().clear().config(Config.find(config.getUrl(), 1).update()).parse(object);
        else LiveConfig.get().load();
    }

    private String parseExt(String ext) {
        if (ext.startsWith("http")) return ext;
        else if (ext.startsWith("file")) return FileUtil.read(ext);
        else if (ext.startsWith("img+")) return Decoder.getExt(ext);
        else if (ext.contains("http") || ext.contains("file")) return ext;
        else if (ext.endsWith(".json") || ext.endsWith(".py")) return parseExt(Utils.convert(config.getUrl(), ext));
        return ext;
    }

    public Spider getCSP(Site site) {
        boolean py = site.getApi().startsWith("py_");
        boolean csp = site.getApi().startsWith("csp_");
        if (py) return pLoader.getSpider(site.getKey(), site.getApi(), site.getExt());
        else if (csp) return jLoader.getSpider(site.getKey(), site.getApi(), site.getExt(), site.getJar());
        else return new SpiderNull();
    }

    public Object[] proxyLocal(Map<?, ?> param) {
        return jLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return jLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return jLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public Site getSite(String key) {
        int index = getSites().indexOf(Site.get(key));
        return index == -1 ? new Site() : getSites().get(index);
    }

    public Parse getParse(String name) {
        int index = getParses().indexOf(Parse.get(name));
        return index == -1 ? null : getParses().get(index);
    }

    public List<Site> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    public List<Parse> getParses() {
        return parses == null ? Collections.emptyList() : parses;
    }

    public List<String> getFlags() {
        return flags == null ? Collections.emptyList() : flags;
    }

    public String getAds() {
        return ads == null ? "" : ads.toString();
    }

    public Config getConfig() {
        return config == null ? Config.vod() : config;
    }

    public Parse getParse() {
        return parse == null ? new Parse() : parse;
    }

    public void setParse(Parse parse) {
        this.parse = parse;
        this.parse.setActivated(true);
        Prefers.putParse(parse.getName());
        for (Parse item : parses) item.setActivated(parse);
    }

    public String getWall() {
        return TextUtils.isEmpty(wall) ? "" : wall;
    }

    private void setWall(String wall) {
        if (Config.wall().getUrl().isEmpty()) WallConfig.get().setUrl(wall);
        this.wall = wall;
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public void setHome(Site home) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getKey()).update();
        for (Site item : getSites()) item.setActivated(home);
    }
}