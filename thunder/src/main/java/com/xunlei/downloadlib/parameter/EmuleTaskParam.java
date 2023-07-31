package com.xunlei.downloadlib.parameter;

public class EmuleTaskParam {

    public int mCreateMode;
    public String mFileName;
    public String mFilePath;
    public int mSeqId;
    public String mUrl;

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
}
