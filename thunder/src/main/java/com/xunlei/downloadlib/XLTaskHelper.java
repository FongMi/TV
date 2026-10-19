package com.xunlei.downloadlib;

import android.net.Uri;

import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class XLTaskHelper {

    private final Set<GetTaskId> tasks = new HashSet<>();
    private XLTaskFactory factory;

    public static XLTaskHelper get() {
        return Loader.INSTANCE;
    }

    private XLTaskFactory getFactory() {
        if (factory == null) factory = new XLTaskFactory(new XLDownloadManager());
        return factory;
    }

    private XLDownloadManager requireManager() {
        if (factory == null) throw new IllegalStateException("Thunder SDK is not initialized");
        return factory.manager;
    }

    private XLDownloadManager requireManager(GetTaskId taskId) {
        if (!tasks.contains(taskId)) throw new IllegalStateException("Thunder task is not active");
        return requireManager();
    }

    private GetTaskId startTask(GetTaskId taskId, int index, Runnable configure) {
        tasks.add(taskId);
        try {
            XLDownloadManager manager = requireManager(taskId);
            configure.run();
            manager.setTaskGsState(taskId.getTaskId(), index, 2);
            manager.startTask(taskId.getTaskId());
            return taskId;
        } catch (RuntimeException e) {
            stopTask(taskId, e);
            throw e;
        }
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) for (File child : Path.list(file)) deleteFile(child);
        if (file.isFile() && !file.getName().toLowerCase(Locale.ROOT).endsWith(".torrent")) Path.clear(file);
    }

    public synchronized void check() {
        GetTaskId taskId = parse("thunder://QUFmdHA6Ly8xMjcuMC4wLjEvbW9kdWxlLWNoZWNrLm1wNFpa", Path.files());
        if (!"ftp://127.0.0.1/module-check.mp4".equals(taskId.getRealUrl())) throw new IllegalStateException("Thunder URL decoding returned an unexpected result");
        if (!"module-check.mp4".equals(taskId.getFileName())) throw new IllegalStateException("Thunder filename parsing returned an unexpected result");
        if (taskId.getTaskId() != 0) throw new IllegalStateException("Thunder check unexpectedly created a task");
    }

    public synchronized GetTaskId parse(String url, File savePath) {
        RuntimeModule.THUNDER.require();
        url = Uri.parse(url).normalizeScheme().toString();
        if (url.startsWith("file://")) return new GetTaskId(url, savePath);
        GetTaskId taskId = getFactory().parse(url, savePath);
        if (!taskId.getRealUrl().startsWith("magnet:?")) return taskId;
        return startTask(taskId, 0, () -> {
        });
    }

    public synchronized GetTaskId addThunderTask(String url, File savePath) {
        RuntimeModule.THUNDER.require();
        url = Uri.parse(url).normalizeScheme().toString();
        if (!url.startsWith("ftp://") && !url.startsWith("ed2k://")) throw new IllegalArgumentException("Unsupported Thunder URL");
        XLTaskFactory factory = getFactory();
        GetTaskId taskId = factory.createDownload(url, savePath);
        return startTask(taskId, 0, () -> factory.configureDownload(taskId));
    }

    public synchronized GetTaskId addTorrentTask(File torrent, File savePath, int index) {
        RuntimeModule.THUNDER.require();
        TorrentFileInfo[] files = getTorrentInfo(torrent).mSubFileInfo;
        checkIndex(files, index);
        XLTaskFactory factory = getFactory();
        GetTaskId taskId = factory.createTorrent(torrent, savePath);
        return startTask(taskId, index, () -> factory.deselectFiles(taskId, files, index));
    }

    private void checkIndex(TorrentFileInfo[] files, int index) {
        if (files != null) for (TorrentFileInfo file : files) if (file.mFileIndex == index) return;
        throw new IllegalArgumentException("Invalid torrent file index: " + index);
    }

    private void stopTask(GetTaskId taskId, RuntimeException error) {
        try {
            stopTask(taskId);
        } catch (RuntimeException e) {
            error.addSuppressed(e);
        }
    }

    public synchronized TorrentInfo getTorrentInfo(File file) {
        RuntimeModule.THUNDER.require();
        TorrentInfo torrentInfo = new TorrentInfo(file);
        getFactory().manager.getTorrentInfo(torrentInfo);
        return torrentInfo;
    }

    public synchronized String getLocalUrl(File file) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        requireManager().getLocalUrl(file.getAbsolutePath(), localUrl);
        return localUrl.mStrUrl;
    }

    public synchronized void deleteTask(GetTaskId taskId) {
        if (taskId.getTaskId() != 0 && !tasks.contains(taskId)) return;
        stopTask(taskId);
        deleteFile(taskId.getSavePath());
    }

    public synchronized void stopTask(GetTaskId taskId) {
        if (!tasks.remove(taskId)) return;
        XLDownloadManager manager = requireManager();
        try {
            manager.stopTask(taskId.getTaskId());
        } finally {
            manager.releaseTask(taskId.getTaskId());
        }
    }

    public synchronized XLTaskInfo getTaskInfo(GetTaskId taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        if (taskId.getTaskId() == 0 && taskId.getSaveFile().exists()) taskInfo.setTaskStatus(2);
        else requireManager(taskId).getTaskInfo(taskId.getTaskId(), 1, taskInfo);
        return taskInfo;
    }

    public synchronized BtSubTaskDetail getBtSubTaskInfo(GetTaskId taskId, int index) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        requireManager(taskId).getBtSubTaskInfo(taskId.getTaskId(), index, subTaskDetail);
        return subTaskDetail;
    }

    public synchronized void release() {
        try {
            if (factory != null) factory.manager.release();
        } finally {
            Path.clear(Path.files("setting.cfg"));
            Path.clear(Path.files("seq_id"));
            factory = null;
            tasks.clear();
        }
    }

    private static class Loader {
        static volatile XLTaskHelper INSTANCE = new XLTaskHelper();
    }
}
