package com.xunlei.downloadlib.parameter;

import com.xunlei.downloadlib.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TorrentInfo {

    public int mFileCount;
    public String mInfoHash;
    public boolean mIsMultiFiles;
    public String mMultiFileBaseFolder;
    public TorrentFileInfo[] mSubFileInfo;
    public File mFile;

    public TorrentInfo(File file) {
        this.mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    private TorrentFileInfo[] getSubFileInfo() {
        return mSubFileInfo == null ? new TorrentFileInfo[0] : mSubFileInfo;
    }

    public List<TorrentFileInfo> getMedias() {
        List<TorrentFileInfo> items = new ArrayList<>();
        for (TorrentFileInfo item : getSubFileInfo()) if (Util.isMedia(item.getExt(), item.getFileSize())) items.add(item.file(getFile()));
        TorrentFileInfo.Sorter.sort(items);
        return items;
    }
}
