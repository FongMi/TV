package com.fongmi.android.tv.api;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.AES;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;

import java.io.BufferedReader;
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
        loadConfig(false, callback);
    }

    public void loadConfig(boolean cache, Callback callback) {
        new Thread(() -> {
            String url = Prefers.getUrl(), pk = ";pk;";
            if (cache) getCacheConfig(url, callback);
            else if (url.contains(pk)) {
                String[] a = url.split(pk);
                if (url.startsWith("http")) decryptWebConfig(a[0], callback, a[1]);
                if (url.startsWith("file")) decryptFileConfig(a[0], callback, a[1]);
            } else if (!url.contains(pk)) {
                if (url.startsWith("http")) decryptWebConfig(url, callback, null);
                if (url.startsWith("file")) decryptFileConfig(url, callback, null);
            } else handler.post(() -> callback.error(0));
        }).start();
    }

    private void decryptFileConfig(String url, Callback callback, String FinalKey) {
        try {
            String content = "", json = "", line = null, ls = System.getProperty("line.separator");
            BufferedReader read = new BufferedReader(new FileReader(FileUtil.getLocal(url)));
            StringBuilder text = new StringBuilder();
            while ((line = read.readLine()) != null) {
                text.append(line);
                text.append(ls);
            }
            text.deleteCharAt(text.length() - 1);
            read.close();
            String reader = text.toString();
            if (AES.isJson(reader)) {
                getFileConfig(reader, callback);
                return;
            } else if (!reader.startsWith("2423")) {
                content = new String(Base64.decode(reader.split("\\*\\*")[1], Base64.DEFAULT));
            } else {
                content = reader;
            }
            if (content.startsWith("2423")) {
                String data2 = content.substring(content.indexOf("2324") + 4, content.length() - 26);
                content = new String(AES.toBytes(content)).toLowerCase();
                String key = AES.rightPading(content.substring(content.indexOf("$#") + 2, content.indexOf("#$")), "0", 16);
                String iv = AES.rightPading(content.substring(content.length() - 13), "0", 16);
                json = AES.CBC(data2, key, iv);
            } else {
                json = AES.ECB(content, FinalKey);
            }
            parseConfig(new Gson().fromJson(json, JsonObject.class), callback);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_get));
        }
    }

    private void decryptWebConfig(String url, Callback callback, String FinalKey) {
        try {
            String content = "", json = "";
            String reader = OKHttp.newCall(url).execute().body().string();
            if (AES.isJson(reader)) {
                getWebConfig(reader, callback);
                return;
            } else if (!reader.startsWith("2423")) {
                content = new String(Base64.decode(reader.split("\\*\\*")[1], Base64.DEFAULT));
            } else {
                content = reader;
            }
            if (content.startsWith("2423")) {
                String data2 = content.substring(content.indexOf("2324") + 4, content.length() - 26);
                content = new String(AES.toBytes(content)).toLowerCase();
                String key = AES.rightPading(content.substring(content.indexOf("$#") + 2, content.indexOf("#$")), "0", 16);
                String iv = AES.rightPading(content.substring(content.length() - 13), "0", 16);
                json = AES.CBC(data2, key, iv);
            } else {
                json = AES.ECB(content, FinalKey);
            }
            parseConfig(new Gson().fromJson(json, JsonObject.class), callback);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_get));
        }
    }

    private void getFileConfig(String url, Callback callback) {
        try {
            parseConfig(new Gson().fromJson(new JsonReader(new FileReader(FileUtil.getLocal(url))), JsonObject.class), callback);
        } catch (Exception e) {
            e.printStackTrace();
            getCacheConfig(url, callback);
        }
    }

    private void getWebConfig(String url, Callback callback) {
        try {
            parseConfig(new Gson().fromJson(OKHttp.newCall(url).execute().body().string(), JsonObject.class), callback);
        } catch (Exception e) {
            e.printStackTrace();
            getCacheConfig(url, callback);
        }
    }

    private void getCacheConfig(String url, Callback callback) {
        String json = Config.find(url).getJson();
        if (!TextUtils.isEmpty(json))
            parseConfig(JsonParser.parseString(json).getAsJsonObject(), callback);
        else handler.post(() -> callback.error(R.string.error_config_get));
    }

    private void parseConfig(JsonObject object, Callback callback) {
        try {
            parseJson(object);
            loader.parseJar("", Json.safeString(object, "spider", ""));
            handler.post(() -> callback.success(object.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private void parseJson(JsonObject object) {
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            Site site = Site.objectFrom(element).sync();
            site.setExt(parseExt(site.getExt()));
            if (site.getKey().equals(Prefers.getHome())) setHome(site);
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
    }

    private String parseExt(String ext) {
        if (ext.startsWith("http")) return ext;
        else if (ext.startsWith("file")) return FileUtil.read(ext);
        else if (ext.endsWith(".json")) return parseExt(Utils.convert(ext));
        return ext;
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
        this.loader.clear();
        this.home = null;
        return this;
    }
}