package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.db.AppDatabase;

import java.util.List;

@Entity
public class History {

    @NonNull
    @PrimaryKey
    private String key;
    private String vodPic;
    private String vodName;
    private String vodFlag;
    private String vodRemarks;
    private String episodeUrl;
    private long createTime;
    private long opening;
    private long ending;
    private long duration;
    private int cid;

    public History() {
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public String getVodPic() {
        return vodPic;
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getVodName() {
        return vodName;
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getVodFlag() {
        return vodFlag;
    }

    public void setVodFlag(String vodFlag) {
        this.vodFlag = vodFlag;
    }

    public String getVodRemarks() {
        return vodRemarks == null ? "" : vodRemarks;
    }

    public void setVodRemarks(String vodRemarks) {
        this.vodRemarks = vodRemarks;
    }

    public String getEpisodeUrl() {
        return episodeUrl == null ? "" : episodeUrl;
    }

    public void setEpisodeUrl(String episodeUrl) {
        this.episodeUrl = episodeUrl;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getOpening() {
        return opening;
    }

    public void setOpening(long opening) {
        this.opening = opening;
    }

    public long getEnding() {
        return ending;
    }

    public void setEnding(long ending) {
        this.ending = ending;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public String getSiteKey() {
        return getKey().substring(0, getKey().lastIndexOf(AppDatabase.SYMBOL));
    }

    public String getVodId() {
        return getKey().substring(getKey().lastIndexOf(AppDatabase.SYMBOL) + AppDatabase.SYMBOL.length());
    }

    public Vod.Flag getFlag() {
        return new Vod.Flag(getVodFlag());
    }

    public Vod.Flag.Episode getEpisode() {
        return new Vod.Flag.Episode(getVodRemarks(), getEpisodeUrl());
    }

    public static History find(String key) {
        return AppDatabase.get().getHistoryDao().find(key);
    }

    public static List<History> find(int cid) {
        return AppDatabase.get().getHistoryDao().find(cid);
    }

    public static void delete(int id) {
        AppDatabase.get().getHistoryDao().delete(id);
    }

    public History save() {
        AppDatabase.get().getHistoryDao().insertOrUpdate(this);
        return this;
    }

    public History update() {
        AppDatabase.get().getHistoryDao().update(this);
        return this;
    }

    public History delete() {
        AppDatabase.get().getHistoryDao().delete(getKey());
        return this;
    }
}
