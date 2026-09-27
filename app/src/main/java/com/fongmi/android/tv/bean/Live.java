package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.gson.ExtAdapter;
import com.fongmi.android.tv.gson.HeaderAdapter;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.Trans;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Entity
public class Live {

    @NonNull
    @PrimaryKey
    @SerializedName("name")
    private String name;

    @Ignore
    @SerializedName("url")
    private String url;

    @Ignore
    @SerializedName("api")
    private String api;

    @Ignore
    @SerializedName("ext")
    @JsonAdapter(ExtAdapter.class)
    private String ext;

    @Ignore
    @SerializedName("jar")
    private String jar;

    @Ignore
    @SerializedName("click")
    private String click;

    @Ignore
    @SerializedName("logo")
    private String logo;

    @Ignore
    @SerializedName("epg")
    private String epg;

    @Ignore
    @SerializedName("ua")
    private String ua;

    @Ignore
    @SerializedName("origin")
    private String origin;

    @Ignore
    @SerializedName("referer")
    private String referer;

    @Ignore
    @SerializedName("timeZone")
    private String timeZone;

    @SerializedName("keep")
    private String keep;

    @Ignore
    @SerializedName("timeout")
    private Integer timeout;

    @Ignore
    @SerializedName("header")
    @JsonAdapter(HeaderAdapter.class)
    private Map<String, String> header;

    @Ignore
    @SerializedName("catchup")
    private Catchup catchup;

    @Ignore
    @SerializedName("core")
    private Core core;

    @Ignore
    @SerializedName("groups")
    private List<Group> groups;

    @SerializedName("boot")
    private boolean boot;

    @SerializedName("pass")
    private boolean pass;

    @Ignore
    private boolean selected;

    @Ignore
    private int width;

    public Live() {
    }

    public Live(@NonNull String name, String url) {
        this.name = name;
        this.url = url;
    }

    public static Live objectFrom(JsonElement element, String spider) {
        try {
            Live live = App.gson().fromJson(element, Live.class);
            if (live.getJar().isEmpty()) live.setJar(spider);
            live.setApi(UrlUtil.convert(live.getApi()));
            live.setExt(UrlUtil.convert(live.getExt()));
            return live.trans();
        } catch (Exception e) {
            return new Live();
        }
    }

    public static List<Live> findAll() {
        return AppDatabase.get().getLiveDao().findAll();
    }

    public static Live find(String name) {
        return AppDatabase.get().getLiveDao().find(name);
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApi() {
        return TextUtils.isEmpty(api) ? "" : api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getExt() {
        return TextUtils.isEmpty(ext) ? "" : ext;
    }

    public void setExt(String ext) {
        this.ext = ext.trim();
    }

    public String getJar() {
        return TextUtils.isEmpty(jar) ? "" : jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public String getLogo() {
        return TextUtils.isEmpty(logo) ? "" : logo;
    }

    public String getEpg() {
        return TextUtils.isEmpty(epg) ? "" : epg;
    }

    public void setEpg(String epg) {
        this.epg = epg;
    }

    public String getUa() {
        return TextUtils.isEmpty(ua) ? "" : ua;
    }

    public String getOrigin() {
        return TextUtils.isEmpty(origin) ? "" : origin;
    }

    public String getReferer() {
        return TextUtils.isEmpty(referer) ? "" : referer;
    }

    public String getTimeZone() {
        return TextUtils.isEmpty(timeZone) ? "" : timeZone;
    }

    public ZoneId getZoneId() {
        try {
            return getTimeZone().isEmpty() ? ZoneId.systemDefault() : ZoneId.of(getTimeZone());
        } catch (Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    public String getKeep() {
        return TextUtils.isEmpty(keep) ? "" : keep;
    }

    public void setKeep(String keep) {
        this.keep = keep;
    }

    public long getTimeout() {
        return timeout == null ? Constant.TIMEOUT_PLAY : TimeUnit.SECONDS.toMillis(Math.max(timeout, 1));
    }

    public Map<String, String> getHeader() {
        return header == null ? new HashMap<>() : header;
    }

    public Catchup getCatchup() {
        return catchup == null ? new Catchup() : catchup;
    }

    public Core getCore() {
        return core == null ? new Core() : core;
    }

    public List<Group> getGroups() {
        return groups = groups == null ? new ArrayList<>() : groups;
    }

    public boolean isBoot() {
        return boot;
    }

    public void setBoot(boolean boot) {
        this.boot = boot;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelected(Live item) {
        this.selected = item.equals(this);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getEpgApi() {
        for (String url : getEpg().split(",")) if (url.contains("{")) return url;
        return getEpg();
    }

    public List<String> getEpgXml() {
        List<String> items = new ArrayList<>();
        for (String epg : getEpg().split(",")) if (!epg.contains("{") && (epg.contains("xml") || epg.contains("gz"))) items.add(epg);
        return items;
    }

    public boolean isEmpty() {
        return getName().isEmpty();
    }

    public Group find(Group item) {
        for (Group group : getGroups()) if (group.getName().equals(item.getName())) return group;
        getGroups().add(item);
        return item;
    }

    public int getBootIcon() {
        return isBoot() ? R.drawable.ic_live_boot : R.drawable.ic_live_block;
    }

    public int getPassIcon() {
        return isPass() ? R.drawable.ic_live_block : R.drawable.ic_live_pass;
    }

    public Live boot(boolean boot) {
        setBoot(boot);
        return this;
    }

    public Live pass(boolean pass) {
        getGroups().clear();
        setPass(pass);
        return this;
    }

    public Live keep(Channel channel) {
        setKeep(channel.getGroup().getName() + AppDatabase.SYMBOL + channel.getName() + AppDatabase.SYMBOL + channel.getCurrent());
        return this;
    }

    public Live trans() {
        if (Trans.pass()) return this;
        this.name = Trans.s2t(name);
        return this;
    }

    public Live sync() {
        sync(find(getName()));
        return this;
    }

    public Live sync(Live item) {
        if (item == null) return this;
        setBoot(item.isBoot());
        setPass(item.isPass());
        setKeep(item.getKeep());
        return this;
    }

    public Live recent() {
        BaseLoader.get().setRecent(getName(), getApi(), getJar());
        return this;
    }

    public Spider spider() {
        return BaseLoader.get().getSpider(getName(), getApi(), getExt(), getJar());
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>(getHeader());
        if (!getUa().isEmpty()) headers.put(HttpHeaders.USER_AGENT, getUa());
        if (!getOrigin().isEmpty()) headers.put(HttpHeaders.ORIGIN, getOrigin());
        if (!getReferer().isEmpty()) headers.put(HttpHeaders.REFERER, getReferer());
        return headers;
    }

    public void save() {
        AppDatabase.get().getLiveDao().insertOrUpdate(this);
    }

    public void saveSettings() {
        saveSettings(List.of(this));
    }

    public static void saveSettings(List<Live> items) {
        List<Live> snapshots = new ArrayList<>(items.size());
        for (Live item : items) snapshots.add(new Live(item.getName(), item.getUrl()).sync(item));
        Task.executeSerial(() -> AppDatabase.get().getLiveDao().saveSettings(snapshots));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Live it)) return false;
        return getName().equals(it.getName());
    }
}
