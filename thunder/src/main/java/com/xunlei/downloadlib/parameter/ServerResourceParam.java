package com.xunlei.downloadlib.parameter;

public class ServerResourceParam {
    public String mCookie;
    public String mRefUrl;
    public int mResType;
    public int mStrategy;
    public String mUrl;

    public ServerResourceParam(String str, String str2, String str3, int i, int i2) {
        this.mUrl = str;
        this.mRefUrl = str2;
        this.mCookie = str3;
        this.mResType = i;
        this.mStrategy = i2;
    }

    public void setUrl(String str) {
        this.mUrl = str;
    }

    public void setRefUrl(String str) {
        this.mRefUrl = str;
    }

    public void setCookie(String str) {
        this.mCookie = str;
    }

    public void setRestype(int i) {
        this.mResType = i;
    }

    public void setStrategy(int i) {
        this.mStrategy = i;
    }

    public boolean checkMemberVar() {
        if (this.mUrl == null || this.mRefUrl == null || this.mCookie == null) {
            return false;
        }
        return true;
    }
}
