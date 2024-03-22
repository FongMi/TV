package com.xunlei.downloadlib.parameter;

public class XLTaskInfo {

    public int mAdditionalResCount;
    public long mAdditionalResPeerBytes;
    public long mAdditionalResPeerSpeed;
    public int mAdditionalResType;
    public long mAdditionalResVipRecvBytes;
    public long mAdditionalResVipSpeed;
    public String mCid;
    public long mDownloadSize;
    public long mDownloadSpeed;
    public int mErrorCode;
    public String mFileName;
    public long mFileSize;
    public String mGcid;
    public int mInfoLen;
    public long mOriginRecvBytes;
    public long mOriginSpeed;
    public long mP2PRecvBytes;
    public long mP2PSpeed;
    public long mP2SRecvBytes;
    public long mP2SSpeed;
    public int mQueryIndexStatus;
    public long mScdnRecvBytes;
    public long mScdnSpeed;
    public long mTaskId;
    public int mTaskStatus;

    public int getTaskStatus() {
        return mTaskStatus;
    }

    public void setTaskStatus(int taskStatus) {
        this.mTaskStatus = taskStatus;
    }

    public String getErrorMsg() {
        return ErrorCode.get(mErrorCode);
    }
}
