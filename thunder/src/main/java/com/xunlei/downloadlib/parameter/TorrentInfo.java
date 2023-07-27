package com.xunlei.downloadlib.parameter;

import com.xunlei.downloadlib.Util;

import java.util.ArrayList;
import java.util.List;

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

    private TorrentFileInfo[] getSubFileInfo() {
        return mSubFileInfo == null ? new TorrentFileInfo[0] : mSubFileInfo;
    }

    public List<TorrentFileInfo> getMedias() {
        List<TorrentFileInfo> items = new ArrayList<>();
        for (TorrentFileInfo item : getSubFileInfo()) if (Util.isMedia(item.getExt())) items.add(item);
        return items;
    }
}
