package com.fongmi.android.tv.player.extractor;

import android.os.SystemClock;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Magnet implements Callable<List<Vod.Flag.Episode>> {

    private final String url;
    private int time;

    public static Magnet get(String url) {
        return new Magnet(url);
    }

    public Magnet(String url) {
        this.url = url;
    }

    private void sleep() {
        SystemClock.sleep(10);
        time += 10;
    }

    @Override
    public List<Vod.Flag.Episode> call() {
        boolean torrent = Sniffer.isTorrent(url);
        List<Vod.Flag.Episode> episodes = new ArrayList<>();
        GetTaskId taskId = XLTaskHelper.get().parse(url, Path.thunder(Util.md5(url)));
        if (!torrent && !taskId.getRealUrl().startsWith("magnet")) return List.of(Vod.Flag.Episode.create(taskId.getFileName(), taskId.getRealUrl()));
        if (torrent) Download.create(url, taskId.getSaveFile()).start();
        else while (XLTaskHelper.get().getTaskInfo(taskId).getTaskStatus() != 2 && time < 5000) sleep();
        List<TorrentFileInfo> medias = XLTaskHelper.get().getTorrentInfo(taskId.getSaveFile()).getMedias();
        for (TorrentFileInfo media : medias) episodes.add(Vod.Flag.Episode.create(media.getFileName(), media.getPlayUrl()));
        XLTaskHelper.get().stopTask(taskId);
        return episodes;
    }

    public static void addAll(List<Vod.Flag.Episode> items, Future<List<Vod.Flag.Episode>> future) {
        try {
            items.addAll(Vod.Flag.Episode.Sorter.sort(future.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
