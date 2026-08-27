package com.fongmi.android.tv.ui.activity;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerSeekView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import androidx.media3.ui.danmaku.DanmakuConfig;
import androidx.media3.ui.danmaku.DanmakuPlayerViewController;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.PlaybackIntent;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class PlaybackActivity extends BaseActivity implements MediaController.Listener, Player.Listener, ServiceConnection {

    private final DanmakuPlayerViewController danmakuController = new DanmakuPlayerViewController();
    private final List<ServiceReadyObserver<?>> serviceReadyObservers = new ArrayList<>();
    private final List<Runnable> foreverObserverRemovers = new ArrayList<>();
    private ListenableFuture<MediaController> mControllerFuture;
    private MediaController mController;
    private PlaybackService mService;
    private boolean initialized;
    private boolean audioOnly;
    private boolean scrubbing;
    private boolean redirect;
    private boolean bound;
    private boolean stop;
    private boolean lock;

    protected MediaController controller() {
        return mController;
    }

    protected PlaybackService service() {
        return mService;
    }

    protected PlayerManager player() {
        return mService.player();
    }

    protected boolean isRedirect() {
        return redirect;
    }

    protected void setRedirect(boolean redirect) {
        this.redirect = redirect;
        if (mService != null) mService.setNavigationCallback(redirect ? null : getNavigationCallback(), getPlaybackKey());
    }

    protected void updateNavigationKey() {
        updateNavigationKey(getPlaybackKey());
    }

    protected void updateNavigationKey(String key) {
        if (mService != null) mService.setNavigationCallback(getNavigationCallback(), key);
    }

    protected boolean isAudioOnly() {
        return audioOnly;
    }

    protected void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    protected boolean isStop() {
        return stop;
    }

    protected void setStop(boolean stop) {
        this.stop = stop;
    }

    protected boolean isLock() {
        return lock;
    }

    protected void setLock(boolean lock) {
        this.lock = lock;
    }

    protected abstract PlaybackService.NavigationCallback getNavigationCallback();

    protected abstract PlayerSeekView getSeekView();

    protected abstract PlayerView getPlayerView();

    protected abstract String getPlaybackKey();

    protected boolean isOwner() {
        String key = getPlaybackKey();
        return key == null || (mService != null && key.equals(player().getKey()));
    }

    protected boolean isBindingOwner() {
        return mService != null && mService.ownsBinding(getNavigationCallback());
    }

    protected boolean hasPlaybackSource() {
        return mService != null && isOwner() && !player().isEmpty();
    }

    protected <T> void observeForever(LiveData<T> liveData, Observer<T> observer) {
        liveData.observeForever(observer);
        foreverObserverRemovers.add(() -> liveData.removeObserver(observer));
    }

    protected <T> void observeWhenServiceReady(LiveData<T> liveData, Observer<T> observer) {
        ServiceReadyObserver<T> serviceObserver = new ServiceReadyObserver<>(observer);
        serviceReadyObservers.add(serviceObserver);
        observeForever(liveData, serviceObserver);
    }

    public void toggleDebugView() {
        getPlayerView().toggleDebugView();
        PlayerSetting.putDebug(getPlayerView().isDebugViewVisible());
    }

    public void onChoose() {
        if (!hasPlaybackSource()) return;
        PlayerManager player = player();
        PlaybackIntent.choose(this, player.getUrl(), player.getHeaders(), player.isVod(), player.getPosition(), player.getMediaTitle());
        setRedirect(true);
    }

    public void onShare(CharSequence title, String url, Map<String, String> headers) {
        PlaybackIntent.share(this, url, headers, title);
        setRedirect(true);
    }

    protected void setSeekNextFocusDown(int id) {
        View timeBar = getSeekView().findViewById(androidx.media3.ui.R.id.exo_progress);
        if (timeBar != null) timeBar.setNextFocusDownId(id);
    }

    protected void setActionFocusBoundary(View view) {
        if (view == null) return;
        if (view.isFocusable() && view.getId() != View.NO_ID) view.setNextFocusDownId(view.getId());
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) setActionFocusBoundary(group.getChildAt(i));
    }

    protected boolean isIdle() {
        return isPlaybackState(Player.STATE_IDLE);
    }

    protected boolean isEnded() {
        return isPlaybackState(Player.STATE_ENDED);
    }

    protected boolean isBuffering() {
        return isPlaybackState(Player.STATE_BUFFERING);
    }

    protected boolean isPaused() {
        return mController != null && !isBuffering() && !isIdle();
    }

    private boolean isPlaybackState(int state) {
        return mController != null && mController.getPlaybackState() == state;
    }

    protected void onServiceConnected() {
    }

    protected void onPrepare() {
    }

    protected void onTracksChanged() {
    }

    protected void onDecodeChanged() {
    }

    protected void onMediaOptionsChanged() {
    }

    protected void onError(String msg) {
    }

    protected void onPlayingChanged(boolean isPlaying) {
    }

    protected void onStateChanged(int state) {
    }

    protected void onSizeChanged(VideoSize size) {
    }

    protected void onReclaim() {
    }

    protected long startPositionMs() {
        return C.TIME_UNSET;
    }

    protected boolean seekTo(long deltaMs) {
        PlayerManager player = player();
        long targetMs = Math.max(0, player.getPosition() + deltaMs);
        long durationMs = player.getDuration();
        boolean seekToEnd = durationMs > 0 && targetMs >= durationMs;
        mController.seekTo(seekToEnd ? durationMs : targetMs);
        if (!seekToEnd) mController.play();
        return seekToEnd;
    }

    protected void startPlayer(String key, Result result, boolean useParse, long timeout, MediaMetadata metadata) {
        startPlayer(key, result, useParse, timeout, startPositionMs(), metadata);
    }

    protected void startPlayer(String key, Result result, boolean useParse, long timeout, long startPositionMs, MediaMetadata metadata) {
        String error = getPlaybackError(result);
        if (error != null) onError(error);
        else startPlayerInternal(key, result, useParse, timeout, startPositionMs, metadata);
    }

    @Nullable
    private String getPlaybackError(Result result) {
        if (result.hasMsg()) return result.getMsg();
        if (result.getRealUrl().isEmpty()) return ResUtil.getString(R.string.error_play_url);
        if (result.getDrm() != null && !FrameworkMediaDrm.isCryptoSchemeSupported(result.getDrm().getUUID())) return ResUtil.getString(R.string.error_play_drm);
        return null;
    }

    private void startPlayerInternal(String key, Result result, boolean useParse, long timeout, long startPositionMs, MediaMetadata metadata) {
        attachPlayerView();
        updateNavigationKey(key);
        if (result.needParse() || useParse) player().parse(key, result, useParse, metadata, startPositionMs);
        else player().start(PlaySpec.from(result, key, metadata), timeout, startPositionMs);
    }

    private void bindPlaybackService() {
        startService(new Intent(this, PlaybackService.class));
        bindService(new Intent(this, PlaybackService.class).setAction(PlaybackService.LOCAL_BIND_ACTION), this, BIND_AUTO_CREATE);
        buildControllerAsync();
        bound = true;
    }

    private void buildControllerAsync() {
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        mControllerFuture = new MediaController.Builder(this, token).setListener(this).buildAsync();
        mControllerFuture.addListener(this::onControllerConnected, ContextCompat.getMainExecutor(this));
    }

    private void onControllerConnected() {
        try {
            mController = mControllerFuture.get();
            getSeekView().setPlayer(mController);
            mController.addListener(this);
            updateKeyIncrement();
        } catch (Exception ignored) {
        }
    }

    private void addSeekListener() {
        getSeekView().getTimeBar().addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(@NonNull TimeBar timeBar, long position) {
                PlaybackActivity.this.setScrubbing(true);
            }

            @Override
            public void onScrubMove(@NonNull TimeBar timeBar, long position) {
                PlaybackActivity.this.setScrubbing(true);
            }

            @Override
            public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
                PlaybackActivity.this.onScrubStop(canceled);
            }
        });
    }

    protected boolean isScrubbing() {
        return scrubbing;
    }

    protected void onScrubStop(boolean canceled) {
        if (!canceled && mController != null && mController.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) mController.play();
        setScrubbing(false);
    }

    private void setScrubbing(boolean scrubbing) {
        if (this.scrubbing == scrubbing) return;
        this.scrubbing = scrubbing;
        onScrubbingChanged(scrubbing);
    }

    protected void onScrubbingChanged(boolean scrubbing) {
    }

    private void updateKeyIncrement() {
        long durationMs = mController == null ? C.TIME_UNSET : mController.getDuration();
        long incrementMs = getKeyTimeIncrementMs(durationMs);
        TimeBar timeBar = getSeekView().getTimeBar();
        timeBar.setKeyTimeIncrement(incrementMs);
    }

    private long getKeyTimeIncrementMs(long durationMs) {
        if (durationMs > TimeUnit.HOURS.toMillis(3)) {
            return TimeUnit.MINUTES.toMillis(5);
        } else if (durationMs > TimeUnit.MINUTES.toMillis(30)) {
            return TimeUnit.MINUTES.toMillis(1);
        } else if (durationMs > TimeUnit.MINUTES.toMillis(15)) {
            return TimeUnit.SECONDS.toMillis(30);
        } else if (durationMs > TimeUnit.MINUTES.toMillis(10)) {
            return TimeUnit.SECONDS.toMillis(15);
        } else {
            return TimeUnit.SECONDS.toMillis(10);
        }
    }

    private PendingIntent buildSessionIntent() {
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private boolean shouldReclaim() {
        return mService != null && !isOwner();
    }

    private void resumePlayback() {
        if (shouldReclaim()) reclaimPlayback();
        else attachPlayerView();
    }

    private void reclaimPlayback() {
        detachPlayerView();
        onReclaim();
    }

    private void claimBinding() {
        if (mService == null) return;
        mService.claimBinding(getNavigationCallback(), this::closePiP);
        mService.setSessionActivity(buildSessionIntent());
    }

    private boolean canActivate() {
        return mService != null && !isFinishing() && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private boolean canDispatch() {
        return !isFinishing() && isBindingOwner();
    }

    private void activateService() {
        if (!canActivate()) return;
        claimBinding();
        if (!isRedirect()) updateNavigationKey();
        dispatchPendingObservers();
        initService();
    }

    private void initService() {
        if (initialized) return;
        initialized = true;
        onServiceConnected();
    }

    private void closePiP() {
        if (!isInPictureInPictureMode()) return;
        detach();
        finish();
    }

    private void attachPlayerView() {
        if (mService != null) syncPlayerView(player().getPlayer());
    }

    private void detachPlayerView() {
        getPlayerView().setPlayer(null);
    }

    private void syncPlayerView(Player player) {
        player().bindPlayerView(getPlayerView());
        danmakuController.bind(getPlayerView());
        getPlayerView().setPlayer(player);
        syncDanmakuSource();
        restoreDebugView();
    }

    private void restoreDebugView() {
        if (PlayerSetting.isDebug() && !getPlayerView().isDebugViewVisible()) getPlayerView().toggleDebugView();
    }

    private void configurePlayerView() {
        PlayerView playerView = getPlayerView();
        playerView.setRender(PlayerSetting.getRender());
        danmakuController.setOkHttpClient(OkHttp.player());
        danmakuController.setEnabled(DanmakuSetting.isShow());
        danmakuController.setConfig(DanmakuSetting.getConfig());
        SubtitleSetting.applyStyle(playerView.getSubtitleView());
    }

    private void syncDanmakuSource() {
        if (mService == null || !isOwner()) return;
        danmakuController.setDataSource(player().getSelectedDanmakuUri());
    }

    private void releasePlaybackService() {
        if (mService != null) releaseService(isOwner());
        detach();
    }

    private void releaseService(boolean playbackOwner) {
        mService.removePlayerCallback(mPlayerCallback);
        if (!mService.releaseBinding(getNavigationCallback())) return;
        if (shouldKeepServiceAlive()) keepServiceAlive(playbackOwner);
        else mService.shutdown();
    }

    private boolean shouldKeepServiceAlive() {
        return mService.hasMediaClient() || mService.hasPlayerCallback();
    }

    private void keepServiceAlive(boolean playbackOwner) {
        if (playbackOwner) mService.suspend();
        mService.resetSessionActivity();
    }

    private void detach() {
        releaseController();
        releaseBinding();
    }

    private void pausePlayback() {
        if (mController != null) mController.pause();
        else if (mService != null) player().pause();
    }

    private void releaseController() {
        if (mControllerFuture != null) MediaController.releaseFuture(mControllerFuture);
        if (mController != null) mController.removeListener(this);
        if (mController != null) getSeekView().setPlayer(null);
        mControllerFuture = null;
        mController = null;
    }

    private void releaseBinding() {
        if (!bound) return;
        bound = false;
        if (mService != null) mService.removePlayerCallback(mPlayerCallback);
        unbindService(this);
        mService = null;
    }

    private void clearObservers() {
        foreverObserverRemovers.forEach(Runnable::run);
        foreverObserverRemovers.clear();
        serviceReadyObservers.clear();
    }

    private void dispatchPendingObservers() {
        if (!canDispatch()) return;
        serviceReadyObservers.forEach(ServiceReadyObserver::dispatch);
    }

    private final PlaybackService.PlayerCallback mPlayerCallback = new PlaybackService.PlayerCallback() {

        @Override
        public void onPrepare() {
            if (isOwner()) PlaybackActivity.this.onPrepare();
        }

        @Override
        public void onTracksChanged() {
            if (isOwner()) PlaybackActivity.this.onTracksChanged();
            restoreDebugView();
        }

        @Override
        public void onDecodeChanged() {
            if (isOwner()) PlaybackActivity.this.onDecodeChanged();
        }

        @Override
        public void onMediaOptionsChanged() {
            if (isOwner()) PlaybackActivity.this.onMediaOptionsChanged();
        }

        @Override
        public void onError(String msg) {
            if (isOwner()) PlaybackActivity.this.onError(msg);
        }

        @Override
        public void onPlayerRebuild(Player player) {
            if (isOwner()) syncPlayerView(player);
        }

        @Override
        public void onDanmakuSourceChanged(@Nullable Uri uri) {
            if (isOwner()) danmakuController.setDataSource(uri);
        }

        @Override
        public void onDanmakuConfigChanged(DanmakuConfig config) {
            if (isOwner()) danmakuController.setConfig(config);
        }

        @Override
        public void onDanmakuEnabledChanged(boolean enabled) {
            if (isOwner()) danmakuController.setEnabled(enabled);
        }

        @Override
        public void onDanmakuSent(String text) {
            if (isOwner()) danmakuController.sendNow(text);
        }
    };

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        configurePlayerView();
        bindPlaybackService();
        addSeekListener();
    }

    @Override
    public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) updateKeyIncrement();
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (!isOwner()) return;
        if (isPlaying) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else if (!isBuffering()) getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onPlayingChanged(isPlaying);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        if (isOwner()) onStateChanged(state);
    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize size) {
        if (isOwner()) onSizeChanged(size);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        mService = ((PlaybackService.LocalBinder) binder).getService();
        mService.addPlayerCallback(mPlayerCallback);
        activateService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        initialized = false;
        mService = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        activateService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        claimBinding();
        setRedirect(false);
        dispatchPendingObservers();
        resumePlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRedirect()) pausePlayback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isOwner() && (isFinishing() || PlayerSetting.isBackgroundOff())) pausePlayback();
        if (!isInPictureInPictureMode()) detachPlayerView();
    }

    @Override
    protected void onDestroy() {
        clearObservers();
        detachPlayerView();
        danmakuController.close();
        super.onDestroy();
        releasePlaybackService();
    }

    private final class ServiceReadyObserver<T> implements Observer<T> {

        private final Observer<T> observer;
        private T pendingValue;
        private boolean pending;

        private ServiceReadyObserver(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void onChanged(T value) {
            if (!canDispatch()) {
                pendingValue = value;
                pending = true;
            } else {
                deliver(value);
            }
        }

        private void deliver(T value) {
            pendingValue = null;
            pending = false;
            observer.onChanged(value);
        }

        private void dispatch() {
            if (pending) deliver(pendingValue);
        }
    }
}
