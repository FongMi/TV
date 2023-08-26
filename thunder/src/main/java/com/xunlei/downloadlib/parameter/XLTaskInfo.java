package com.xunlei.downloadlib.parameter;

import android.os.Parcel;
import android.os.Parcelable;

public class XLTaskInfo implements Parcelable {

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

    public XLTaskInfo() {
    }

    public int getTaskStatus() {
        return mTaskStatus;
    }

    public void setTaskStatus(int taskStatus) {
        this.mTaskStatus = taskStatus;
    }

    public String getErrorMsg() {
        return ErrorCode.get(mErrorCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mAdditionalResCount);
        dest.writeLong(this.mAdditionalResPeerBytes);
        dest.writeLong(this.mAdditionalResPeerSpeed);
        dest.writeInt(this.mAdditionalResType);
        dest.writeLong(this.mAdditionalResVipRecvBytes);
        dest.writeLong(this.mAdditionalResVipSpeed);
        dest.writeString(this.mCid);
        dest.writeLong(this.mDownloadSize);
        dest.writeLong(this.mDownloadSpeed);
        dest.writeInt(this.mErrorCode);
        dest.writeString(this.mFileName);
        dest.writeLong(this.mFileSize);
        dest.writeString(this.mGcid);
        dest.writeInt(this.mInfoLen);
        dest.writeLong(this.mOriginRecvBytes);
        dest.writeLong(this.mOriginSpeed);
        dest.writeLong(this.mP2PRecvBytes);
        dest.writeLong(this.mP2PSpeed);
        dest.writeLong(this.mP2SRecvBytes);
        dest.writeLong(this.mP2SSpeed);
        dest.writeInt(this.mQueryIndexStatus);
        dest.writeLong(this.mScdnRecvBytes);
        dest.writeLong(this.mScdnSpeed);
        dest.writeLong(this.mTaskId);
        dest.writeInt(this.mTaskStatus);
    }

    protected XLTaskInfo(Parcel in) {
        this.mAdditionalResCount = in.readInt();
        this.mAdditionalResPeerBytes = in.readLong();
        this.mAdditionalResPeerSpeed = in.readLong();
        this.mAdditionalResType = in.readInt();
        this.mAdditionalResVipRecvBytes = in.readLong();
        this.mAdditionalResVipSpeed = in.readLong();
        this.mCid = in.readString();
        this.mDownloadSize = in.readLong();
        this.mDownloadSpeed = in.readLong();
        this.mErrorCode = in.readInt();
        this.mFileName = in.readString();
        this.mFileSize = in.readLong();
        this.mGcid = in.readString();
        this.mInfoLen = in.readInt();
        this.mOriginRecvBytes = in.readLong();
        this.mOriginSpeed = in.readLong();
        this.mP2PRecvBytes = in.readLong();
        this.mP2PSpeed = in.readLong();
        this.mP2SRecvBytes = in.readLong();
        this.mP2SSpeed = in.readLong();
        this.mQueryIndexStatus = in.readInt();
        this.mScdnRecvBytes = in.readLong();
        this.mScdnSpeed = in.readLong();
        this.mTaskId = in.readLong();
        this.mTaskStatus = in.readInt();
    }

    public static final Creator<XLTaskInfo> CREATOR = new Creator<XLTaskInfo>() {
        @Override
        public XLTaskInfo createFromParcel(Parcel source) {
            return new XLTaskInfo(source);
        }

        @Override
        public XLTaskInfo[] newArray(int size) {
            return new XLTaskInfo[size];
        }
    };
}
