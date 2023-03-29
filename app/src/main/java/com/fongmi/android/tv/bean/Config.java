package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.db.AppDatabase;

import java.util.List;

@Entity(indices = @Index(value = {"url", "type"}, unique = true))
public class Config {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int type;
    private long time;
    private String url;
    private String json;
    private String name;
    private String home;
    private String parse;

    public static Config create(int type) {
        return create("", type);
    }

    public static Config create(String url, int type) {
        return new Config(url, "", type);
    }

    public static Config create(String url, String name, int type) {
        return new Config(url, name, type);
    }

    public Config(String url, String name, int type) {
        this.url = url;
        this.name = name;
        this.type = type;
        this.id = (int) insert();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getParse() {
        return parse;
    }

    public void setParse(String parse) {
        this.parse = parse;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Config type(int type) {
        setType(type);
        return this;
    }

    public Config name(String name) {
        setName(name);
        return this;
    }

    public Config json(String json) {
        setJson(json);
        return this;
    }

    public Config home(String home) {
        setHome(home);
        return this;
    }

    public Config parse(String parse) {
        setParse(parse);
        return this;
    }

    public String getDesc() {
        if (!TextUtils.isEmpty(getName())) return getName();
        if (!TextUtils.isEmpty(getUrl())) return getUrl();
        return "";
    }

    public static List<Config> getAll(int type) {
        return AppDatabase.get().getConfigDao().findByType(type);
    }

    public static void delete(String url) {
        AppDatabase.get().getConfigDao().delete(url);
    }

    public static void delete(String url, int type) {
        AppDatabase.get().getConfigDao().delete(url, type);
    }

    public static Config vod() {
        Config item = AppDatabase.get().getConfigDao().findOne(0);
        return item == null ? create(0) : item;
    }

    public static Config live() {
        Config item = AppDatabase.get().getConfigDao().findOne(1);
        return item == null ? create(1) : item;
    }

    public static Config wall() {
        Config item = AppDatabase.get().getConfigDao().findOne(2);
        return item == null ? create(2) : item;
    }

    public static Config find(int id) {
        return AppDatabase.get().getConfigDao().findById(id);
    }

    public static Config find(String url, int type) {
        Config item = AppDatabase.get().getConfigDao().find(url, type);
        return item == null ? create(url, type) : item.type(type);
    }

    public static Config find(String url, String name, int type) {
        Config item = AppDatabase.get().getConfigDao().find(url, type);
        return item == null ? create(url, name, type) : item.type(type).name(name);
    }

    public static Config find(Config config, int type) {
        Config item = AppDatabase.get().getConfigDao().find(config.getUrl(), type);
        return item == null ? create(config.getUrl(), config.getName(), type) : item.type(type).name(config.getName());
    }

    public static Config find(Depot depot, int type) {
        Config item = AppDatabase.get().getConfigDao().find(depot.getUrl(), type);
        return item == null ? create(depot.getUrl(), depot.getName(), type) : item.type(type).name(depot.getName());
    }

    public long insert() {
        return getUrl().isEmpty() ? -1 : AppDatabase.get().getConfigDao().insert(this);
    }

    public Config update() {
        setTime(System.currentTimeMillis());
        AppDatabase.get().getConfigDao().update(this);
        return this;
    }

    public void delete() {
        AppDatabase.get().getConfigDao().delete(getUrl(), getType());
        History.delete(getId());
        Keep.delete(getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Config)) return false;
        Config it = (Config) obj;
        return getId() == it.getId();
    }
}
