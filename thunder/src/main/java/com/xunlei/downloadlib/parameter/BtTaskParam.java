package com.xunlei.downloadlib.parameter;

public class BtTaskParam {

    public int mSeqId;
    public int mCreateMode;
    public int mMaxConcurrent;
    public String mFilePath;
    public String mTorrentPath;

    public void setSeqId(int seqId) {
        this.mSeqId = seqId;
    }

    public void setCreateMode(int createMode) {
        this.mCreateMode = createMode;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.mMaxConcurrent = maxConcurrent;
    }

    public void setFilePath(String filePath) {
        this.mFilePath = filePath;
    }

    public void setTorrentPath(String torrentPath) {
        this.mTorrentPath = torrentPath;
    }
}
