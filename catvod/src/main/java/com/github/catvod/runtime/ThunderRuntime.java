package com.github.catvod.runtime;

import java.io.File;
import java.util.List;

public interface ThunderRuntime extends RuntimePlugin {

    Task parse(String url, File savePath);

    Task addThunderTask(String url, File savePath);

    Task addTorrentTask(File torrent, File savePath, int index);

    List<Media> getTorrentInfo(File torrent);

    String getLocalUrl(File file);

    void deleteTask(Task task);

    void stopTask(Task task);

    Status getTaskInfo(Task task);

    Status getBtSubTaskInfo(Task task, int index);

    void release();

    interface Task {

        File getSaveFile();

        String getFileName();

        String getRealUrl();
    }

    record Status(int taskStatus, String errorMessage) {
    }

    record Media(String fileName, String size, String playUrl) {
    }
}
