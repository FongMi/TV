package com.xunlei.downloadlib.parameter;

public class MagnetTaskParam {
    public String mFileName;
    public String mFilePath;
    public String mUrl;

    public MagnetTaskParam(String str, String str2, String str3) {
        this.mFileName = str;
        this.mFilePath = str2;
        this.mUrl = str3;
    }

    public MagnetTaskParam() {
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



    public boolean checkMemberVar() {
        if (this.mFileName == null || this.mFilePath == null || this.mUrl == null) {
            return false;
        }
        return true;
    }
}
