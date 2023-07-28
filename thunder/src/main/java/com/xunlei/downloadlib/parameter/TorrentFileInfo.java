package com.xunlei.downloadlib.parameter;

import android.text.TextUtils;

import java.io.File;

public class TorrentFileInfo {

    public boolean isSelected;
    public String mFileName;
    public String mSubPath;
    public int mFileIndex;
    public int mRealIndex;
    public long mFileSize;

    public boolean isSelected() {
        return isSelected;
    }

    public String getFileName() {
        return TextUtils.isEmpty(mFileName) ? "" : mFileName;
    }

    public String getSubPath() {
        return mSubPath;
    }

    public int getFileIndex() {
        return mFileIndex;
    }

    public int getRealIndex() {
        return mRealIndex;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public String getPlayUrl(File file) {
        return "torrent://" + file.getAbsolutePath() + "?name=" + getFileName() + "&index=" + getFileIndex();
    }

    public String getExt() {
        return getFileName().contains(".") ? getFileName().substring(getFileName().lastIndexOf(".") + 1).toLowerCase() : "";
    }
}
