package com.fongmi.android.tv.browse;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.api.config.VodConfig;
import com.github.catvod.utils.Task;
import com.github.catvod.utils.Trans;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class BrowseSearch {

    private static final int CATALOG_LIMIT = 50;
    private static final int TIMEOUT_SECONDS = 5;
    private static final Map<String, ImmutableList<MediaItem>> results = new ConcurrentHashMap<>();
    private static final Map<String, MediaItem> items = new ConcurrentHashMap<>();
    private static final Set<ListenableFuture<?>> requests = new HashSet<>();
    private static int generation;

    static void clear() {
        synchronized (results) {
            generation++;
            requests.forEach(future -> future.cancel(true));
            requests.clear();
            items.clear();
            results.clear();
        }
    }

    @NonNull
    static ImmutableList<MediaItem> search(@NonNull String query) {
        String keyword = key(query);
        if (TextUtils.isEmpty(keyword) || Thread.currentThread().isInterrupted()) return ImmutableList.of();
        int current;
        synchronized (results) {
            current = generation;
        }
        VodConfig.get().ensureLoaded();
        synchronized (results) {
            if (current != generation || Thread.currentThread().isInterrupted()) return ImmutableList.of();
        }
        List<ListenableFuture<List<MediaItem>>> futures = submitSearches(query, keyword);
        synchronized (results) {
            if (current == generation) requests.addAll(futures);
            else futures.forEach(future -> future.cancel(true));
        }
        ImmutableList<MediaItem> matches = selectResults(collect(futures), keyword);
        synchronized (results) {
            requests.removeAll(futures);
            if (current != generation || Thread.currentThread().isInterrupted()) return ImmutableList.of();
            results.put(keyword, matches);
            matches.forEach(item -> items.put(item.mediaId, item));
        }
        return matches;
    }

    private static List<ListenableFuture<List<MediaItem>>> submitSearches(String query, String keyword) {
        List<ListenableFuture<List<MediaItem>>> futures = new ArrayList<>();
        futures.add(Task.largeExecutor().submit(() -> LiveBrowse.search(keyword)));
        VodConfig.get().getSites().stream().filter(site -> site.isSearchable()).forEach(site -> futures.add(Task.largeExecutor().submit(() -> VodBrowse.search(site, query.trim()))));
        return futures;
    }

    private static ImmutableList<MediaItem> selectResults(List<MediaItem> found, String keyword) {
        found.sort((a, b) -> score(b, keyword) - score(a, keyword));
        ImmutableList.Builder<MediaItem> selected = ImmutableList.builder();
        int live = 0, vod = 0;
        for (MediaItem item : found) if (item.mediaId.startsWith(LiveBrowse.LIVE_CH) ? live++ < CATALOG_LIMIT : vod++ < CATALOG_LIMIT) selected.add(item);
        return selected.build();
    }

    private static List<MediaItem> collect(List<ListenableFuture<List<MediaItem>>> futures) {
        List<MediaItem> found = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        try {
            for (ListenableFuture<List<MediaItem>> future : futures) if (!collect(found, future, deadline)) break;
        } finally {
            futures.forEach(future -> future.cancel(true));
        }
        return found;
    }

    private static boolean collect(List<MediaItem> found, ListenableFuture<List<MediaItem>> future, long deadline) {
        try {
            found.addAll(future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return false;
        } catch (CancellationException ignored) {
        } catch (ExecutionException | TimeoutException error) {
            Log.w("BrowseSearch", "Unable to search media source", error);
        }
        return true;
    }

    @NonNull
    static ImmutableList<MediaItem> getResults(@NonNull String query, int page, int pageSize) {
        return BrowseTree.page(results.getOrDefault(key(query), ImmutableList.of()), page, pageSize);
    }

    @Nullable
    static MediaItem getItem(@NonNull String mediaId) {
        return items.get(mediaId);
    }

    private static int score(MediaItem item, String keyword) {
        CharSequence title = item.mediaMetadata.title;
        String name = title == null ? "" : key(title.toString());
        return name.equals(keyword) ? 2 : name.contains(keyword) ? 1 : 0;
    }

    static String key(String query) {
        return Trans.t2s(query).trim().toLowerCase(Locale.ROOT);
    }
}
