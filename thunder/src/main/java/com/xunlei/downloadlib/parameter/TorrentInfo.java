package com.xunlei.downloadlib.parameter;

public class TorrentInfo {

    public int mFileCount;
    public String mInfoHash;
    public boolean mIsMultiFiles;
    public String mMultiFileBaseFolder;
    public TorrentFileInfo[] mSubFileInfo;

    public int getFileCount() {
        return mFileCount;
    }

    public String getInfoHash() {
        return mInfoHash;
    }

    public boolean isMultiFiles() {
        return mIsMultiFiles;
    }

    public String getMultiFileBaseFolder() {
        return mMultiFileBaseFolder;
    }

    public TorrentFileInfo[] getSubFileInfo() {
        return mSubFileInfo;
    }
}
