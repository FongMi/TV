package com.fongmi.android.tv.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingSimpleBasePlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaLibraryService.MediaLibrarySession;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionError;
import androidx.media3.session.SessionResult;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.danmaku.DanmakuConfig;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.browse.BrowsePlayback;
import com.fongmi.android.tv.browse.BrowseTree;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.player.media.ArtworkBitmapLoader;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Task;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class PlaybackService extends MediaLibraryService implements MediaLibrarySession.Callback, PlayerManager.Callback {

    public static final String LOCAL_BIND_ACTION = BuildConfig.APPLICATION_ID.concat(".LOCAL_BIND");

    private static final SessionCommand COMMAND_REPEAT = new SessionCommand(ActionEvent.REPEAT, Bundle.EMPTY);
    private static final SessionCommand COMMAND_KEEP = new SessionCommand(BuildConfig.APPLICATION_ID.concat(".keep"), Bundle.EMPTY);
    private static final SessionCommand COMMAND_SPEED = new SessionCommand(BuildConfig.APPLICATION_ID.concat(".speed"), Bundle.EMPTY);
    private static final String ACTION_MEDIA_BROWSER_SERVICE = "android.media.browse.MediaBrowserService";

    private static volatile boolean running;

    private final List<PlayerCallback> playerCallbacks = new CopyOnWriteArrayList<>();
    private final MediaClients clients = new MediaClients();
    private final IBinder binder = new LocalBinder();

    private NavigationBinding navigation;
    private BrowsePlayback browsePlayback;
    private MediaLibrarySession session;
    private BrowseLibrary library;
    private LiveMetadata liveMetadata;
    private ActivityBinding binding;
    private PlayerManager player;
    private SessionPlayer sessionPlayer;
    private Future<?> navigationTask;
    private Future<?> mediaItemsTask;
    private long navigationGeneration;

    public static boolean isRunning() {
        return running;
    }

    public void claimBinding(NavigationCallback owner, Runnable onReplaced) {
        if (ownsBinding(owner)) return;
        liveMetadata.cancel();
        saveProgress();
        cancelNavigation();
        if (binding != null) binding.onReplaced().run();
        binding = new ActivityBinding(owner, onReplaced);
        player.resetDecoderModesForNewSession();
    }

    public boolean ownsBinding(NavigationCallback owner) {
        return binding != null && binding.owner() == owner;
    }

    public boolean releaseBinding(NavigationCallback owner) {
        if (navigation != null && navigation.owner() == owner) setNavigationCallback(null, null);
        if (!ownsBinding(owner)) return false;
        binding = null;
        return true;
    }

    public PlayerManager player() {
        return player;
    }

    public SessionToken getSessionToken() {
        return session.getToken();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        player = new PlayerManager(this);
        sessionPlayer = new SessionPlayer(player.getPlayer());
        session = new MediaLibrarySession.Builder(this, sessionPlayer, this).setBitmapLoader(new ArtworkBitmapLoader(this)).build();
        library = new BrowseLibrary(session);
        liveMetadata = new LiveMetadata(player, library);
        session.setSessionActivity(buildDefaultIntent());
        EventBus.getDefault().register(this);
        Server.get().setService(this);
        setupNotification();
        addSession(session);
    }

    private PendingIntent buildDefaultIntent() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) intent = new Intent();
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void setupNotification() {
        DefaultMediaNotificationProvider provider = new DefaultMediaNotificationProvider.Builder(this).build();
        updateMediaButtons();
        provider.setSmallIcon(R.drawable.ic_notification);
        setMediaNotificationProvider(provider);
    }

    private CommandButton buildStopButton() {
        return new CommandButton.Builder(CommandButton.ICON_STOP).setPlayerCommand(Player.COMMAND_STOP).setDisplayName(getString(R.string.play_stop)).build();
    }

    private CommandButton buildRepeatButton() {
        return new CommandButton.Builder(player.isRepeatOne() ? CommandButton.ICON_REPEAT_ONE : CommandButton.ICON_REPEAT_OFF).setSessionCommand(COMMAND_REPEAT).setDisplayName(getString(R.string.play_repeat)).build();
    }

    private void updateMediaButtons() {
        if (session == null) return;
        ImmutableList.Builder<CommandButton> buttons = ImmutableList.builder();
        buttons.add(buildRepeatButton(), buildStopButton());
        Boolean kept = getKeepState();
        if (kept != null) buttons.add(new CommandButton.Builder(kept ? CommandButton.ICON_HEART_FILLED : CommandButton.ICON_HEART_UNFILLED).setSessionCommand(COMMAND_KEEP).setDisplayName(getString(kept ? R.string.auto_remove_favorite : R.string.auto_add_favorite)).build());
        if (canSetAutoSpeed()) buttons.add(new CommandButton.Builder(CommandButton.ICON_PLAYBACK_SPEED).setSessionCommand(COMMAND_SPEED).setDisplayName(getString(R.string.play_speed_hint, SpeedSetting.format(player.getSpeed()))).build());
        session.setMediaButtonPreferences(buttons.build());
    }

    @Nullable
    private Boolean getKeepState() {
        if (canDispatchNavigation()) return navigation.owner().isKept();
        BrowsePlayback playback = getBrowsePlayback();
        return playback == null ? null : playback.isKept();
    }

    private boolean toggleKeep() {
        if (getKeepState() == null) return false;
        boolean local = canDispatchNavigation();
        BrowsePlayback playback = getBrowsePlayback();
        boolean changed = local ? navigation.owner().onKeep() : playback != null && playback.toggleKeep();
        if (changed && !local) RefreshEvent.keep();
        return changed;
    }

    private boolean canSetAutoSpeed() {
        MediaItem item = player.getCurrentMediaItem();
        return canRemoteSetSpeed() && (canDispatchNavigation() ? navigation.owner().canSetSpeed() : item != null && !item.mediaId.startsWith("LC:"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) handleAction(intent.getAction());
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleAction(String action) {
        if (ActionEvent.PLAY.equals(action)) dispatchPlayPause(true);
        else if (ActionEvent.PAUSE.equals(action)) dispatchPlayPause(false);
        else if (ActionEvent.PREV.equals(action)) dispatchPrev();
        else if (ActionEvent.NEXT.equals(action)) dispatchNext();
        else if (ActionEvent.STOP.equals(action)) dispatchStop();
        else if (ActionEvent.AUDIO.equals(action)) dispatchAudio();
        else if (ActionEvent.REPEAT.equals(action)) dispatchRepeat();
        else if (ActionEvent.REPLAY.equals(action)) dispatchReplay();
    }

    private void dispatchPlayPause(boolean play) {
        if (hasExternalPlayback()) dispatch(play ? NavigationCallback::onPlay : NavigationCallback::onPause);
        else if (play) player.play();
        else player.pause();
    }

    public boolean canRemoteNavigate() {
        MediaItem item = player.getCurrentMediaItem();
        return canDispatchNavigation() || item != null && BrowseTree.canNavigate(item.mediaId);
    }

    public boolean hasRemotePlayback() {
        return !player.isReleased() && player.getCurrentMediaItem() != null && player.getPlaybackState() != Player.STATE_IDLE && !hasExternalPlayback();
    }

    public boolean canRemoteSeek() {
        return hasRemotePlayback() && player.canSeek() && player.getDuration() > 0;
    }

    public boolean canRemoteSetSpeed() {
        return hasRemotePlayback() && !player.isLive() && player.getPlayer().isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH);
    }

    public boolean dispatchRemoteSubtitle(Sub sub) {
        if (!hasRemotePlayback() || player.isLive()) return false;
        player.setSub(sub);
        return true;
    }

    public boolean dispatchRemoteDanmaku(Danmaku danmaku) {
        if (!hasRemotePlayback() || player.isLive()) return false;
        player.setDanmaku(danmaku);
        return true;
    }

    public boolean dispatchRemoteControl(String type, long position, float speed) {
        if (!hasRemotePlayback()) return false;
        switch (type) {
            case "play" -> {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                dispatchPlayPause(true);
            }
            case "pause" -> dispatchPlayPause(false);
            case "stop" -> dispatchStop();
            case "prev" -> {
                if (!canRemoteNavigate()) return false;
                dispatchPrev();
            }
            case "next" -> {
                if (!canRemoteNavigate()) return false;
                dispatchNext();
            }
            case "repeat_on", "repeat_off" -> {
                if (player.isLive()) return false;
                player.setRepeatOne(type.equals("repeat_on"));
            }
            case "seek" -> {
                if (position < 0 || !canRemoteSeek()) return false;
                player.seekTo(Math.min(position, player.getDuration()));
            }
            case "speed" -> {
                if (!canRemoteSetSpeed()) return false;
                player.setSpeed(speed);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean isLocalBind(Intent intent) {
        return LOCAL_BIND_ACTION.equals(intent != null ? intent.getAction() : null);
    }

    private boolean isBrowserBind(Intent intent) {
        return ACTION_MEDIA_BROWSER_SERVICE.equals(intent != null ? intent.getAction() : null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (isLocalBind(intent)) return binder;
        if (isBrowserBind(intent)) clients.bind();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isBrowserBind(intent)) releaseBrowser();
        if (isLocalBind(intent)) tryShutdown();
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        tryShutdown();
    }

    @Override
    public void onDisconnected(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
        if (isAppController(controller)) return;
        releaseController(controller);
    }

    @Override
    public void onDestroy() {
        running = false;
        liveMetadata.cancel();
        cancelNavigation();
        library.release();
        saveProgress();
        browsePlayback = null;
        BrowseTree.clear();
        releaseSession();
        player.release();
        removeForeground();
        Server.get().setService(null);
        EventBus.getDefault().unregister(this);
        HomeActivity.releaseResources();
        super.onDestroy();
    }

    public boolean stopPlayback(NavigationCallback owner) {
        if (!ownsBinding(owner)) return false;
        stopPlayback();
        return true;
    }

    private void stopPlayback() {
        liveMetadata.cancel();
        cancelNavigation();
        player.stop();
        Source.get().stop();
    }

    private void stopAndClear() {
        stopPlayback();
        browsePlayback = null;
        player.clearPreload();
        player.clearMediaItems();
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
        if (navigation == null && !hasMediaClient()) shutdown();
    }

    private void releaseBrowser() {
        clients.unbind();
        if (!hasMediaClient()) releaseMediaState();
        else tryShutdown();
    }

    private void releaseController(@NonNull MediaSession.ControllerInfo controller) {
        clients.disconnect(controller);
        if (!hasMediaClient()) releaseMediaState();
        else tryShutdown();
    }

    private void releaseMediaState() {
        if (navigation != null) return;
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
        if (session == null) return;
        if (canDispatchNavigation()) {
            navigation.owner().onSaveProgress();
            return;
        }
        BrowsePlayback playback = getBrowsePlayback();
        if (playback != null) playback.saveProgress(playback.getMediaId(), player.getPosition(), player.getDuration());
    }

    @Nullable
    private BrowsePlayback getBrowsePlayback() {
        MediaItem item = player.getCurrentMediaItem();
        return browsePlayback != null && item != null && browsePlayback.matches(item.mediaId) ? browsePlayback : null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (session == null) return;
        if (event.getType() == RefreshEvent.Type.KEEP) {
            library.notifyChildrenChanged(BrowseTree.VOD_KEEP);
            library.notifyChildrenChanged(BrowseTree.LIVE_KEEP);
            updateMediaButtons();
        } else if (event.getType() == RefreshEvent.Type.HISTORY) {
            library.notifyChildrenChanged("VOD");
            BrowsePlayback playback = getBrowsePlayback();
            String parent = playback == null ? null : playback.getFolderId();
            if (parent != null) library.notifyChildrenChanged(parent);
            updateMediaButtons();
        } else if (event.getType() == RefreshEvent.Type.HOME) {
            cancelNavigation();
            library.invalidate();
            BrowseTree.clearVod();
            library.notifyChildrenChanged("VOD");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (session == null) return;
        liveMetadata.cancel();
        cancelNavigation();
        library.invalidate();
        if (event.isVod()) {
            BrowseTree.clearVod();
            library.notifyChildrenChanged("VOD");
            library.notifyChildrenChanged(BrowseTree.VOD_KEEP);
        } else if (event.isLive()) {
            BrowseTree.clearSearch();
        }
        if (event.isVod() || event.isLive()) {
            library.notifyChildrenChanged("LIVE");
            library.notifyChildrenChanged(BrowseTree.LIVE_KEEP);
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
        clients.connect(controller, getPackageName());
        SessionCommands commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon().add(COMMAND_REPEAT).add(COMMAND_KEEP).add(COMMAND_SPEED).build();
        return new MediaLibrarySession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).build();
    }

    private boolean isAppController(@NonNull MediaSession.ControllerInfo controller) {
        return clients.isSelf(controller, getPackageName());
    }

    @NonNull
    @Override
    public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull SessionCommand customCommand, @NonNull Bundle args) {
        if (COMMAND_REPEAT.customAction.equals(customCommand.customAction)) {
            dispatchRepeat();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        if (COMMAND_KEEP.customAction.equals(customCommand.customAction)) return Futures.immediateFuture(new SessionResult(toggleKeep() ? SessionResult.RESULT_SUCCESS : SessionError.ERROR_NOT_SUPPORTED));
        if (COMMAND_SPEED.customAction.equals(customCommand.customAction)) {
            if (!canSetAutoSpeed()) return Futures.immediateFuture(new SessionResult(SessionError.ERROR_NOT_SUPPORTED));
            float speed = player.getSpeed();
            player.setSpeed(speed < 1 ? 1 : speed < 1.25f ? 1.25f : speed < 1.5f ? 1.5f : speed < 2 ? 2 : 1);
            updateMediaButtons();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        return MediaLibrarySession.Callback.super.onCustomCommand(session, controller, customCommand, args);
    }

    public boolean hasMediaClient() {
        return clients.hasAny();
    }

    public void setSessionActivity(PendingIntent pendingIntent) {
        if (session != null) session.setSessionActivity(pendingIntent);
    }

    public void resetSessionActivity() {
        setSessionActivity(buildDefaultIntent());
    }

    public void setNavigationCallback(NavigationCallback navigationCallback, String key) {
        if (navigationCallback == null) navigation = null;
        else if (navigation == null || navigation.owner() != navigationCallback || !Objects.equals(navigation.key(), key)) navigation = new NavigationBinding(navigationCallback, key);
        if (sessionPlayer != null) sessionPlayer.updateCommands();
        updateMediaButtons();
    }

    private boolean isNavigationOwner() {
        return navigation != null && (navigation.key() == null || navigation.key().equals(player.getKey()));
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
        if (!canRemoteNavigate()) return;
        if (canDispatchNavigation()) dispatch(action);
        else navigateItem(delta);
    }

    private boolean canDispatchNavigation() {
        return hasExternalPlayback() || isNavigationOwner();
    }

    private boolean hasExternalPlayback() {
        return navigation != null && navigation.owner().isExternalPlaybackActive();
    }

    public void dispatchStop() {
        cancelNavigation();
        if (hasExternalPlayback()) {
            dispatch(NavigationCallback::onStop);
        } else if (player.getPlaybackState() == Player.STATE_IDLE) {
            return;
        } else if (canDispatchNavigation()) {
            dispatch(NavigationCallback::onStop);
        } else {
            saveProgress();
            stopAndClear();
        }
    }

    public void dispatchRepeat() {
        player.setRepeatOne(!player.isRepeatOne());
    }

    public void dispatchReplay() {
        if (canDispatchNavigation()) {
            dispatch(NavigationCallback::onReplay);
        } else {
            player.seekTo(0);
            player.play();
        }
    }

    public void dispatchAudio() {
        dispatch(NavigationCallback::onAudio);
    }

    private void dispatch(Consumer<NavigationCallback> action) {
        NavigationBinding target = navigation;
        long generation = navigationGeneration;
        if (target != null) App.post(() -> {
            if (navigation == target && generation == navigationGeneration && canDispatchNavigation()) action.accept(target.owner());
        });
    }

    private void navigateItem(int delta) {
        MediaItem current = player.getCurrentMediaItem();
        if (current == null) return;
        cancelNavigation();
        long generation = navigationGeneration;
        BrowsePlayback playback = getBrowsePlayback();
        History bookmark = playback == null ? null : playback.copyHistory();
        navigationTask = Task.submit(() -> loadNextItem(current, delta, bookmark, generation));
    }

    private void loadNextItem(MediaItem current, int delta, History bookmark, long generation) {
        try {
            MediaItem next = BrowseTree.navigate(current.mediaId, delta);
            if (next == null || next.localConfiguration == null) return;
            MediaItem selection = BrowsePlayback.withBookmark(next, bookmark);
            App.post(() -> {
                if (session != null && generation == navigationGeneration && current.equals(player.getCurrentMediaItem())) startBrowse(selection, 0);
            });
        } catch (Exception e) {
            App.post(() -> {
                if (session != null && generation == navigationGeneration && current.equals(player.getCurrentMediaItem())) sendBrowseError(session, e);
            });
        }
    }

    private void cancelNavigation() {
        navigationGeneration++;
        if (navigationTask != null) navigationTask.cancel(true);
        if (mediaItemsTask != null) mediaItemsTask.cancel(true);
        navigationTask = null;
        mediaItemsTask = null;
        player.cancelBrowse();
    }

    private void setBrowseItems(@NonNull List<MediaItem> items, int startIndex, long startPositionMs) {
        if (items.isEmpty()) {
            stopAndClear();
            return;
        }
        int idx = (startIndex >= 0 && startIndex < items.size()) ? startIndex : 0;
        MediaItem item = items.get(idx);
        if (item.localConfiguration == null) return;
        setBrowseItem(item, startPositionMs);
    }

    private final class SessionPlayer extends ForwardingSimpleBasePlayer {

        private SessionPlayer(Player player) {
            super(player);
            player.addListener(listener);
        }

        private void replacePlayer(Player player) {
            getPlayer().removeListener(listener);
            setPlayer(player);
            player.addListener(listener);
        }

        private void updateCommands() {
            invalidateState();
        }

        @Override
        protected ListenableFuture<?> handlePrepare() {
            if (!hasExternalPlayback()) player.prepareBrowse();
            return Futures.immediateVoidFuture();
        }

        @Override
        protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
            if (hasExternalPlayback()) dispatchPlayPause(playWhenReady);
            else getPlayer().setPlayWhenReady(playWhenReady);
            return Futures.immediateVoidFuture();
        }

        @Override
        protected ListenableFuture<?> handleSetMediaItems(List<MediaItem> items, int startIndex, long startPositionMs) {
            setBrowseItems(items, startIndex, startPositionMs);
            return Futures.immediateVoidFuture();
        }

        @Override
        protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int command) {
            if (command == COMMAND_SEEK_TO_PREVIOUS || command == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) dispatchPrev();
            else if (command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) dispatchNext();
            else if (player.getTimeshift().isActive()) return player.getTimeshift().seekTo(positionMs);
            else return super.handleSeek(mediaItemIndex, positionMs, command);
            return Futures.immediateVoidFuture();
        }

        @Override
        protected ListenableFuture<?> handleStop() {
            dispatchStop();
            return Futures.immediateVoidFuture();
        }

        @Override
        protected State getState() {
            State state = player.getTimeshift().getState(super.getState());
            Player.Commands.Builder commands = state.availableCommands.buildUpon().removeAll(COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(COMMAND_STOP);
            if (canRemoteNavigate()) commands.addAll(COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM);
            return state.buildUpon().setAvailableCommands(commands.build()).build();
        }
    }

    @Override
    public void onTimeshiftChanged() {
        if (sessionPlayer != null) sessionPlayer.updateCommands();
    }

    private void startBrowse(MediaItem item, long startPositionMs) {
        saveProgress();
        if (!setBrowseItem(item, startPositionMs)) return;
        player.prepareBrowse();
        player.play();
    }

    private boolean setBrowseItem(MediaItem item, long startPositionMs) {
        BrowsePlayback selection = BrowsePlayback.from(item);
        if (selection == null) return false;
        Result result = selection.getResult();
        cancelNavigation();
        if (result.hasMsg()) {
            session.sendError(new SessionError(SessionError.ERROR_UNKNOWN, result.getMsg()));
        } else if (Source.isPush(result.getUrl().v())) {
            redirectPush(result.getUrl().v());
        } else {
            if (!player.browse(result, item.mediaId, item.mediaMetadata, startPositionMs)) return false;
            if (binding != null) binding.onReplaced().run();
            binding = null;
            browsePlayback = selection;
            selection.activate(startPositionMs);
            setNavigationCallback(null, null);
            resetSessionActivity();
            liveMetadata.refresh();
            return true;
        }
        return false;
    }

    private void redirectPush(String url) {
        NavigationCallback callback = navigation == null ? null : navigation.owner();
        if (callback != null && ownsBinding(callback) && callback.onPush(url)) return;
        session.sendError(new SessionError(SessionError.ERROR_NOT_SUPPORTED, ResUtil.getString(R.string.error_play_url)));
    }

    @Override
    public void onPreloadError(Object tag) {
        playerCallbacks.forEach(callback -> callback.onPreloadError(tag));
    }

    @Override
    public void onPreloadTransition(Object tag, long oldPositionMs, long oldDurationMs) {
        playerCallbacks.forEach(callback -> callback.onPreloadTransition(tag, oldPositionMs, oldDurationMs));
    }

    @Override
    public void onPrepare() {
        if (binding != null && getBrowsePlayback() == null) browsePlayback = null;
        playerCallbacks.forEach(PlayerCallback::onPrepare);
    }

    @Override
    public void onTracksChanged() {
        playerCallbacks.forEach(PlayerCallback::onTracksChanged);
    }

    @Override
    public void onDecodeChanged() {
        playerCallbacks.forEach(PlayerCallback::onDecodeChanged);
    }

    @Override
    public void onMediaOptionsChanged() {
        playerCallbacks.forEach(PlayerCallback::onMediaOptionsChanged);
    }

    @Override
    public void onError(String msg) {
        playerCallbacks.forEach(callback -> callback.onError(msg));
    }

    @Override
    public boolean onRefresh() {
        boolean handled = false;
        for (PlayerCallback callback : playerCallbacks) handled |= callback.onRefresh();
        return handled;
    }

    @Override
    public void onPlayerRebuild(Player newPlayer) {
        sessionPlayer.replacePlayer(newPlayer);
        playerCallbacks.forEach(callback -> callback.onPlayerRebuild(newPlayer));
    }

    @Override
    public void onDanmakuSourceChanged(@Nullable Uri uri) {
        playerCallbacks.forEach(callback -> callback.onDanmakuSourceChanged(uri));
    }

    @Override
    public void onDanmakuConfigChanged(DanmakuConfig config) {
        playerCallbacks.forEach(callback -> callback.onDanmakuConfigChanged(config));
    }

    @Override
    public void onDanmakuEnabledChanged(boolean enabled) {
        playerCallbacks.forEach(callback -> callback.onDanmakuEnabledChanged(enabled));
    }

    @Override
    public void onDanmakuSent(String text) {
        playerCallbacks.forEach(callback -> callback.onDanmakuSent(text));
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_ENDED && !canDispatchNavigation() && getBrowsePlayback() != null) navigateItem(1);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            updateMediaButtons();
        }

        @Override
        public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
            updateMediaButtons();
        }

        @Override
        public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
            updateMediaButtons();
        }

        @Override
        public void onAvailableCommandsChanged(@NonNull Player.Commands availableCommands) {
            updateMediaButtons();
        }
    };

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @Nullable MediaLibraryService.LibraryParams params) {
        return library.getRoot(params);
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        return library.getChildren(parentId, page, pageSize, params);
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, @Nullable MediaLibraryService.LibraryParams params) {
        return library.search(browser, query, params);
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, int page, int pageSize, @Nullable MediaLibraryService.LibraryParams params) {
        return library.getSearchResult(query, page, pageSize, params);
    }

    @NonNull
    @Override
    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String mediaId) {
        return library.getItem(mediaId);
    }

    @NonNull
    @Override
    public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
        cancelNavigation();
        saveProgress();
        long generation = navigationGeneration;
        BrowsePlayback playback = getBrowsePlayback();
        History bookmark = playback == null ? null : playback.copyHistory();
        ListenableFuture<MediaSession.MediaItemsWithStartPosition> future = Task.executor().submit(() -> {
            try {
                return resolveMediaItems(mediaItems, startIndex, startPositionMs, bookmark);
            } catch (Exception e) {
                App.post(() -> {
                    if (this.session == session && generation == navigationGeneration) sendBrowseError(session, e);
                });
                throw e;
            }
        });
        future = Futures.transform(future, items -> items, App::post);
        mediaItemsTask = future;
        return future;
    }

    private MediaSession.MediaItemsWithStartPosition resolveMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs, History bookmark) throws Exception {
        List<MediaItem> resolved = new ArrayList<>(mediaItems);
        int index = resolved.isEmpty() ? 0 : Math.clamp(startIndex, 0, resolved.size() - 1);
        if (!resolved.isEmpty()) resolved.set(index, BrowseTree.resolveForPlayback(resolved.get(index), bookmark));
        BrowsePlayback selection = resolved.isEmpty() ? null : BrowsePlayback.from(resolved.get(index));
        long resumePositionMs = selection == null ? C.TIME_UNSET : selection.getResumePositionMs();
        long positionMs = startPositionMs != C.TIME_UNSET ? startPositionMs : resumePositionMs;
        return new MediaSession.MediaItemsWithStartPosition(resolved, index, positionMs);
    }

    private void sendBrowseError(MediaSession session, Exception error) {
        session.sendError(new SessionError(SessionError.ERROR_UNKNOWN,
                error.getMessage() == null ? ResUtil.getString(R.string.error_play_url) : error.getMessage()));
    }

    public interface PlayerCallback {

        default void onPreloadError(Object tag) {
        }

        default void onPreloadTransition(Object tag, long oldPositionMs, long oldDurationMs) {
        }

        default void onPrepare() {
        }

        default void onTracksChanged() {
        }

        default void onDecodeChanged() {
        }

        default void onMediaOptionsChanged() {
        }

        default void onError(String msg) {
        }

        default boolean onRefresh() {
            return false;
        }

        default void onPlayerRebuild(Player player) {
        }

        default void onDanmakuSourceChanged(@Nullable Uri uri) {
        }

        default void onDanmakuConfigChanged(DanmakuConfig config) {
        }

        default void onDanmakuEnabledChanged(boolean enabled) {
        }

        default void onDanmakuSent(String text) {
        }
    }

    public interface NavigationCallback {

        default boolean canSetSpeed() {
            return true;
        }

        @Nullable
        default Boolean isKept() {
            return null;
        }

        default boolean onKeep() {
            return false;
        }

        default void onSaveProgress() {
        }

        default boolean onPush(String url) {
            return false;
        }

        default boolean isExternalPlaybackActive() {
            return false;
        }

        default void onPlay() {
        }

        default void onPause() {
        }

        default void onPrev() {
        }

        default void onNext() {
        }

        default void onStop() {
        }

        default void onReplay() {
        }

        default void onAudio() {
        }
    }

    private record NavigationBinding(NavigationCallback owner, String key) {
    }

    private record ActivityBinding(NavigationCallback owner, Runnable onReplaced) {
    }

    public class LocalBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}
