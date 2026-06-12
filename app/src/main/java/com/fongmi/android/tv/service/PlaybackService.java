package com.fongmi.android.tv.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionError;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.browse.BrowseTree;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.engine.PlaySpec;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Task;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PlaybackService extends MediaLibraryService implements MediaLibrarySession.Callback, PlayerManager.Callback {

    public static final String LOCAL_BIND_ACTION = BuildConfig.APPLICATION_ID.concat(".LOCAL_BIND");

    private static volatile boolean running;

    private final List<PlayerCallback> playerCallbacks = new CopyOnWriteArrayList<>();
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private String currentSiteKey = "";
    private String currentVodId = "";
    private String currentEpisodeUrl = "";
    private boolean wasPlaying = false;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            notifyProgress();
            progressHandler.postDelayed(this, 5000);
        }
    };
    private final IBinder binder = new LocalBinder();

    private NavigationCallback navigationCallback;
    private MediaLibrarySession session;
    private Runnable onNewBinding;
    private boolean externalBound;
    private PlayerManager player;
    private String navigationKey;
    private Player exoPlayer;

    public static boolean isRunning() {
        return running;
    }

    public void replaceBinding(Runnable callback) {
        if (onNewBinding != null) onNewBinding.run();
        onNewBinding = callback;
    }

    public PlayerManager player() {
        return player;
    }

    private boolean hasNavigationCallback() {
        return navigationCallback != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        player = new PlayerManager(this);
        exoPlayer = player.getPlayer();
        exoPlayer.addListener(listener);
        session = new MediaLibrarySession.Builder(this, wrap(exoPlayer), this).build();
        session.setSessionActivity(buildDefaultIntent());
        EventBus.getDefault().register(this);
        Server.get().setService(this);
        setupNotification();
    }

    private PendingIntent buildDefaultIntent() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) intent = new Intent();
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void setupNotification() {
        DefaultMediaNotificationProvider provider = new DefaultMediaNotificationProvider.Builder(this).build();
        session.setMediaButtonPreferences(ImmutableList.of(buildStopButton()));
        provider.setSmallIcon(R.drawable.ic_notification);
        setMediaNotificationProvider(provider);
    }

    private CommandButton buildStopButton() {
        return new CommandButton.Builder(CommandButton.ICON_STOP).setPlayerCommand(Player.COMMAND_STOP).setDisplayName(getString(androidx.media3.ui.R.string.exo_controls_stop_description)).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) handleAction(intent.getAction());
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleAction(String action) {
        if (ActionEvent.PLAY.equals(action)) player.play();
        else if (ActionEvent.PAUSE.equals(action)) player.pause();
        else if (ActionEvent.PREV.equals(action)) dispatchPrev();
        else if (ActionEvent.NEXT.equals(action)) dispatchNext();
        else if (ActionEvent.STOP.equals(action)) dispatchStop();
        else if (ActionEvent.LOOP.equals(action)) dispatchLoop();
        else if (ActionEvent.AUDIO.equals(action)) dispatchAudio();
        else if (ActionEvent.REPLAY.equals(action)) dispatchReplay();
    }

    private boolean isLocalBind(Intent intent) {
        return LOCAL_BIND_ACTION.equals(intent != null ? intent.getAction() : null);
    }

    private boolean isExternalBind(Intent intent) {
        return "android.media.browse.MediaBrowserService".equals(intent != null ? intent.getAction() : null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (isLocalBind(intent)) return binder;
        if (isExternalBind(intent)) externalBound = true;
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isExternalBind(intent)) releaseExternal();
        if (isLocalBind(intent)) tryShutdown();
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        tryShutdown();
    }

    @Override
    public void onDisconnected(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
        if (controller.getPackageName().equals(getPackageName())) return;
        tryShutdown();
    }

    @Override
    public void onDestroy() {
        stopProgressTracking();
        notifyPlaybackStop();
        running = false;
        releaseSession();
        player.release();
        removeForeground();
        Server.get().setService(null);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void stopAndClear() {
        player.stop();
        exoPlayer.clearMediaItems();
    }

    public void suspend() {
        stopAndClear();
        removeForeground();
    }

    public void shutdown() {
        if (!running) return;
        running = false;
        stopAndClear();
        stopSelf();
    }

    private void tryShutdown() {
        if (!hasNavigationCallback() && !hasExternalClient()) shutdown();
    }

    private void releaseExternal() {
        externalBound = false;
        saveProgress();
        BrowseTree.clear();
        tryShutdown();
    }

    private void releaseSession() {
        if (session == null) return;
        session.release();
        session = null;
    }

    private void removeForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void saveProgress() {
        if (hasNavigationCallback() || session == null) return;
        if (BrowseTree.saveProgress(exoPlayer.getCurrentPosition(), exoPlayer.getDuration())) {
            session.notifyChildrenChanged("VOD", 0, null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (session == null) return;
        if (event.isVod()) {
            BrowseTree.clearVod();
            session.notifyChildrenChanged("VOD", 0, null);
        } else if (event.isLive()) {
            BrowseTree.clearLive();
            session.notifyChildrenChanged("LIVE", 0, null);
        }
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @NonNull
    @Override
    public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
        return new MediaLibrarySession.ConnectionResult.AcceptedResultBuilder(session).build();
    }

    public boolean hasExternalClient() {
        return externalBound;
    }

    public void setSessionActivity(PendingIntent pendingIntent) {
        if (session != null) session.setSessionActivity(pendingIntent);
    }

    public void resetSessionActivity() {
        setSessionActivity(buildDefaultIntent());
    }

    public void setNavigationCallback(NavigationCallback navigationCallback, String key) {
        this.navigationCallback = navigationCallback;
        this.navigationKey = key;
    }

    private boolean isNavigationOwner() {
        return navigationKey == null || navigationKey.equals(player.getKey());
    }

    public void addPlayerCallback(PlayerCallback callback) {
        playerCallbacks.add(callback);
    }

    public void removePlayerCallback(PlayerCallback callback) {
        playerCallbacks.remove(callback);
    }

    public boolean hasPlayerCallback() {
        return !playerCallbacks.isEmpty();
    }

    public void dispatchPrev() {
        dispatchNavigate(NavigationCallback::onPrev, -1);
    }

    public void dispatchNext() {
        dispatchNavigate(NavigationCallback::onNext, 1);
    }

    private void dispatchNavigate(Consumer<NavigationCallback> action, int delta) {
        if (hasNavigationCallback() && isNavigationOwner()) dispatch(action);
        else navigateItem(delta);
    }

    public void dispatchStop() {
        if (exoPlayer.getPlaybackState() == Player.STATE_IDLE) return;
        if (hasNavigationCallback() && isNavigationOwner()) dispatch(NavigationCallback::onStop);
        else stopAndClear();
    }

    public void dispatchLoop() {
        dispatch(NavigationCallback::onLoop);
    }

    public void dispatchReplay() {
        if (hasNavigationCallback() && isNavigationOwner()) dispatch(NavigationCallback::onReplay);
        else {
            exoPlayer.seekTo(0);
            exoPlayer.play();
        }
    }

    public void dispatchAudio() {
        dispatch(NavigationCallback::onAudio);
    }

    private void dispatch(Consumer<NavigationCallback> action) {
        NavigationCallback callback = navigationCallback;
        if (callback != null) App.post(() -> action.accept(callback));
    }

    private void navigateItem(int delta) {
        MediaItem current = exoPlayer.getCurrentMediaItem();
        if (current == null) return;
        Task.submit(() -> {
            try {
                MediaItem next = BrowseTree.navigate(current.mediaId, delta);
                if (next == null || next.localConfiguration == null) return;
                Result result = BrowseTree.consumeBrowseResult(next.mediaId);
                if (result == null || !isRunning()) return;
                App.post(() -> startBrowse(player, next, result, 0));
            } catch (Exception ignored) {
            }
        });
    }

    private boolean isSameItem(MediaItem item) {
        if (item == null || item.localConfiguration == null) return false;
        return item.localConfiguration.uri.toString().equals(player.getUrl());
    }

    private void interceptItem(@NonNull MediaItem item, long startPositionMs) {
        if (isSameItem(item)) return;
        playViaManager(item, startPositionMs);
    }

    private void interceptItems(@NonNull List<MediaItem> items, int startIndex, long startPositionMs) {
        if (items.isEmpty()) return;
        int idx = (startIndex >= 0 && startIndex < items.size()) ? startIndex : 0;
        interceptItem(items.get(idx), startPositionMs > 0 ? startPositionMs : 0);
    }

    private ForwardingPlayer wrap(Player base) {
        return new ForwardingPlayer(base) {
            @Override
            public void setMediaItem(@NonNull MediaItem item) {
                interceptItem(item, 0);
            }

            @Override
            public void setMediaItem(@NonNull MediaItem item, boolean resetPosition) {
                interceptItem(item, 0);
            }

            @Override
            public void setMediaItem(@NonNull MediaItem item, long startPositionMs) {
                interceptItem(item, startPositionMs);
            }

            @Override
            public void setMediaItems(@NonNull List<MediaItem> items) {
                interceptItems(items, 0, 0);
            }

            @Override
            public void setMediaItems(@NonNull List<MediaItem> items, boolean resetPosition) {
                interceptItems(items, 0, 0);
            }

            @Override
            public void setMediaItems(@NonNull List<MediaItem> items, int startIndex, long startPositionMs) {
                interceptItems(items, startIndex, startPositionMs);
            }

            @Override
            public void seekToPrevious() {
                dispatchPrev();
            }

            @Override
            public void seekToPreviousMediaItem() {
                dispatchPrev();
            }

            @Override
            public void seekToNext() {
                dispatchNext();
            }

            @Override
            public void seekToNextMediaItem() {
                dispatchNext();
            }

            @Override
            public void stop() {
                dispatchStop();
            }

            @NonNull
            @Override
            public Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon().add(COMMAND_SEEK_TO_PREVIOUS).add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).add(COMMAND_SEEK_TO_NEXT).add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(COMMAND_SEEK_BACK).add(COMMAND_SEEK_FORWARD).add(COMMAND_STOP).build();
            }
        };
    }

    private void playViaManager(MediaItem item, long startPositionMs) {
        if (item == null || item.localConfiguration == null) return;
        android.util.Log.d("PlaybackService", "playViaManager: mediaId=" + item.mediaId);
        Result result = BrowseTree.consumeBrowseResult(item.mediaId);
        android.util.Log.d("PlaybackService", "playViaManager: result=" + (result != null));
        if (result != null) {
            startBrowse(player, item, result, startPositionMs);
        } else {
            extractPlaybackInfo(item);
        }
    }

    private void startBrowse(PlayerManager manager, MediaItem item, Result result, long startPositionMs) {
        currentSiteKey = result.getKey();
        currentVodId = result.getVodId();
        currentEpisodeUrl = result.getEpisodeId();
        android.util.Log.d("PlaybackService", "startBrowse: key=" + currentSiteKey + ", vodId=" + currentVodId + ", episodeUrl=" + currentEpisodeUrl);
        manager.startBrowse(PlaySpec.from(result, item.mediaId, item.mediaMetadata));
        if (startPositionMs > 0) manager.seekTo(startPositionMs);
        notifyPlaybackStart();
        startProgressTracking();
    }

    private void extractPlaybackInfo(MediaItem item) {
        if (item.mediaId == null || !item.mediaId.contains("@@@")) return;
        String[] parts = item.mediaId.split("@@@", 3);
        if (parts.length < 2) return;
        currentSiteKey = parts[0];
        currentVodId = parts[1];
        currentEpisodeUrl = "";

        if (item.mediaMetadata.artist != null) {
            currentEpisodeUrl = item.mediaMetadata.artist.toString();
        }

        com.fongmi.android.tv.bean.History history = com.fongmi.android.tv.bean.History.find(currentVodId);
        if (history != null && currentEpisodeUrl.isEmpty()) {
            currentEpisodeUrl = history.getEpisodeUrl();
        }

        android.util.Log.d("PlaybackService", "extractPlaybackInfo: key=" + currentSiteKey + ", vodId=" + currentVodId + ", episodeUrl=" + currentEpisodeUrl);
    }

    private void ensurePlaybackInfo() {
        if (currentSiteKey.isEmpty() || currentVodId.isEmpty()) {
            MediaItem item = player.getCurrentMediaItem();
            if (item != null) extractPlaybackInfo(item);
        }
    }

    private void notifyPlaybackStart() {
        notifySpider("playback_start");
    }

    private void notifyPlaybackPause() {
        notifySpider("playback_pause");
    }

    private void notifyPlaybackResume() {
        notifySpider("playback_resume");
    }

    private void notifyPlaybackStop() {
        notifySpider("playback_stop");
    }

    private void notifyProgress() {
        if (!player.isPlaying()) return;
        ensurePlaybackInfo();
        notifySpider("playback_progress");
    }

    private void notifySpider(String event) {
        try {
            android.util.Log.d("PlaybackService", "notifySpider: event=" + event + ", siteKey=" + currentSiteKey + ", vodId=" + currentVodId);
            com.fongmi.android.tv.bean.Site site = com.fongmi.android.tv.api.config.VodConfig.get().getSite(currentSiteKey);
            if (site == null || site.spider() == null) return;
            android.util.Log.d("PlaybackService", "notifySpider: calling spider.onPlayback");
            site.spider().onPlayback(event, currentVodId, currentEpisodeUrl, player.getPosition(), player.getDuration());
        } catch (Exception ignored) {
        }
    }

    private void startProgressTracking() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.postDelayed(progressRunnable, 5000);
    }

    private void stopProgressTracking() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    public void onPrepare() {
        playerCallbacks.forEach(PlayerCallback::onPrepare);
    }

    @Override
    public void onTracksChanged() {
        playerCallbacks.forEach(PlayerCallback::onTracksChanged);
    }

    @Override
    public void onTitlesChanged() {
        playerCallbacks.forEach(PlayerCallback::onTitlesChanged);
    }

    @Override
    public void onError(String msg) {
        playerCallbacks.forEach(callback -> callback.onError(msg));
    }

    @Override
    public void onPlayerRebuild(Player newPlayer) {
        exoPlayer.removeListener(listener);
        exoPlayer = newPlayer;
        exoPlayer.addListener(listener);
        if (session != null) session.setPlayer(wrap(newPlayer));
        playerCallbacks.forEach(callback -> callback.onPlayerRebuild(newPlayer));
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_ENDED && !(hasNavigationCallback() && isNavigationOwner())) navigateItem(1);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            ensurePlaybackInfo();
            if (isPlaying && !wasPlaying) {
                notifyPlaybackResume();
                startProgressTracking();
            } else if (!isPlaying && wasPlaying) {
                notifyPlaybackPause();
                stopProgressTracking();
            }
            wasPlaying = isPlaying;
        }
    };

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @Nullable MediaLibraryService.LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.getRootItem(), params));
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        return Task.executor().submit(() -> LibraryResult.ofItemList(BrowseTree.getChildren(parentId), params));
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, @Nullable MediaLibraryService.LibraryParams params) {
        Task.execute(() -> {
            ImmutableList<MediaItem> results = BrowseTree.search(query);
            App.post(() -> session.notifySearchResultChanged(browser, query, results.size(), params));
        });
        return Futures.immediateFuture(LibraryResult.ofVoid(params));
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        return Futures.immediateFuture(LibraryResult.ofItemList(BrowseTree.getSearchResult(), params));
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String mediaId) {
        MediaItem item = BrowseTree.getItem(mediaId);
        return Futures.immediateFuture(item != null ? LibraryResult.ofItem(item, null) : LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
    }

    @NonNull
    @Override
    public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        saveProgress();
        return Task.executor().submit(() -> {
            List<MediaItem> resolved = mediaItems.stream().map(BrowseTree::resolveOrKeep).toList();
            int index = Math.max(startIndex, 0);
            long position = BrowseTree.consumeResumePosition();
            return new MediaSession.MediaItemsWithStartPosition(resolved, index, position);
        });
    }

    public interface PlayerCallback {

        default void onPrepare() {
        }

        default void onTracksChanged() {
        }

        default void onTitlesChanged() {
        }

        default void onError(String msg) {
        }

        default void onPlayerRebuild(Player player) {
        }
    }

    public interface NavigationCallback {

        default void onPrev() {
        }

        default void onNext() {
        }

        default void onStop() {
        }

        default void onLoop() {
        }

        default void onReplay() {
        }

        default void onAudio() {
        }
    }

    public class LocalBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}