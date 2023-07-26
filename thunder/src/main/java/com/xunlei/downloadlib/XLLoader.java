package com.xunlei.downloadlib;

import com.ghost.thunder.BuildConfig;
import com.github.catvod.utils.Github;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.GetDownloadLibVersion;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.ThunderUrlInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

class XLLoader {

    public XLLoader() {
        System.load(Github.getSo("xl_stat_" + BuildConfig.FLAVOR));
        System.load(Github.getSo("xl_thunder_sdk_" + BuildConfig.FLAVOR));
    }

    public native int createBtMagnetTask(String str, String str2, String str3, GetTaskId getTaskId);

    public native int createBtTask(String str, String str2, int i, int i2, int i3, GetTaskId getTaskId);

    public native int createEmuleTask(String str, String str2, String str3, int i, int i2, GetTaskId getTaskId);

    public native int createP2spTask(String str, String str2, String str3, String str4, String str5, String str6, String str7, int i, int i2, GetTaskId getTaskId);

    public native int deselectBtSubTask(long j, BtIndexSet btIndexSet);

    public native int getBtSubTaskInfo(long j, int i, BtSubTaskDetail btSubTaskDetail);

    public native int getDownloadLibVersion(GetDownloadLibVersion getDownloadLibVersion);

    public native int getFileNameFromUrl(String str, GetFileName getFileName);

    public native int getLocalUrl(String str, XLTaskLocalUrl xLTaskLocalUrl);

    public native int getTaskInfo(long j, int i, XLTaskInfo xLTaskInfo);

    public native int getTorrentInfo(String str, TorrentInfo torrentInfo);

    public native int init(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, int i, int i2, int i3);

    public native int notifyNetWorkType(int i);

    public native int parserThunderUrl(String str, ThunderUrlInfo thunderUrlInfo);

    public native int releaseTask(long j);

    public native int selectBtSubTask(long j, BtIndexSet btIndexSet);

    public native int setDownloadTaskOrigin(long j, String str);

    public native int setHttpHeaderProperty(long j, String str, String str2);

    public native int setLocalProperty(String str, String str2);

    public native int setMiUiVersion(String str);

    public native int setNotifyNetWorkCarrier(int i);

    public native int setNotifyWifiBSSID(String str);

    public native int setOriginUserAgent(long j, String str);

    public native int setSpeedLimit(long j, long j2);

    public native int setStatReportSwitch(boolean z);

    public native int setTaskGsState(long j, int i, int i2);

    public native int startTask(long j);

    public native int stopTask(long j);

    public native int unInit();
}
