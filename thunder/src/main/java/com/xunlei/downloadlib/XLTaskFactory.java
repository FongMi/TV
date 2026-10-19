package com.xunlei.downloadlib;

import android.net.Uri;

import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class XLTaskFactory {

    private static final Pattern MAGNET_QUERY = Pattern.compile("^magnet:/*\\?");
    final XLDownloadManager manager;
    private int seq;

    XLTaskFactory(XLDownloadManager manager) {
        this.manager = manager;
    }

    GetTaskId parse(String url, File savePath) {
        if (url.startsWith("thunder://")) url = manager.parserThunderUrl(url);
        url = Uri.parse(url).normalizeScheme().toString();
        url = normalizeMagnetUrl(url);
        String fileName = manager.getFileNameFromUrl(url);
        GetTaskId taskId = new GetTaskId(savePath, fileName, url);
        if (url.startsWith("magnet:?")) createMagnetTask(taskId);
        return taskId;
    }

    private void createMagnetTask(GetTaskId taskId) {
        MagnetTaskParam param = new MagnetTaskParam();
        param.setFilePath(taskId.getSavePath().getAbsolutePath());
        param.setFileName(taskId.getFileName());
        param.setUrl(taskId.getRealUrl());
        XLDownloadManager.checkErrorCode(manager.createBtMagnetTask(param, taskId));
    }

    private String normalizeMagnetUrl(String url) {
        Matcher matcher = MAGNET_QUERY.matcher(url);
        if (!matcher.find()) return url;
        int fragment = url.indexOf('#');
        int end = fragment < 0 ? url.length() : fragment;
        String[] params = url.substring(matcher.end(), end).split("&", -1);
        for (int i = 0; i < params.length; i++) {
            int index = params[i].indexOf('=');
            if (index >= 0 && "xt".equals(Uri.decode(params[i].substring(0, index)))) {
                String value = Uri.decode(params[i].substring(index + 1));
                params[i] = "xt=" + Uri.encode(value, ":");
            }
        }
        return "magnet:?" + String.join("&", params) + url.substring(end);
    }

    GetTaskId createDownload(String url, File savePath) {
        GetTaskId taskId = new GetTaskId(savePath, manager.getFileNameFromUrl(url), url);
        if (url.startsWith("ftp://")) createFtpTask(taskId);
        else createEmuleTask(taskId);
        return taskId;
    }

    private void createFtpTask(GetTaskId taskId) {
        P2spTaskParam param = new P2spTaskParam();
        param.setFilePath(taskId.getSavePath().getAbsolutePath());
        param.setSeqId(++seq);
        param.setFileName(taskId.getFileName());
        param.setCreateMode(1);
        param.setUrl(taskId.getRealUrl());
        param.setCookie("");
        param.setRefUrl("");
        param.setUser("");
        param.setPass("");
        XLDownloadManager.checkErrorCode(manager.createP2spTask(param, taskId));
    }

    private void createEmuleTask(GetTaskId taskId) {
        EmuleTaskParam param = new EmuleTaskParam();
        param.setFilePath(taskId.getSavePath().getAbsolutePath());
        param.setSeqId(++seq);
        param.setFileName(taskId.getFileName());
        param.setCreateMode(1);
        param.setUrl(taskId.getRealUrl());
        XLDownloadManager.checkErrorCode(manager.createEmuleTask(param, taskId));
    }

    GetTaskId createTorrent(File torrent, File savePath) {
        BtTaskParam param = new BtTaskParam();
        param.setCreateMode(1);
        param.setMaxConcurrent(3);
        param.setSeqId(++seq);
        param.setFilePath(savePath.getAbsolutePath());
        param.setTorrentPath(torrent.getAbsolutePath());
        GetTaskId taskId = new GetTaskId(savePath);
        XLDownloadManager.checkErrorCode(manager.createBtTask(param, taskId));
        return taskId;
    }

    void configureDownload(GetTaskId taskId) {
        manager.setDownloadTaskOrigin(taskId.getTaskId(), "out_app/out_app_paste");
        manager.setOriginUserAgent(taskId.getTaskId(), "AndroidDownloadManager/4.4.4 (Linux; U; Android 4.4.4; Build/KTU84Q)");
    }

    void deselectFiles(GetTaskId taskId, TorrentFileInfo[] files, int index) {
        if (files.length <= 1) return;
        List<Integer> indices = Arrays.stream(files).filter(file -> file.mFileIndex != index).map(file -> file.mFileIndex).toList();
        BtIndexSet btIndexSet = new BtIndexSet(indices.size());
        for (int i = 0; i < indices.size(); i++) btIndexSet.mIndexSet[i] = indices.get(i);
        manager.deselectBtSubTask(taskId.getTaskId(), btIndexSet);
    }
}
