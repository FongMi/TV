package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.Download;
import com.github.catvod.runtime.ThunderRuntime;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.UriUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

public class Thunder implements Source.Extractor {

    private static final List<String> SCHEMES = List.of("magnet", "ed2k", "ftp");
    private ThunderRuntime runtime;
    private ThunderRuntime.Task taskId;

    private ThunderRuntime runtime() {
        if (runtime == null) runtime = RuntimeModule.THUNDER.entry(ThunderRuntime.class);
        return runtime;
    }

    @Override
    public boolean match(Uri uri) {
        return SCHEMES.contains(UriUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) throws Exception {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        Uri uri = UriUtil.uri(url);
        Selection selection = "magnet".equals(UriUtil.scheme(uri)) ? getSelection(uri) : null;
        ThunderRuntime.Task task = createTask(url, selection);
        try {
            if (selection != null) return waitTorrentTask(task, selection);
            synchronized (this) {
                if (taskId != task || Thread.currentThread().isInterrupted()) throw new CancellationException();
                return runtime().getLocalUrl(task.getSaveFile());
            }
        } catch (Exception e) {
            stopFailedTask(task, e);
            throw e;
        }
    }

    private synchronized void stopFailedTask(ThunderRuntime.Task task, Exception error) {
        if (taskId == task) {
            try {
                stop();
            } catch (Exception cleanup) {
                error.addSuppressed(cleanup);
            }
        }
    }

    private synchronized ThunderRuntime.Task createTask(String url, Selection selection) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        stop();
        if (selection != null) return taskId = runtime().addTorrentTask(selection.torrent(), selection.torrent().getParentFile(), selection.index());
        return taskId = runtime().addThunderTask(url, Path.thunder(Crypto.md5(url)));
    }

    private Selection getSelection(Uri uri) throws IOException {
        String path = uri.getPath();
        String name = uri.getQueryParameter("name");
        int index = Integer.parseInt(uri.getQueryParameter("index"));
        if (path == null || path.isEmpty() || name == null || name.isEmpty() || index < 0) throw new IllegalArgumentException("Invalid torrent play URI");
        File torrent = new File(path).getCanonicalFile();
        File parent = torrent.getParentFile();
        String root = Path.thunder().getCanonicalPath() + File.separator;
        if (parent == null || !parent.getPath().startsWith(root)) throw new IllegalArgumentException("Torrent is outside Thunder task directory");
        File media = new File(parent, name).getCanonicalFile();
        if (!media.getPath().startsWith(parent.getPath() + File.separator)) throw new IllegalArgumentException("Torrent filename escapes task directory");
        return new Selection(torrent, media, index);
    }

    private String waitTorrentTask(ThunderRuntime.Task task, Selection selection) throws Exception {
        for (int i = 0; i < 100; i++) {
            synchronized (this) {
                if (taskId != task || Thread.currentThread().isInterrupted()) throw new CancellationException();
                ThunderRuntime.Status info = runtime().getTaskInfo(task);
                if (info.taskStatus() == 3) throw new ExtractException(info.errorMessage());
                info = runtime().getBtSubTaskInfo(task, selection.index());
                if (info.taskStatus() == 3) throw new ExtractException(info.errorMessage());
                if (info.taskStatus() != 0) return runtime().getLocalUrl(selection.media());
            }
            Thread.sleep(100);
        }
        throw new ExtractException(ResUtil.getString(R.string.error_play_timeout));
    }

    @Override
    public synchronized void stop() {
        if (taskId == null) return;
        ThunderRuntime.Task task = taskId;
        taskId = null;
        runtime.deleteTask(task);
    }

    @Override
    public synchronized void exit() {
        try {
            stop();
        } finally {
            if (runtime != null) runtime.release();
        }
    }

    private record Selection(File torrent, File media, int index) {
    }

    public record Parser(String url) implements Callable<List<Episode>> {

        private static final Pattern PATTERN = Pattern.compile("^(magnet|thunder|ed2k):.*", Pattern.CASE_INSENSITIVE);

        public static boolean match(String url) {
            return PATTERN.matcher(url).find() || isTorrent(url);
        }

        public static Parser get(String url) {
            return new Parser(url);
        }

        private static boolean isTorrent(String url) {
            return !"magnet".equals(UriUtil.scheme(url)) && UriUtil.path(url.split(";", 2)[0]).toLowerCase(Locale.ROOT).endsWith(".torrent");
        }

        private Episode create(ThunderRuntime.Media info) {
            return Episode.create(info.fileName(), info.size(), info.playUrl());
        }

        @Override
        public List<Episode> call() throws Exception {
            ThunderRuntime runtime = RuntimeModule.THUNDER.entry(ThunderRuntime.class);
            ThunderRuntime.Task taskId = runtime.parse(url, Path.thunder(Crypto.md5(url)));
            Exception failure = null;
            try {
                String realUrl = taskId.getRealUrl();
                boolean torrent = isTorrent(realUrl);
                if (!torrent && !"magnet".equals(UriUtil.scheme(realUrl))) return List.of(Episode.create(taskId.getFileName(), taskId.getRealUrl()));
                return readEpisodes(runtime, resolveTorrent(runtime, taskId, torrent));
            } catch (Exception e) {
                failure = e;
                throw e;
            } finally {
                stopParseTask(runtime, taskId, failure);
            }
        }

        private void stopParseTask(ThunderRuntime runtime, ThunderRuntime.Task taskId, Exception failure) throws Exception {
            try {
                runtime.stopTask(taskId);
            } catch (Exception e) {
                if (failure == null) throw e;
                failure.addSuppressed(e);
            }
        }

        private File resolveTorrent(ThunderRuntime runtime, ThunderRuntime.Task taskId, boolean torrent) throws Exception {
            if (torrent && taskId.getRealUrl().startsWith("http")) Download.create(taskId.getRealUrl(), taskId.getSaveFile()).get();
            if (!torrent) waitDone(runtime, taskId);
            return taskId.getSaveFile();
        }

        private List<Episode> readEpisodes(ThunderRuntime runtime, File torrent) {
            return runtime.getTorrentInfo(torrent).stream().map(this::create).toList();
        }

        private void waitDone(ThunderRuntime runtime, ThunderRuntime.Task taskId) throws Exception {
            for (int i = 0; i < 100; i++) {
                ThunderRuntime.Status info = runtime.getTaskInfo(taskId);
                if (info.taskStatus() == 3) throw new ExtractException(info.errorMessage());
                if (info.taskStatus() == 2) return;
                Thread.sleep(100);
            }
            throw new ExtractException(ResUtil.getString(R.string.error_play_timeout));
        }
    }
}
