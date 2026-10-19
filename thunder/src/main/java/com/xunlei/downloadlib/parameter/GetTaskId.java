package com.xunlei.downloadlib.parameter;

import android.net.Uri;

import com.github.catvod.runtime.ThunderRuntime;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;

public class GetTaskId implements ThunderRuntime.Task {

    public long mTaskId;
    public File mSavePath;
    public String mFileName;
    public String mRealUrl;

    public GetTaskId(File savePath) {
        this.mSavePath = savePath;
    }

    public GetTaskId(String url, File savePath) {
        File file = new File(Uri.parse(url).getPath());
        saveFile(file, savePath);
        this.mFileName = file.getName();
        this.mSavePath = savePath;
        this.mRealUrl = url;
    }

    public GetTaskId(File savePath, String fileName, String realUrl) {
        this.mSavePath = savePath;
        this.mFileName = fileName;
        this.mRealUrl = realUrl;
    }

    private void saveFile(File file, File savePath) {
        try {
            copyFile(file, new File(savePath, file.getName()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void copyFile(File file, File dest) throws IOException {
        if (!file.isFile()) throw new IOException("Unable to read " + file);
        Path.writeAtomically(dest, file);
    }

    public long getTaskId() {
        return this.mTaskId;
    }

    public File getSavePath() {
        return mSavePath;
    }

    @Override
    public String getFileName() {
        return mFileName;
    }

    @Override
    public String getRealUrl() {
        return mRealUrl;
    }

    @Override
    public File getSaveFile() {
        return new File(getSavePath(), getFileName());
    }
}
