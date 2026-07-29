package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;

import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.ui.PlayerSeekView;
import androidx.media3.ui.PlayerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.databinding.ActivityCastBinding;
import com.fongmi.android.tv.dlna.CastAction;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.playback.PlaybackAction;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.dialog.PlayerEngineDialog;
import com.fongmi.android.tv.ui.dialog.SpeedSettingDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jupnp.support.contentdirectory.DIDLParser;

public class CastActivity extends PlaybackActivity implements CustomKeyDownVod.Listener {

    private final Object mDlnaOwner = new Object();
    private ActivityCastBinding mBinding;
    private DLNARendererService mRenderer;
    private CustomKeyDownVod mKeyDown;
    private String mPlaybackKey;
    private CastAction mAction;
    private Runnable mR1;
    private Runnable mR2;
    private Clock mClock;
    private boolean bound;
    private long position;
    private int scale;

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCastBinding.inflate(getLayoutInflater());
    }

    @Override
    protected PlaybackService.NavigationCallback getNavigationCallback() {
        return mNavigationCallback;
    }

    @Override
    protected String getPlaybackKey() {
        return mPlaybackKey;
    }

    @Override
    protected PlayerView getPlayerView() {
        return mBinding.player;
    }

    @Override
    protected PlayerSeekView getSeekView() {
        return mBinding.control.seek;
    }

    @Override
    protected void onServiceConnected() {
        mBinding.control.action.decode.setText(player().getDecodeText());
        setAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!intent.hasExtra(CastAction.KEY_EXTRA)) return;
        setIntent(intent);
        if (mRenderer != null) mRenderer.activateDlna(mDlnaOwner);
        setAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        bound = bindService(new Intent(this, DLNARendererService.class), mRendererConnection, Context.BIND_AUTO_CREATE);
        mClock = Clock.create(mBinding.widget.clock);
        mKeyDown = CustomKeyDownVod.create(this);
        mKeyDown.setFull(true);
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        setVideoView();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.action.reset.setOnClickListener(view -> onReset());
        mBinding.control.action.player.setOnClickListener(view -> onPlayer());
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
    }

    private void setVideoView() {
        setSeekNextFocusDown(R.id.reset);
        setScale(scale = PlayerSetting.getScale());
        setActionFocusBoundary(mBinding.control.action.getRoot());
        PlayerEngineDialog.setText(mBinding.control.action.player);
    }

    private String getName() {
        try {
            return new DIDLParser().parse(mAction.getCurrentURIMetaData()).getItems().get(0).getTitle();
        } catch (Exception e) {
            return mAction.getCurrentURI();
        }
    }

    private void setAction(Intent intent) {
        mAction = intent.getParcelableExtra(CastAction.KEY_EXTRA);
        if (mAction == null) return;
        mBinding.widget.title.setText(getName());
        mBinding.widget.title.setSelected(true);
        resetMedia();
        start();
    }

    private void resetMedia() {
        hideError();
        hideControl();
        player().setSpeed(1.0f);
        player().setRepeatOne(false);
    }

    private void start() {
        mPlaybackKey = mAction.getCurrentURI();
        startPlayer(mPlaybackKey, mAction.result(), false, Constant.TIMEOUT_PLAY, buildMetadata());
    }

    private void setPlaybackMode() {
        PlayerEngineDialog.setText(mBinding.control.action.player, player());
        mBinding.control.action.decode.setText(player().getDecodeText());
    }

    private void setScale(int scale) {
        mBinding.player.setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void onScale() {
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        scale = scale == array.length - 1 ? 0 : ++scale;
        setScale(scale);
    }

    private void onSpeed() {
        SpeedSettingDialog.create().player(player()).show(this);
        hideControl();
    }

    private boolean onSpeedLong() {
        PlaybackAction.toggleSpeed(player(), mBinding.widget.message);
        return true;
    }

    private void onReset() {
        if (player().isEmpty()) return;
        position = player().getPosition();
        start();
    }

    private void onPlayer() {
        PlayerEngineDialog.show(this, mBinding.control.action.player, player());
        hideControl();
    }

    private void onDecode() {
        if (player().isEmpty()) return;
        position = player().getPosition();
        player().toggleDecode();
    }

    private void onTrack(View view) {
        TrackDialog.create().type(Integer.parseInt(view.getTag().toString())).player(player()).view(mBinding.player.getSubtitleView()).show(this);
        hideControl();
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        App.post(mR2, 0);
        hideCenter();
        hideError();
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR2);
        Traffic.reset();
    }

    private void showError(String text) {
        PlaybackAction.hideSpeedHint(mBinding.widget.message);
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showInfo() {
        mBinding.widget.top.setVisibility(View.VISIBLE);
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.duration.setText(player().getDurationTime());
        mBinding.widget.position.setText(player().getPositionTime(0));
    }

    private void hideInfo() {
        mBinding.widget.top.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void showControl() {
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        mBinding.control.action.reset.requestFocus();
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.progress.traffic);
        App.post(mR2, 1000);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.PLAYER) onReset();
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) player().setSub(Sub.from(event.getPath()));
    }

    private void setTrackVisible() {
        mBinding.control.action.text.setVisibility(player().haveTrack(C.TRACK_TYPE_TEXT) || player().isVod() ? View.VISIBLE : View.GONE);
        mBinding.control.action.audio.setVisibility(player().haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.action.video.setVisibility(player().haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private MediaMetadata buildMetadata() {
        String title = mBinding.widget.title.getText().toString();
        return MediaItemFactory.buildMetadata(title, "", "", "");
    }

    private void onPaused() {
        controller().pause();
    }

    private void onPlay() {
        if (isEnded()) controller().seekTo(0);
        if (!player().isEmpty() && isIdle()) controller().prepare();
        controller().play();
    }

    private void consumePendingSeek() {
        if (service() == null || player().isEmpty() || mRenderer == null) return;
        long seekMs = mRenderer.consumePendingSeekMs();
        if (seekMs >= 0) player().seekTo(seekMs);
    }

    private final ServiceConnection mRendererConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!bound) return;
            mRenderer = ((DLNARendererService.LocalBinder) binder).getService();
            mRenderer.activateDlna(mDlnaOwner);
            consumePendingSeek();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRenderer = null;
        }
    };

    private final PlaybackService.NavigationCallback mNavigationCallback = new PlaybackService.NavigationCallback() {
        @Override
        public void onStop() {
            finish();
        }
    };

    @Override
    protected void onPrepare() {
        setPosition();
        setPlaybackMode();
        consumePendingSeek();
    }

    @Override
    protected void onDecodeChanged() {
        setPlaybackMode();
    }

    @Override
    protected void onTracksChanged() {
        setTrackVisible();
    }

    @Override
    protected void onError(String msg) {
        if (mRenderer != null) mRenderer.notifyError();
        player().resetTrack();
        player().reset();
        player().stop();
        showError(msg);
    }

    @Override
    protected void onStateChanged(int state) {
        switch (state) {
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                hideProgress();
                player().reset();
                break;
            case Player.STATE_ENDED:
                checkEnded();
                break;
        }
    }

    private void setPosition() {
        if (position <= 0) return;
        player().seekTo(position);
        position = 0;
    }

    private void checkEnded() {
        CastAction next = mRenderer != null ? mRenderer.consumeNext() : null;
        if (next == null) return;
        mAction = next;
        mBinding.widget.title.setText(getName());
        mBinding.widget.title.setSelected(true);
        resetMedia();
        start();
    }

    @Override
    protected void onSizeChanged(VideoSize size) {
        mBinding.widget.size.setText(player().getSizeText());
    }

    @Override
    protected void onPlayingChanged(boolean isPlaying) {
        if (isPlaying) {
            hideCenter();
        } else if (isPaused()) {
            showInfo();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) onToggle();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (isGone(mBinding.control.getRoot()) && mKeyDown.hasEvent(event) && service() != null) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeeking(long time) {
        if (player().isEmpty()) return;
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.duration.setText(player().getDurationTime());
        mBinding.widget.position.setText(player().getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        hideProgress();
    }

    @Override
    public void onSeekEnd(long time) {
        if (player().isEmpty()) return;
        mKeyDown.reset();
        seekTo(time);
    }

    @Override
    public void onSpeedUp() {
        if (!player().isPlaying()) return;
        PlaybackAction.startSpeedPress(player(), mBinding.widget.message);
    }

    @Override
    public void onSpeedEnd() {
        PlaybackAction.hideSpeedHint(mBinding.widget.message);
        player().setSpeed(1.0f);
    }

    @Override
    public void onKeyUp() {
        showControl();
    }

    @Override
    public void onKeyDown() {
        showControl();
    }

    @Override
    public void onKeyCenter() {
        if (player().isPlaying()) onPaused();
        else onPlay();
        hideControl();
    }

    @Override
    public void onSingleTap() {
        onToggle();
    }

    @Override
    public void onDoubleTap() {
        onKeyCenter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClock.stop().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (PlayerSetting.isBackgroundOff()) mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.center)) {
            hideCenter();
        } else {
            super.onBackInvoked();
        }
    }

    private void releaseRenderer() {
        if (mRenderer != null) mRenderer.deactivateDlna(mDlnaOwner);
        if (bound) unbindService(mRendererConnection);
        mRenderer = null;
        bound = false;
    }

    @Override
    protected void onDestroy() {
        mClock.release();
        releaseRenderer();
        App.removeCallbacks(mR1, mR2);
        super.onDestroy();
    }
}
