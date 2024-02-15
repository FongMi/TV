package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.utils.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class LiveConfig {

    private List<Live> lives;
    private Config config;
    private boolean sync;
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

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static String getResp() {
        return get().getHome().getCore().getResp();
    }

    public static int getHomeIndex() {
        return get().getLives().indexOf(get().getHome());
    }

    public static boolean isOnly() {
        return get().getLives().size() == 1;
    }

    public static boolean isEmpty() {
        return get().getHome().isEmpty();
    }

    public static boolean hasUrl() {
        return getUrl() != null && getUrl().length() > 0;
    }

    public static void load(Config config, Callback callback) {
        get().clear().config(config).load(callback);
    }

    public LiveConfig init() {
        this.home = null;
        return config(Config.live());
    }

    public LiveConfig config(Config config) {
        this.config = config;
        if (config.getUrl() == null) return this;
        this.sync = config.getUrl().equals(VodConfig.getUrl());
        return this;
    }

    public LiveConfig clear() {
        getLives().clear();
        this.home = null;
        return this;
    }

    public void load() {
        if (isEmpty()) load(new Callback());
    }

    public void load(Callback callback) {
        App.execute(() -> loadConfig(callback));
    }

    private void loadConfig(Callback callback) {
        try {
            parseConfig(Decoder.getJson(config.getUrl()), callback);
        } catch (Throwable e) {
            if (TextUtils.isEmpty(config.getUrl())) App.post(() -> callback.error(""));
            else App.post(() -> callback.error(Notify.getError(R.string.error_config_get, e)));
            e.printStackTrace();
        }
    }

    private void parseConfig(String text, Callback callback) {
        if (Json.invalid(text)) {
            parseText(text, callback);
        } else {
            checkJson(Json.parse(text).getAsJsonObject(), callback);
        }
    }

    private void parseText(String text, Callback callback) {
        Live live = new Live(config.getUrl()).sync();
        LiveParser.text(live, text);
        getLives().remove(live);
        getLives().add(live);
        setHome(live, true);
        App.post(callback::success);
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
        for (Depot item : items) configs.add(Config.find(item, 1));
        Config.delete(config.getUrl());
        config = configs.get(0);
        loadConfig(callback);
    }

    private void parseConfig(JsonObject object, Callback callback) {
        List<JsonElement> lives = Json.safeListElement(object, "lives");
        if (lives.size() > 0) for (JsonElement element : lives) add(Live.objectFrom(element).check());
        for (Live live : getLives()) if (live.getName().equals(config.getHome())) setHome(live, true);
        if (home == null) setHome(getLives().isEmpty() ? new Live() : getLives().get(0), true);
        if (callback != null) App.post(callback::success);
    }

    private void add(Live live) {
        if (!getLives().contains(live)) getLives().add(live.sync());
    }

    private void bootLive() {
        Setting.putBootLive(false);
        LiveActivity.start(App.get());
    }

    public void parse(JsonObject object) {
        parseConfig(object, null);
    }

    private void setKeep(List<Group> items) {
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

    private int[] getKeep(List<Group> items) {
        String[] splits = Setting.getKeep().split(AppDatabase.SYMBOL);
        if (splits.length < 4 || !home.getName().equals(splits[0])) return new int[]{1, 0};
        for (int i = 0; i < items.size(); i++) {
            Group group = items.get(i);
            if (group.getName().equals(splits[1])) {
                int j = group.find(splits[2]);
                if (j != -1 && splits.length == 4) group.getChannel().get(j).setLine(splits[3]);
                if (j != -1) return new int[]{i, j};
            }
        }
        return new int[]{1, 0};
    }

    public void setKeep(Channel channel) {
        if (home == null || channel.getGroup().isHidden() || channel.getUrls().isEmpty()) return;
        Setting.putKeep(home.getName() + AppDatabase.SYMBOL + channel.getGroup().getName() + AppDatabase.SYMBOL + channel.getName() + AppDatabase.SYMBOL + channel.getCurrent());
    }

    public int[] find(List<Group> items) {
        setKeep(items);
        return getKeep(items);
    }

    public int[] find(String number, List<Group> items) {
        for (int i = 0; i < items.size(); i++) {
            int j = items.get(i).find(Integer.parseInt(number));
            if (j != -1) return new int[]{i, j};
        }
        return new int[]{-1, -1};
    }

    public boolean needSync(String url) {
        return sync || TextUtils.isEmpty(config.getUrl()) || url.equals(config.getUrl());
    }

    public List<Live> getLives() {
        return lives == null ? lives = new ArrayList<>() : lives;
    }

    public Config getConfig() {
        return config == null ? Config.live() : config;
    }

    public Live getHome() {
        return home == null ? new Live() : home;
    }

    public void setHome(Live home) {
        setHome(home, false);
    }

    private void setHome(Live home, boolean check) {
        this.home = home;
        this.home.setActivated(true);
        config.home(home.getName()).update();
        for (Live item : getLives()) item.setActivated(home);
        if (App.activity() != null && App.activity() instanceof LiveActivity) return;
        if (check) if (home.isBoot() || Setting.isBootLive()) App.post(this::bootLive);
    }
}
