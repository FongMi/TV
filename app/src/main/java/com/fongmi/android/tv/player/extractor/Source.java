package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Task;
import com.github.catvod.utils.UriUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Source {

    private final List<Extractor> extractors;
    private long sequence;
    private int active;

    public Source() {
        extractors = List.of(new Push(), new Video(), new WebView(), new Strm(), new Thunder(), new TVBus(), new YouTube());
    }

    public static Source get() {
        return Loader.INSTANCE;
    }

    public static boolean isBuiltin(String url) {
        if (url == null) return false;
        url = url.trim();
        for (int i = 0; i < url.length(); i++) if (Character.isWhitespace(url.charAt(i))) return false;
        Uri uri = UriUtil.uri(url);
        return switch (UriUtil.scheme(uri)) {
            case "video" -> url.regionMatches(true, 0, "video://", 0, 8);
            case "webview" -> UrlUtil.isWebView(url);
            case "magnet", "thunder", "ed2k", "ftp" -> true;
            case "http", "https" -> YouTube.isYouTube(uri) || Thunder.Parser.match(url);
            case "file" -> Thunder.Parser.match(url);
            default -> false;
        };
    }

    public static boolean isPush(String url) {
        return url != null && url.regionMatches(true, 0, "push://", 0, 7);
    }

    private Extractor getExtractor(Uri uri) {
        return extractors.stream().filter(extractor -> extractor.match(uri)).findFirst().orElse(null);
    }

    public boolean canPreload(Uri uri) {
        return getExtractor(uri) == null;
    }

    private synchronized void begin() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        sequence++;
        active++;
        notifyAll();
    }

    private synchronized void end() {
        active--;
        notifyAll();
    }

    private void addCallable(Iterator<Episode> iterator, List<Callable<List<Episode>>> items) {
        String url = iterator.next().getUrl();
        String scheme = UriUtil.scheme(url);
        if ("push".equals(scheme) || "video".equals(scheme) || "webview".equals(scheme)) return;
        if (Thunder.Parser.match(url)) {
            items.add(Thunder.Parser.get(url));
            iterator.remove();
        } else if (YouTube.Parser.match(url)) {
            items.add(YouTube.Parser.get(url));
            iterator.remove();
        }
    }

    public void parse(Vod vod) throws Exception {
        begin();
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            for (Flag flag : vod.getFlags()) parseEpisodes(flag, executor);
        } finally {
            end();
        }
    }

    private void parseEpisodes(Flag flag, ExecutorService executor) throws Exception {
        List<Callable<List<Episode>>> items = new ArrayList<>();
        Iterator<Episode> iterator = flag.getEpisodes().iterator();
        while (iterator.hasNext()) addCallable(iterator, items);
        for (Future<List<Episode>> future : executor.invokeAll(items, 30, TimeUnit.SECONDS)) {
            try {
                flag.getEpisodes().addAll(future.get());
            } catch (CancellationException ignored) {
            }
        }
    }

    public String fetch(Result result) throws Exception {
        begin();
        try {
            Uri uri = result.getUrl().uri();
            String url = result.getUrl().v();
            Extractor extractor = getExtractor(uri);
            if (extractor != null) result.setParse(0);
            if (extractor instanceof Video) result.setParse(1);
            url = extractor == null ? url : extractor.fetch(result);
            BaseLoader.get().requireProxy(url);
            return url;
        } finally {
            end();
        }
    }

    public synchronized void stop() {
        extractors.forEach(Extractor::stop);
    }

    public synchronized void exit() {
        long current = sequence;
        Task.execute(() -> exit(current));
    }

    private synchronized void exit(long current) {
        try {
            while (current == sequence && active > 0) wait();
            if (current == sequence) extractors.forEach(Extractor::exit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public interface Extractor {

        default String fetch(Result result) throws Exception {
            return fetch(result.getUrl().v());
        }

        String fetch(String url) throws Exception;

        boolean match(Uri uri);

        default void stop() {}

        default void exit() {}
    }

    private static class Loader {
        static volatile Source INSTANCE = new Source();
    }
}
