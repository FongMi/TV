package com.fongmi.android.tv.player.extractor;

import android.content.ContentResolver;
import android.net.Uri;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.TorrentUrl;
import com.github.catvod.net.Download;
import com.github.catvod.runtime.ThunderRuntime;
import com.github.catvod.utils.Auth;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.UriUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class Thunder implements Source.Extractor {

    private static final List<String> SCHEMES = List.of("magnet", "thunder", "ed2k", "ftp");
    private ThunderRuntime runtime;
    private ThunderRuntime.Task taskId;

    private ThunderRuntime runtime() {
        if (runtime == null) runtime = RuntimeModule.THUNDER.entry(ThunderRuntime.class);
        return runtime;
    }

    @Override
    public boolean match(Uri uri) {
        return SCHEMES.contains(UriUtil.scheme(uri)) || Parser.isTorrent(uri.toString());
    }

    @Override
    public String fetch(Result result) throws Exception {
        return fetch(result, result.getUrl().v());
    }

    @Override
    public String fetch(Result result, String id) throws Exception {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        result.setDirect();
        String url = result.getUrl().v();
        TorrentUrl selected = TorrentUrl.match(id) ? TorrentUrl.from(id) : null;
        return Parser.isDeferred(url) ? fetchTorrent(result, id, url, selected) : fetchDownload(url, selected);
    }

    private String fetchTorrent(Result result, String id, String url, TorrentUrl selected) throws Exception {
        url = TorrentUrl.original(url);
        File cached = result.getTorrentFile();
        List<Episode> files = readFiles(result, url, cached);
        if (!"magnet".equals(UriUtil.scheme(files.get(0).getUrl()))) return fetchDownload(files.get(0).getUrl(), selected);
        String original = TorrentUrl.original(id);
        List<Episode> episodes = files.stream().map(file -> createResolvedEpisode(original, file)).toList();
        int position = findSelectedFile(episodes, selected);
        episodes.get(position).setSelected(true);
        if (cached == null) {
            result.setTorrentFile(getSelection(UriUtil.uri(files.get(0).getUrl())).torrent());
            result.setResolvedEpisodes(episodes);
            return url;
        }
        return fetchTorrentFile(result, files.get(position));
    }

    private List<Episode> readFiles(Result result, String url, File cached) throws Exception {
        ThunderRuntime runtime = runtime();
        List<Episode> files = cached != null && cached.isFile() ? Parser.get(url).readEpisodes(runtime, cached) : Parser.get(url, result).call();
        if (files.isEmpty()) throw new ExtractException(ResUtil.getString(R.string.error_play_url));
        return files;
    }

    private String fetchTorrentFile(Result result, Episode file) throws Exception {
        String localUrl = fetch(file.getUrl());
        result.setResolvedEpisodes(List.of());
        return localUrl;
    }

    private String fetchDownload(String url, TorrentUrl selected) throws Exception {
        if (selected != null) throw new IllegalArgumentException("Torrent source is no longer available");
        return fetch(url);
    }

    private Episode createResolvedEpisode(String id, Episode file) {
        Uri uri = UriUtil.uri(file.getUrl());
        TorrentUrl selection = new TorrentUrl(id, uri.getQueryParameter("name"), Integer.parseInt(uri.getQueryParameter("index")));
        return Episode.create(file.getName(), file.getDesc(), selection.url());
    }

    private int findSelectedFile(List<Episode> episodes, TorrentUrl selected) {
        if (selected == null) return 0;
        for (int i = 0; i < episodes.size(); i++) {
            TorrentUrl item = TorrentUrl.from(episodes.get(i).getUrl());
            if (item.index() == selected.index() && item.name().equals(selected.name())) return i;
        }
        throw new IllegalArgumentException("Torrent file is no longer available");
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
        Throwable failure = null;
        try {
            stop();
        } catch (RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            releaseRuntime(failure);
        }
    }

    private void releaseRuntime(Throwable failure) {
        ThunderRuntime runtime = this.runtime;
        this.runtime = null;
        if (runtime == null) return;
        try {
            runtime.release();
        } catch (RuntimeException | Error e) {
            if (failure == null) throw e;
            failure.addSuppressed(e);
        }
    }

    private record Selection(File torrent, File media, int index) {
    }

    public record Parser(String url, Result result) implements Callable<List<Episode>> {

        private static final Pattern PATTERN = Pattern.compile("^(magnet|thunder|ed2k):.*", Pattern.CASE_INSENSITIVE);
        private static final Pattern MAGNET = Pattern.compile("^magnet:/*\\?", Pattern.CASE_INSENSITIVE);
        private static final Semaphore PARSE_LOCK = new Semaphore(1, true);

        private static boolean isRawMagnet(String url) {
            return MAGNET.matcher(url).find();
        }

        public static boolean isDeferred(String url) {
            return isRawMagnet(url) || TorrentUrl.match(url) || isTorrent(url) || "thunder".equals(UriUtil.scheme(url));
        }

        public static boolean match(String url) {
            return PATTERN.matcher(url).find() || isTorrent(url);
        }

        public static Parser get(String url) {
            return get(url, null);
        }

        public static Parser get(String url, Result result) {
            return new Parser(url, result);
        }

        public static boolean isTorrent(String url) {
            if ("magnet".equals(UriUtil.scheme(url))) return false;
            Uri uri = UriUtil.uri(url);
            String name = ContentResolver.SCHEME_CONTENT.equals(UriUtil.scheme(uri)) ? FileUtil.getDisplayName(uri) : UriUtil.path(url.split(";", 2)[0]);
            return name.toLowerCase(Locale.ROOT).endsWith(".torrent");
        }

        private Episode create(ThunderRuntime.Media info) {
            return Episode.create(info.fileName(), info.size(), info.playUrl());
        }

        @Override
        public List<Episode> call() throws Exception {
            if (Thread.interrupted()) throw new InterruptedException();
            if (!isRawMagnet(url) && !"thunder".equals(UriUtil.scheme(url))) return parseEpisodes();
            PARSE_LOCK.acquire();
            try {
                return parseEpisodes();
            } finally {
                PARSE_LOCK.release();
            }
        }

        private List<Episode> parseEpisodes() throws Exception {
            ThunderRuntime runtime = RuntimeModule.THUNDER.entry(ThunderRuntime.class);
            File directory = Path.thunder(Crypto.md5(url));
            if (ContentResolver.SCHEME_CONTENT.equals(UriUtil.scheme(url))) return readEpisodes(runtime, importTorrent(directory));
            ThunderRuntime.Task taskId = runtime.parse(url, directory);
            Exception failure = null;
            try {
                return readTaskEpisodes(runtime, taskId);
            } catch (Exception e) {
                failure = e;
                throw e;
            } finally {
                stopParseTask(runtime, taskId, failure);
            }
        }

        private List<Episode> readTaskEpisodes(ThunderRuntime runtime, ThunderRuntime.Task taskId) throws Exception {
            String realUrl = taskId.getRealUrl();
            boolean torrent = isTorrent(realUrl);
            if (!torrent && !"magnet".equals(UriUtil.scheme(realUrl))) return List.of(Episode.create(taskId.getFileName(), taskId.getRealUrl()));
            return readEpisodes(runtime, resolveTorrent(runtime, taskId, torrent));
        }

        private void stopParseTask(ThunderRuntime runtime, ThunderRuntime.Task taskId, Exception failure) throws Exception {
            try {
                runtime.stopTask(taskId);
            } catch (Exception e) {
                if (failure == null) throw e;
                failure.addSuppressed(e);
            }
        }

        private File importTorrent(File directory) throws IOException {
            File torrent = new File(directory, "source.torrent");
            Path.writeAtomically(torrent, Path.open(UriUtil.uri(url), "Unable to open torrent file"));
            return torrent;
        }

        private File resolveTorrent(ThunderRuntime runtime, ThunderRuntime.Task taskId, boolean torrent) throws Exception {
            if (torrent && taskId.getRealUrl().startsWith("http")) downloadTorrent(taskId);
            if (!torrent) waitDone(runtime, taskId);
            return taskId.getSaveFile();
        }

        private void downloadTorrent(ThunderRuntime.Task taskId) {
            Map<String, String> headers = result == null ? Map.of() : result.getHeader();
            Auth.Context auth = result == null ? null : result.getAuth();
            String url = auth == null ? taskId.getRealUrl() : auth.getUrl(taskId.getRealUrl(), headers);
            Request.Builder builder = new Request.Builder().url(url).headers(Headers.of(headers)).tag(Auth.Context.class, auth);
            if (auth != null) auth.getHeaders(url, headers).forEach(builder::header);
            new Download(builder.build(), taskId.getSaveFile()).get();
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
