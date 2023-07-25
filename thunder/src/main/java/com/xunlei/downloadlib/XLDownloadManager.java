package com.xunlei.downloadlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.xunlei.downloadlib.android.XLUtil;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.BtTaskStatus;
import com.xunlei.downloadlib.parameter.CIDTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.ErrorCodeToMsg;
import com.xunlei.downloadlib.parameter.GetDownloadHead;
import com.xunlei.downloadlib.parameter.GetDownloadLibVersion;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.InitParam;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.MaxDownloadSpeedParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.PeerResourceParam;
import com.xunlei.downloadlib.parameter.ScdnResourceParam;
import com.xunlei.downloadlib.parameter.ServerResourceParam;
import com.xunlei.downloadlib.parameter.ThunderUrlInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.UrlQuickInfo;
import com.xunlei.downloadlib.parameter.XLConstant;
import com.xunlei.downloadlib.parameter.XLProductInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfoEx;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class XLDownloadManager {

    public static XLConstant.XLManagerStatus mDownloadManagerState = XLConstant.XLManagerStatus.MANAGER_UNINIT;

    private static final String TAG = XLDownloadManager.class.getSimpleName();
    private static boolean mAllowExecution = true;
    private static Map<String, Object> mErrcodeStringMap;
    private static boolean mIsLoadErrcodeMsg;
    private static int mRunningRefCount = 0;
    private XLAppKeyChecker mAppkeyChecker;
    private Timer mGetGuidTimer;
    private TimerTask mGetGuidTimerTask;
    private XLLoader mLoader;
    private int mQueryGuidCount;
    private NetworkChangeReceiver mReceiver;

    private static class Loader {
        static volatile XLDownloadManager INSTANCE = new XLDownloadManager();
    }

    public static XLDownloadManager get() {
        return Loader.INSTANCE;
    }

    private XLDownloadManager() {
        this.mLoader = null;
        this.mReceiver = null;
        this.mAppkeyChecker = null;
        this.mQueryGuidCount = 0;
        this.mLoader = new XLLoader();
    }

    static int access$208(XLDownloadManager xLDownloadManager) {
        int i = xLDownloadManager.mQueryGuidCount;
        xLDownloadManager.mQueryGuidCount = i + 1;
        return i;
    }

    public XLConstant.XLManagerStatus getManagerStatus() {
        return mDownloadManagerState;
    }

    private void doMonitorNetworkChange() {
        Log.i(TAG, "doMonitorNetworkChange()");
        if (this.mContext != null && this.mReceiver == null) {
            this.mReceiver = new NetworkChangeReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            Log.i(TAG, "register Receiver");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    private void undoMonitorNetworkChange() {
        NetworkChangeReceiver networkChangeReceiver;
        Log.i(TAG, "undoMonitorNetworkChange()");
        Context context = this.mContext;
        if (context != null && (networkChangeReceiver = this.mReceiver) != null) {
            try {
                context.unregisterReceiver(networkChangeReceiver);
                Log.i(TAG, "unregister Receiver");
            } catch (IllegalArgumentException unused) {
                Log.e(TAG, "Receiver not registered");
            }
            this.mReceiver = null;
        }
    }

    private synchronized void increRefCount() {
        mRunningRefCount++;
    }

    private synchronized void decreRefCount() {
        mRunningRefCount--;
    }

    public synchronized int init(Context context, InitParam initParam) {
        return init(context, initParam, true);
    }

    public synchronized int init(Context context, InitParam initParam, boolean z) {
        if (!mIsLoadErrcodeMsg) {
            loadErrcodeString(context);
            mIsLoadErrcodeMsg = true;
        }
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(context == null || initParam == null)) {
            if (initParam.checkMemberVar()) {
                this.mContext = context;
                mAllowExecution = z;
                if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING) {
                    Log.i(TAG, "XLDownloadManager is already init");
                    return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
                }
                if (this.mLoader != null) {
                    XLAppKeyChecker xLAppKeyChecker = new XLAppKeyChecker(context, initParam.mAppKey);
                    this.mAppkeyChecker = xLAppKeyChecker;
                    if (!xLAppKeyChecker.verify()) {
                        Log.i(TAG, "appKey check failed");
                        return XLConstant.XLErrorCode.APPKEY_CHECKER_ERROR;
                    }
                    Log.i(TAG, "appKey check successful");
                    i = this.mLoader.init(this.mAppkeyChecker.getSoAppKey(), "com.android.providers.downloads", initParam.mAppVersion, "", getPeerid(), getGuid(), initParam.mStatSavePath, initParam.mStatCfgSavePath, mAllowExecution ? XLUtil.getNetworkType(context) : 0, initParam.mPermissionLevel, initParam.mQueryConfOnInit);
                    if (i != 9000) {
                        mDownloadManagerState = XLConstant.XLManagerStatus.MANAGER_INIT_FAIL;
                    } else {
                        mDownloadManagerState = XLConstant.XLManagerStatus.MANAGER_RUNNING;
                        doMonitorNetworkChange();
                        setLocalProperty("PhoneModel", Build.MODEL);
                    }
                }
                return i;
            }
        }
        return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
    }

    public synchronized int uninit() {
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (mRunningRefCount != 0) {
            Log.i(TAG, "some function of XLDownloadManager is running, uninit failed!");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        if (!(mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_UNINIT || this.mLoader == null)) {
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING) {
                undoMonitorNetworkChange();
            }
            stopGetGuidTimer();
            i = this.mLoader.unInit();
            mDownloadManagerState = XLConstant.XLManagerStatus.MANAGER_UNINIT;
            this.mContext = null;
        }
        return i;
    }

    int notifyNetWorkType(int i, XLLoader xLLoader) {
        if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && xLLoader != null) {
            try {
                return xLLoader.notifyNetWorkType(i);
            } catch (Error e) {
                Log.e(TAG, "notifyNetWorkType failed," + e.getMessage());
            }
        }
        return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
    }

    public int createP2spTask(P2spTaskParam p2spTaskParam, GetTaskId getTaskId) {
        XLLoader xLLoader;
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(p2spTaskParam == null || getTaskId == null || !p2spTaskParam.checkMemberVar())) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && (xLLoader = this.mLoader) != null) {
                i = xLLoader.createP2spTask(p2spTaskParam.mUrl, p2spTaskParam.mRefUrl, p2spTaskParam.mCookie, p2spTaskParam.mUser, p2spTaskParam.mPass, p2spTaskParam.mFilePath, p2spTaskParam.mFileName, p2spTaskParam.mCreateMode, p2spTaskParam.mSeqId, getTaskId);
            }
            decreRefCount();
        }
        return i;
    }

    public int releaseTask(long j) {
        XLLoader xLLoader;
        increRefCount();
        int releaseTask = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.releaseTask(j);
        decreRefCount();
        return releaseTask;
    }

    int setTaskAppInfo(long j, String str, String str2, String str3) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null || str2 == null || str3 == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setTaskAppInfo(j, str, str2, str3);
    }

    public int setTaskAllowUseResource(long j, int i) {
        XLLoader xLLoader;
        increRefCount();
        int taskAllowUseResource = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setTaskAllowUseResource(j, i);
        decreRefCount();
        return taskAllowUseResource;
    }

    public int setTaskUidWithPid(long j, int i, int i2) {
        XLLoader xLLoader;
        increRefCount();
        int taskUidWithPid = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setTaskUidWithPid(j, i, i2);
        decreRefCount();
        return taskUidWithPid;
    }

    public int startTask(long j) {
        XLLoader xLLoader;
        increRefCount();
        int startTask = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.startTask(j);
        decreRefCount();
        return startTask;
    }

    int switchOriginToAllResDownload(long j) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.switchOriginToAllResDownload(j);
    }

    public int stopTask(long j) {
        XLLoader xLLoader;
        increRefCount();
        int stopTask = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.stopTask(j);
        Log.i(TAG, "XLStopTask()----- ret=" + stopTask);
        decreRefCount();
        return stopTask;
    }

    public int stopTaskWithReason(long j, int i) {
        XLLoader xLLoader;
        increRefCount();
        int stopTaskWithReason = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.stopTaskWithReason(j, i);
        Log.i(TAG, "XLStopTask()----- ret=" + stopTaskWithReason);
        decreRefCount();
        return stopTaskWithReason;
    }

    public int getTaskInfo(long j, int i, XLTaskInfo xLTaskInfo) {
        XLLoader xLLoader;
        increRefCount();
        int taskInfo = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || xLTaskInfo == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getTaskInfo(j, i, xLTaskInfo);
        decreRefCount();
        return taskInfo;
    }

    public int getTaskInfoEx(long j, XLTaskInfoEx xLTaskInfoEx) {
        XLLoader xLLoader;
        increRefCount();
        int taskInfoEx = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || xLTaskInfoEx == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getTaskInfoEx(j, xLTaskInfoEx);
        decreRefCount();
        return taskInfoEx;
    }

    public int getLocalUrl(String str, XLTaskLocalUrl xLTaskLocalUrl) {
        XLLoader xLLoader;
        increRefCount();
        int localUrl = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || xLTaskLocalUrl == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getLocalUrl(str, xLTaskLocalUrl);
        decreRefCount();
        return localUrl;
    }

    public int addServerResource(long j, ServerResourceParam serverResourceParam) {
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (serverResourceParam != null && serverResourceParam.checkMemberVar()) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && this.mLoader != null) {
                Log.i(TAG, "respara.mUrl=" + serverResourceParam.mUrl);
                i = this.mLoader.addServerResource(j, serverResourceParam.mUrl, serverResourceParam.mRefUrl, serverResourceParam.mCookie, serverResourceParam.mResType, serverResourceParam.mStrategy);
            }
            decreRefCount();
        }
        return i;
    }

    public int addPeerResource(long j, PeerResourceParam peerResourceParam) {
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (peerResourceParam != null && peerResourceParam.checkMemberVar()) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING) {
                XLLoader xLLoader = this.mLoader;
                if (xLLoader != null) {
                    i = xLLoader.addPeerResource(j, peerResourceParam.mPeerId, peerResourceParam.mUserId, peerResourceParam.mJmpKey, peerResourceParam.mVipCdnAuth, peerResourceParam.mInternalIp, peerResourceParam.mTcpPort, peerResourceParam.mUdpPort, peerResourceParam.mResLevel, peerResourceParam.mResPriority, peerResourceParam.mCapabilityFlag, peerResourceParam.mResType);
                }
            }
            decreRefCount();
        }
        return i;
    }

    public int addScdnResource(long j, ScdnResourceParam scdnResourceParam) {
        Log.d(TAG, "XLDownloadManager::addScdnResource beg, taskId=[" + j + "]");
        if (scdnResourceParam == null || scdnResourceParam.unixPath.isEmpty()) {
            return XLConstant.XLErrorCode.INVALID_ARGUMENT;
        }
        if (this.mLoader == null) {
            Log.e(TAG, "XLDownloadManager::addScdnResource mLoader is null, taskId=[" + j + "]");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        } else if (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING) {
            Log.e(TAG, "XLDownloadManager::addScdnResource not running, taskId=[" + j + "]");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        } else {
            increRefCount();
            int addScdnResource = this.mLoader.addScdnResource(j, scdnResourceParam.unixPath);
            decreRefCount();
            Log.d(TAG, "XLDownloadManager::addScdnResource end, ret=[" + addScdnResource + "]");
            return addScdnResource;
        }
    }

    public int removeServerResource(long j, int i) {
        XLLoader xLLoader;
        increRefCount();
        int removeAddedServerResource = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.removeAddedServerResource(j, i);
        decreRefCount();
        return removeAddedServerResource;
    }

    int requeryTaskIndex(long j) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.requeryIndex(j);
    }

    public int setOriginUserAgent(long j, String str) {
        XLLoader xLLoader;
        increRefCount();
        int originUserAgent = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setOriginUserAgent(j, str);
        decreRefCount();
        return originUserAgent;
    }

    public int setUserId(String str) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setUserId(str);
    }

    public int getDownloadHeader(long j, GetDownloadHead getDownloadHead) {
        XLLoader xLLoader;
        increRefCount();
        int downloadHeader = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || getDownloadHead == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getDownloadHeader(j, getDownloadHead);
        decreRefCount();
        return downloadHeader;
    }

    public int setFileName(long j, String str) {
        XLLoader xLLoader;
        increRefCount();
        int fileName = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setFileName(j, str);
        decreRefCount();
        return fileName;
    }

    int notifyNetWorkCarrier(int i) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setNotifyNetWorkCarrier(i);
    }

    int notifyWifiBSSID(String str, XLLoader xLLoader) {
        if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && xLLoader != null) {
            if (str == null || str.length() == 0 || str == "<unknown ssid>") {
                str = "";
            }
            try {
                return xLLoader.setNotifyWifiBSSID(str);
            } catch (Error e) {
                Log.e(TAG, "setNotifyWifiBSSID failed," + e.getMessage());
            }
        }
        return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
    }

    public int setDownloadTaskOrigin(long j, String str) {
        XLLoader xLLoader;
        increRefCount();
        int downloadTaskOrigin = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setDownloadTaskOrigin(j, str);
        decreRefCount();
        return downloadTaskOrigin;
    }

    int setMac(String str) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setMac(str);
    }

    int setImei(String str) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setImei(str);
    }

    private int setLocalProperty(String str, String str2) {
        XLLoader xLLoader;
        return (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null || str2 == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setLocalProperty(str, str2);
    }

    public int setOSVersion(String str) {
        XLLoader xLLoader;
        increRefCount();
        int miUiVersion = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setMiUiVersion(str);
        decreRefCount();
        return miUiVersion;
    }

    public int setHttpHeaderProperty(long j, String str, String str2) {
        XLLoader xLLoader;
        increRefCount();
        int httpHeaderProperty = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || str == null || str2 == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setHttpHeaderProperty(j, str, str2);
        decreRefCount();
        return httpHeaderProperty;
    }

    public int getDownloadLibVersion(GetDownloadLibVersion getDownloadLibVersion) {
        XLLoader xLLoader;
        increRefCount();
        int downloadLibVersion = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || getDownloadLibVersion == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getDownloadLibVersion(getDownloadLibVersion);
        decreRefCount();
        return downloadLibVersion;
    }

    public int getProductInfo(XLProductInfo xLProductInfo) {
        increRefCount();
        if (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || this.mContext == null || xLProductInfo == null) {
            decreRefCount();
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        xLProductInfo.mProductKey = this.mAppkeyChecker.getSoAppKey();
        xLProductInfo.mProductName = "com.android.providers.downloads";
        return 9000;
    }

    private String getPeerid() {
        String peerid;
        if (mAllowExecution && (peerid = XLUtil.getPeerId()) != null) {
            return peerid;
        }
//        return "08318B49CB04004V";
        String uuid = PreferenceMgr.getString(mContext, "phoneId5", null);
        if (uuid == null || uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString().replace("-", "");
            uuid = uuid.substring(0, 12).toUpperCase() + "004V";
            Log.d(TAG, "getPeerid3: " + uuid);
            PreferenceMgr.put(mContext, "phoneId5", uuid);
        }
        return uuid;
    }

    private String getGuid() {
        if (!mAllowExecution) {
            return "00000000000000_000000000000";
        }
        new XLUtil.GuidInfo();
        XLUtil.GuidInfo generateGuid = XLUtil.generateGuid();
        if (generateGuid.mType != XLUtil.GUID_TYPE.ALL) {
            Log.i(TAG, "Start the GetGuidTimer");
            startGetGuidTimer();
        }
        return generateGuid.mGuid;
    }

    private void startGetGuidTimer() {
        this.mGetGuidTimer = new Timer();
        TimerTask r2 = new TimerTask() {
            /* class com.xunlei.downloadlib.XLDownloadManager.C19271 */

            public void run() {
                if (XLDownloadManager.this.mQueryGuidCount < 5) {
                    XLDownloadManager.access$208(XLDownloadManager.this);
                    new XLUtil.GuidInfo();
                    XLUtil.GuidInfo generateGuid = XLUtil.generateGuid();
                    if (generateGuid.mType == XLUtil.GUID_TYPE.ALL) {
                        XLDownloadManager.this.stopGetGuidTimer();
                    }
                    if (generateGuid.mType != XLUtil.GUID_TYPE.DEFAULT) {
                        XLDownloadManager.this.mLoader.setLocalProperty("Guid", generateGuid.mGuid);
                        return;
                    }
                    return;
                }
                XLDownloadManager.this.stopGetGuidTimer();
            }
        };
        this.mGetGuidTimerTask = r2;
        this.mGetGuidTimer.schedule(r2, 5000, 60000);
    }

    /* access modifiers changed from: private */
    public void stopGetGuidTimer() {
        Timer timer = this.mGetGuidTimer;
        if (timer instanceof Timer) {
            timer.cancel();
            this.mGetGuidTimer.purge();
            this.mGetGuidTimer = null;
            Log.i(TAG, "stopGetGuidTimer");
        }
        TimerTask timerTask = this.mGetGuidTimerTask;
        if (timerTask instanceof TimerTask) {
            timerTask.cancel();
            this.mGetGuidTimerTask = null;
        }
    }

    public int enterPrefetchMode(long j) {
        XLLoader xLLoader;
        increRefCount();
        int enterPrefetchMode = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.enterPrefetchMode(j);
        decreRefCount();
        return enterPrefetchMode;
    }

    public int setTaskLxState(long j, int i, int i2) {
        XLLoader xLLoader;
        increRefCount();
        int taskLxState = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setTaskLxState(j, i, i2);
        decreRefCount();
        return taskLxState;
    }

    public int setTaskGsState(long j, int i, int i2) {
        XLLoader xLLoader;
        increRefCount();
        int taskGsState = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setTaskGsState(j, i, i2);
        decreRefCount();
        return taskGsState;
    }

    public int setReleaseLog(boolean z, String str, int i, int i2) {
        int i3;
        XLLoader xLLoader;
        increRefCount();
        if (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) {
            i3 = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        } else {
            i3 = z ? xLLoader.setReleaseLog(1, str, i, i2) : xLLoader.setReleaseLog(0, null, 0, 0);
        }
        decreRefCount();
        return i3;
    }

    public int setReleaseLog(boolean z, String str) {
        return setReleaseLog(z, str, 0, 0);
    }

    public boolean isLogTurnOn() {
        XLLoader xLLoader;
        increRefCount();
        boolean isLogTurnOn = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? false : xLLoader.isLogTurnOn();
        decreRefCount();
        return isLogTurnOn;
    }

    public int setStatReportSwitch(boolean z) {
        XLLoader xLLoader;
        increRefCount();
        int statReportSwitch = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.setStatReportSwitch(z);
        decreRefCount();
        return statReportSwitch;
    }

    public int enterUltimateSpeed(int i) {
        XLLoader xLLoader;
        increRefCount();
        int enterUltimateSpeed = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.enterUltimateSpeed(i);
        decreRefCount();
        return enterUltimateSpeed;
    }

    public int createBtMagnetTask(MagnetTaskParam magnetTaskParam, GetTaskId getTaskId) {
        XLLoader xLLoader;
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(magnetTaskParam == null || getTaskId == null || !magnetTaskParam.checkMemberVar())) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && (xLLoader = this.mLoader) != null) {
                i = xLLoader.createBtMagnetTask(magnetTaskParam.mUrl, magnetTaskParam.mFilePath, magnetTaskParam.mFileName, getTaskId);
            }
            decreRefCount();
        }
        return i;
    }

    public int createEmuleTask(EmuleTaskParam emuleTaskParam, GetTaskId getTaskId) {
        XLLoader xLLoader;
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(emuleTaskParam == null || getTaskId == null || !emuleTaskParam.checkMemberVar())) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && (xLLoader = this.mLoader) != null) {
                i = xLLoader.createEmuleTask(emuleTaskParam.mUrl, emuleTaskParam.mFilePath, emuleTaskParam.mFileName, emuleTaskParam.mCreateMode, emuleTaskParam.mSeqId, getTaskId);
            }
            decreRefCount();
        }
        return i;
    }

    public int createBtTask(BtTaskParam btTaskParam, GetTaskId getTaskId) {
        XLLoader xLLoader;
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(btTaskParam == null || getTaskId == null || !btTaskParam.checkMemberVar())) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && (xLLoader = this.mLoader) != null) {
                i = xLLoader.createBtTask(btTaskParam.mTorrentPath, btTaskParam.mFilePath, btTaskParam.mMaxConcurrent, btTaskParam.mCreateMode, btTaskParam.mSeqId, getTaskId);
            }
            decreRefCount();
        }
        return i;
    }

    public int getTorrentInfo(String str, TorrentInfo torrentInfo) {
        increRefCount();
        XLLoader xLLoader = this.mLoader;
        int torrentInfo2 = (xLLoader == null || str == null || torrentInfo == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getTorrentInfo(str, torrentInfo);
        decreRefCount();
        return torrentInfo2;
    }

    public int getBtSubTaskStatus(long j, BtTaskStatus btTaskStatus, int i, int i2) {
        XLLoader xLLoader;
        increRefCount();
        int btSubTaskStatus = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || btTaskStatus == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getBtSubTaskStatus(j, btTaskStatus, i, i2);
        decreRefCount();
        return btSubTaskStatus;
    }

    public int getBtSubTaskInfo(long j, int i, BtSubTaskDetail btSubTaskDetail) {
        XLLoader xLLoader;
        increRefCount();
        int btSubTaskInfo = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || btSubTaskDetail == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getBtSubTaskInfo(j, i, btSubTaskDetail);
        decreRefCount();
        return btSubTaskInfo;
    }

    public int selectBtSubTask(long j, BtIndexSet btIndexSet) {
        XLLoader xLLoader;
        increRefCount();
        int selectBtSubTask = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || btIndexSet == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.selectBtSubTask(j, btIndexSet);
        decreRefCount();
        return selectBtSubTask;
    }

    public int deselectBtSubTask(long j, BtIndexSet btIndexSet) {
        XLLoader xLLoader;
        increRefCount();
        int deselectBtSubTask = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || btIndexSet == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.deselectBtSubTask(j, btIndexSet);
        decreRefCount();
        return deselectBtSubTask;
    }

    public int btAddServerResource(long j, int i, ServerResourceParam serverResourceParam) {
        if (serverResourceParam == null) {
            Log.e(TAG, "btAddServerResource serverResPara is null, task=[" + j + ":" + i + "]");
            return 9112;
        }
        Log.d(TAG, "btAddServerResource beg, task=[" + j + ":" + i + "] mUrl=[" + serverResourceParam.mUrl + "] mRefUrl=[" + serverResourceParam.mRefUrl + "] mCookie=[" + serverResourceParam.mCookie + "] mResType=[" + serverResourceParam.mResType + "] mStrategy=[" + serverResourceParam.mStrategy + "]");
        if (!serverResourceParam.checkMemberVar()) {
            Log.e(TAG, "btAddServerResource checkMemberVar failed, task=[" + j + ":" + i + "] mUrl=[" + serverResourceParam.mUrl + "] mRefUrl=[" + serverResourceParam.mRefUrl + "] mCookie=[" + serverResourceParam.mCookie + "]");
            return 9112;
        }
        try {
            increRefCount();
            if (this.mLoader == null) {
                Log.e(TAG, "btAddServerResource mLoader is null, task=[" + j + ":" + i + "]");
                return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
            } else if (XLConstant.XLManagerStatus.MANAGER_RUNNING != mDownloadManagerState) {
                Log.e(TAG, "btAddServerResource mDownloadManagerState is invaild, task=[" + j + ":" + i + "] mDownloadManagerState=[" + mDownloadManagerState + "]");
                decreRefCount();
                return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
            } else {
                int btAddServerResource = this.mLoader.btAddServerResource(j, i, serverResourceParam.mUrl, serverResourceParam.mRefUrl, serverResourceParam.mCookie, serverResourceParam.mResType, serverResourceParam.mStrategy);
                if (9000 != btAddServerResource) {
                    Log.e(TAG, "btAddServerResource btAddServerResource failed, task=[" + j + ":" + i + "] errno=[" + btAddServerResource + "]");
                    decreRefCount();
                    return btAddServerResource;
                }
                Log.d(TAG, "btAddServerResource end success, task=[" + j + ":" + i + "]");
                decreRefCount();
                return 9000;
            }
        } finally {
            decreRefCount();
        }
    }

    public int btAddPeerResource(long j, int i, PeerResourceParam peerResourceParam) {
        if (peerResourceParam == null) {
            Log.e(TAG, "btAddPeerResource peerResPara is null, task=[" + j + ":" + i + "]");
            return 9112;
        }
        Log.d(TAG, "btAddPeerResource beg, task=[" + j + ":" + i + "] mPeerId=[" + peerResourceParam.mPeerId + "] mUserId=[" + peerResourceParam.mUserId + "] mJmpKey=[" + peerResourceParam.mJmpKey + "] mVipCdnAuth=[" + peerResourceParam.mVipCdnAuth + "] mInternalIp=[" + peerResourceParam.mInternalIp + "] mTcpPort=[" + peerResourceParam.mTcpPort + "] mUdpPort=[" + peerResourceParam.mUdpPort + "] mResLevel=[" + peerResourceParam.mResLevel + "] mResPriority=[" + peerResourceParam.mResPriority + "] mCapabilityFlag=[" + peerResourceParam.mCapabilityFlag + "] mResType=[" + peerResourceParam.mResType + "]");
        if (!peerResourceParam.checkMemberVar()) {
            Log.e(TAG, "btAddPeerResource peerResPara checkMemberVar failed, task=[" + j + ":" + i + "]");
            return 9112;
        }
        try {
            increRefCount();
            if (this.mLoader == null) {
                Log.e(TAG, "btAddPeerResource mLoader is null, task=[" + j + ":" + i + "]");
                return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
            } else if (XLConstant.XLManagerStatus.MANAGER_RUNNING != mDownloadManagerState) {
                Log.e(TAG, "btAddPeerResource mDownloadManagerState is invaild, task=[" + j + ":" + i + "] mDownloadManagerState=[" + mDownloadManagerState + "]");
                decreRefCount();
                return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
            } else {
                int btAddPeerResource = this.mLoader.btAddPeerResource(j, i, peerResourceParam.mPeerId, peerResourceParam.mUserId, peerResourceParam.mJmpKey, peerResourceParam.mVipCdnAuth, peerResourceParam.mInternalIp, peerResourceParam.mTcpPort, peerResourceParam.mUdpPort, peerResourceParam.mResLevel, peerResourceParam.mResPriority, peerResourceParam.mCapabilityFlag, peerResourceParam.mResType);
                if (9000 != btAddPeerResource) {
                    Log.e(TAG, "btAddPeerResource btAddPeerResource failed, task=[" + j + ":" + i + "] errno=[" + btAddPeerResource + "]");
                    decreRefCount();
                    return btAddPeerResource;
                }
                Log.d(TAG, "btAddPeerResource end success, task=[" + j + ":" + i + "]");
                decreRefCount();
                return 9000;
            }
        } finally {
            decreRefCount();
        }
    }

    public int btRemoveAddedResource(long j, int i, int i2) {
        XLLoader xLLoader;
        increRefCount();
        int btRemoveAddedResource = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.btRemoveAddedResource(j, i, i2);
        decreRefCount();
        return btRemoveAddedResource;
    }

    private void loadErrcodeString(Context context) {
        if (context == null) {
            Log.e(TAG, "loadErrcodeString, context invalid");
        } else {
            mErrcodeStringMap = XLUtil.parseJSONString(ErrorCodeToMsg.ErrCodeToMsg);
        }
    }

    public String getErrorCodeMsg(int i) {
        String num = Integer.toString(i);
        Map<String, Object> map = mErrcodeStringMap;
        String str = null;
        if (!(map == null || num == null)) {
            Object obj = map.get(num);
            if (obj != null) {
                str = obj.toString().trim();
            }
            Log.i(TAG, "errcode:" + i + ", errcodeMsg:" + str);
        }
        return str;
    }

    public int getUrlQuickInfo(long j, UrlQuickInfo urlQuickInfo) {
        XLLoader xLLoader;
        increRefCount();
        int urlQuickInfo2 = (mDownloadManagerState != XLConstant.XLManagerStatus.MANAGER_RUNNING || (xLLoader = this.mLoader) == null || urlQuickInfo == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getUrlQuickInfo(j, urlQuickInfo);
        decreRefCount();
        return urlQuickInfo2;
    }

    public int createCIDTask(CIDTaskParam cIDTaskParam, GetTaskId getTaskId) {
        XLLoader xLLoader;
        int i = XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        if (!(cIDTaskParam == null || getTaskId == null || !cIDTaskParam.checkMemberVar())) {
            increRefCount();
            if (mDownloadManagerState == XLConstant.XLManagerStatus.MANAGER_RUNNING && (xLLoader = this.mLoader) != null) {
                i = xLLoader.createCIDTask(cIDTaskParam.mCid, cIDTaskParam.mGcid, cIDTaskParam.mBcid, cIDTaskParam.mFilePath, cIDTaskParam.mFileName, cIDTaskParam.mFileSize, cIDTaskParam.mCreateMode, cIDTaskParam.mSeqId, getTaskId);
            }
            decreRefCount();
        }
        return i;
    }

    public String parserThunderUrl(String str) {
        ThunderUrlInfo thunderUrlInfo = new ThunderUrlInfo();
        XLLoader xLLoader = this.mLoader;
        if (9000 == ((xLLoader == null || str == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.parserThunderUrl(str, thunderUrlInfo))) {
            return thunderUrlInfo.mUrl;
        }
        return null;
    }

    public int getFileNameFromUrl(String str, GetFileName getFileName) {
        XLLoader xLLoader = this.mLoader;
        return (xLLoader == null || str == null || getFileName == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getFileNameFromUrl(str, getFileName);
    }

    public int getNameFromUrl(String str, String str2) {
        XLLoader xLLoader = this.mLoader;
        return (xLLoader == null || str == null || str2 == null) ? XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR : xLLoader.getNameFromUrl(str, str2);
    }

    public int setSpeedLimit(long j, long j2) {
        Log.d(TAG, "debug: XLDownloadManager::setSpeedLimit beg, maxDownloadSpeed=[" + j + "] maxUploadSpeed=[" + j2 + "]");
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            Log.e(TAG, "error: XLDownloadManager::setSpeedLimit mLoader is null, maxDownloadSpeed=[" + j + "] maxUploadSpeed=[" + j2 + "] ret=[" + XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR + "]");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        int speedLimit = xLLoader.setSpeedLimit(j, j2);
        Log.d(TAG, "debug: XLDownloadManager::setSpeedLimit end, maxDownloadSpeed=[" + j + "] maxUploadSpeed=[" + j2 + "] ret=[" + speedLimit + "]");
        return speedLimit;
    }

    public int setBtPriorSubTask(long j, int i) {
        Log.d(TAG, "XLDownloadManager::setBtPriorSubTask beg, taskId=[" + j + "] fileIndex=[" + i + "]");
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            Log.e(TAG, "XLDownloadManager::setBtPriorSubTask mLoader is null, taskId=[" + j + "] fileIndex=[" + i + "]");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        int btPriorSubTask = xLLoader.setBtPriorSubTask(j, i);
        if (9000 != btPriorSubTask) {
            Log.e(TAG, "XLDownloadManager::setBtPriorSubTask end, taskId=[" + j + "] fileIndex=[" + i + "] ret=[" + btPriorSubTask + "]");
            return btPriorSubTask;
        }
        Log.d(TAG, " XLDownloadManager::setBtPriorSubTask end, taskId=[" + j + "] fileIndex=[" + i + "]");
        return 9000;
    }

    public int getMaxDownloadSpeed(MaxDownloadSpeedParam maxDownloadSpeedParam) {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            Log.e(TAG, "XLDownloadManager::getMaxDownloadSpeed mLoader is null");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        int maxDownloadSpeed = xLLoader.getMaxDownloadSpeed(maxDownloadSpeedParam);
        if (9000 != maxDownloadSpeed) {
            Log.e(TAG, "XLDownloadManager::getMaxDownloadSpeed end, ret=[" + maxDownloadSpeed + "]");
            return maxDownloadSpeed;
        }
        Log.d(TAG, "XLDownloadManager::getMaxDownloadSpeed end, speed=[" + maxDownloadSpeedParam.mSpeed + "] ret=[" + maxDownloadSpeed + "]");
        return maxDownloadSpeed;
    }

    public int statExternalInfo(long j, int i, String str, String str2) {
        Log.d(TAG, "XLDownloadManager::statExternalInfo beg, taskId=[" + j + "] fileIndex=[" + i + "] key=[" + str + "] value=[" + str2 + "]");
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            Log.e(TAG, "XLDownloadManager::statExternalInfo mLoader is null, taskId=[" + j + "] fileIndex=[" + i + "]");
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        int statExternalInfo = xLLoader.statExternalInfo(j, i, str, str2);
        if (9000 != statExternalInfo) {
            Log.e(TAG, "XLDownloadManager::statExternalInfo end, taskId=[" + j + "] fileIndex=[" + i + "] ret=[" + statExternalInfo + "]");
            return statExternalInfo;
        }
        Log.d(TAG, "XLDownloadManager::statExternalInfo end, taskId=[" + j + "] fileIndex=[" + i + "] ret=[" + statExternalInfo + "]");
        return statExternalInfo;
    }

    public int statExternalInfo(long j, int i, String str, int i2) {
        return statExternalInfo(j, i, str, String.valueOf(i2));
    }

    public int initScdn(String str) {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        if (str == null) {
            return 9112;
        }
        if (xLLoader.XYVodSDK_initUnixSock(str) == 0) {
            return 9000;
        }
        return 119000;
    }

    public int releaseScdn() {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        xLLoader.XYVodSDK_release();
        return 9000;
    }

    public int setScdnLogEnable(int i) {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        xLLoader.XYVodSDK_setLogEnable(i);
        return 9000;
    }

    public String getScdnUnixPath() {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return "";
        }
        return xLLoader.XYVodSDK_getUnixSockPath();
    }

    public String getScdnVersion() {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return "";
        }
        return xLLoader.XYVodSDK_getVersion();
    }

    public int stopScdnTask(String str) {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        if (str == null) {
            return 9112;
        }
        xLLoader.XYVodSDK_stopTask(str);
        return 9000;
    }

    public int setScdnStableVersion(String str) {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        if (str == null) {
            return 9112;
        }
        return xLLoader.XYVodSDK_setStableVersion(str);
    }

    public int setNetworkEnable() {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return XLConstant.XLErrorCode.DOWNLOAD_MANAGER_ERROR;
        }
        return xLLoader.XYVodSDK_setNetworkEnable();
    }

    public boolean getSdkEnabled() {
        XLLoader xLLoader = this.mLoader;
        if (xLLoader == null) {
            return false;
        }
        return xLLoader.XYVodSDK_getSdkEnabled();
    }

    private class NetworkChangeHandlerThread implements Runnable {
        private boolean m_allow_execution = true;
        private Context m_context = null;
        private XLLoader m_loader = null;

        public NetworkChangeHandlerThread(Context context, XLLoader xLLoader, boolean z) {
            this.m_context = context;
            this.m_loader = xLLoader;
            this.m_allow_execution = z;
        }

        public void run() {
            if (this.m_allow_execution) {
                int networkType = XLUtil.getNetworkType(this.m_context);
                Log.d(XLDownloadManager.TAG, "NetworkChangeHandlerThread nettype=" + networkType);
                XLDownloadManager.this.notifyNetWorkType(networkType, this.m_loader);
                String bssid = XLUtil.getBSSID(this.m_context);
                Log.d(XLDownloadManager.TAG, "NetworkChangeHandlerThread bssid=" + bssid);
                XLDownloadManager.this.notifyWifiBSSID(bssid, this.m_loader);
                XLUtil.NetWorkCarrier netWorkCarrier = XLUtil.getNetWorkCarrier(this.m_context);
                Log.d(XLDownloadManager.TAG, "NetworkChangeHandlerThread NetWorkCarrier=" + netWorkCarrier);
                XLDownloadManager.this.notifyNetWorkCarrier(netWorkCarrier.ordinal());
            }
        }
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {
        private static final String TAG = "TAG_DownloadReceiver";

        public NetworkChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                XLDownloadManager xLDownloadManager = XLDownloadManager.this;
                new Thread(new NetworkChangeHandlerThread(context, xLDownloadManager.mLoader, XLDownloadManager.mAllowExecution)).start();
            }
        }
    }
}