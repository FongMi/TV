package com.xunlei.downloadlib.parameter;

public class InitParam {
    public String mAppKey;
    public String mAppVersion;
    public int mPermissionLevel;
    public int mQueryConfOnInit;
    public String mStatCfgSavePath;
    public String mStatSavePath;

    public InitParam() {
    }

    public InitParam(String str, String str2, String str3, String str4, int i, int i2) {
        this.mAppKey = str;
        this.mAppVersion = str2;
        this.mStatSavePath = str3;
        this.mStatCfgSavePath = str4;
        this.mPermissionLevel = i;
        this.mQueryConfOnInit = i2;
    }

    public boolean checkMemberVar() {
        if (this.mAppKey == null || this.mAppVersion == null || this.mStatSavePath == null || this.mStatCfgSavePath == null) {
            return false;
        }
        return true;
    }
}
