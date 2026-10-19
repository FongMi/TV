package com.xunlei.downloadlib.parameter;

import com.xunlei.downloadlib.Util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TorrentInfo {

    public TorrentFileInfo[] mSubFileInfo;
    public String mMultiFileBaseFolder;
    public boolean mIsMultiFiles;
    public String mInfoHash;
    public int mFileCount;
    public File mFile;

    public TorrentInfo(File file) {
        this.mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    public List<TorrentFileInfo> getMedias() {
        if (mSubFileInfo == null) return List.of();
        return Arrays.stream(mSubFileInfo)
                .filter(item -> Util.isMedia(item.getExt(), item.getFileSize()))
                .map(item -> item.file(getFile()))
                .sorted()
                .toList();
    }
}
