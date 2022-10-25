package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Live {

    @SerializedName("type")
    private int type;
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
    @SerializedName("channels")
    private List<Channel> channels;
    @SerializedName("groups")
    private List<Group> groups;

    private boolean activated;

    public static Live objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Live.class);
    }

    public Live() {
    }

    public Live(String url) {
        this.name = url;
        this.url = url;
    }

    public Live(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public int getType() {
        return type;
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

    public List<Channel> getChannels() {
        return channels = channels == null ? new ArrayList<>() : channels;
    }

    public List<Group> getGroups() {
        return groups = groups == null ? new ArrayList<>() : groups;
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

    public String getActivatedName() {
        return (isActivated() ? "√ " : "").concat(getName());
    }

    public boolean isProxy() {
        return getGroup().equals("redirect") && getChannels().size() > 0 && getChannels().get(0).getUrls().size() > 0 && getChannels().get(0).getUrls().get(0).startsWith("proxy");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Live)) return false;
        Live it = (Live) obj;
        return getName().equals(it.getName()) && getUrl().equals(it.getUrl());
    }
}
