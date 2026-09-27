package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.parser.EpgParser;
import com.fongmi.android.tv.utils.Formatters;
import com.github.catvod.utils.Json;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    public static Epg objectFrom(String str, String key, ZoneId zoneId) {
        return objectFrom(str, key, null, zoneId);
    }

    public static Epg objectFrom(String str, String key, String date, ZoneId zoneId) {
        if (!Json.isObj(str)) return EpgParser.getEpg(str, key, date, zoneId);
        try {
            Epg item = App.gson().fromJson(str, Epg.class);
            item.setTime(zoneId);
            item.setKey(key);
            return item;
        } catch (Exception e) {
            return new Epg();
        }
    }

    public static Epg create(String key, String date) {
        Epg item = new Epg();
        item.setKey(key);
        item.setDate(date);
        item.setList(new ArrayList<>());
        return item;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDate() {
        return TextUtils.isEmpty(date) ? "" : date;
    }

    public void setDate(String date) {
        this.date = date;
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

    private void setTime(ZoneId zoneId) {
        setList(new ArrayList<>(new LinkedHashSet<>(getList())));
        for (EpgData item : getList()) {
            item.setStartTime(parseEpgTime(getDate().concat(item.getStart()), zoneId));
            item.setEndTime(parseEpgTime(getDate().concat(item.getEnd()), zoneId));
            if (item.getEndTime() < item.getStartTime()) item.checkDay(zoneId);
            item.trans();
        }
    }

    public EpgData getEpgData() {
        for (EpgData item : getList()) if (item.isSelected()) return item;
        return new EpgData();
    }

    public Epg selected() {
        long now = System.currentTimeMillis();
        for (EpgData item : getList()) item.setSelected(item.isInRange(now));
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isSelected()) return i;
        return -1;
    }

    public int getInRange() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < getList().size(); i++) if (getList().get(i).isInRange(now)) return i;
        return -1;
    }

    private long parseEpgTime(String source, ZoneId zoneId) {
        try {
            var fmt = source.length() > 16 ? Formatters.EPG_DT_LONG : Formatters.EPG_DT_SHORT;
            return LocalDateTime.parse(source, fmt).atZone(zoneId).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
