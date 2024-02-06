package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class Epg {

    @SerializedName("key")
    private String key;
    @SerializedName("date")
    private String date;
    @SerializedName("epg_data")
    private List<EpgData> list;

    private int width;

    public static Epg objectFrom(String str, String key, SimpleDateFormat format) {
        try {
            Epg item = App.gson().fromJson(str, Epg.class);
            item.setTime(format);
            item.setKey(key);
            return item;
        } catch (Exception e) {
            return new Epg();
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDate() {
        return TextUtils.isEmpty(date) ? "" : date;
    }

    public List<EpgData> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<EpgData> list) {
        this.list = list;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean equal(String date) {
        return getDate().equals(date);
    }

    private void setTime(SimpleDateFormat format) {
        setList(new ArrayList<>(new LinkedHashSet<>(getList())));
        for (EpgData item : getList()) {
            item.setStartTime(Util.format(format, getDate().concat(item.getStart())));
            item.setEndTime(Util.format(format, getDate().concat(item.getEnd())));
            item.setTitle(Trans.s2t(item.getTitle()));
        }
    }

    public String getEpg() {
        for (EpgData item : getList()) if (item.isSelected()) return item.format();
        return "";
    }

    public Epg selected() {
        for (EpgData item : getList()) item.setSelected(item.isInRange());
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isSelected()) return i;
        return -1;
    }
}
