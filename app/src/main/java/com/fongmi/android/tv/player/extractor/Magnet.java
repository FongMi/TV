package com.fongmi.android.tv.player.extractor;

import android.os.SystemClock;

import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;

import java.util.List;
import java.util.concurrent.Callable;

public class Magnet implements Callable<List<TorrentFileInfo>> {

    private final String url;

    public static Magnet get(String url) {
        return new Magnet(url);
    }

    public Magnet(String url) {
        this.url = url;
    }

    @Override
    public List<TorrentFileInfo> call() {
        GetTaskId taskId = XLTaskHelper.get().addThunderTask(url, Path.thunder(Util.md5(url)));
        while (XLTaskHelper.get().getTaskInfo(taskId).getTaskStatus() != 2) SystemClock.sleep(10);
        List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
        XLTaskHelper.get().stopTask(taskId);
        return medias;
    }
}
