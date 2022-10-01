package com.fongmi.android.tv.bean;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.utils.Prefers;

import java.util.List;

@Entity(indices = @Index(value = {"url"}, unique = true))
public class Config {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private long time;
    private String url;
    private String json;

    public static Config create() {
        return new Config(Prefers.getUrl());
    }

    public Config(String url) {
        this.url = url;
        this.time = System.currentTimeMillis();
        this.id = (int) insert();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public static List<Config> getAll() {
        List<Config> items = AppDatabase.get().getConfigDao().getAll();
        if (items.size() > 0) items.remove(0);
        return items;
    }

    public static Config find(int id) {
        return AppDatabase.get().getConfigDao().find(id);
    }

    public static Config find(String url) {
        Config item = AppDatabase.get().getConfigDao().find(url);
        return item == null ? Config.create() : item.newTime();
    }

    public static void save(String json) {
        Config item = find(Prefers.getUrl()).json(json);
        ApiConfig.get().setCid(item.update().getId());
    }

    public Config newTime() {
        setTime(System.currentTimeMillis());
        return this;
    }

    public Config json(String json) {
        setJson(json);
        return this;
    }

    public long insert() {
        return AppDatabase.get().getConfigDao().insert(this);
    }

    public Config update() {
        AppDatabase.get().getConfigDao().update(this);
        return this;
    }

    public void delete() {
        AppDatabase.get().getConfigDao().delete(getUrl());
        History.delete(getId());
        Keep.delete(getId());
    }
}
