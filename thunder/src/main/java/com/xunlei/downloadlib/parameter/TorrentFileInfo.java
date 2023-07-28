package com.xunlei.downloadlib.parameter;

import android.text.TextUtils;

import java.io.File;

public class TorrentFileInfo {

    public boolean isSelected;
    public String mFileName;
    public String mSubPath;
    public long mFileSize;
    public int mFileIndex;
    public int mRealIndex;
    public File mFile;

    public String getFileName() {
        return TextUtils.isEmpty(mFileName) ? "" : mFileName;
    }

    public int getFileIndex() {
        return mFileIndex;
    }

    public File getFile() {
        return mFile;
    }

    public TorrentFileInfo file(File file) {
        this.mFile = file;
        return this;
    }

    public String getPlayUrl() {
        return "torrent://" + getFile().getAbsolutePath() + "?name=" + getFileName() + "&index=" + getFileIndex();
    }

    public String getExt() {
        return getFileName().contains(".") ? getFileName().substring(getFileName().lastIndexOf(".") + 1).toLowerCase() : "";
    }
}
