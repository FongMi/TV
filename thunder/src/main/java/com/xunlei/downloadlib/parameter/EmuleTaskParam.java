package com.xunlei.downloadlib.parameter;

public class EmuleTaskParam {
    public int mCreateMode;
    public String mFileName;
    public String mFilePath;
    public int mSeqId;
    public String mUrl;

    public EmuleTaskParam() {
    }

    public EmuleTaskParam(String str, String str2, String str3, int i, int i2) {
        this.mFileName = str;
        this.mFilePath = str2;
        this.mUrl = str3;
        this.mCreateMode = i;
        this.mSeqId = i2;
    }

    public void setFileName(String str) {
        this.mFileName = str;
    }

    public void setFilePath(String str) {
        this.mFilePath = str;
    }

    public void setUrl(String str) {
        this.mUrl = str;
    }

    public void setCreateMode(int i) {
        this.mCreateMode = i;
    }

    public void setSeqId(int i) {
        this.mSeqId = i;
    }

    public boolean checkMemberVar() {
        if (this.mFileName == null || this.mFilePath == null || this.mUrl == null) {
            return false;
        }
        return true;
    }
}
