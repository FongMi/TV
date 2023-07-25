package com.xunlei.downloadlib;

import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskStatus;
import com.xunlei.downloadlib.parameter.GetDownloadHead;
import com.xunlei.downloadlib.parameter.GetDownloadLibVersion;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.MaxDownloadSpeedParam;
import com.xunlei.downloadlib.parameter.ThunderUrlInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.UrlQuickInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfoEx;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

class XLLoader {

    public XLLoader() {
        System.loadLibrary("xl_stat");
        System.loadLibrary("xl_thunder_sdk");
    }

    public native boolean XYVodSDK_getSdkEnabled();

    public native String XYVodSDK_getUnixSockPath();

    public native String XYVodSDK_getVersion();

    public native int XYVodSDK_initUnixSock(String str);

    public native int XYVodSDK_networkChanged();

    public native int XYVodSDK_release();

    public native int XYVodSDK_setLogEnable(int i);

    public native int XYVodSDK_setNetworkEnable();

    public native int XYVodSDK_setStableVersion(String str);

    public native int XYVodSDK_stopTask(String str);

    public native int addPeerResource(long j, String str, long j2, String str2, String str3, int i, int i2, int i3, int i4, int i5, int i6, int i7);

    public native int addScdnResource(long j, String str);

    public native int addServerResource(long j, String str, String str2, String str3, int i, int i2);

    public native int btAddPeerResource(long j, int i, String str, long j2, String str2, String str3, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    public native int btAddServerResource(long j, int i, String str, String str2, String str3, int i2, int i3);

    public native int btRemoveAddedResource(long j, int i, int i2);

    public native int createBtMagnetTask(String str, String str2, String str3, GetTaskId getTaskId);

    public native int createBtTask(String str, String str2, int i, int i2, int i3, GetTaskId getTaskId);

    public native int createCIDTask(String str, String str2, String str3, String str4, String str5, long j, int i, int i2, GetTaskId getTaskId);

    public native int createEmuleTask(String str, String str2, String str3, int i, int i2, GetTaskId getTaskId);

    public native int createP2spTask(String str, String str2, String str3, String str4, String str5, String str6, String str7, int i, int i2, GetTaskId getTaskId);

    public native int deselectBtSubTask(long j, BtIndexSet btIndexSet);

    public native int enterPrefetchMode(long j);

    public native int enterUltimateSpeed(int i);

    public native int getBtSubTaskInfo(long j, int i, BtSubTaskDetail btSubTaskDetail);

    public native int getBtSubTaskStatus(long j, BtTaskStatus btTaskStatus, int i, int i2);

    public native int getDownloadHeader(long j, GetDownloadHead getDownloadHead);

    public native int getDownloadLibVersion(GetDownloadLibVersion getDownloadLibVersion);

    public native int getFileNameFromUrl(String str, GetFileName getFileName);

    public native int getLocalUrl(String str, XLTaskLocalUrl xLTaskLocalUrl);

    public native int getMaxDownloadSpeed(MaxDownloadSpeedParam maxDownloadSpeedParam);

    public native int getNameFromUrl(String str, String str2);

    public native int getTaskInfo(long j, int i, XLTaskInfo xLTaskInfo);

    public native int getTaskInfoEx(long j, XLTaskInfoEx xLTaskInfoEx);

    public native int getTorrentInfo(String str, TorrentInfo torrentInfo);

    public native int getUrlQuickInfo(long j, UrlQuickInfo urlQuickInfo);

    public native int init(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, int i, int i2, int i3);

    public native boolean isLogTurnOn();

    public native int notifyNetWorkType(int i);

    public native int parserThunderUrl(String str, ThunderUrlInfo thunderUrlInfo);

    public native int releaseTask(long j);

    public native int removeAddedServerResource(long j, int i);

    public native int requeryIndex(long j);

    public native int selectBtSubTask(long j, BtIndexSet btIndexSet);

    public native int setBtPriorSubTask(long j, int i);

    public native int setDownloadTaskOrigin(long j, String str);

    public native int setFileName(long j, String str);

    public native int setHttpHeaderProperty(long j, String str, String str2);

    public native int setImei(String str);

    public native int setLocalProperty(String str, String str2);

    public native int setMac(String str);

    public native int setMiUiVersion(String str);

    public native int setNotifyNetWorkCarrier(int i);

    public native int setNotifyWifiBSSID(String str);

    public native int setOriginUserAgent(long j, String str);

    public native int setReleaseLog(int i, String str, int i2, int i3);

    public native int setSpeedLimit(long j, long j2);

    public native int setStatReportSwitch(boolean z);

    public native int setTaskAllowUseResource(long j, int i);

    public native int setTaskAppInfo(long j, String str, String str2, String str3);

    public native int setTaskGsState(long j, int i, int i2);

    public native int setTaskLxState(long j, int i, int i2);

    public native int setTaskUidWithPid(long j, int i, int i2);

    public native int setUserId(String str);

    public native int startTask(long j);

    public native int statExternalInfo(long j, int i, String str, String str2);

    public native int stopTask(long j);

    public native int stopTaskWithReason(long j, int i);

    public native int switchOriginToAllResDownload(long j);

    public native int unInit();
}
