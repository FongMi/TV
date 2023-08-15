package com.xunlei.downloadlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import com.xunlei.downloadlib.android.XLUtil;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.GetDownloadLibVersion;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.InitParam;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.ThunderUrlInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

public class XLDownloadManager {

    private final String TAG = XLDownloadManager.class.getSimpleName();
    private XLLoader loader;

    private NetworkChangeReceiver receiver;
    private Context context;

    public XLDownloadManager() {
        this.context = Init.context();
        this.loader = new XLLoader();
        this.init();
    }

    public void init() {
        InitParam param = new InitParam(context.getFilesDir().getPath());
        loader.init(param.getSoKey(), "com.android.providers.downloads", param.mAppVersion, "", getPeerId(), getGuid(), param.mStatSavePath, param.mStatCfgSavePath, XLUtil.getNetworkType(context), param.mPermissionLevel, param.mQueryConfOnInit);
        getDownloadLibVersion(new GetDownloadLibVersion());
        setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
        setLocalProperty("PhoneModel", Build.MODEL);
        setStatReportSwitch(false);
        setSpeedLimit(-1, -1);
        registerReceiver();
    }

    public void release() {
        if (loader != null) loader.unInit();
        unregisterReceiver();
        context = null;
        loader = null;
    }

    private void registerReceiver() {
        receiver = new NetworkChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(receiver, intentFilter);
    }

    private void unregisterReceiver() {
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }

    private void notifyNetWorkType(int type) {
        try {
            loader.notifyNetWorkType(type);
        } catch (Error e) {
            Log.e(TAG, "notifyNetWorkType failed," + e.getMessage());
        }
    }

    private String getPeerId() {
        String uuid = Prefers.getString("phoneId5", "");
        if (uuid.isEmpty()) Prefers.put("phoneId5", XLUtil.getPeerId());
        return uuid;
    }

    private String getGuid() {
        return XLUtil.getGuid();
    }

    public void releaseTask(long taskId) {
        loader.releaseTask(taskId);
    }

    public void startTask(long taskId) {
        loader.startTask(taskId);
    }

    public void stopTask(long taskId) {
        loader.stopTask(taskId);
    }

    public void getTaskInfo(long taskId, int i, XLTaskInfo taskInfo) {
        loader.getTaskInfo(taskId, i, taskInfo);
    }

    public void getLocalUrl(String filePath, XLTaskLocalUrl localUrl) {
        loader.getLocalUrl(filePath, localUrl);
    }

    public void setOriginUserAgent(long taskId, String userAgent) {
        loader.setOriginUserAgent(taskId, userAgent);
    }

    private void notifyNetWorkCarrier(int carrier) {
        loader.setNotifyNetWorkCarrier(carrier);
    }

    private void notifyWifiBSSID(String bssid) {
        try {
            loader.setNotifyWifiBSSID(bssid);
        } catch (Error e) {
            Log.e(TAG, "setNotifyWifiBSSID failed," + e.getMessage());
        }
    }

    public void setDownloadTaskOrigin(long taskId, String str) {
        loader.setDownloadTaskOrigin(taskId, str);
    }

    private void setLocalProperty(String key, String value) {
        loader.setLocalProperty(key, value);
    }

    public void setOSVersion(String str) {
        loader.setMiUiVersion(str);
    }

    public void getDownloadLibVersion(GetDownloadLibVersion version) {
        loader.getDownloadLibVersion(version);
    }

    public void setTaskGsState(long j, int i, int i2) {
        loader.setTaskGsState(j, i, i2);
    }

    public void setStatReportSwitch(boolean value) {
        loader.setStatReportSwitch(value);
    }

    public int createP2spTask(P2spTaskParam param, GetTaskId taskId) {
        return loader.createP2spTask(param.mUrl, param.mRefUrl, param.mCookie, param.mUser, param.mPass, param.mFilePath, param.mFileName, param.mCreateMode, param.mSeqId, taskId);
    }

    public int createBtMagnetTask(MagnetTaskParam param, GetTaskId taskId) {
        return loader.createBtMagnetTask(param.mUrl, param.mFilePath, param.mFileName, taskId);
    }

    public int createEmuleTask(EmuleTaskParam param, GetTaskId taskId) {
        return loader.createEmuleTask(param.mUrl, param.mFilePath, param.mFileName, param.mCreateMode, param.mSeqId, taskId);
    }

    public int createBtTask(BtTaskParam param, GetTaskId taskId) {
        return loader.createBtTask(param.mTorrentPath, param.mFilePath, param.mMaxConcurrent, param.mCreateMode, param.mSeqId, taskId);
    }

    public void getTorrentInfo(TorrentInfo info) {
        loader.getTorrentInfo(info.getFile().getAbsolutePath(), info);
    }

    public void getBtSubTaskInfo(long taskId, int index, BtSubTaskDetail detail) {
        loader.getBtSubTaskInfo(taskId, index, detail);
    }

    public void deselectBtSubTask(long taskId, BtIndexSet btIndexSet) {
        loader.deselectBtSubTask(taskId, btIndexSet);
    }

    public String parserThunderUrl(String url) {
        ThunderUrlInfo thunderUrlInfo = new ThunderUrlInfo();
        loader.parserThunderUrl(url, thunderUrlInfo);
        return thunderUrlInfo.mUrl;
    }

    public String getFileNameFromUrl(String url) {
        GetFileName getFileName = new GetFileName();
        loader.getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    public void setSpeedLimit(long min, long max) {
        loader.setSpeedLimit(min, max);
    }

    private class NetworkChangeHandlerThread implements Runnable {

        @Override
        public void run() {
            notifyNetWorkCarrier(XLUtil.getNetWorkCarrier(context).ordinal());
            notifyNetWorkType(XLUtil.getNetworkType(context));
            notifyWifiBSSID(XLUtil.getBSSID(context));
        }
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                new Thread(new NetworkChangeHandlerThread()).start();
            }
        }
    }
}