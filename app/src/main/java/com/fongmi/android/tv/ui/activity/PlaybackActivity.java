package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.iso.IsoNavigationSession;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerSeekView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.media3.ui.TimeBar;
import androidx.media3.ui.danmaku.Danmaku;
import androidx.media3.ui.danmaku.DanmakuConfig;
import androidx.media3.ui.danmaku.DanmakuPlayerViewController;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.PlaybackIntent;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.engine.DiscMenuController;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrowserWebView;
import com.fongmi.android.tv.ui.custom.DiscMenuOverlayView;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class PlaybackActivity extends BaseActivity implements MediaController.Listener, Player.Listener, ServiceConnection {

    private final DanmakuPlayerViewController danmakuController = new DanmakuPlayerViewController();
    private final List<ServiceReadyObserver<?>> serviceReadyObservers = new ArrayList<>();
    private final MutableLiveData<PlayerManager> playbackPlayerState = new MutableLiveData<>(null);
    private final List<Runnable> foreverObserverRemovers = new ArrayList<>();
    private final Runnable discMenuOverlayUpdate = this::updateDiscMenuOverlay;
    private final SparseBooleanArray discMenuHeldKeys = new SparseBooleanArray();
    private final int[] discMenuSurfaceLocation = new int[2];
    private ListenableFuture<MediaController> mControllerFuture;
    private MediaController mController;
    private PlaybackService mService;
    private PlaybackResume pendingResume;
    @Nullable private BrowserWebView webView;
    @Nullable private DiscMenuOverlayView discMenuOverlay;
    @Nullable private CloudflareDialog webChallengeDialog;
    @Nullable private ViewGroup webChallengeParent;
    @Nullable private ViewGroup.LayoutParams webChallengeParams;
    private int webChallengeIndex;
    private boolean resumeWebPlaybackOnResume;
    private int webPlaybackState;
    private boolean initialized;
    private boolean audioOnly;
    private boolean scrubbing;
    private boolean redirect;
    private boolean bound;
    private boolean stop;
    private boolean lock;
    private int discMenuOpenRequest;
    private boolean discMenuOpening;

    protected MediaController controller() {
        return mController;
    }

    protected PlaybackService service() {
        return mService;
    }

    protected PlayerManager player() {
        return mService.player();
    }

    @Nullable
    private DiscMenuController discMenu() {
        if (mService == null || !isOwner() || isWebPlaybackActive()) return null;
        return player().getDiscMenuController();
    }

    protected boolean hasDiscMenu() {
        DiscMenuController menu = discMenu();
        return menu != null && player().isDiscMenuAvailable() && menu.hasMenu();
    }

    protected boolean isIsoNavigationPlayback() {
        DiscMenuController menu = discMenu();
        return menu != null && menu.isNavigationPlayback();
    }

    protected boolean isDiscMenuActive() {
        DiscMenuController menu = discMenu();
        return menu != null && menu.isActive();
    }

    protected boolean isDiscMenuTransition() {
        return discMenuOpening || isDiscMenuActive();
    }

    protected boolean dispatchDiscMenuKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (!discMenuHeldKeys.get(keyCode)) return false;
            discMenuHeldKeys.delete(keyCode);
            return true;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (event.getRepeatCount() > 0) return discMenuHeldKeys.get(keyCode);
        discMenuHeldKeys.delete(keyCode);
        DiscMenuController menu = discMenu();
        if (isLock() || menu == null) return false;
        String action = switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> "up";
            case KeyEvent.KEYCODE_DPAD_DOWN -> "down";
            case KeyEvent.KEYCODE_DPAD_LEFT -> "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT -> "right";
            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> "select";
            case KeyEvent.KEYCODE_BACK -> "prev";
            case KeyEvent.KEYCODE_MEDIA_TOP_MENU -> "menu";
            case KeyEvent.KEYCODE_MENU -> "title-menu";
            case KeyEvent.KEYCODE_TV_CONTENTS_MENU -> "popup";
            default -> null;
        };
        if (action == null) return false;
        boolean opensMenu = "menu".equals(action) || "title-menu".equals(action) || "popup".equals(action);
        boolean canHandle = opensMenu ? hasDiscMenu() : menu.acceptsNavigationKeys();
        if (!canHandle) return false;
        boolean primaryRequest = !"popup".equals(action);
        if (opensMenu ? !requestDiscMenuOpen(menu, action, primaryRequest) : !menu.sendAction(action)) return false;
        discMenuHeldKeys.put(keyCode, true);
        return true;
    }

    protected void openDiscMenu() {
        openDiscMenu("menu", true);
    }

    protected boolean openDiscTitleMenu() {
        return openDiscMenu("title-menu", true);
    }

    protected boolean openDiscPopupMenu() {
        return openDiscMenu("popup", false);
    }

    private boolean openDiscMenu(String action, boolean allowPopupFallback) {
        DiscMenuController menu = discMenu();
        return menu != null && hasDiscMenu() && requestDiscMenuOpen(menu, action, allowPopupFallback);
    }

    private boolean requestDiscMenuOpen(DiscMenuController menu, String action, boolean allowPopupFallback) {
        if (menu.sendAction(action)) {
            startDiscMenuOverlay();
            monitorDiscMenuOpen(menu, action, allowPopupFallback);
            return true;
        }
        if (allowPopupFallback && menu.hasMenu() && menu.sendAction("popup")) {
            startDiscMenuOverlay();
            monitorDiscMenuOpen(menu, "popup", false);
            return true;
        }
        if (!menu.hasMenu()) onDiscMenuUnavailable();
        return false;
    }

    private void monitorDiscMenuOpen(DiscMenuController menu, String action, boolean allowPopupFallback) {
        discMenuOpening = true;
        onDiscMenuOpening();
        int request = ++discMenuOpenRequest;
        menu.observeOpen(action, result -> onDiscMenuOpenResult(menu, request, allowPopupFallback, result));
    }

    private void invalidateDiscMenuOpenRequest() {
        discMenuOpenRequest++;
        discMenuOpening = false;
    }

    private void onDiscMenuOpenResult(DiscMenuController menu, int request, boolean allowPopupFallback,
                                      DiscMenuController.OpenResult result) {
        if (request != discMenuOpenRequest || discMenu() != menu) return;
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
                || result == DiscMenuController.OpenResult.CANCELLED
                || result == DiscMenuController.OpenResult.OPENED) {
            discMenuOpening = false;
            return;
        }
        if (result == DiscMenuController.OpenResult.UNAVAILABLE
                && allowPopupFallback && menu.hasMenu() && menu.sendAction("popup")) {
            startDiscMenuOverlay();
            monitorDiscMenuOpen(menu, "popup", false);
            return;
        }
        discMenuOpening = false;
        getPlayerView().removeCallbacks(discMenuOverlayUpdate);
        int state = player().getPlayer().getPlaybackState();
        if (!menu.hasMenu()) onDiscMenuUnavailable();
        if (state == Player.STATE_BUFFERING) onStateChanged(state);
        Notify.show(R.string.play_disc_menu_unavailable);
    }

    protected boolean handleDiscMenuBack() {
        DiscMenuController menu = discMenu();
        return !isLock() && menu != null && menu.isActive() && menu.sendAction("prev");
    }

    protected boolean dispatchDiscMenuTouch(MotionEvent event) {
        DiscMenuController menu = discMenu();
        if (isLock() || menu == null || !menu.isActive() || !menu.supportsPointer()) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getPointerCount() != 1) return true;
        View surface = getPlayerView().getVideoSurfaceView();
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return true;
        surface.getLocationOnScreen(discMenuSurfaceLocation);
        float x = (event.getRawX() - discMenuSurfaceLocation[0]) / surface.getWidth();
        float y = (event.getRawY() - discMenuSurfaceLocation[1]) / surface.getHeight();
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                && event.getEventTime() - event.getDownTime() >= ViewConfiguration.getLongPressTimeout()) {
            onDiscMenuLongPress();
        } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_MOVE
                || event.getActionMasked() == MotionEvent.ACTION_UP) {
            menu.sendPointer(x, y, event.getActionMasked() == MotionEvent.ACTION_UP);
        }
        return true;
    }

    protected void onDiscMenuLongPress() {
    }

    private void startDiscMenuOverlay() {
        DiscMenuController menu = discMenu();
        if (menu == null || !menu.hasMenu()) return;
        PlayerView playerView = getPlayerView();
        if (menu.hasExternalGraphics()) {
            FrameLayout frame = playerView.getOverlayFrameLayout();
            if (frame != null && discMenuOverlay == null) {
                discMenuOverlay = new DiscMenuOverlayView(this, playerView);
                frame.addView(discMenuOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
        playerView.removeCallbacks(discMenuOverlayUpdate);
        playerView.post(discMenuOverlayUpdate);
    }

    private void updateDiscMenuOverlay() {
        DiscMenuOverlayView overlayView = discMenuOverlay;
        if (isWebPlaybackActive()) return;
        DiscMenuController menu = discMenu();
        updateKeepScreenOn(mController != null && mController.isPlaying());
        if (menu != null && menu.isActive()) {
            discMenuOpening = false;
        }
        if (menu == null || !menu.isNavigationPlayback()) {
            if (overlayView != null) {
                overlayView.clear();
                overlayView.setVisibility(View.GONE);
            }
            return;
        }
        if (overlayView != null) {
            if (menu.hasExternalGraphics()) {
                IsoNavigationSession.MenuOverlay overlay = menu.getHdmvOverlay(overlayView.getVersion());
                if (overlay != null) overlayView.setOverlay(overlay);
                overlayView.setHighlight(menu.getDvdHighlight());
                overlayView.setVisibility(View.VISIBLE);
            } else {
                overlayView.clear();
                overlayView.setVisibility(View.GONE);
            }
        }
        getPlayerView().postDelayed(discMenuOverlayUpdate, 50);
    }

    public List<Danmaku> getDanmakuItems() {
        return danmakuController.getItems();
    }

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

    protected void onBdjPreparing() {
    }

    protected void onTracksChanged() {
    }

    protected void onDecodeChanged() {
    }

    protected void onMediaOptionsChanged() {
    }

    protected void onError(String msg) {
    }

    protected boolean onRefresh() {
        return false;
    }

    protected void onPlayingChanged(boolean isPlaying) {
    }

    protected void onStateChanged(int state) {
    }

    protected void onDiscMenuOpening() {
    }

    protected void onDiscMenuUnavailable() {
    }

    protected void onWebPlaybackChanged(boolean active) {
    }

    protected void onWebFullscreenChanged(boolean fullscreen) {
    }

    protected boolean supportsWebSeek() {
        return false;
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
        invalidateDiscMenuOpenRequest();
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
        return null;
    }

    private void startPlayerInternal(String key, Result result, boolean useParse, long timeout, long startPositionMs, MediaMetadata metadata) {
        invalidateDiscMenuOpenRequest();
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
        TimeBar timeBar = getSeekView().getTimeBar();
        timeBar.addListener(new TimeBar.OnScrubListener() {
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
        PlaybackResume resume = pendingResume;
        pendingResume = null;
        if (shouldReclaim()) reclaimPlayback();
        else {
            attachPlayerView();
            if (resume == null || mService == null || isFinishing() || isInPictureInPictureMode() || !isOwner() || !isBindingOwner()) return;
            Player current = player().getPlayer();
            Player target = mController != null ? mController : current;
            if (!resume.matches(current) || target.getPlayWhenReady() || !canResume(current)) return;
            target.play();
        }
    }

    private void reclaimPlayback() {
        detachPlayerView();
        onReclaim();
    }

    private void claimBinding() {
        if (mService == null) return;
        mService.claimBinding(getNavigationCallback(), () -> {
            playbackPlayerState.setValue(null);
            pendingResume = null;
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
        onServiceConnected();
        initialized = true;
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
        getPlayerView().setPlayer(null);
    }

    private void hidePlayerViewForWebPlayback() {
        PlayerView playerView = getPlayerView();
        playerView.removeCallbacks(discMenuOverlayUpdate);
        if (discMenuOverlay != null) {
            discMenuOverlay.clear();
            discMenuOverlay.setVisibility(View.GONE);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View shutter = playerView.findViewById(androidx.media3.ui.R.id.exo_shutter);
        if (shutter != null) shutter.setVisibility(View.VISIBLE);
        playerView.setPlayer(null);
        playerView.setVisibility(View.INVISIBLE);
    }

    private void syncPlayerView(Player player) {
        player().bindPlayerView(getPlayerView());
        danmakuController.bind(getPlayerView());
        getPlayerView().setPlayer(player);
        startDiscMenuOverlay();
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
        if (mService == null || !isOwner() || !isBindingOwner() || (isInPictureInPictureMode() && !isFinishing())) {
            pendingResume = null;
            return;
        }
        Player current = player().getPlayer();
        Player target = mController != null ? mController : current;
        if (isFinishing() || (pendingResume != null && !pendingResume.matches(current))) pendingResume = null;
        if (!isFinishing() && current != null && target.getPlayWhenReady() && canResume(current) && current.getCurrentMediaItem() != null) {
            pendingResume = new PlaybackResume(current, current.getCurrentMediaItem());
        }
        if (mController != null) mController.pause();
        else if (mService != null) player().pause();
    }

    private static boolean canResume(Player player) {
        return player.getPlaybackState() == Player.STATE_READY || player.getPlaybackState() == Player.STATE_BUFFERING;
    }

    private record PlaybackResume(Player player, MediaItem item) {
        boolean matches(Player current) {
            if (player != current) return false;
            MediaItem mediaItem = current.getCurrentMediaItem();
            return mediaItem != null && item.mediaId.equals(mediaItem.mediaId) && Objects.equals(item.localConfiguration, mediaItem.localConfiguration);
        }
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
        public void onBdjPreparing() {
            if (isOwner()) PlaybackActivity.this.onBdjPreparing();
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
        public boolean onRefresh() {
            if (!isOwner()) return false;
            boolean handled = PlaybackActivity.this.onRefresh();
            updatePlaybackPlayerState();
            return handled;
        }

        @Override
        public void onPlayerRebuild(Player player) {
            if (!isOwner()) return;
            syncPlayerView(player);
            if (initialized && isBindingOwner() && playbackPlayerState.getValue() != null) playbackPlayerState.setValue(player());
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
        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            invalidateDiscMenuOpenRequest();
            if (pendingResume != null && (mService == null || !pendingResume.matches(player().getPlayer()))) pendingResume = null;
        }
        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) updateKeyIncrement();
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (!isOwner()) return;
        updateKeepScreenOn(isPlaying);
        onPlayingChanged(isPlaying);
    }

    private void updateKeepScreenOn(boolean isPlaying) {
        int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        boolean screenOn = (getWindow().getAttributes().flags & flag) != 0;
        boolean keepScreenOn = isPlaying || isDiscMenuActive();
        if (keepScreenOn && !screenOn) getWindow().addFlags(flag);
        else if (!keepScreenOn && !isBuffering() && screenOn) getWindow().clearFlags(flag);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        if (!isOwner()) return;
        onStateChanged(state);
        startDiscMenuOverlay();
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
        pendingResume = null;
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
        startDiscMenuOverlay();
        if (webView != null) webView.onResume();
        if (!isWebPlaybackActive()) resumePlayback();
        else {
            pendingResume = null;
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
        getPlayerView().removeCallbacks(discMenuOverlayUpdate);
        if ((isWebPlaybackActive() || isOwner()) && (isFinishing() || PlayerSetting.isBackgroundOff())) pausePlayback();
        if (!isInPictureInPictureMode()) detachPlayerView();
    }

    @Override
    protected void onDestroy() {
        getPlayerView().removeCallbacks(discMenuOverlayUpdate);
        if (discMenuOverlay != null) discMenuOverlay.clear();
        pendingResume = null;
        releaseWebView();
        clearObservers();
        detachPlayerView();
        danmakuController.close();
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
