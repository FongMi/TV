package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Url {

    @SerializedName("values")
    private List<Value> values;
    @SerializedName("position")
    private int position;

    public static Url objectFrom(JsonElement element) {
        try {
            return App.gson().fromJson(element, Url.class);
        } catch (Exception e) {
            return create();
        }
    }

    public static Url create() {
        return new Url();
    }

    public List<Value> getValues() {
        return values = values == null ? new ArrayList<>() : values;
    }

    public int getPosition() {
        return position;
    }

    public String v() {
        return v(getPosition());
    }

    public String v(int position) {
        return position >= getValues().size() ? "" : getValues().get(position).getV();
    }

    public String n(int position) {
        return position >= getValues().size() ? "" : getValues().get(position).getN();
    }

    public Url add(String v) {
        getValues().add(new Value("", v));
        return this;
    }

    public Url add(String n, String v) {
        getValues().add(new Value(n, v));
        return this;
    }

    public Url replace(String url) {
        getValues().get(getPosition()).setV(url);
        return this;
    }

    public Url set(int position) {
        this.position = Math.min(position, getValues().size() - 1);
        return this;
    }

    public boolean isEmpty() {
        return getValues().isEmpty() || TextUtils.isEmpty(v());
    }

    public boolean isMulti() {
        return getValues().size() > 1;
    }
}
