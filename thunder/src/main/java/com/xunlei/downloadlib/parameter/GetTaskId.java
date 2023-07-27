package com.xunlei.downloadlib.parameter;

public class GetTaskId {

    private long mTaskId;
    private String mSavePath;

    public long getTaskId() {
        return this.mTaskId;
    }

    public void setSavePath(String savePath) {
        this.mSavePath = savePath;
    }

    public String getSavePath() {
        return mSavePath;
    }
}
