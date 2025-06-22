package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.hook.Hook;
import com.github.catvod.net.OkHttp;
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
        return !getResp().isEmpty() ? Server.get().getAddress("/tvbus") : TextUtils.isEmpty(auth) ? "" : UrlUtil.convert(auth);
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : getString(name);
    }

    public String getPass() {
        return TextUtils.isEmpty(pass) ? "" : getString(pass);
    }

    public String getBroker() {
        return TextUtils.isEmpty(broker) ? "" : UrlUtil.convert(broker);
    }

    public String getDomain() {
        return TextUtils.isEmpty(domain) ? "" : getString(domain);
    }

    public String getResp() {
        return TextUtils.isEmpty(resp) ? "" : getString(resp);
    }

    public String getSign() {
        return TextUtils.isEmpty(sign) ? "" : getString(sign);
    }

    public String getPkg() {
        return TextUtils.isEmpty(pkg) ? "" : getString(pkg);
    }

    public String getSo() {
        return TextUtils.isEmpty(so) ? "" : UrlUtil.convert(so);
    }

    public Hook getHook() {
        return !getPkg().isEmpty() && !getSign().isEmpty() ? new Hook(getSign(), getPkg()) : null;
    }

    private String getString(String value) {
        return (value = UrlUtil.convert(value)).startsWith("http") ? OkHttp.string(value) : value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Core)) return false;
        Core it = (Core) obj;
        return getSign().equals(it.getSign());
    }
}
