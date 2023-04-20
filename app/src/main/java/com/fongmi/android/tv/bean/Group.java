package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.StringRes;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {

    @SerializedName("channel")
    private List<Channel> channel;
    @SerializedName("logo")
    private String logo;
    @SerializedName("name")
    private String name;
    @SerializedName("pass")
    private String pass;

    private boolean selected;
    private int position;

    public static List<Group> arrayFrom(String str) {
        Type listType = new TypeToken<List<Group>>() {}.getType();
        List<Group> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static Group create(String name) {
        return new Group(name);
    }

    public static Group create(@StringRes int resId) {
        return new Group(ResUtil.getString(resId));
    }

    public Group(String name) {
        this.name = name;
        if (!name.contains("_")) return;
        setName(name.split("_")[0]);
        setPass(name.split("_")[1]);
    }

    public List<Channel> getChannel() {
        return channel = channel == null ? new ArrayList<>() : channel;
    }

    public void setChannel(List<Channel> channel) {
        this.channel = channel;
    }

    public String getLogo() {
        return TextUtils.isEmpty(logo) ? "" : logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return TextUtils.isEmpty(pass) ? "" : pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isHidden() {
        return !TextUtils.isEmpty(getPass());
    }

    public boolean isKeep() {
        return getName().equals(ResUtil.getString(R.string.keep));
    }

    public boolean skip() {
        return isKeep();
    }

    public void loadLogo(ImageView view) {
        ImgUtil.loadLive(getLogo(), view);
    }

    public int find(int number) {
        return getChannel().lastIndexOf(Channel.create(number));
    }

    public int find(String name) {
        return getChannel().lastIndexOf(Channel.create(name));
    }

    public void add(Channel channel) {
        int index = getChannel().indexOf(channel);
        if (index == -1) getChannel().add(Channel.create(channel));
        else getChannel().get(index).getUrls().addAll(channel.getUrls());
    }

    public Channel find(Channel channel) {
        int index = getChannel().indexOf(channel);
        if (index != -1) return getChannel().get(index);
        getChannel().add(channel);
        return channel;
    }

    public Channel current() {
        return getChannel().get(getPosition()).group(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof Group)) return false;
        Group it = (Group) obj;
        return getName().equals(it.getName()) && getChannel().size() == it.getChannel().size();
    }
}
