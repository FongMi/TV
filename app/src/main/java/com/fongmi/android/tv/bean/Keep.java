package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.db.AppDatabase;

import java.util.List;

@Entity
public class Keep {

    @NonNull
    @PrimaryKey
    private String key;
    private String vodName;
    private String vodPic;
    private long createTime;
    private int cid;

    @NonNull
    public String getKey() {
        return key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public String getVodName() {
        return vodName;
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getVodPic() {
        return vodPic;
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
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

    public static Keep find(String key) {
        return AppDatabase.get().getKeepDao().find(ApiConfig.getCid(), key);
    }

    public static List<Keep> getAll() {
        return AppDatabase.get().getKeepDao().getAll();
    }

    public Keep save() {
        AppDatabase.get().getKeepDao().insert(this);
        return this;
    }

    public void delete() {
        AppDatabase.get().getKeepDao().delete(getCid(), getKey());
    }
}
