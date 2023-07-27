package com.fongmi.android.tv.player.extractor;

import android.net.Uri;
import android.os.SystemClock;

import com.fongmi.android.tv.player.Source;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.xunlei.downloadlib.XLTaskHelper;
import com.xunlei.downloadlib.parameter.GetTaskId;

import java.io.File;
import java.util.Objects;

public class Thunder implements Source.Extractor {

    private GetTaskId taskId;

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("ed2k") || scheme.equals("ftp") || scheme.equals("torrent");
    }

    @Override
    public String fetch(String url) throws Exception {
        Uri uri = Uri.parse(url);
        boolean torrent = "torrent".equals(uri.getScheme());
        return torrent ? fetchTorrent(uri) : fetchThunder(url);
    }

    private String fetchTorrent(Uri uri) {
        File torrent = new File(uri.getPath());
        String name = uri.getQueryParameter("name");
        int index = Integer.parseInt(uri.getQueryParameter("index"));
        taskId = XLTaskHelper.get().addTorrentTask(torrent, Objects.requireNonNull(torrent.getParentFile()), index);
        while (XLTaskHelper.get().getBtSubTaskInfo(taskId, index).mTaskInfo.mTaskStatus == 0) SystemClock.sleep(10);
        return XLTaskHelper.get().getLocalUrl(new File(torrent.getParent(), name));
    }

    private String fetchThunder(String url) {
        File folder = Path.thunder(Util.md5(url));
        taskId = XLTaskHelper.get().addThunderTask(url, folder);
        return XLTaskHelper.get().getLocalUrl(taskId.getSaveFile());
    }

    @Override
    public void stop() {
        if (taskId == null) return;
        XLTaskHelper.get().deleteTask(taskId);
        taskId = null;
    }

    @Override
    public void exit() {
        XLTaskHelper.get().release();
    }
}
