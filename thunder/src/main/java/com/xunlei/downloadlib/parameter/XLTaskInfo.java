package com.xunlei.downloadlib.parameter;

import android.os.Parcel;
import android.os.Parcelable;

public class XLTaskInfo implements Parcelable {
    public static final Creator<XLTaskInfo> CREATOR = new Creator<XLTaskInfo>() {
        @Override
        public XLTaskInfo createFromParcel(Parcel parcel) {
            return new XLTaskInfo(parcel);
        }

        @Override
        public XLTaskInfo[] newArray(int i) {
            return new XLTaskInfo[i];
        }
    };
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

    public int describeContents() {
        return 0;
    }

    public XLTaskInfo() {
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTaskId);
        parcel.writeString(this.mFileName);
        parcel.writeInt(this.mInfoLen);
        parcel.writeInt(this.mTaskStatus);
        parcel.writeInt(this.mErrorCode);
        parcel.writeLong(this.mFileSize);
        parcel.writeLong(this.mDownloadSize);
        parcel.writeLong(this.mDownloadSpeed);
        parcel.writeInt(this.mQueryIndexStatus);
        parcel.writeString(this.mCid);
        parcel.writeString(this.mGcid);
        parcel.writeLong(this.mOriginSpeed);
        parcel.writeLong(this.mOriginRecvBytes);
        parcel.writeLong(this.mP2SSpeed);
        parcel.writeLong(this.mP2SRecvBytes);
        parcel.writeLong(this.mP2PSpeed);
        parcel.writeLong(this.mP2PRecvBytes);
        parcel.writeInt(this.mAdditionalResCount);
        parcel.writeInt(this.mAdditionalResType);
        parcel.writeLong(this.mAdditionalResVipSpeed);
        parcel.writeLong(this.mAdditionalResVipRecvBytes);
        parcel.writeLong(this.mAdditionalResPeerSpeed);
        parcel.writeLong(this.mAdditionalResPeerBytes);
        parcel.writeLong(this.mScdnSpeed);
        parcel.writeLong(this.mScdnRecvBytes);
    }

    public XLTaskInfo(Parcel parcel) {
        this.mTaskId = parcel.readLong();
        this.mFileName = parcel.readString();
        this.mInfoLen = parcel.readInt();
        this.mTaskStatus = parcel.readInt();
        this.mErrorCode = parcel.readInt();
        this.mFileSize = parcel.readLong();
        this.mDownloadSize = parcel.readLong();
        this.mDownloadSpeed = parcel.readLong();
        this.mQueryIndexStatus = parcel.readInt();
        this.mCid = parcel.readString();
        this.mGcid = parcel.readString();
        this.mOriginSpeed = parcel.readLong();
        this.mOriginRecvBytes = parcel.readLong();
        this.mP2SSpeed = parcel.readLong();
        this.mP2SRecvBytes = parcel.readLong();
        this.mP2PSpeed = parcel.readLong();
        this.mP2PRecvBytes = parcel.readLong();
        this.mAdditionalResCount = parcel.readInt();
        this.mAdditionalResType = parcel.readInt();
        this.mAdditionalResVipSpeed = parcel.readLong();
        this.mAdditionalResVipRecvBytes = parcel.readLong();
        this.mAdditionalResPeerSpeed = parcel.readLong();
        this.mAdditionalResPeerBytes = parcel.readLong();
        this.mScdnSpeed = parcel.readLong();
        this.mScdnRecvBytes = parcel.readLong();
    }
}
