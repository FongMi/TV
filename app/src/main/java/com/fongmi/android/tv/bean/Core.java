package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class Core {

    @SerializedName("auth")
    private String auth;
    @SerializedName("name")
    private String name;
    @SerializedName("pass")
    private String pass;
    @SerializedName("broker")
    private String broker;

    public String getAuth() {
        return TextUtils.isEmpty(auth) ? "" : auth;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getPass() {
        return TextUtils.isEmpty(pass) ? "" : pass;
    }

    public String getBroker() {
        return TextUtils.isEmpty(broker) ? "" : broker;
    }
}
