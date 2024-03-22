package com.xunlei.downloadlib.parameter;

import android.os.Parcel;
import android.os.Parcelable;

public class XLTaskInfo implements Parcelable {
    public static final Creator<XLTaskInfo> CREATOR = new Creator<XLTaskInfo>() {
        @Override // android.os.Parcelable.Creator
        public final XLTaskInfo createFromParcel(Parcel parcel) {
            return new XLTaskInfo(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public final XLTaskInfo[] newArray(int i) {
            return new XLTaskInfo[i];
        }
    };
    public int mAddedHighSourceState;
    public int mAdditionalResCount;
    public long mAdditionalResDCDNBytes;
    public long mAdditionalResDCDNSpeed;
    public long mAdditionalResPeerBytes;
    public long mAdditionalResPeerSpeed;
    public int mAdditionalResType;
    public long mAdditionalResVipRecvBytes;
    public long mAdditionalResVipSpeed;
    public long mCheckedSize;
    public String mCid;
    public int mDcdnState;
    public long mDownloadFileCount;
    public long mDownloadSize;
    public long mDownloadSpeed;
    public int mErrorCode;
    public String mFileName;
    public long mFileSize;
    public String mGcid;
    public int mInfoLen;
    public int mLanPeerResState;
    public int mOriginErrcode;
    public long mOriginRecvBytes;
    public long mOriginSpeed;
    public long mP2PRecvBytes;
    public long mP2PSpeed;
    public long mP2SRecvBytes;
    public long mP2SSpeed;
    public int mQueryIndexStatus;
    public long mTaskId;
    public int mTaskStatus;
    public long mTotalFileCount;

    public XLTaskInfo() {
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
        this.mTotalFileCount = parcel.readLong();
        this.mDownloadFileCount = parcel.readLong();
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
        this.mDcdnState = parcel.readInt();
        this.mCheckedSize = parcel.readLong();
        this.mLanPeerResState = parcel.readInt();
        this.mOriginErrcode = parcel.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTaskId);
        parcel.writeString(this.mFileName);
        parcel.writeInt(this.mInfoLen);
        parcel.writeInt(this.mTaskStatus);
        parcel.writeInt(this.mErrorCode);
        parcel.writeLong(this.mFileSize);
        parcel.writeLong(this.mDownloadSize);
        parcel.writeLong(this.mDownloadSpeed);
        parcel.writeLong(this.mTotalFileCount);
        parcel.writeLong(this.mDownloadFileCount);
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
        parcel.writeInt(this.mDcdnState);
        parcel.writeLong(this.mCheckedSize);
        parcel.writeInt(this.mLanPeerResState);
        parcel.writeInt(this.mOriginErrcode);
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
}
