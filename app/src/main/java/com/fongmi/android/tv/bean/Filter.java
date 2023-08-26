package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.github.catvod.utils.Trans;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filter implements Parcelable {

    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("value")
    private List<Value> value;

    public static Filter objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Filter.class);
    }

    public static List<Filter> arrayFrom(String str) {
        Type listType = new TypeToken<List<Filter>>() {}.getType();
        List<Filter> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Filter() {
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public List<Value> getValue() {
        return value == null ? Collections.emptyList() : value;
    }

    public Filter trans() {
        if (Trans.pass()) return this;
        for (Value value : getValue()) value.trans();
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.key);
        dest.writeString(this.name);
        dest.writeList(this.value);
    }

    protected Filter(Parcel in) {
        this.key = in.readString();
        this.name = in.readString();
        this.value = new ArrayList<>();
        in.readList(this.value, Value.class.getClassLoader());
    }

    public static final Creator<Filter> CREATOR = new Creator<>() {
        @Override
        public Filter createFromParcel(Parcel source) {
            return new Filter(source);
        }

        @Override
        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };
}
