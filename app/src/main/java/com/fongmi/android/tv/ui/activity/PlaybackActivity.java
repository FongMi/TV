package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
import androidx.media3.ui.SubtitleView;
import androidx.media3.ui.TimeBar;
import androidx.media3.ui.danmaku.DanmakuConfig;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.PlaybackIntent;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrowserWebView;
import com.fongmi.android.tv.ui.dialog.CloudflareDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class PlaybackActivity extends BaseActivity implements MediaController.Listener, Player.Listener, ServiceConnection {

    private final List<ServiceReadyObserver<?>> serviceReadyObservers = new ArrayList<>();
    private final List<Runnable> foreverObserverRemovers = new ArrayList<>();
    private final MutableLiveData<PlayerManager> playbackPlayerState = new MutableLiveData<>(null);
    @Nullable private BrowserWebView webView;
    @Nullable private CloudflareDialog webChallengeDialog;
    @Nullable private ViewGroup webChallengeParent;
    @Nullable private ViewGroup.LayoutParams webChallengeParams;
    private int webChallengeIndex;
    private int webPlaybackState;
    private boolean resumeWebPlaybackOnResume;
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

    protected boolean hasDiscMenu() {
        return hasPlaybackSource() && player().hasDiscMenu();
    }

    protected boolean isIsoNavigationPlayback() {
        return isDiscMenuActive();
    }

    protected boolean isDiscMenuActive() {
        return hasPlaybackSource() && player().isDiscMenuActive();
    }

    protected boolean isDiscMenuTransition() {
        return isDiscMenuActive();
    }

    protected boolean dispatchDiscMenuKey(KeyEvent event) {
        if (isLock() || !hasDiscMenu()) return false;
        String action = switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP -> "up";
            case KeyEvent.KEYCODE_DPAD_DOWN -> "down";
            case KeyEvent.KEYCODE_DPAD_LEFT -> "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT -> "right";
            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> "select";
            case KeyEvent.KEYCODE_MEDIA_TOP_MENU -> "menu";
            case KeyEvent.KEYCODE_TV_CONTENTS_MENU -> "popup";
            default -> null;
        };
        if (action == null) return false;
        if (!isDiscMenuActive() && !"menu".equals(action) && !"popup".equals(action)) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) return true;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        return player().sendDiscMenuAction(action);
    }

    protected void openDiscMenu() {
        openDiscMenuAction("menu");
    }

    protected boolean openDiscTitleMenu() {
        return openDiscMenuAction("title-menu");
    }

    protected boolean openDiscPopupMenu() {
        return openDiscMenuAction("popup");
    }

    private boolean openDiscMenuAction(String action) {
        if (!hasDiscMenu() || !player().sendDiscMenuAction(action)) {
            onDiscMenuUnavailable();
            Notify.show(R.string.play_disc_menu_unavailable);
            return false;
        }
        onDiscMenuOpening();
        return true;
    }

    protected boolean handleDiscMenuBack() {
        return isDiscMenuActive() && player().sendDiscMenuAction("prev");
    }

    protected boolean dispatchDiscMenuTouch(MotionEvent event) {
        if (isLock() || !isDiscMenuActive() || event.getPointerCount() != 1) return false;
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) return false;
        View video = getPlayerView().getVideoSurfaceView();
        if (video == null || video.getWidth() <= 0 || video.getHeight() <= 0) return false;
        int[] origin = new int[2];
        video.getLocationOnScreen(origin);
        float x = (event.getRawX() - origin[0]) / video.getWidth();
        float y = (event.getRawY() - origin[1]) / video.getHeight();
        return player().sendDiscMenuPointer(x, y, action == MotionEvent.ACTION_UP);
    }
    protected void onDiscMenuLongPress() { }

    public List<Danmaku> getDanmakuItems() {
        return mService == null ? List.of() : player().getDanmakus();
    }

    @Nullable
    public PlayerManager getPlaybackPlayer() {
        return mService == null ? null : player();
    }

    public LiveData<PlayerManager> getPlaybackPlayerState() {
        return playbackPlayerState;
    }

    public SubtitleView getPlaybackSubtitleView() {
        return getPlayerView().getSubtitleView();
    }

    private void updatePlaybackPlayerState() {
        PlayerManager current = initialized && isBindingOwner() && isOwner() && !player().isReleased() ? player() : null;
        if (playbackPlayerState.getValue() != current) playbackPlayerState.setValue(current);
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

    protected void onBdjPreparing() { }
    protected boolean onRefresh() { return false; }
    protected void onDiscMenuOpening() { }
    protected void onDiscMenuUnavailable() { }
    protected void onDiscMenuAvailabilityChanged() { }
    protected void onWebPlaybackChanged(boolean active) { }
    protected void onWebFullscreenChanged(boolean fullscreen) { }
    protected boolean supportsWebSeek() { return false; }

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
        if (isWebPlaybackActive()) {
            long positionMs = getActivePlaybackPosition();
            long durationMs = getActivePlaybackDuration();
            if (positionMs < 0 || durationMs <= 0) return false;
            long targetMs = Math.max(0, positionMs + deltaMs);
            boolean seekToEnd = targetMs >= durationMs;
            webView.seekVideo(seekToEnd ? durationMs : targetMs);
            if (!seekToEnd) webView.resumeVideo();
            return seekToEnd;
        }
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

    protected boolean startWebPlayback(ViewGroup parent, Result result) {
        return startWebPlayback(parent, result, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    protected boolean startWebPlayback(ViewGroup parent, Result result, @Nullable Consumer<MotionEvent> touchObserver) {
        String value = result.getRealUrl();
        if (!UrlUtil.isWebView(value) || result.hasMsg()) {
            releaseWebView();
            return false;
        }
        String url = UrlUtil.unwrapWebView(value);
        String scheme = UrlUtil.scheme(url);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            releaseWebView();
            onError(ResUtil.getString(R.string.error_play_url));
            return true;
        }
        PlayerManager player = player();
        player.reset();
        player.stop();
        player.clear();
        hidePlayerViewForWebPlayback();
        if (webView == null) {
            webView = BrowserWebView.createPlayback(this, webViewListener);
            addWebView(parent);
        } else if (webView.getParent() != parent) {
            if (webView.getParent() instanceof ViewGroup oldParent) oldParent.removeView(webView);
            addWebView(parent);
        }
        webView.setOnTouchListener(touchObserver == null ? null : (view, event) -> {
            touchObserver.accept(event);
            return false;
        });
        resumeWebPlaybackOnResume = false;
        webPlaybackState = Player.STATE_BUFFERING;
        getSeekView().setPlayer(null);
        getSeekView().setVisibility(View.GONE);
        webView.loadPlayback(url, result.getHeader(), result.getClick());
        onWebPlaybackChanged(true);
        onStateChanged(Player.STATE_BUFFERING);
        webView.requestFocus();
        return true;
    }

    private void addWebView(ViewGroup parent) {
        int playerIndex = parent.indexOfChild(getPlayerView());
        parent.addView(webView, playerIndex < 0 ? 0 : playerIndex + 1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    protected boolean isWebPlayback(Result result) {
        return UrlUtil.isWebView(result.getRealUrl());
    }

    protected boolean isWebPlaybackActive() {
        return webView != null && webView.isPlaybackActive();
    }

    protected boolean isWebPlaybackPlaying() {
        return webView != null && webView.isVideoPlaying();
    }

    protected boolean hasActivePlaybackSession() {
        return isWebPlaybackActive() || (mService != null && isOwner() && player().hasPlaySpec());
    }

    protected long getActivePlaybackPosition() {
        if (!isWebPlaybackActive()) return mService != null && isOwner() ? player().getPosition() : C.TIME_UNSET;
        long position = webView.getVideoPosition();
        return position < 0 ? C.TIME_UNSET : position;
    }

    protected long getActivePlaybackDuration() {
        if (!isWebPlaybackActive()) return mService != null && isOwner() ? player().getDuration() : C.TIME_UNSET;
        long duration = webView.getVideoDuration();
        return duration <= 0 ? C.TIME_UNSET : duration;
    }

    protected boolean canTrackActivePlaybackProgress() {
        return getActivePlaybackPosition() >= 0 && getActivePlaybackDuration() > 0;
    }

    protected boolean replayWebPlayback(long position) {
        if (!isWebPlaybackActive()) return false;
        webView.seekVideo(Math.max(0, position));
        webView.resumeVideo();
        return true;
    }

    protected boolean pauseWebPlayback() {
        if (!isWebPlaybackActive()) return false;
        resumeWebPlaybackOnResume = false;
        webView.pauseVideo();
        return true;
    }

    protected boolean stopWebPlayback() {
        if (!isWebPlaybackActive()) return false;
        webView.stopPlayback();
        resumeWebPlaybackOnResume = false;
        webPlaybackState = Player.STATE_IDLE;
        syncSeekPlayer();
        getSeekView().setVisibility(View.VISIBLE);
        onWebPlaybackChanged(false);
        return true;
    }

    protected boolean resumeWebPlayback() {
        if (!isWebPlaybackActive()) return false;
        webView.resumeVideo();
        return true;
    }

    protected int getWebVideoWidth() {
        return webView == null ? 0 : webView.getVideoWidth();
    }

    protected int getWebVideoHeight() {
        return webView == null ? 0 : webView.getVideoHeight();
    }

    protected boolean hasWebMediaTarget() {
        return webView != null && webView.hasMediaTarget();
    }

    protected boolean handleWebViewNavigation() {
        return webView != null && webView.handleBackNavigation();
    }

    @Nullable
    private String getPlaybackError(Result result) {
        if (result.hasMsg()) return result.getMsg();
        if (result.getRealUrl().isEmpty()) return ResUtil.getString(R.string.error_play_url);
        if (result.getDrm() != null && !FrameworkMediaDrm.isCryptoSchemeSupported(result.getDrm().getUUID())) return ResUtil.getString(R.string.error_play_drm);
        return null;
    }

    private void startPlayerInternal(String key, Result result, boolean useParse, long timeout, long startPositionMs, MediaMetadata metadata) {
        releaseWebView();
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
            syncSeekPlayer();
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
                PlaybackActivity.this.onScrubStop(position, canceled);
            }
        });
    }

    protected boolean isScrubbing() {
        return scrubbing;
    }

    protected void onScrubStop(long position, boolean canceled) {
        if (!canceled) {
            if (isWebPlaybackActive()) webView.seekVideo(position);
            else if (mController != null && mController.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) mController.setPlayWhenReady(true);
        }
        setScrubbing(false);
    }

    private void syncSeekPlayer() {
        getSeekView().setPlayer(isWebPlaybackActive() ? null : mController);
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
        if (Util.isLeanback()) return Constant.INTERVAL_SEEK;
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
        mService.claimBinding(getNavigationCallback(), () -> {
            playbackPlayerState.setValue(null);
            closePiP();
        });
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
        updatePlaybackPlayerState();
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
        getPlayerView().setVisibility(View.VISIBLE);
        if (mService != null) syncPlayerView(player().getPlayer());
    }

    private void detachPlayerView() {
        if (mService != null) player().bindPlayerView(null);
        getPlayerView().setPlayer(null);
    }

    private void hidePlayerViewForWebPlayback() {
        PlayerView playerView = getPlayerView();
        if (mService != null) player().bindPlayerView(null);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View shutter = playerView.findViewById(androidx.media3.ui.R.id.exo_shutter);
        if (shutter != null) shutter.setVisibility(View.VISIBLE);
        playerView.setPlayer(null);
        playerView.setVisibility(View.INVISIBLE);
    }

    private void syncPlayerView(Player player) {
        getPlayerView().setPlayer(player);
        player().bindPlayerView(getPlayerView());
        syncDanmakuSource();
        restoreDebugView();
    }

    private void restoreDebugView() {
        if (PlayerSetting.isDebug() && !getPlayerView().isDebugViewVisible()) getPlayerView().toggleDebugView();
    }

    private void configurePlayerView() {
        PlayerView playerView = getPlayerView();
        playerView.setRender(PlayerSetting.getRender());
        playerView.setDanmakuOkHttpClient(OkHttp.player());
        playerView.setDanmakuEnabled(DanmakuSetting.isShow());
        playerView.setDanmakuConfig(DanmakuSetting.getConfig());
        SubtitleSetting.applyStyle(this, playerView.getSubtitleView());
    }

    private void syncDanmakuSource() {
        if (mService == null || !isOwner()) return;
        getPlayerView().setDanmakuSource(player().getSelectedDanmakuUri());
    }

    private void releasePlaybackService() {
        playbackPlayerState.setValue(null);
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
        if (isWebPlaybackActive()) {
            resumeWebPlaybackOnResume = !isFinishing() && (resumeWebPlaybackOnResume || !webView.isPlaybackPaused());
            webView.pauseVideo();
            return;
        }
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
        playbackPlayerState.setValue(null);
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
            updatePlaybackPlayerState();
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
            if (!isOwner()) return;
            syncPlayerView(player);
            updatePlaybackPlayerState();
        }

        @Override
        public void onDanmakuSourceChanged(Uri uri) {
            if (isOwner()) getPlayerView().setDanmakuSource(uri);
        }

        @Override
        public void onDanmakuConfigChanged(DanmakuConfig config) {
            if (isOwner()) getPlayerView().setDanmakuConfig(config);
        }

        @Override
        public void onDanmakuEnabledChanged(boolean enabled) {
            if (isOwner()) getPlayerView().setDanmakuEnabled(enabled);
        }

        @Override
        public void onDanmakuSent(String text) {
            if (isOwner()) getPlayerView().sendDanmaku(text);
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
        if (isOwner()) {
            onStateChanged(state);
            onDiscMenuAvailabilityChanged();
        }
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
        playbackPlayerState.setValue(null);
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
        updatePlaybackPlayerState();
        if (webView != null) webView.onResume();
        if (!isWebPlaybackActive()) resumePlayback();
        else {
            if (resumeWebPlaybackOnResume) webView.resumeVideo();
            resumeWebPlaybackOnResume = false;
        }
    }

    @Override
    protected void onPause() {
        if (webView != null && !isInPictureInPictureMode()) webView.onPause();
        super.onPause();
        if (isRedirect()) pausePlayback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ((isWebPlaybackActive() || isOwner()) && (isFinishing() || PlayerSetting.isBackgroundOff())) pausePlayback();
        if (!isInPictureInPictureMode()) detachPlayerView();
    }

    @Override
    protected void onDestroy() {
        releaseWebView();
        clearObservers();
        detachPlayerView();
        super.onDestroy();
        releasePlaybackService();
    }

    private void releaseWebView() {
        releaseWebView(true);
    }

    private void releaseWebView(boolean destroy) {
        if (webView == null) return;
        BrowserWebView current = webView;
        current.pauseVideo();
        current.hideCustomView();
        hideWebChallenge(current);
        webView = null;
        resumeWebPlaybackOnResume = false;
        webPlaybackState = Player.STATE_IDLE;
        if (current.getParent() instanceof ViewGroup parent) parent.removeView(current);
        if (destroy) current.release();
        syncSeekPlayer();
        getSeekView().setVisibility(View.VISIBLE);
        onWebPlaybackChanged(false);
    }

    private final BrowserWebView.PlaybackListener webViewListener = new BrowserWebView.PlaybackListener() {
        @Override
        public void onPlayingChanged(@NonNull BrowserWebView view, boolean isPlaying) {
            if (webView != view) return;
            if (isPlaying) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            PlaybackActivity.this.onPlayingChanged(isPlaying);
        }

        @Override
        public void onStateChanged(@NonNull BrowserWebView view, @NonNull BrowserWebView.PlaybackState state) {
            if (webView != view) return;
            updateWebSeek(state);
            int playerState = state.ended() ? Player.STATE_ENDED : state.buffering() ? Player.STATE_BUFFERING : Player.STATE_READY;
            if (webPlaybackState == playerState) return;
            webPlaybackState = playerState;
            PlaybackActivity.this.onStateChanged(playerState);
        }

        @Override
        public void onFailure(@NonNull BrowserWebView view, boolean rendererGone) {
            if (webView != view) return;
            releaseWebView(!rendererGone);
            onError(ResUtil.getString(R.string.error_play_url));
        }

        @Override
        public void onFullscreenChanged(@NonNull BrowserWebView view, boolean fullscreen) {
            if (webView == view) onWebFullscreenChanged(fullscreen);
        }

        @Override
        public void onChallengeChanged(@NonNull BrowserWebView view, boolean visible) {
            if (webView != view) return;
            if (visible) showWebChallenge(view);
            else hideWebChallenge(view);
        }
    };

    private void showWebChallenge(@NonNull BrowserWebView view) {
        if (webChallengeDialog != null) return;
        view.hideCustomView();
        if (!(view.getParent() instanceof ViewGroup parent)) return;
        webChallengeParent = parent;
        webChallengeIndex = parent.indexOfChild(view);
        webChallengeParams = view.getLayoutParams();
        parent.removeView(view);
        webChallengeDialog = CloudflareDialog.create(this, view, ignored -> onWebChallengeDismissed(view)).show();
    }

    private void hideWebChallenge(@NonNull BrowserWebView view) {
        CloudflareDialog dialog = webChallengeDialog;
        webChallengeDialog = null;
        if (dialog != null) dialog.dismiss();
        restoreWebChallenge(view);
    }

    private void onWebChallengeDismissed(@NonNull BrowserWebView view) {
        webChallengeDialog = null;
        restoreWebChallenge(view);
    }

    private void restoreWebChallenge(@NonNull BrowserWebView view) {
        ViewGroup parent = webChallengeParent;
        ViewGroup.LayoutParams params = webChallengeParams;
        webChallengeParent = null;
        webChallengeParams = null;
        if (webView != view || parent == null || view.getParent() != null) return;
        if (params == null) params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parent.addView(view, Math.min(Math.max(0, webChallengeIndex), parent.getChildCount()), params);
        view.requestFocus();
    }

    private void updateWebSeek(@NonNull BrowserWebView.PlaybackState state) {
        if (!supportsWebSeek()) return;
        PlayerSeekView seekView = getSeekView();
        seekView.setVisibility(state.isSeekable() ? View.VISIBLE : View.GONE);
        if (!state.isSeekable() || scrubbing) return;
        TimeBar timeBar = seekView.getTimeBar();
        timeBar.setKeyTimeIncrement(getKeyTimeIncrementMs(state.durationMs()));
        timeBar.setDuration(state.durationMs());
        timeBar.setPosition(state.positionMs());
        timeBar.setBufferedPosition(state.positionMs());
        TextView position = seekView.findViewById(androidx.media3.ui.R.id.exo_position);
        TextView duration = seekView.findViewById(androidx.media3.ui.R.id.exo_duration);
        if (position != null) position.setText(Util.timeMs(state.positionMs()));
        if (duration != null) duration.setText(Util.timeMs(state.durationMs()));
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
