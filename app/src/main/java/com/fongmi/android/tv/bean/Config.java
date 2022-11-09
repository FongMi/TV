package com.fongmi.android.tv.bean;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.api.ApiConfig;
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
    private String home;

    public static Config create(String url, int type) {
        return new Config(url, type);
    }

    public Config(String url, int type) {
        this.url = url;
        this.type = type;
        this.time = System.currentTimeMillis();
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

    public Config json(String json) {
        setJson(json);
        return this;
    }

    public Config home(String home) {
        setHome(home);
        return this;
    }

    public static List<Config> getAll(int type) {
        return AppDatabase.get().getConfigDao().findByType(type);
    }

    public static void delete(String url, int type) {
        if (type == 2) AppDatabase.get().getConfigDao().delete(type);
        else AppDatabase.get().getConfigDao().delete(url, type);
    }

    public static Config vod() {
        Config item = AppDatabase.get().getConfigDao().findOne(0);
        return item == null ? create("", 0) : item;
    }

    public static Config live() {
        Config item = AppDatabase.get().getConfigDao().findOne(1);
        return item == null ? create(ApiConfig.getUrl(), 1) : item;
    }

    public static Config wall() {
        Config item = AppDatabase.get().getConfigDao().findOne(2);
        return item == null ? create("", 2) : item;
    }

    public static Config find(int id) {
        return AppDatabase.get().getConfigDao().findById(id);
    }

    public static Config find(String url, int type) {
        Config item = AppDatabase.get().getConfigDao().find(url, type);
        return item == null ? create(url, type) : item.type(type);
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
