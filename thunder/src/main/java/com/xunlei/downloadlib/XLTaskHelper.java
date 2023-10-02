package com.xunlei.downloadlib;

import com.github.catvod.utils.Path;
import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLConstant;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class XLTaskHelper {

    private XLDownloadManager manager;
    private AtomicInteger seq;

    private static class Loader {
        static volatile XLTaskHelper INSTANCE = new XLTaskHelper();
    }

    public synchronized static XLTaskHelper get() {
        return Loader.INSTANCE;
    }

    private synchronized AtomicInteger getSeq() {
        return seq = seq == null ? new AtomicInteger(0) : seq;
    }

    private synchronized XLDownloadManager getManager() {
        return manager = manager == null ? new XLDownloadManager() : manager;
    }

    private synchronized GetTaskId startTask(GetTaskId taskId, int index) {
        getManager().startTask(taskId.getTaskId());
        getManager().setTaskGsState(taskId.getTaskId(), index, 2);
        return taskId;
    }

    public synchronized GetTaskId parse(String url, File savePath) {
        if (url.startsWith("file://")) return new GetTaskId(url, savePath);
        if (url.startsWith("thunder://")) url = getManager().parserThunderUrl(url);
        String fileName = getManager().getFileNameFromUrl(url);
        GetTaskId taskId = new GetTaskId(savePath, fileName, url);
        if (!url.startsWith("magnet:?")) return taskId;
        MagnetTaskParam param = new MagnetTaskParam();
        param.setFilePath(savePath.getAbsolutePath());
        param.setFileName(fileName);
        param.setUrl(url);
        int code = getManager().createBtMagnetTask(param, taskId);
        if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
        return startTask(taskId, 0);
    }

    public synchronized GetTaskId addThunderTask(String url, File savePath) {
        String fileName = getManager().getFileNameFromUrl(url);
        GetTaskId taskId = new GetTaskId(savePath, fileName, url);
        if (url.startsWith("ftp://")) {
            P2spTaskParam param = new P2spTaskParam();
            param.setFilePath(savePath.getAbsolutePath());
            param.setSeqId(getSeq().incrementAndGet());
            param.setFileName(fileName);
            param.setCreateMode(1);
            param.setUrl(url);
            param.setCookie("");
            param.setRefUrl("");
            param.setUser("");
            param.setPass("");
            int code = getManager().createP2spTask(param, taskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
            getManager().setDownloadTaskOrigin(taskId.getTaskId(), "out_app/out_app_paste");
            getManager().setOriginUserAgent(taskId.getTaskId(), "AndroidDownloadManager/5.41.2.4980 (Linux; U; Android 4.4.4; Build/KTU84Q)");
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam param = new EmuleTaskParam();
            param.setFilePath(savePath.getAbsolutePath());
            param.setSeqId(getSeq().incrementAndGet());
            param.setFileName(fileName);
            param.setCreateMode(1);
            param.setUrl(url);
            int code = getManager().createEmuleTask(param, taskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) return taskId;
        }
        return startTask(taskId, 0);
    }

    public synchronized GetTaskId addTorrentTask(File torrent, File savePath, int index) {
        TorrentInfo torrentInfo = getTorrentInfo(torrent);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(getSeq().incrementAndGet());
        taskParam.setFilePath(savePath.getAbsolutePath());
        taskParam.setTorrentPath(torrent.getAbsolutePath());
        GetTaskId taskId = new GetTaskId(savePath);
        int code = getManager().createBtTask(taskParam, taskId);
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
            getManager().deselectBtSubTask(taskId.getTaskId(), btIndexSet);
        }
        return startTask(taskId, index);
    }

    public synchronized TorrentInfo getTorrentInfo(File file) {
        TorrentInfo torrentInfo = new TorrentInfo(file);
        getManager().getTorrentInfo(torrentInfo);
        return torrentInfo;
    }

    public synchronized String getLocalUrl(File file) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        getManager().getLocalUrl(file.getAbsolutePath(), localUrl);
        return localUrl.mStrUrl;
    }

    public synchronized void deleteTask(GetTaskId taskId) {
        new Thread(() -> deleteFile(taskId.getSavePath())).start();
        stopTask(taskId);
    }

    private synchronized void deleteFile(File dir) {
        if (dir.isDirectory()) for (File file : Path.list(dir)) deleteFile(file);
        if (!dir.getAbsolutePath().endsWith(".torrent")) dir.delete();
    }

    public synchronized void stopTask(GetTaskId taskId) {
        getManager().stopTask(taskId.getTaskId());
        getManager().releaseTask(taskId.getTaskId());
    }

    public synchronized XLTaskInfo getTaskInfo(GetTaskId taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        if (taskId.getSaveFile().exists()) taskInfo.setTaskStatus(2);
        else getManager().getTaskInfo(taskId.getTaskId(), 1, taskInfo);
        return taskInfo;
    }

    public synchronized BtSubTaskDetail getBtSubTaskInfo(GetTaskId taskId, int index) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        getManager().getBtSubTaskInfo(taskId.getTaskId(), index, subTaskDetail);
        return subTaskDetail;
    }

    public synchronized void release() {
        if (manager != null) manager.release();
        manager = null;
        seq = null;
    }
}