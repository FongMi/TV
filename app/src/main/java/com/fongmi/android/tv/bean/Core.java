package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.Utils;
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
    @SerializedName("resp")
    private String resp;

    public String getAuth() {
        return TextUtils.isEmpty(auth) ? "" : Utils.convert(auth);
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

    public String getResp() {
        return TextUtils.isEmpty(resp) ? "" : resp;
    }
}
