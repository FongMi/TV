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
import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;

import java.io.FileReader;
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
    private List<Live> lives;
    private List<Site> sites;
    private JarLoader loader;
    private Handler handler;
    private Parse parse;
    private Site home;
    private int cid;

    private static class Loader {
        static volatile ApiConfig INSTANCE = new ApiConfig();
    }

    public static ApiConfig get() {
        return Loader.INSTANCE;
    }

    public static String getHomeName() {
        return get().getHome().getName();
    }

    public static int getHomeIndex() {
        return get().getSites().indexOf(get().getHome());
    }

    public static String getSiteName(String key) {
        return get().getSite(key).getName();
    }

    public static int getCid() {
        return get().cid;
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
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_get));
        }
    }

    private void getWebConfig(String url, Callback callback) {
        try {
            parseConfig(new Gson().fromJson(OKHttp.newCall(url).execute().body().string(), JsonObject.class), callback);
        } catch (Exception e) {
            e.printStackTrace();
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
            Site site = Site.objectFrom(element).sync();
            site.setExt(parseExt(site.getExt()));
            if (site.getKey().equals(Prefers.getHome())) setHome(site);
            if (!sites.contains(site)) sites.add(site);
        }
        for (JsonElement element : object.get("parses").getAsJsonArray()) {
            Parse parse = Parse.objectFrom(element);
            if (parse.getName().equals(Prefers.getParse())) setParse(parse);
            if (!parses.contains(parse)) parses.add(parse);
        }
        if (home == null) setHome(sites.isEmpty() ? new Site() : sites.get(0));
        if (parse == null) setParse(parses.isEmpty() ? new Parse() : parses.get(0));
        flags.addAll(Json.safeList(object, "flags"));
        ads.addAll(Json.safeList(object, "ads"));
    }

    private String parseExt(String ext) {
        if (ext.startsWith("http")) return ext;
        else if (ext.startsWith("file")) return FileUtil.read(ext);
        else if (ext.endsWith(".json")) return parseExt(FileUtil.convert(ext));
        return ext;
    }

    private void parseJar(String spider) throws Exception {
        String[] texts = spider.split(";md5;");
        String md5 = spider.startsWith("http") && texts.length > 1 ? texts[1].trim() : "";
        String url = texts[0];
        if (md5.length() > 0 && FileUtil.equals(md5)) {
            loader.load(FileUtil.getJar());
        } else if (url.startsWith("http")) {
            loader.load(FileUtil.write(FileUtil.getJar(), OKHttp.newCall(url).execute().body().bytes()));
        } else if (url.startsWith("file")) {
            loader.load(FileUtil.getLocal(url));
        } else if (!url.isEmpty()) {
            parseJar(FileUtil.convert(url));
        }
    }

    public Spider getCSP(Site site) {
        return loader.getSpider(site.getKey(), site.getApi(), site.getExt(), site.getJar());
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

    public Parse getParse(String name) {
        int index = parses.indexOf(Parse.get(name));
        return index == -1 ? null : parses.get(index);
    }

    public List<Site> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    public List<Parse> getParses() {
        return parses == null ? Collections.emptyList() : parses;
    }

    public String getAds() {
        return ads == null ? "" : ads.toString();
    }

    public List<String> getFlags() {
        return flags == null ? Collections.emptyList() : flags;
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public void setHome(Site home) {
        this.home = home;
        this.home.setActivated(true);
        Prefers.putHome(home.getKey());
        for (Site item : sites) item.setActivated(home);
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

    public void setCid(int cid) {
        this.cid = cid;
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
}