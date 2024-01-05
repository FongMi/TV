package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.StringRes;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {

    @SerializedName("channel")
    private List<Channel> channel;
    @SerializedName("name")
    private String name;
    @SerializedName("pass")
    private String pass;

    private boolean selected;
    private int position;

    public static List<Group> arrayFrom(String str) {
        Type listType = new TypeToken<List<Group>>() {}.getType();
        List<Group> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static Group create() {
        return create("");
    }

    public static Group create(@StringRes int resId) {
        return new Group(ResUtil.getString(resId));
    }

    public static Group create(String name) {
        return new Group(name);
    }

    public static Group create(String name, boolean pass) {
        return new Group(name, pass);
    }

    public Group(String name) {
        this(name, false);
    }

    public Group(String name, boolean pass) {
        this.name = name;
        this.position = -1;
        if (name.contains("_")) parse(pass);
    }

    private void parse(boolean pass) {
        String[] splits = name.split("_");
        setName(splits[0]);
        if (pass || splits.length == 1) return;
        setPass(splits[1]);
    }

    public List<Channel> getChannel() {
        return channel = channel == null ? new ArrayList<>() : channel;
    }

    public void setChannel(List<Channel> channel) {
        this.channel = channel;
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

    public boolean isEmpty() {
        return getChannel().isEmpty();
    }

    public boolean skip() {
        return isKeep();
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
