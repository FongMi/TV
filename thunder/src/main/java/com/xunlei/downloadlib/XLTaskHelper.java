package com.xunlei.downloadlib;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;

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
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLConstant;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class XLTaskHelper {

    private final List<Pair<String, String>> requestHeaders;
    private final XLDownloadManager downloadManager;
    private final AtomicInteger seq;

    private static class Loader {
        static volatile XLTaskHelper INSTANCE = new XLTaskHelper();
    }

    public static XLTaskHelper get() {
        return Loader.INSTANCE;
    }

    public XLTaskHelper() {
        this.seq = new AtomicInteger(0);
        this.requestHeaders = new ArrayList<>();
        this.downloadManager = new XLDownloadManager();
    }

    public void init(Context context, String a, String b) {
        InitParam param = new InitParam();
        param.mAppKey = a;
        param.mAppVersion = b;
        param.mStatSavePath = context.getFilesDir().getPath();
        param.mStatCfgSavePath = context.getFilesDir().getPath();
        param.mPermissionLevel = 1;
        param.mQueryConfOnInit = 0;
        downloadManager.init(context, param);
        downloadManager.setStatReportSwitch(false);
        downloadManager.getDownloadLibVersion(new GetDownloadLibVersion());
        downloadManager.setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
        downloadManager.setSpeedLimit(-1, -1);
    }

    public synchronized long addThunderTask(String url, String savePath, String fileName) {
        if (url.startsWith("thunder://")) url = downloadManager.parserThunderUrl(url);
        GetTaskId getTaskId = new GetTaskId();
        if (TextUtils.isEmpty(fileName)) {
            GetFileName getFileName = new GetFileName();
            int code = downloadManager.getFileNameFromUrl(url, getFileName);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
            fileName = getFileName.getFileName();
        }
        if (url.startsWith("ftp://")) {
            P2spTaskParam taskParam = new P2spTaskParam();
            taskParam.setCreateMode(1);
            taskParam.setFileName(fileName);
            taskParam.setFilePath(savePath);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCookie("");
            taskParam.setRefUrl("");
            taskParam.setUser("");
            taskParam.setPass("");
            int code = downloadManager.createP2spTask(taskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
            downloadManager.setDownloadTaskOrigin(getTaskId.getTaskId(), "out_app/out_app_paste");
            downloadManager.setOriginUserAgent(getTaskId.getTaskId(), "AndroidDownloadManager/5.41.2.4980 (Linux; U; Android 4.4.4; Build/KTU84Q)");
            addRequestHeadersToXlEngine(getTaskId.getTaskId());
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam taskParam = new EmuleTaskParam();
            taskParam.setFilePath(savePath);
            taskParam.setFileName(fileName);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCreateMode(1);
            int code = downloadManager.createEmuleTask(taskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
        }
        downloadManager.startTask(getTaskId.getTaskId());
        downloadManager.setTaskGsState(getTaskId.getTaskId(), 0, 2);
        return getTaskId.getTaskId();
    }

    public synchronized long addMagnetTask(String url, String savePath, String fileName) throws Exception {
        if (url.startsWith("magnet:?")) {
            if (TextUtils.isEmpty(fileName)) {
                GetFileName getFileName = new GetFileName();
                downloadManager.getFileNameFromUrl(url, getFileName);
                fileName = getFileName.getFileName();
            }
            MagnetTaskParam magnetTaskParam = new MagnetTaskParam();
            magnetTaskParam.setFileName(fileName);
            magnetTaskParam.setFilePath(savePath);
            magnetTaskParam.setUrl(url);
            GetTaskId getTaskId = new GetTaskId();
            int code = downloadManager.createBtMagnetTask(magnetTaskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
            downloadManager.startTask(getTaskId.getTaskId());
            downloadManager.setTaskGsState(getTaskId.getTaskId(), 0, 2);
            return getTaskId.getTaskId();
        } else {
            throw new Exception("url illegal: " + url);
        }
    }

    public synchronized long addTorrentTask(String torrentPath, String savePath, int index) {
        TorrentInfo torrentInfo = new TorrentInfo();
        downloadManager.getTorrentInfo(torrentPath, torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setFilePath(savePath);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setTorrentPath(torrentPath);
        GetTaskId getTaskId = new GetTaskId();
        int code = downloadManager.createBtTask(taskParam, getTaskId);
        if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
        if (fileInfos.length > 1) {
            List<Integer> list = new CopyOnWriteArrayList<>();
            for (TorrentFileInfo fileInfo : fileInfos) {
                if (fileInfo.mFileIndex != index) {
                    list.add(fileInfo.mFileIndex);
                }
            }
            BtIndexSet btIndexSet = new BtIndexSet(list.size());
            for (int i = 0; i < list.size(); i++) btIndexSet.mIndexSet[i] = list.get(i);
            downloadManager.deselectBtSubTask(getTaskId.getTaskId(), btIndexSet);
        }
        downloadManager.startTask(getTaskId.getTaskId());
        downloadManager.setTaskGsState(getTaskId.getTaskId(), index, 2);
        return getTaskId.getTaskId();
    }

    public void addHeader(String key, String value) {
        requestHeaders.add(Pair.create(key, value));
    }

    private Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(this.requestHeaders);
    }

    private void addRequestHeadersToXlEngine(long taskId) {
        for (Pair<String, String> pair : this.getHeaders()) {
            if (!(pair.first == null || pair.second == null)) {
                downloadManager.setHttpHeaderProperty(taskId, pair.first, pair.second);
            }
        }
    }

    public synchronized String getFileName(String url) {
        if (url.startsWith("thunder://")) url = downloadManager.parserThunderUrl(url);
        GetFileName getFileName = new GetFileName();
        downloadManager.getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    public synchronized TorrentInfo getTorrentInfo(String torrentPath) {
        TorrentInfo torrentInfo = new TorrentInfo();
        downloadManager.getTorrentInfo(torrentPath, torrentInfo);
        return torrentInfo;
    }

    public synchronized String getLocalUrl(String filePath) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        downloadManager.getLocalUrl(filePath, localUrl);
        return localUrl.mStrUrl;
    }

    public synchronized void deleteTask(long taskId, String savePath) {
        stopTask(taskId);
        if (TextUtils.isEmpty(savePath)) return;
        new Handler(Daemon.looper()).post(() -> {
            try {
                new LinuxFileCommand(Runtime.getRuntime()).deleteDirectory(savePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void stopTask(long taskId) {
        downloadManager.stopTask(taskId);
        downloadManager.releaseTask(taskId);
    }

    public synchronized XLTaskInfo getTaskInfo(long taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        downloadManager.getTaskInfo(taskId, 1, taskInfo);
        return taskInfo;
    }

    public synchronized BtSubTaskDetail getBtSubTaskInfo(long taskId, int index) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        downloadManager.getBtSubTaskInfo(taskId, index, subTaskDetail);
        return subTaskDetail;
    }

    public void release() {
        downloadManager.release();
    }
}