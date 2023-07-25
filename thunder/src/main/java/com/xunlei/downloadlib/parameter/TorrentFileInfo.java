package com.xunlei.downloadlib.parameter;

public class TorrentFileInfo {

    public int mFileIndex;
    public String mFileName;
    public long mFileSize;
    public int mRealIndex;
    public String mSubPath;
    public String playUrl;
    public String hash;
    public String torrentPath;
    public boolean isSelected;

    public int getFileIndex() {
        return mFileIndex;
    }

    public String getFileName() {
        return mFileName;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public int getRealIndex() {
        return mRealIndex;
    }

    public String getSubPath() {
        return mSubPath;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public String getHash() {
        return hash;
    }

    public String getTorrentPath() {
        return torrentPath;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
