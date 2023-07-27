package com.xunlei.downloadlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

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
    private final XLLoader loader;

    private NetworkChangeReceiver receiver;
    private final Context context;
    private boolean init;

    public XLDownloadManager(Context context) {
        this.loader = new XLLoader();
        this.context = context;
        this.init();
    }

    public synchronized void init() {
        if (init) {
            Log.i(TAG, "XLDownloadManager is already init");
            return;
        }
        InitParam param = new InitParam(context.getFilesDir().getAbsolutePath());
        int code = loader.init(param.getSoKey(), "com.android.providers.downloads", param.mAppVersion, "", getPeerId(), getGuid(), param.mStatSavePath, param.mStatCfgSavePath, XLUtil.getNetworkType(context), param.mPermissionLevel, param.mQueryConfOnInit);
        if (code != 9000) {
            init = false;
            Log.i(TAG, "XLDownloadManager init fail");
        } else {
            getDownloadLibVersion(new GetDownloadLibVersion());
            setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
            setLocalProperty("PhoneModel", Build.MODEL);
            setStatReportSwitch(false);
            setSpeedLimit(-1, -1);
            registerReceiver();
            init = true;
        }
    }

    public synchronized void release() {
        unregisterReceiver();
        loader.unInit();
        init = false;
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
        String uuid = Prefers.getString(context, "phoneId5", "");
        if (uuid.isEmpty()) Prefers.put(context, "phoneId5", XLUtil.generatePeerId());
        return uuid;
    }

    private String getGuid() {
        return XLUtil.generateGuid().mGuid;
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

    public void setHttpHeaderProperty(long taskId, String key, String value) {
        loader.setHttpHeaderProperty(taskId, key, value);
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

    public void getTorrentInfo(String path, TorrentInfo info) {
        loader.getTorrentInfo(path, info);
    }

    public void getBtSubTaskInfo(long taskId, int index, BtSubTaskDetail detail) {
        loader.getBtSubTaskInfo(taskId, index, detail);
    }

    public void selectBtSubTask(long taskId, BtIndexSet btIndexSet) {
        loader.selectBtSubTask(taskId, btIndexSet);
    }

    public void deselectBtSubTask(long taskId, BtIndexSet btIndexSet) {
        loader.deselectBtSubTask(taskId, btIndexSet);
    }

    public String parserThunderUrl(String url) {
        ThunderUrlInfo thunderUrlInfo = new ThunderUrlInfo();
        loader.parserThunderUrl(url, thunderUrlInfo);
        return thunderUrlInfo.mUrl;
    }

    public int getFileNameFromUrl(String url, GetFileName name) {
        return loader.getFileNameFromUrl(url, name);
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