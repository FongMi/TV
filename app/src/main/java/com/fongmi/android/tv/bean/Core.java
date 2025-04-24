package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.hook.Hook;
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
    @SerializedName("domain")
    private String domain;
    @SerializedName("resp")
    private String resp;
    @SerializedName("sign")
    private String sign;
    @SerializedName("pkg")
    private String pkg;
    @SerializedName("so")
    private String so;

    public String getAuth() {
        return TextUtils.isEmpty(auth) ? "" : UrlUtil.convert(auth);
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

    public String getDomain() {
        return TextUtils.isEmpty(domain) ? "" : domain;
    }

    public String getResp() {
        return TextUtils.isEmpty(resp) ? "" : resp;
    }

    public String getSign() {
        return TextUtils.isEmpty(sign) ? "" : sign;
    }

    public String getPkg() {
        return TextUtils.isEmpty(pkg) ? "" : pkg;
    }

    public String getSo() {
        return TextUtils.isEmpty(so) ? "" : so;
    }

    public Hook getHook() {
        return !getPkg().isEmpty() && !getSign().isEmpty() ? new Hook(getSign(), getPkg()) : null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Core)) return false;
        Core it = (Core) obj;
        return getSign().equals(it.getSign());
    }
}
