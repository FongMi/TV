package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.fongmi.android.tv.utils.ImgUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Channel {

    @SerializedName("urls")
    private List<String> urls;
    @SerializedName("number")
    private String number;
    @SerializedName("icon")
    private String icon;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private Group type;
    @SerializedName("ua")
    private String ua;

    private boolean select;

    public static Channel objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Channel.class);
    }

    public static Channel create(String number) {
        return new Channel(String.format(Locale.getDefault(), "%03d", Integer.valueOf(number)));
    }

    public Channel(String number) {
        this.number = number;
    }

    public Channel(int number, String name, String... urls) {
        this(number, name, new ArrayList<>(Arrays.asList(urls)));
    }

    public Channel(int number, String name, List<String> urls) {
        this.number = String.format(Locale.getDefault(), "%03d", number);
        this.name = name;
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getIcon() {
        return TextUtils.isEmpty(icon) ? "" : icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Group getType() {
        return type;
    }

    public void setType(Group type) {
        this.type = type;
    }

    public String getUa() {
        return TextUtils.isEmpty(ua) ? "" : ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public int getVisible() {
        return getIcon().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public void loadIcon(ImageView view) {
        if (!getIcon().isEmpty()) ImgUtil.load(getIcon(), view);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Channel)) return false;
        Channel it = (Channel) obj;
        return getNumber().equals(it.getNumber()) || getName().equals(it.getName());
    }
}
