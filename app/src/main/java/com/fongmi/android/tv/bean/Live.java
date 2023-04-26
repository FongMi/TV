package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Live {

    @SerializedName("type")
    private int type;
    @SerializedName("boot")
    private boolean boot;
    @SerializedName("name")
    private String name;
    @SerializedName("group")
    private String group;
    @SerializedName("url")
    private String url;
    @SerializedName("logo")
    private String logo;
    @SerializedName("epg")
    private String epg;
    @SerializedName("ua")
    private String ua;
    @SerializedName("referer")
    private String referer;
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

    public static Live objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Live.class);
    }

    public static List<Live> arrayFrom(String str) {
        Type listType = new TypeToken<List<Live>>() {}.getType();
        List<Live> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Live() {
    }

    public Live(String url) {
        this.name = Uri.parse(url).getLastPathSegment();
        this.url = url;
    }

    public Live(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public boolean isBoot() {
        return boot;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getGroup() {
        return TextUtils.isEmpty(group) ? "" : group;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
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

    public String getReferer() {
        return TextUtils.isEmpty(referer) ? "" : referer;
    }

    public JsonElement getHeader() {
        return header;
    }

    public int getPlayerType() {
        return playerType == null ? -1 : playerType == 1 ? 1 : 0;
    }

    public List<Channel> getChannels() {
        return channels = channels == null ? new ArrayList<>() : channels;
    }

    public List<Group> getGroups() {
        return groups = groups == null ? new ArrayList<>() : groups;
    }

    public Core getCore() {
        return core;
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

    public Live check() {
        boolean proxy = getChannels().size() > 0 && getChannels().get(0).getUrls().size() > 0 && getChannels().get(0).getUrls().get(0).startsWith("proxy");
        if (proxy) setProxy();
        return this;
    }

    private void setProxy() {
        this.url = Utils.checkProxy(getChannels().get(0).getUrls().get(0));
        this.name = getChannels().get(0).getName();
        this.type = 2;
    }

    public Group find(Group item) {
        for (Group group : getGroups()) if (group.getName().equals(item.getName())) return group;
        getGroups().add(item);
        return item;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Live)) return false;
        Live it = (Live) obj;
        return getName().equals(it.getName()) && getUrl().equals(it.getUrl());
    }
}
