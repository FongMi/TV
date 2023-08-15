package com.xunlei.downloadlib.parameter;

import com.github.catvod.utils.Path;

import java.io.File;

public class GetTaskId {

    public long mTaskId;
    public File mSavePath;
    public String mFileName;
    public String mRealUrl;

    public GetTaskId(File savePath) {
        this.mSavePath = savePath;
    }

    public GetTaskId(String url, File savePath) {
        File file = new File(url.substring(7));
        File dest = new File(savePath, file.getName());
        Path.copy(file, dest);
        this.mFileName = file.getName();
        this.mSavePath = savePath;
        this.mRealUrl = url;
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
