package com.fongmi.android.tv.api;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.utils.Json;
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

    private List<Site> sites;
    private List<Parse> parses;
    private List<String> flags;
    private JarLoader jarLoader;
    private PyLoader pyLoader;
    private JsLoader jsLoader;
    private Config config;
    private Parse parse;
    private String wall;
    private String ads;
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

    public static boolean hasPush() {
        return get().getSite("push_agent") != null;
    }

    public static boolean hasParse() {
        return get().getParses().size() > 0;
    }

    public ApiConfig init() {
        this.ads = null;
        this.wall = null;
        this.home = null;
        this.parse = null;
        this.config = Config.vod();
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.jarLoader = new JarLoader();
        this.pyLoader = new PyLoader();
        this.jsLoader = new JsLoader();
        return this;
    }

    public ApiConfig config(Config config) {
        this.config = config;
        return this;
    }

    public ApiConfig clear() {
        this.ads = null;
        this.wall = null;
        this.home = null;
        this.parse = null;
        this.sites.clear();
        this.flags.clear();
        this.parses.clear();
        this.jarLoader.clear();
        this.pyLoader.clear();
        this.jsLoader.clear();
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
            checkJson(JsonParser.parseString(Decoder.getJson(config.getUrl())).getAsJsonObject(), callback);
        } catch (Exception e) {
            if (config.getUrl().isEmpty()) App.post(() -> callback.error(0));
            else loadCache(callback);
            LiveConfig.get().load();
            e.printStackTrace();
        }
    }

    private void loadCache(Callback callback) {
        if (!TextUtils.isEmpty(config.getJson())) checkJson(JsonParser.parseString(config.getJson()).getAsJsonObject(), callback);
        else App.post(() -> callback.error(R.string.error_config_get));
    }

    private void checkJson(JsonObject object, Callback callback) {
        if (object.has("urls")) {
            parseDepot(object, callback);
        } else {
            parseConfig(object, callback);
        }
    }

    private void parseDepot(JsonObject object, Callback callback) {
        List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls").toString());
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) configs.add(Config.find(item, 0));
        Config.delete(config.getUrl());
        config = configs.get(0);
        loadConfig(callback);
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            initSite(object);
            initLive(object);
            initParse(object);
            initOther(object);
            jarLoader.parseJar("", Json.safeString(object, "spider"));
            config.json(object.toString()).update();
            App.post(callback::success);
        } catch (Exception e) {
            e.printStackTrace();
            App.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private void initSite(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            Site site = Site.objectFrom(element).sync();
            site.setApi(parseApi(site.getApi()));
            site.setExt(parseExt(site.getExt()));
            if (site.getKey().equals(config.getHome())) setHome(site);
            if (!sites.contains(site)) sites.add(site);
        }
    }

    private void initLive(JsonObject object) {
        boolean hasLive = object.has("lives");
        if (hasLive) Config.create(config.getUrl(), 1);
        boolean loadApi = hasLive && LiveConfig.get().isSame(config.getUrl());
        if (loadApi) LiveConfig.get().clear().config(Config.find(config.getUrl(), 1).update()).parse(object);
        else LiveConfig.get().load();
    }

    private void initParse(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "parses")) {
            Parse parse = Parse.objectFrom(element);
            if (parse.getName().equals(config.getParse()) && parse.getType() > 1) setParse(parse);
            if (!parses.contains(parse)) parses.add(parse);
        }
    }

    private void initOther(JsonObject object) {
        if (parses.size() > 0) parses.add(0, Parse.god());
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        if (parse == null) setParse(parses.isEmpty() ? new Parse() : parses.get(0));
        setFlags(Json.safeListString(object, "flags"));
        setWall(Json.safeString(object, "wallpaper"));
        setAds(Json.safeListString(object, "ads"));
    }

    private String parseApi(String api) {
        if (TextUtils.isEmpty(api)) return api;
        if (api.startsWith("http")) return api;
        if (api.startsWith("file")) return Utils.convert(api);
        if (api.endsWith(".js")) return parseApi(Utils.convert(config.getUrl(), api));
        return api;
    }

    private String parseExt(String ext) {
        if (TextUtils.isEmpty(ext)) return ext;
        if (ext.startsWith("http")) return ext;
        if (ext.startsWith("file")) return Utils.convert(ext);
        if (ext.startsWith("img+")) return Decoder.getExt(ext);
        if (ext.contains("http") || ext.contains("file")) return ext;
        if (ext.endsWith(".txt") || ext.endsWith(".json") || ext.endsWith(".py") || ext.endsWith(".js")) return parseExt(Utils.convert(config.getUrl(), ext));
        return ext;
    }

    public Spider getCSP(Site site) {
        boolean js = site.getApi().contains(".js");
        boolean py = site.getApi().startsWith("py_");
        boolean csp = site.getApi().startsWith("csp_");
        if (js) return jsLoader.getSpider(site.getKey(), site.getApi(), site.getExt());
        if (py) return pyLoader.getSpider(site.getKey(), site.getApi(), site.getExt());
        if (csp) return jarLoader.getSpider(site.getKey(), site.getApi(), site.getExt(), site.getJar());
        else return new SpiderNull();
    }

    public void setJar(String key) {
        jarLoader.setJar(key);
    }

    public Object[] proxyLocal(Map<?, ?> param) {
        return jarLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Exception {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Exception {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
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

    public List<Parse> getParses(int type) {
        List<Parse> items = new ArrayList<>();
        for (Parse item : getParses()) if (item.getType() == type) items.add(item);
        return items;
    }

    public List<Parse> getParses(int type, String flag) {
        List<Parse> items = new ArrayList<>();
        for (Parse item : getParses(type)) if (item.getExt().getFlag().contains(flag)) items.add(item);
        if (items.isEmpty()) items.addAll(getParses(type));
        return items;
    }

    public List<String> getFlags() {
        return flags == null ? Collections.emptyList() : flags;
    }

    private void setFlags(List<String> flags) {
        this.flags.addAll(flags);
    }

    public String getAds() {
        return TextUtils.isEmpty(ads) ? "" : ads;
    }

    private void setAds(List<String> ads) {
        this.ads = TextUtils.join(",", ads);
    }

    public Config getConfig() {
        return config == null ? Config.vod() : config;
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

    public Parse getParse() {
        return parse == null ? new Parse() : parse;
    }

    public void setParse(Parse parse) {
        this.parse = parse;
        this.parse.setActivated(true);
        config.parse(parse.getName()).update();
        for (Parse item : getParses()) item.setActivated(parse);
    }
}