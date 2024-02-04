package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.db.AppDatabase;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(ignoredColumns = {"type", "group", "url", "jar", "logo", "epg", "ua", "click", "origin", "referer", "timeout", "header", "playerType", "channels", "groups", "core", "activated", "width"})
public class Live {

    @NonNull
    @PrimaryKey
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private int type;
    @SerializedName("boot")
    private boolean boot;
    @SerializedName("pass")
    private boolean pass;
    @SerializedName("group")
    private String group;
    @SerializedName("url")
    private String url;
    @SerializedName("jar")
    private String jar;
    @SerializedName("logo")
    private String logo;
    @SerializedName("epg")
    private String epg;
    @SerializedName("ua")
    private String ua;
    @SerializedName("click")
    private String click;
    @SerializedName("origin")
    private String origin;
    @SerializedName("referer")
    private String referer;
    @SerializedName("timeout")
    private Integer timeout;
    @SerializedName("header")
    private JsonElement header;
    @SerializedName("playerType")
    private Integer playerType;
    @SerializedName("channels")
    private List<Channel> channels;
    @SerializedName("groups")
    private List<Group> groups;
    @SerializedName("core")
    private Core core;

    private boolean activated;
    private int width;

    public static Live objectFrom(JsonElement element) {
        return App.gson().fromJson(element, Live.class);
    }

    public static List<Live> arrayFrom(String str) {
        Type listType = new TypeToken<List<Live>>() {}.getType();
        List<Live> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Live() {
    }

    public Live(String url) {
        this.name = url.startsWith("file") ? new File(url).getName() : Uri.parse(url).getLastPathSegment();
        this.url = url;
    }

    public Live(@NonNull String name, String url) {
        this.name = name;
        this.url = url;
    }

    public int getType() {
        return type;
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

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getGroup() {
        return TextUtils.isEmpty(group) ? "" : group;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getJar() {
        return TextUtils.isEmpty(jar) ? "" : jar;
    }

    public String getLogo() {
        return TextUtils.isEmpty(logo) ? "" : logo;
    }

    public String getEpg() {
        return TextUtils.isEmpty(epg) ? "" : epg;
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

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public Integer getTimeout() {
        return timeout == null ? Constant.TIMEOUT_PLAY : Math.max(timeout, 1) * 1000;
    }

    public JsonElement getHeader() {
        return header;
    }

    public int getPlayerType() {
        return playerType == null ? -1 : Math.min(playerType, 2);
    }

    public List<Channel> getChannels() {
        return channels = channels == null ? new ArrayList<>() : channels;
    }

    public List<Group> getGroups() {
        return groups = groups == null ? new ArrayList<>() : groups;
    }

    public Core getCore() {
        return core == null ? new Core() : core;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Live item) {
        this.activated = item.equals(this);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isEmpty() {
        return getName().isEmpty();
    }

    public Live check() {
        boolean proxy = getChannels().size() > 0 && getChannels().get(0).getUrls().size() > 0 && getChannels().get(0).getUrls().get(0).startsWith("proxy");
        if (proxy) setProxy();
        return this;
    }

    private void setProxy() {
        this.url = getChannels().get(0).getUrls().get(0);
        this.name = getChannels().get(0).getName();
        this.type = 2;
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

    public Live sync() {
        Live item = find(getName());
        if (item == null) return this;
        setBoot(item.isBoot());
        setPass(item.isPass());
        return this;
    }

    public static Live find(String name) {
        return AppDatabase.get().getLiveDao().find(name);
    }

    public void save() {
        AppDatabase.get().getLiveDao().insertOrUpdate(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Live)) return false;
        Live it = (Live) obj;
        return getName().equals(it.getName());
    }
}
