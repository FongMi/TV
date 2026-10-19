package com.xunlei.downloadlib.parameter;

import android.net.Uri;
import android.text.TextUtils;

import com.xunlei.downloadlib.Util;

import java.io.File;
import java.util.Locale;

public class TorrentFileInfo implements Comparable<TorrentFileInfo> {

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

    public long getFileSize() {
        return mFileSize;
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

    public String getSize() {
        return Util.size(mFileSize);
    }

    public String getPlayUrl() {
        String name = TextUtils.isEmpty(mSubPath) ? getFileName() : mSubPath + "/" + getFileName();
        return new Uri.Builder()
                .scheme("magnet")
                .authority("")
                .path(getFile().getAbsolutePath())
                .appendQueryParameter("name", name)
                .appendQueryParameter("index", String.valueOf(getFileIndex()))
                .build()
                .toString();
    }

    public String getExt() {
        return getFileName().contains(".") ? getFileName().substring(getFileName().lastIndexOf(".") + 1).toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public int compareTo(TorrentFileInfo other) {
        return this.getFileName().compareToIgnoreCase(other.getFileName());
    }
}
