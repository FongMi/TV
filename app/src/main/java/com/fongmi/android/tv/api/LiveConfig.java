package com.fongmi.android.tv.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
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

    public static int getHomeIndex() {
        return get().getLives().indexOf(get().getHome());
    }

    public LiveConfig init() {
        this.home = null;
        this.config = Config.live();
        this.lives = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        return this;
    }

    public LiveConfig config(Config config) {
        this.config = config;
        return this;
    }

    public LiveConfig clear() {
        this.home = null;
        this.lives.clear();
        this.lives.addAll(ApiConfig.get().getLives());
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
            if (!Json.valid(json)) parse(json);
            else parse(JsonParser.parseString(json).getAsJsonObject());
            handler.post(callback::success);
        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> callback.error(R.string.error_config_parse));
        }
    }

    private String getText(String url) throws Exception {
        if (url.startsWith("file")) return FileUtil.read(url);
        else if (url.startsWith("http")) return OKHttp.newCall(url).execute().body().string();
        else if (url.endsWith(".txt") || url.endsWith(".m3u")) return getText(Utils.convert(url));
        else if (url.length() > 0 && url.length() % 4 == 0) return getText(new String(Base64.decode(url, Base64.DEFAULT)));
        else return "";
    }

    private void parse(String text) {
        Live live = new Live(config.getUrl());
        LiveParser.start(live, text);
        lives.remove(live);
        lives.add(live);
        setHome(live);
    }

    public List<Live> parse(JsonObject object) {
        List<Live> items = new ArrayList<>();
        if (!object.has("lives")) return Collections.emptyList();
        for (JsonElement element : Json.safeListElement(object, "lives")) items.add(parse(Live.objectFrom(element)));
        if (home == null) setHome(lives.isEmpty() ? new Live() : lives.get(0));
        return items;
    }

    private Live parse(Live live) {
        try {
            if (live.isProxy()) live = new Live(live.getChannels().get(0).getName(), live.getChannels().get(0).getUrls().get(0).split("ext=")[1]);
            if (live.getType() == 0) LiveParser.start(live, getText(live.getUrl()));
            if (live.getGroups().size() > 0 && !lives.contains(live)) lives.add(live);
            if (live.getName().equals(config.getHome())) setHome(live);
            return live;
        } catch (Exception e) {
            e.printStackTrace();
            return new Live();
        }
    }

    public void setKeep(Group group, Channel channel) {
        if (!group.isHidden()) Prefers.putKeep(home.getName() + AppDatabase.SYMBOL + group.getName() + AppDatabase.SYMBOL + channel.getName());
    }

    public void setKeep(List<Group> items) {
        List<String> key = new ArrayList<>();
        for (Keep keep : Keep.getLive()) key.add(keep.getKey());
        for (Group group : items) {
            if (group.isKeep()) continue;
            for (Channel channel : group.getChannel()) {
                if (key.contains(channel.getName())) {
                    items.get(0).add(channel);
                }
            }
        }
    }

    public int[] getKeep(List<Group> items) {
        String[] splits = Prefers.getKeep().split(AppDatabase.SYMBOL);
        if (!home.getName().equals(splits[0])) return new int[]{1, 0};
        for (int i = 0; i < items.size(); i++) {
            Group group = items.get(i);
            if (group.getName().equals(splits[1])) {
                int j = group.find(splits[2]);
                if (j != -1) return new int[]{i, j};
            }
        }
        return new int[]{1, 0};
    }

    public int[] find(String number, List<Group> items) {
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
        return config == null ? Config.live() : config;
    }

    public Live getHome() {
        return home;
    }

    public boolean isEmpty() {
        return getHome() == null || getHome().getGroups().isEmpty();
    }

    public void remove(List<Live> lives) {
        if (lives.contains(home)) home = null;
        this.lives.removeAll(lives);
        lives.clear();
    }

    public void setHome(Live home) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getName()).update();
        for (Live item : lives) item.setActivated(home);
    }
}
