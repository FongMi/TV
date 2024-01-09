package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.api.loader.JarLoader;
import com.fongmi.android.tv.api.loader.JsLoader;
import com.fongmi.android.tv.api.loader.PyLoader;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
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

public class VodConfig {

    private List<Doh> doh;
    private List<Rule> rules;
    private List<Site> sites;
    private List<Parse> parses;
    private List<String> flags;
    private JarLoader jarLoader;
    private PyLoader pyLoader;
    private JsLoader jsLoader;
    private boolean loadLive;
    private Config config;
    private Parse parse;
    private String wall;
    private String ads;
    private Site home;

    private static class Loader {
        static volatile VodConfig INSTANCE = new VodConfig();
    }

    public static VodConfig get() {
        return Loader.INSTANCE;
    }

    public static int getCid() {
        return get().getConfig().getId();
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static int getHomeIndex() {
        return get().getSites().indexOf(get().getHome());
    }

    public static boolean hasParse() {
        return get().getParses().size() > 0;
    }

    public static void load(Config config, Callback callback) {
        get().clear().config(config).load(callback);
    }

    public VodConfig init() {
        this.ads = null;
        this.wall = null;
        this.home = null;
        this.parse = null;
        this.config = Config.vod();
        this.doh = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.parses = new ArrayList<>();
        this.jarLoader = new JarLoader();
        this.pyLoader = new PyLoader();
        this.jsLoader = new JsLoader();
        this.loadLive = false;
        return this;
    }

    public VodConfig config(Config config) {
        this.config = config;
        return this;
    }

    public VodConfig clear() {
        this.ads = null;
        this.wall = null;
        this.home = null;
        this.parse = null;
        this.doh.clear();
        this.rules.clear();
        this.sites.clear();
        this.flags.clear();
        this.parses.clear();
        this.jarLoader.clear();
        this.pyLoader.clear();
        this.jsLoader.clear();
        this.loadLive = true;
        return this;
    }

    public void load(Callback callback) {
        App.execute(() -> loadConfig(callback));
    }

    private void loadConfig(Callback callback) {
        try {
            checkJson(JsonParser.parseString(Decoder.getJson(config.getUrl())).getAsJsonObject(), callback);
        } catch (Throwable e) {
            if (TextUtils.isEmpty(config.getUrl())) App.post(() -> callback.error(""));
            else loadCache(callback, e);
            e.printStackTrace();
        }
    }

    private void loadCache(Callback callback, Throwable e) {
        if (!TextUtils.isEmpty(config.getJson())) checkJson(JsonParser.parseString(config.getJson()).getAsJsonObject(), callback);
        else App.post(() -> callback.error(Notify.getError(R.string.error_config_get, e)));
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
            initParse(object);
            initOther(object);
            if (loadLive && object.has("lives")) initLive(object);
            jarLoader.parseJar("", Json.safeString(object, "spider"));
            config.json(object.toString()).update();
            App.post(callback::success);
        } catch (Throwable e) {
            e.printStackTrace();
            App.post(() -> callback.error(Notify.getError(R.string.error_config_parse, e)));
        }
    }

    private void initSite(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            Site site = Site.objectFrom(element);
            if (sites.contains(site)) continue;
            site.setApi(parseApi(site.getApi()));
            site.setExt(parseExt(site.getExt()));
            sites.add(site.sync());
        }
        for (Site site : sites) {
            if (site.getKey().equals(config.getHome())) {
                setHome(site);
            }
        }
    }

    private void initLive(JsonObject object) {
        Config temp = Config.find(config, 1);
        boolean sync = LiveConfig.get().needSync(config.getUrl());
        if (sync) LiveConfig.get().clear().config(temp).parse(object);
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
        setRules(Rule.arrayFrom(object.getAsJsonArray("rules")));
        setDoh(Doh.arrayFrom(object.getAsJsonArray("doh")));
        setFlags(Json.safeListString(object, "flags"));
        setWall(Json.safeString(object, "wallpaper"));
        setAds(Json.safeListString(object, "ads"));
    }

    private String parseApi(String api) {
        if (api.startsWith("file") || api.startsWith("assets")) return UrlUtil.convert(api);
        return api;
    }

    private String parseExt(String ext) {
        if (ext.startsWith("file") || ext.startsWith("assets")) return UrlUtil.convert(ext);
        if (ext.startsWith("img+")) return Decoder.getExt(ext);
        return ext;
    }

    public Spider getSpider(Site site) {
        boolean js = site.getApi().contains(".js");
        boolean py = site.getApi().contains(".py");
        boolean csp = site.getApi().startsWith("csp_");
        if (py) return pyLoader.getSpider(site.getKey(), site.getApi(), site.getExt());
        else if (js) return jsLoader.getSpider(site.getKey(), site.getApi(), site.getExt(), site.getJar());
        else if (csp) return jarLoader.getSpider(site.getKey(), site.getApi(), site.getExt(), site.getJar());
        else return new SpiderNull();
    }

    public void setRecent(Site site) {
        boolean js = site.getApi().contains(".js");
        boolean py = site.getApi().contains(".py");
        boolean csp = site.getApi().startsWith("csp_");
        if (js) jsLoader.setRecent(site.getKey());
        else if (py) pyLoader.setRecent(site.getKey());
        else if (csp) jarLoader.setRecent(site.getJar());
    }

    public Object[] proxyLocal(Map<String, String> params) {
        if (params.containsKey("do") && params.get("do").equals("js")) {
            return jsLoader.proxyInvoke(params);
        } else if (params.containsKey("do") && params.get("do").equals("py")) {
            return pyLoader.proxyInvoke(params);
        } else {
            return jarLoader.proxyInvoke(params);
        }
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public List<Doh> getDoh() {
        List<Doh> items = Doh.get(App.get());
        if (doh == null) return items;
        items.removeAll(doh);
        items.addAll(doh);
        return items;
    }

    public void setDoh(List<Doh> doh) {
        this.doh = doh;
    }

    public List<Rule> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    public void setRules(List<Rule> rules) {
        for (Rule rule : rules) if ("proxy".equals(rule.getName())) OkHttp.selector().setHosts(rule.getHosts());
        rules.remove(Rule.create("proxy"));
        this.rules = rules;
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

    public Parse getParse() {
        return parse == null ? new Parse() : parse;
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public String getWall() {
        return TextUtils.isEmpty(wall) ? "" : wall;
    }

    public Parse getParse(String name) {
        int index = getParses().indexOf(Parse.get(name));
        return index == -1 ? null : getParses().get(index);
    }

    public Site getSite(String key) {
        int index = getSites().indexOf(Site.get(key));
        return index == -1 ? new Site() : getSites().get(index);
    }

    public void setParse(Parse parse) {
        this.parse = parse;
        this.parse.setActivated(true);
        config.parse(parse.getName()).update();
        for (Parse item : getParses()) item.setActivated(parse);
    }

    public void setHome(Site home) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getKey()).update();
        for (Site item : getSites()) item.setActivated(home);
    }

    private void setWall(String wall) {
        this.wall = wall;
        boolean load = !TextUtils.isEmpty(wall) && WallConfig.get().needSync(wall);
        if (load) WallConfig.get().config(Config.find(wall, config.getName(), 2).update());
    }
}