package com.xunlei.downloadlib;

import com.github.catvod.runtime.ThunderRuntime;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.XLTaskInfo;

import java.io.File;
import java.util.List;

public final class RuntimeEntry implements ThunderRuntime {

    @Override
    public void check() {
        XLTaskHelper.get().check();
    }

    @Override
    public Task parse(String url, File savePath) {
        return XLTaskHelper.get().parse(url, savePath);
    }

    @Override
    public Task addThunderTask(String url, File savePath) {
        return XLTaskHelper.get().addThunderTask(url, savePath);
    }

    @Override
    public Task addTorrentTask(File torrent, File savePath, int index) {
        return XLTaskHelper.get().addTorrentTask(torrent, savePath, index);
    }

    @Override
    public List<Media> getTorrentInfo(File torrent) {
        return XLTaskHelper.get().getTorrentInfo(torrent).getMedias().stream()
                .map(info -> new Media(info.getFileName(), info.getSize(), info.getPlayUrl()))
                .toList();
    }

    @Override
    public String getLocalUrl(File file) {
        return XLTaskHelper.get().getLocalUrl(file);
    }

    @Override
    public void deleteTask(Task task) {
        XLTaskHelper.get().deleteTask((GetTaskId) task);
    }

    @Override
    public void stopTask(Task task) {
        XLTaskHelper.get().stopTask((GetTaskId) task);
    }

    private Status status(XLTaskInfo info) {
        return new Status(info.getTaskStatus(), info.getErrorMsg());
    }

    @Override
    public Status getTaskInfo(Task task) {
        return status(XLTaskHelper.get().getTaskInfo((GetTaskId) task));
    }

    @Override
    public Status getBtSubTaskInfo(Task task, int index) {
        return status(XLTaskHelper.get().getBtSubTaskInfo((GetTaskId) task, index).mTaskInfo);
    }

    @Override
    public void release() {
        XLTaskHelper.get().release();
    }
}
