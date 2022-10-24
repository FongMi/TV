package com.fongmi.android.tv.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Prefers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class LiveConfig {

    private List<Live> lives;
    private Handler handler;
    private Config config;
    private Live home;

    private static class Loader {
        static volatile LiveConfig INSTANCE = new LiveConfig();
    }

    public static LiveConfig get() {
        return Loader.INSTANCE;
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public LiveConfig init() {
        this.config = Config.live();
        this.lives = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        return this;
    }

    public void load() {
        load(new Callback());
    }

    public void load(Callback callback) {
        new Thread(() -> loadConfig(callback)).start();
    }

    private void loadConfig(Callback callback) {
        try {
            parseConfig(Decoder.getJson(config.getUrl()), callback);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(config.getUrl().isEmpty() ? 0 : R.string.error_config_get));
        }
    }

    private void parseConfig(String json, Callback callback) {
        try {
            if (!Json.valid(json)) parseTxt(json);
            else parseJson(JsonParser.parseString(json).getAsJsonObject());
            handler.post(callback::success);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private void parseTxt(String txt) {
        Live live = new Live(config.getUrl());
        parse(live, txt);
        lives.add(live);
        setHome(live);
    }

    public void parseJson(JsonObject object) {
        if (!object.has("lives")) return;
        for (JsonElement element : Json.safeListElement(object, "lives")) parse(Live.objectFrom(element));
        if (home == null) setHome(lives.isEmpty() ? new Live() : lives.get(0));
    }

    public void parse(Live live) {
        try {
            if (live.isProxy()) live = new Live(live.getChannels().get(0).getName(), live.getChannels().get(0).getUrl().split("ext=")[1]);
            if (live.getType() == 0) parse(live, getTxt(live.getUrl()));
            if (live.getGroups().size() > 0) lives.add(live);
            if (live.getName().equals(config.getHome())) setHome(live);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTxt(String url) throws Exception {
        if (url.startsWith("file")) return FileUtil.read(url);
        else if (url.startsWith("http")) return OKHttp.newCall(url).execute().body().string();
        else if (url.length() % 4 == 0) return getTxt(new String(Base64.decode(url, Base64.DEFAULT)));
        else return "";
    }

    private void parse(Live live, String txt) {
        int number = 0;
        for (String line : txt.split("\n")) {
            String[] split = line.split(",");
            if (split.length < 2) continue;
            if (line.contains("#genre#")) {
                live.getGroups().add(new Group(split[0]));
            }
            if (split[1].contains("://")) {
                Group group = live.getGroups().get(live.getGroups().size() - 1);
                Channel channel = new Channel(split[0], split[1].split("#"));
                int index = group.getChannel().indexOf(channel);
                if (index != -1) group.getChannel().get(index).getUrls().addAll(channel.getUrls());
                else group.getChannel().add(channel.setNumber(++number));
            }
        }
    }

    public void setKeep(Group group, Channel channel) {
        Prefers.putKeep(home.getName() + AppDatabase.SYMBOL + group.getName() + AppDatabase.SYMBOL + channel.getName());
    }

    public int[] getKeep() {
        String[] splits = Prefers.getKeep().split(AppDatabase.SYMBOL);
        if (!home.getName().equals(splits[0])) return new int[]{-1, -1};
        for (int i = 0; i < home.getGroups().size(); i++) {
            Group group = home.getGroups().get(i);
            if (group.getName().equals(splits[1])) {
                int j = group.find(splits[2]);
                if (j != -1) return new int[]{i, j};
            }
        }
        return new int[]{-1, -1};
    }

    public int[] find(String number) {
        List<Group> items = home.getGroups();
        for (int i = 0; i < items.size(); i++) {
            int j = items.get(i).find(Integer.parseInt(number));
            if (j != -1) return new int[]{i, j};
        }
        return new int[]{-1, -1};
    }

    public List<Live> getLives() {
        return lives;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Live getHome() {
        return home;
    }

    public void setHome(Live home) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getName()).update();
        for (Live item : lives) item.setActivated(home);
    }

    public LiveConfig clear() {
        this.lives.clear();
        this.home = null;
        return this;
    }
}
