package com.xunlei.downloadlib.parameter;

import java.io.File;

public class GetTaskId {

    public long mTaskId;
    public File mSavePath;
    public String mFileName;
    public String mRealUrl;

    public GetTaskId(File savePath) {
        this.mSavePath = savePath;
    }

    public GetTaskId(File savePath, String fileName, String realUrl) {
        this.mSavePath = savePath;
        this.mFileName = fileName;
        this.mRealUrl = realUrl;
    }

    public long getTaskId() {
        return this.mTaskId;
    }

    public File getSavePath() {
        return mSavePath;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getRealUrl() {
        return mRealUrl;
    }

    public File getSaveFile() {
        return new File(getSavePath(), getFileName());
    }
}
