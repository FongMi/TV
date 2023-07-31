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
}
