package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.Utils;
import com.fongmi.hook.PackageManager;
import com.google.gson.annotations.SerializedName;

public class Core extends PackageManager {

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
    @SerializedName("pkg")
    private String pkg;
    @SerializedName("so")
    private String so;

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

    public String getPkg() {
        return TextUtils.isEmpty(pkg) ? "" : pkg;
    }

    public String getSo() {
        return TextUtils.isEmpty(so) ? "" : so;
    }

    public boolean hook() {
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Core)) return false;
        Core it = (Core) obj;
        return getSo().equals(it.getSo());
    }
}
