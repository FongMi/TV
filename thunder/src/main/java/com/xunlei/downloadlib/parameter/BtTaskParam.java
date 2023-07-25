package com.xunlei.downloadlib.parameter;

public class BtTaskParam {
    public int mCreateMode;
    public String mFilePath;
    public int mMaxConcurrent;
    public int mSeqId;
    public String mTorrentPath;

    public BtTaskParam() {
    }

    public BtTaskParam(String str, String str2, int i, int i2, int i3) {
        this.mTorrentPath = str;
        this.mFilePath = str2;
        this.mMaxConcurrent = i;
        this.mCreateMode = i2;
        this.mSeqId = i3;
    }

    public void setTorrentPath(String str) {
        this.mTorrentPath = str;
    }

    public void setFilePath(String str) {
        this.mFilePath = str;
    }

    public void setMaxConcurrent(int i) {
        this.mMaxConcurrent = i;
    }

    public void setCreateMode(int i) {
        this.mCreateMode = i;
    }

    public void setSeqId(int i) {
        this.mSeqId = i;
    }

    public boolean checkMemberVar() {
        if (this.mTorrentPath == null || this.mFilePath == null) {
            return false;
        }
        return true;
    }
}
