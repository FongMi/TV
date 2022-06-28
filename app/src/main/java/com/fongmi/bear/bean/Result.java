package com.fongmi.bear.bean;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Result {

    @SerializedName("class")
    private List<Type> types;
    @SerializedName("list")
    private List<Vod> list;

    public static Result objectFrom(String str) {
        Result result = new Gson().fromJson(str, Result.class);
        return result == null ? new Result() : result;
    }

    public List<Type> getTypes() {
        return types;
    }

    public List<Vod> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<Vod> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
