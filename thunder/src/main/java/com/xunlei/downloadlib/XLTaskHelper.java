package com.xunlei.downloadlib;

import android.text.TextUtils;
import android.util.Pair;

import com.github.catvod.Init;
import com.github.catvod.utils.Path;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
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
        seq = new AtomicInteger(0);
        requestHeaders = new ArrayList<>();
        downloadManager = new XLDownloadManager(Init.getContext());
    }

    public synchronized GetTaskId addThunderTask(String url, String savePath, String fileName) {
        if (url.startsWith("thunder://")) url = downloadManager.parserThunderUrl(url);
        GetTaskId taskId = new GetTaskId();
        taskId.setSavePath(savePath);
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
            int code = downloadManager.createBtMagnetTask(param, taskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
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
            int code = downloadManager.createP2spTask(param, taskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
            downloadManager.setDownloadTaskOrigin(taskId.getTaskId(), "out_app/out_app_paste");
            downloadManager.setOriginUserAgent(taskId.getTaskId(), "AndroidDownloadManager/5.41.2.4980 (Linux; U; Android 4.4.4; Build/KTU84Q)");
            addRequestHeadersToXlEngine(taskId.getTaskId());
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam param = new EmuleTaskParam();
            param.setFilePath(savePath);
            param.setFileName(fileName);
            param.setUrl(url);
            param.setSeqId(seq.incrementAndGet());
            param.setCreateMode(1);
            int code = downloadManager.createEmuleTask(param, taskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
        }
        downloadManager.startTask(taskId.getTaskId());
        downloadManager.setTaskGsState(taskId.getTaskId(), 0, 2);
        return taskId;
    }

    public synchronized GetTaskId addTorrentTask(File torrent, String savePath, int index) {
        TorrentInfo torrentInfo = new TorrentInfo();
        downloadManager.getTorrentInfo(torrent.getAbsolutePath(), torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setMaxConcurrent(3);
        taskParam.setFilePath(savePath);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setTorrentPath(torrent.getAbsolutePath());
        GetTaskId taskId = new GetTaskId();
        taskId.setSavePath(savePath);
        int code = downloadManager.createBtTask(taskParam, taskId);
        if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
        if (fileInfos.length > 1) {
            List<Integer> list = new CopyOnWriteArrayList<>();
            for (TorrentFileInfo fileInfo : fileInfos) {
                if (fileInfo.mFileIndex != index) {
                    list.add(fileInfo.mFileIndex);
                }
            }
            BtIndexSet btIndexSet = new BtIndexSet(list.size());
            for (int i = 0; i < list.size(); i++) btIndexSet.mIndexSet[i] = list.get(i);
            downloadManager.deselectBtSubTask(taskId.getTaskId(), btIndexSet);
        }
        downloadManager.startTask(taskId.getTaskId());
        downloadManager.setTaskGsState(taskId.getTaskId(), index, 2);
        return taskId;
    }

    public void addHeader(String key, String value) {
        requestHeaders.add(Pair.create(key, value));
    }

    private Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(requestHeaders);
    }

    private void addRequestHeadersToXlEngine(long taskId) {
        for (Pair<String, String> pair : getHeaders()) {
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

    public synchronized String getLocalUrl(String path) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        downloadManager.getLocalUrl(path, localUrl);
        return localUrl.mStrUrl;
    }

    public synchronized void deleteTask(GetTaskId taskId) {
        Path.clear(taskId.getSavePath());
        stopTask(taskId);
    }

    public synchronized void stopTask(GetTaskId taskId) {
        downloadManager.stopTask(taskId.getTaskId());
        downloadManager.releaseTask(taskId.getTaskId());
    }

    public synchronized XLTaskInfo getTaskInfo(GetTaskId taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        downloadManager.getTaskInfo(taskId.getTaskId(), 1, taskInfo);
        return taskInfo;
    }

    public synchronized BtSubTaskDetail getBtSubTaskInfo(GetTaskId taskId, int index) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        downloadManager.getBtSubTaskInfo(taskId.getTaskId(), index, subTaskDetail);
        return subTaskDetail;
    }

    public void release() {
        downloadManager.release();
    }
}