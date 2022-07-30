package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class History {

    @NonNull
    @PrimaryKey
    private String key;
    private String vodPic;
    private String vodName;
    private String vodRemarks;
    private String episodeUrl;
    private long createTime;
    private long duration;

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

    public String getVodRemarks() {
        return vodRemarks;
    }

    public void setVodRemarks(String vodRemarks) {
        this.vodRemarks = vodRemarks;
    }

    public String getEpisodeUrl() {
        return episodeUrl;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getSiteKey() {
        return getKey().split("_")[0];
    }

    public String getFlag() {
        return getKey().split("_")[1];
    }

    public String getVodId() {
        return getKey().split("_")[2];
    }
}
