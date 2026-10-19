package com.fongmi.android.tv.service;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService.LibraryParams;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession.ControllerInfo;
import androidx.media3.session.SessionError;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.browse.BrowseTree;
import com.github.catvod.utils.Task;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;

final class BrowseLibrary {

    private final List<ListenableFuture<?>> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, ListenableFuture<Integer>> childCountTasks = new HashMap<>();
    private final Map<String, ListenableFuture<ImmutableList<MediaItem>>> searchTasks = new HashMap<>();

    private MediaLibrarySession session;
    private long generation;

    BrowseLibrary(MediaLibrarySession session) {
        this.session = session;
    }

    ListenableFuture<LibraryResult<MediaItem>> getRoot(@Nullable LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.getRootItem(), params));
    }

    ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> getChildren(String parentId, int page, int pageSize, @Nullable LibraryParams params) {
        return submit(() -> LibraryResult.ofItemList(BrowseTree.getChildren(parentId, page, pageSize), params));
    }

    ListenableFuture<LibraryResult<MediaItem>> getItem(String mediaId) {
        return submit(() -> {
            MediaItem item = BrowseTree.getItem(mediaId);
            return item != null ? LibraryResult.ofItem(item, null) : LibraryResult.ofError(SessionError.ERROR_BAD_VALUE);
        });
    }

    ListenableFuture<LibraryResult<Void>> search(ControllerInfo browser, String query, @Nullable LibraryParams params) {
        long current = generation;
        ListenableFuture<ImmutableList<MediaItem>> task = searchTasks.computeIfAbsent(query, key -> submit(() -> BrowseTree.search(key)));
        Futures.addCallback(task, Task.callback(results -> {
            searchTasks.remove(query, task);
            if (session != null && current == generation) session.notifySearchResultChanged(browser, query, results.size(), params);
        }, error -> {
            searchTasks.remove(query, task);
            if (session != null && current == generation && !(error instanceof CancellationException)) Log.w("BrowseLibrary", "Unable to search media", error);
        }), App::post);
        return Futures.immediateFuture(LibraryResult.ofVoid(params));
    }

    ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> getSearchResult(String query, int page, int pageSize, @Nullable LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofItemList(BrowseTree.getSearchResult(query, page, pageSize), params));
    }

    void notifyChildrenChanged(String parent) {
        if (session == null || session.getSubscribedControllers(parent).isEmpty()) return;
        ListenableFuture<Integer> task = submit(() -> BrowseTree.getChildCount(parent));
        ListenableFuture<Integer> previous = childCountTasks.put(parent, task);
        if (previous != null) previous.cancel(true);
        Futures.addCallback(task, Task.callback(count -> {
            if (childCountTasks.remove(parent, task) && session != null) session.notifyChildrenChanged(parent, count, null);
        }, error -> {
            if (childCountTasks.remove(parent, task) && !(error instanceof CancellationException)) Log.w("BrowseLibrary", "Unable to refresh browse children: " + parent, error);
        }), App::post);
    }

    private <T> ListenableFuture<T> submit(Callable<T> task) {
        ListenableFuture<T> future = Futures.transform(Task.executor().submit(task), result -> result, App::post);
        tasks.add(future);
        future.addListener(() -> tasks.remove(future), MoreExecutors.directExecutor());
        return future;
    }

    void invalidate() {
        generation++;
        tasks.forEach(future -> future.cancel(true));
        tasks.clear();
        childCountTasks.clear();
        searchTasks.clear();
    }

    void release() {
        invalidate();
        session = null;
    }
}
