package com.xunlei.downloadlib.parameter;

public class P2spTaskParam {
    public String mCookie;
    public int mCreateMode;
    public String mFileName;
    public String mFilePath;
    public String mPass;
    public String mRefUrl;
    public int mSeqId;
    public String mUrl;
    public String mUser;

    public P2spTaskParam() {
    }

    public P2spTaskParam(String str, String str2, String str3, String str4, String str5, String str6, String str7, int i, int i2) {
        this.mFileName = str;
        this.mFilePath = str2;
        this.mUrl = str3;
        this.mCookie = str4;
        this.mRefUrl = str5;
        this.mUser = str6;
        this.mPass = str7;
        this.mCreateMode = i;
        this.mSeqId = i2;
    }

    public void setUrl(String str) {
        this.mUrl = str;
    }

    public void setFileName(String str) {
        this.mFileName = str;
    }

    public void setFilePath(String str) {
        this.mFilePath = str;
    }

    public void setCookie(String str) {
        this.mCookie = str;
    }

    public void setRefUrl(String str) {
        this.mRefUrl = str;
    }

    public void setUser(String str) {
        this.mUser = str;
    }

    public void setPass(String str) {
        this.mPass = str;
    }

    public void setCreateMode(int i) {
        this.mCreateMode = i;
    }

    public void setSeqId(int i) {
        this.mSeqId = i;
    }

    public boolean checkMemberVar() {
        if (this.mFileName == null || this.mFilePath == null || this.mUrl == null || this.mCookie == null || this.mRefUrl == null || this.mUser == null || this.mPass == null) {
            return false;
        }
        return true;
    }
}
