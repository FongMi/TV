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
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLConstant;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.io.File;
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

    public void init(Context context) {
        downloadManager.init(context);
        downloadManager.getDownloadLibVersion(new GetDownloadLibVersion());
        downloadManager.setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
        downloadManager.setStatReportSwitch(false);
        downloadManager.setSpeedLimit(-1, -1);
    }

    public synchronized long addThunderTask(String url, String savePath, String fileName) {
        if (url.startsWith("thunder://")) url = downloadManager.parserThunderUrl(url);
        GetTaskId getTaskId = new GetTaskId();
        if (TextUtils.isEmpty(fileName)) {
            GetFileName getFileName = new GetFileName();
            downloadManager.getFileNameFromUrl(url, getFileName);
            fileName = getFileName.getFileName();
        }
        if (url.startsWith("magnet:?")) {
            MagnetTaskParam param = new MagnetTaskParam();
            param.setFileName(fileName);
            param.setFilePath(savePath);
            param.setUrl(url);
            int code = downloadManager.createBtMagnetTask(param, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
        } else if (url.startsWith("ftp://")) {
            P2spTaskParam param = new P2spTaskParam();
            param.setCreateMode(1);
            param.setFileName(fileName);
            param.setFilePath(savePath);
            param.setUrl(url);
            param.setSeqId(seq.incrementAndGet());
            param.setCookie("");
            param.setRefUrl("");
            param.setUser("");
            param.setPass("");
            int code = downloadManager.createP2spTask(param, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
            downloadManager.setDownloadTaskOrigin(getTaskId.getTaskId(), "out_app/out_app_paste");
            downloadManager.setOriginUserAgent(getTaskId.getTaskId(), "AndroidDownloadManager/5.41.2.4980 (Linux; U; Android 4.4.4; Build/KTU84Q)");
            addRequestHeadersToXlEngine(getTaskId.getTaskId());
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam param = new EmuleTaskParam();
            param.setFilePath(savePath);
            param.setFileName(fileName);
            param.setUrl(url);
            param.setSeqId(seq.incrementAndGet());
            param.setCreateMode(1);
            int code = downloadManager.createEmuleTask(param, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return -1;
        }
        downloadManager.startTask(getTaskId.getTaskId());
        downloadManager.setTaskGsState(getTaskId.getTaskId(), 0, 2);
        return getTaskId.getTaskId();
    }

    public synchronized long addTorrentTask(File torrent, File cache, int index) {
        TorrentInfo torrentInfo = new TorrentInfo();
        downloadManager.getTorrentInfo(torrent.getAbsolutePath(), torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setFilePath(cache.getAbsolutePath());
        taskParam.setTorrentPath(torrent.getAbsolutePath());
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

    public synchronized TorrentInfo getTorrentInfo(File file) {
        TorrentInfo torrentInfo = new TorrentInfo();
        downloadManager.getTorrentInfo(file.getAbsolutePath(), torrentInfo);
        return torrentInfo;
    }

    public synchronized String getLocalUrl(File file) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        downloadManager.getLocalUrl(file.getAbsolutePath(), localUrl);
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