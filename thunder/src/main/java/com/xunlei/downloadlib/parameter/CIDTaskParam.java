package com.xunlei.downloadlib.parameter;

public class CIDTaskParam {
    public String mBcid;
    public String mCid;
    public int mCreateMode;
    public String mFileName;
    public String mFilePath;
    public long mFileSize;
    public String mGcid;
    public int mSeqId;

    public CIDTaskParam(String str, String str2, String str3, String str4, String str5, long j, int i, int i2) {
        this.mCid = str;
        this.mGcid = str2;
        this.mBcid = str3;
        this.mFilePath = str4;
        this.mFileName = str5;
        this.mFileSize = j;
        this.mCreateMode = i;
        this.mSeqId = i2;
    }

    public void setCid(String str) {
        this.mCid = str;
    }

    public void setGcid(String str) {
        this.mGcid = str;
    }

    public void setBcid(String str) {
        this.mBcid = str;
    }

    public void setFilePath(String str) {
        this.mFilePath = str;
    }

    public void setFileName(String str) {
        this.mFileName = str;
    }

    public void setFileSize(long j) {
        this.mFileSize = j;
    }

    public void setCreateMode(int i) {
        this.mCreateMode = i;
    }

    public void setSeqId(int i) {
        this.mSeqId = i;
    }

    public boolean checkMemberVar() {
        if (this.mCid == null || this.mGcid == null || this.mBcid == null || this.mFilePath == null || this.mFileName == null) {
            return false;
        }
        return true;
    }
}
