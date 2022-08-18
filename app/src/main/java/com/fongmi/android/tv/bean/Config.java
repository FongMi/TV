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

    public static Config create() {
        return new Config(Prefers.getUrl());
    }

    public Config(String url) {
        this.url = url;
        this.time = System.currentTimeMillis();
        this.id = (int) insert();
    }

    public Config setTime() {
        setTime(System.currentTimeMillis());
        return this;
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

    public static Config find(String url) {
        Config item = AppDatabase.get().getConfigDao().find(url);
        return item == null ? Config.create() : item.setTime();
    }

    public static void save() {
        Config item = find(Prefers.getUrl());
        ApiConfig.get().setCid(item.update().getId());
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
    }
}
