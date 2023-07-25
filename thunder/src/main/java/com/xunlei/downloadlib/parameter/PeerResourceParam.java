package com.xunlei.downloadlib.parameter;

public class PeerResourceParam {
    public int mCapabilityFlag;
    public int mInternalIp;
    public String mJmpKey;
    public String mPeerId;
    public int mResLevel;
    public int mResPriority;
    public int mResType;
    public int mTcpPort;
    public int mUdpPort;
    public long mUserId;
    public String mVipCdnAuth;

    public PeerResourceParam(String str, long j, String str2, String str3, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.mPeerId = str;
        this.mUserId = j;
        this.mJmpKey = str2;
        this.mVipCdnAuth = str3;
        this.mInternalIp = i;
        this.mTcpPort = i2;
        this.mUdpPort = i3;
        this.mResLevel = i4;
        this.mResPriority = i5;
        this.mCapabilityFlag = i6;
        this.mResType = i7;
    }

    public void setPeerId(String str) {
        this.mPeerId = str;
    }

    public void setUserId(long j) {
        this.mUserId = j;
    }

    public void setJmpKey(String str) {
        this.mJmpKey = str;
    }

    public void setVipCdnAuth(String str) {
        this.mVipCdnAuth = str;
    }

    public void setInternalIp(int i) {
        this.mInternalIp = i;
    }

    public void setTcpPort(int i) {
        this.mTcpPort = i;
    }

    public void setUdpPort(int i) {
        this.mUdpPort = i;
    }

    public void setResLevel(int i) {
        this.mResLevel = i;
    }

    public void setResPriority(int i) {
        this.mResPriority = i;
    }

    public void setCapabilityFlag(int i) {
        this.mCapabilityFlag = i;
    }

    public void setResType(int i) {
        this.mResType = i;
    }

    public boolean checkMemberVar() {
        if (this.mPeerId == null || this.mJmpKey == null || this.mVipCdnAuth == null) {
            return false;
        }
        return true;
    }
}
