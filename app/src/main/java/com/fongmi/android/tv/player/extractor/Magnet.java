package com.fongmi.android.tv.player.extractor;

import android.os.SystemClock;

import com.fongmi.android.tv.bean.Vod;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class Magnet implements Callable<List<Vod.Flag.Episode>> {

    private final String url;

    public static Magnet get(String url) {
        return new Magnet(url);
    }

    public Magnet(String url) {
        this.url = url;
    }

    @Override
    public List<Vod.Flag.Episode> call() {
        List<Vod.Flag.Episode> episodes = new ArrayList<>();
        GetTaskId taskId = XLTaskHelper.get().parse(url, Path.thunder(Util.md5(url)));
        if (!taskId.getRealUrl().startsWith("magnet")) return List.of(Vod.Flag.Episode.create(taskId.getFileName(), taskId.getRealUrl()));
        while (XLTaskHelper.get().getTaskInfo(taskId).getTaskStatus() != 2) SystemClock.sleep(10);
        List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
        for (TorrentFileInfo media : medias) episodes.add(Vod.Flag.Episode.create(media.getFileName(), media.getPlayUrl()));
        XLTaskHelper.get().stopTask(taskId);
        return episodes;
    }
}
