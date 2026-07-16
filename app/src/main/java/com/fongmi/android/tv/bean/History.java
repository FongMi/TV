package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.utils.Task;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Entity
public class History implements Diffable<History> {

    @NonNull
    @PrimaryKey
    @SerializedName("key")
    private String key;
    @SerializedName("vodPic")
    private String vodPic;
    @SerializedName("vodName")
    private String vodName;
    @SerializedName("vodFlag")
    private String vodFlag;
    @SerializedName("vodRemarks")
    private String vodRemarks;
    @SerializedName("episodeUrl")
    private String episodeUrl;
    @SerializedName("revSort")
    private boolean revSort;
    @SerializedName("revPlay")
    private boolean revPlay;
    @SerializedName("createTime")
    private long createTime;
    @SerializedName("opening")
    private long opening;
    @SerializedName("ending")
    private long ending;
    @SerializedName("position")
    private long position;
    @SerializedName("duration")
    private long duration;
    @SerializedName("speed")
    private float speed;
    @SerializedName("scale")
    private int scale;
    @SerializedName("cid")
    private int cid;

    private transient long saveTime;

    public History() {
        this.speed = 1;
        this.scale = -1;
        this.ending = C.TIME_UNSET;
        this.opening = C.TIME_UNSET;
        this.position = C.TIME_UNSET;
        this.duration = C.TIME_UNSET;
    }

    public static History objectFrom(String str) {
        return App.gson().fromJson(str, History.class);
    }

    public static List<History> arrayFrom(String str) {
        Type listType = TypeToken.getParameterized(List.class, History.class).getType();
        List<History> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static List<History> get() {
        return get(VodConfig.getCid());
    }

    public static List<History> get(int cid) {
        return AppDatabase.get().getHistoryDao().find(cid, System.currentTimeMillis() - Constant.HISTORY_TIME);
    }

    public static History find(String key) {
        return AppDatabase.get().getHistoryDao().find(VodConfig.getCid(), key);
    }

    public static List<History> findByName(String name) {
        try {
            return AppDatabase.get().getHistoryDao().findByName(VodConfig.getCid(), name);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static void delete(int cid) {
        AppDatabase.get().getHistoryDao().delete(cid);
    }

    public static void clear(int cid) {
        Task.executeSerial(() -> delete(cid));
    }

    public static void sync(List<History> targets) {
        targets.forEach(target -> {
            List<History> items = findByName(target.getVodName());
            if (items.isEmpty()) target.cid(VodConfig.getCid()).save();
            else {
                long latestTime = items.stream().mapToLong(History::getCreateTime).max().orElse(0L);
                if (target.getCreateTime() > latestTime) target.cid(VodConfig.getCid()).mergeFrom(items, true).save();
            }
        });
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

    public boolean isRevSort() {
        return revSort;
    }

    public void setRevSort(boolean revSort) {
        this.revSort = revSort;
    }

    public boolean isRevPlay() {
        return revPlay;
    }

    public void setRevPlay(boolean revPlay) {
        this.revPlay = revPlay;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void markSaveScheduled() {
        saveTime = System.currentTimeMillis();
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

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public History cid(int cid) {
        setKey(getSiteKey().concat(AppDatabase.SYMBOL).concat(getVodId()).concat(AppDatabase.SYMBOL) + cid);
        setCid(cid);
        return this;
    }

    public String getSiteName() {
        return VodConfig.get().getSite(getSiteKey()).getName();
    }

    public String getSiteKey() {
        return getKey().split(AppDatabase.SYMBOL)[0];
    }

    public String getVodId() {
        return getKey().split(AppDatabase.SYMBOL)[1];
    }

    public Flag getFlag() {
        return Flag.create(getVodFlag());
    }

    public Episode getEpisode() {
        return Episode.create(getVodRemarks(), getEpisodeUrl());
    }

    public int getSiteVisible() {
        return TextUtils.isEmpty(getSiteName()) ? View.GONE : View.VISIBLE;
    }

    public int getRevPlayText() {
        return isRevPlay() ? R.string.play_backward : R.string.play_forward;
    }

    public int getRevPlayHint() {
        return isRevPlay() ? R.string.play_backward_hint : R.string.play_forward_hint;
    }

    private boolean shouldMerge(History item, boolean force) {
        if (!force && getKey().equals(item.getKey())) return false;
        if (getDuration() <= 0 || item.getDuration() <= 0) return false;
        return Math.abs(getDuration() - item.getDuration()) <= TimeUnit.MINUTES.toMillis(10);
    }

    private void copyTo(History item) {
        if (getEnding() != C.TIME_UNSET) item.setEnding(getEnding());
        if (getOpening() != C.TIME_UNSET) item.setOpening(getOpening());
    }

    public boolean canSave() {
        return getPosition() >= 0 && getDuration() > 0;
    }

    public boolean canScheduleSave() {
        return System.currentTimeMillis() - saveTime > TimeUnit.SECONDS.toMillis(5);
    }

    public History copy() {
        History item = new History();
        item.key = key;
        item.vodPic = vodPic;
        item.vodName = vodName;
        item.vodFlag = vodFlag;
        item.vodRemarks = vodRemarks;
        item.episodeUrl = episodeUrl;
        item.revSort = revSort;
        item.revPlay = revPlay;
        item.createTime = createTime;
        item.opening = opening;
        item.ending = ending;
        item.position = position;
        item.duration = duration;
        item.speed = speed;
        item.scale = scale;
        item.cid = cid;
        return item;
    }

    public History merge() {
        return merge(false);
    }

    private History merge(boolean force) {
        return mergeFrom(findByName(getVodName()), force);
    }

    private History mergeFrom(List<History> items, boolean force) {
        List<History> matches = items.stream().filter(item -> item.shouldMerge(this, force)).toList();
        if (matches.isEmpty()) return this;
        matches.get(0).copyTo(this);
        matches.forEach(History::delete);
        return this;
    }

    public void replace(String key) {
        delete();
        setKey(key);
    }

    public History save(int cid) {
        return cid(cid).merge(true).save();
    }

    public History save() {
        saveTime = System.currentTimeMillis();
        AppDatabase.get().getHistoryDao().insertOrUpdate(this);
        return this;
    }

    public History delete() {
        AppDatabase.get().getHistoryDao().delete(getCid(), getKey());
        AppDatabase.get().getTrackDao().delete(getKey());
        return this;
    }

    public void findEpisode(List<Flag> flags) {
        if (flags.isEmpty()) return;
        setVodFlag(flags.get(0).getFlag());
        if (!flags.get(0).getEpisodes().isEmpty()) setVodRemarks(flags.get(0).getEpisodes().get(0).getName());
        for (History item : findByName(getVodName())) {
            if (getPosition() >= 0) break;
            for (Flag flag : flags) {
                Episode episode = flag.find(item.getVodRemarks(), true);
                if (episode == null) continue;
                item.copyTo(this);
                setVodFlag(flag.getFlag());
                setPosition(item.getPosition());
                setEpisodeUrl(episode.getUrl());
                setVodRemarks(episode.getName());
                break;
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof History it)) return false;
        return Objects.equals(getKey(), it.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }

    @Override
    public boolean isSameItem(History other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(History other) {
        return getVodName().equals(other.getVodName()) && getVodPic().equals(other.getVodPic()) && getCreateTime() == other.getCreateTime();
    }
}
