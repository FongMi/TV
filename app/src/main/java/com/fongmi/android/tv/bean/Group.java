package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Trans;
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
    private int width;

    public Group(String name) {
        this(name, false);
    }

    public Group(String name, boolean pass) {
        this.name = name;
        this.position = -1;
        if (name.contains("_")) parse(pass);
        if (name.isEmpty()) setName(ResUtil.getString(R.string.setting_live));
    }

    public static List<Group> arrayFrom(String str) {
        Type listType = TypeToken.getParameterized(List.class, Group.class).getType();
        List<Group> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static Group create() {
        return create(R.string.setting_live);
    }

    public static Group create(@StringRes int resId) {
        return new Group(ResUtil.getString(resId));
    }

    public static Group create(String name, boolean pass) {
        return new Group(name, pass);
    }

    private void parse(boolean pass) {
        String[] splits = name.split("_", 2);
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
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
        List<Channel> items = getChannel();
        for (int i = items.size() - 1; i >= 0; i--) {
            try {
                if (Integer.parseInt(items.get(i).getNumber()) == number) return i;
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    public int find(String name) {
        return getChannel().lastIndexOf(Channel.create(name));
    }

    public void add(Channel channel) {
        Channel exist = getChannel().stream().filter(item -> item.equals(channel)).findFirst().orElse(null);
        if (exist != null) exist.getUrls().addAll(channel.getUrls());
        else getChannel().add(Channel.create(channel));
    }

    public Channel find(Channel channel) {
        Channel exist = getChannel().stream().filter(item -> item.equals(channel)).findFirst().orElse(null);
        if (exist != null) return exist;
        getChannel().add(channel);
        return channel;
    }

    public Channel current() {
        return getChannel().get(getPosition()).group(this);
    }

    public Group trans() {
        if (Trans.pass()) return this;
        this.name = Trans.s2t(name);
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof Group it)) return false;
        return getName().equals(it.getName()) && getChannel().size() == it.getChannel().size();
    }
}
