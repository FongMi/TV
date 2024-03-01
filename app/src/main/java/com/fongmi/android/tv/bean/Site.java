package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.gson.ExtAdapter;
import com.github.catvod.utils.Json;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import okhttp3.Headers;

@Entity
public class Site implements Parcelable {

    @NonNull
    @PrimaryKey
    @SerializedName("key")
    private String key;

    @Ignore
    @SerializedName("name")
    private String name;

    @Ignore
    @SerializedName("api")
    private String api;

    @Ignore
    @JsonAdapter(ExtAdapter.class)
    @SerializedName("ext")
    private String ext;

    @Ignore
    @SerializedName("jar")
    private String jar;

    @Ignore
    @SerializedName("click")
    private String click;

    @Ignore
    @SerializedName("playUrl")
    private String playUrl;

    @Ignore
    @SerializedName("type")
    private Integer type;

    @Ignore
    @SerializedName("indexs")
    private Integer indexs;

    @Ignore
    @SerializedName("timeout")
    private Integer timeout;

    @Ignore
    @SerializedName("playerType")
    private Integer playerType;

    @SerializedName("searchable")
    private Integer searchable;

    @SerializedName("changeable")
    private Integer changeable;

    @Ignore
    @SerializedName("categories")
    private List<String> categories;

    @Ignore
    @SerializedName("header")
    private JsonElement header;

    @Ignore
    @SerializedName("style")
    private Style style;

    @Ignore
    private boolean activated;

    public static Site objectFrom(JsonElement element) {
        try {
            return App.gson().fromJson(element, Site.class);
        } catch (Exception e) {
            return new Site();
        }
    }

    public static Site get(String key) {
        Site site = new Site();
        site.setKey(key);
        return site;
    }

    public static Site get(String key, String name) {
        Site site = new Site();
        site.setKey(key);
        site.setName(name);
        return site;
    }

    public Site() {
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApi() {
        return TextUtils.isEmpty(api) ? "" : api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getExt() {
        return TextUtils.isEmpty(ext) ? "" : ext;
    }

    public void setExt(String ext) {
        this.ext = ext.trim();
    }

    public String getJar() {
        return TextUtils.isEmpty(jar) ? "" : jar;
    }

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public String getPlayUrl() {
        return TextUtils.isEmpty(playUrl) ? "" : playUrl;
    }

    public Integer getType() {
        return type == null ? 0 : type;
    }

    public Integer getTimeout() {
        return timeout == null ? Constant.TIMEOUT_PLAY : Math.max(timeout, 1) * 1000;
    }

    public int getPlayerType() {
        return playerType == null ? -1 : Math.min(playerType, 2);
    }

    public Integer getSearchable() {
        return searchable == null ? 1 : searchable;
    }

    public void setSearchable(Integer searchable) {
        this.searchable = searchable;
    }

    public Integer getChangeable() {
        return changeable == null ? 1 : changeable;
    }

    public void setChangeable(Integer changeable) {
        this.changeable = changeable;
    }

    public boolean isIndexs() {
        return getIndexs() == 1;
    }

    public Integer getIndexs() {
        if (Setting.isAggregatedSearch() && (indexs == null || indexs == 1)) return 1;
        return indexs == null ? 0 : indexs;
    }

    public List<String> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }

    public JsonElement getHeader() {
        return header;
    }

    public Style getStyle() {
        return style;
    }

    public Style getStyle(Style style) {
        return getStyle() != null ? getStyle() : style != null ? style : Style.rect();
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Site item) {
        this.activated = item.equals(this);
    }

    public boolean isSearchable() {
        return getSearchable() == 1;
    }

    public Site setSearchable(boolean searchable) {
        if (getSearchable() != 0) setSearchable(searchable ? 1 : 2);
        return this;
    }

    public boolean isChangeable() {
        return getChangeable() == 1;
    }

    public Site setChangeable(boolean changeable) {
        if (getChangeable() != 0) setChangeable(changeable ? 1 : 2);
        return this;
    }

    public boolean isEmpty() {
        return getKey().isEmpty() && getName().isEmpty();
    }

    public Headers getHeaders() {
        return Headers.of(Json.toMap(getHeader()));
    }

    public Site sync() {
        Site item = find(getKey());
        if (item == null) return this;
        if (getChangeable() != 0) setChangeable(Math.max(1, item.getChangeable()));
        if (getSearchable() != 0) setSearchable(Math.max(1, item.getSearchable()));
        return this;
    }

    public static Site find(String key) {
        return AppDatabase.get().getSiteDao().find(key);
    }

    public void save() {
        AppDatabase.get().getSiteDao().insertOrUpdate(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Site)) return false;
        Site it = (Site) obj;
        return getKey().equals(it.getKey());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.key);
        dest.writeString(this.name);
        dest.writeString(this.api);
        dest.writeString(this.ext);
        dest.writeString(this.jar);
        dest.writeString(this.click);
        dest.writeString(this.playUrl);
        dest.writeValue(this.type);
        dest.writeValue(this.timeout);
        dest.writeValue(this.playerType);
        dest.writeValue(this.searchable);
        dest.writeValue(this.changeable);
        dest.writeValue(this.indexs);
        dest.writeStringList(this.categories);
        dest.writeParcelable(this.style, flags);
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
    }

    protected Site(Parcel in) {
        this.key = in.readString();
        this.name = in.readString();
        this.api = in.readString();
        this.ext = in.readString();
        this.jar = in.readString();
        this.click = in.readString();
        this.playUrl = in.readString();
        this.type = (Integer) in.readValue(Integer.class.getClassLoader());
        this.timeout = (Integer) in.readValue(Integer.class.getClassLoader());
        this.playerType = (Integer) in.readValue(Integer.class.getClassLoader());
        this.searchable = (Integer) in.readValue(Integer.class.getClassLoader());
        this.changeable = (Integer) in.readValue(Integer.class.getClassLoader());
        this.indexs = (Integer) in.readValue(Integer.class.getClassLoader());
        this.categories = in.createStringArrayList();
        this.style = in.readParcelable(Style.class.getClassLoader());
        this.activated = in.readByte() != 0;
    }

    public static final Creator<Site> CREATOR = new Creator<>() {
        @Override
        public Site createFromParcel(Parcel source) {
            return new Site(source);
        }

        @Override
        public Site[] newArray(int size) {
            return new Site[size];
        }
    };
}
