package com.xunlei.downloadlib.parameter;


public class InitParam {
    public String mAppVersion;
    public String mGuid;
    public String mLogSavePath;
    public int mPermissionLevel;
    public String mStatCfgSavePath;
    public String mStatSavePath;

    public InitParam(String path) {
        this.mAppVersion = "1.18.2";
        this.mPermissionLevel = 3;
        this.mStatSavePath = path;
        this.mStatCfgSavePath = path;
        this.mLogSavePath = path;
        this.mGuid = "000000000000000000";
    }

}
