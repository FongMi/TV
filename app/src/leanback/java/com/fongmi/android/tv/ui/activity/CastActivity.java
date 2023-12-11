package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.viewbinding.ViewBinding;

import com.android.cast.dlna.dmr.CastAction;
import com.android.cast.dlna.dmr.DLNARendererService;
import com.android.cast.dlna.dmr.RenderControl;
import com.android.cast.dlna.dmr.RenderState;
import com.android.cast.dlna.dmr.RendererServiceBinder;
import com.android.cast.dlna.dmr.service.RendererInterfaceKt;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.ActivityCastBinding;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownCast;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class CastActivity extends BaseActivity implements CustomKeyDownCast.Listener, TrackDialog.Listener, RenderControl, ServiceConnection, Clock.Callback, SubtitleCallback {

    private ActivityCastBinding mBinding;
    private DLNARendererService mService;
    private CustomKeyDownCast mKeyDown;
    private RenderState mState;
    private CastAction mAction;
    private DIDLParser mParser;
    private Players mPlayers;
    private Runnable mR1;
    private Runnable mR2;
    private Clock mClock;
    private long position;
    private long duration;
    private int scale;

    private PlayerView getExo() {
        return Setting.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCastBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().putExtras(intent);
        checkAction();
    }

    @Override
    protected void initView() {
        bindService(new Intent(this, DLNARendererService.class), this, Context.BIND_AUTO_CREATE);
        mClock = Clock.create(mBinding.widget.time);
        mKeyDown = CustomKeyDownCast.create(this);
        mPlayers = new Players().init(this);
        mParser = new DIDLParser();
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        setVideoView();
        checkAction();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.control.text.setAddListener(this::onTextAdd);
        mBinding.control.text.setSubListener(this::onTextSub);
        mBinding.control.speed.setAddListener(this::onSpeedAdd);
        mBinding.control.speed.setSubListener(this::onSpeedSub);
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.reset.setOnClickListener(view -> onReset());
        mBinding.control.player.setOnClickListener(view -> onPlayer());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
    }

    private String getName() {
        try {
            return mParser.parse(mAction.getCurrentURIMetaData()).getItems().get(0).getId();
        } catch (Exception e) {
            return mAction.getCurrentURI();
        }
    }

    private void checkAction() {
        mAction = getIntent().getParcelableExtra(RendererInterfaceKt.keyExtraCastAction);
        mBinding.widget.title.setText(getName());
        position = duration = 0;
        start();
    }

    private void start() {
        mPlayers.setMediaSource(mAction.getCurrentURI());
        showProgress();
        setMetadata();
        hideCenter();
    }

    private void setVideoView() {
        mPlayers.set(getExo(), getIjk());
        mPlayers.setPlayer(Setting.getPlayer());
        mBinding.control.speed.setText(mPlayers.getSpeedText());
        findViewById(R.id.timeBar).setNextFocusUpId(R.id.reset);
        getExo().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        getIjk().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[0]);
        setScale(scale = Setting.getScale());
        setSubtitle(Setting.getSubtitle());
        setPlayerView();
        setDecodeView();
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.player.setText(mPlayers.getPlayerText());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
    }

    private void setDecodeView() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void onScale() {
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        scale = scale == array.length - 1 ? 0 : ++scale;
        setScale(scale);
    }

    private void onSpeed() {
        mBinding.control.speed.setText(mPlayers.addSpeed());
    }

    private void onSpeedAdd() {
        mBinding.control.speed.setText(mPlayers.addSpeed(0.25f));
    }

    private void onSpeedSub() {
        mBinding.control.speed.setText(mPlayers.subSpeed(0.25f));
    }

    private boolean onSpeedLong() {
        mBinding.control.speed.setText(mPlayers.toggleSpeed());
        return true;
    }

    private void onReset() {
        start();
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
        setPlayerView();
        onReset();
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        mPlayers.set(getExo(), getIjk());
        setDecodeView();
        onReset();
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private void onTextAdd() {
        int size = Math.min(Setting.getSubtitle() + 1, 48);
        mBinding.control.text.setText(String.valueOf(size));
        Setting.putSubtitle(size);
        setSubtitle(size);
    }

    private void onTextSub() {
        int size = Math.max(Setting.getSubtitle() - 1, 14);
        mBinding.control.text.setText(String.valueOf(size));
        Setting.putSubtitle(size);
        setSubtitle(size);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR2, 0);
        hideError();
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR2);
        Traffic.reset();
    }

    private void showError(String text) {
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showInfo() {
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.info.setVisibility(View.VISIBLE);
    }

    private void hideInfo() {
        mBinding.widget.center.setVisibility(View.GONE);
        mBinding.widget.info.setVisibility(View.GONE);
    }

    private void showControl() {
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        mBinding.control.reset.requestFocus();
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.text.setText(R.string.play_track_text);
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (ActionEvent.PLAY.equals(event.getAction()) || ActionEvent.PAUSE.equals(event.getAction())) {
            onKeyCenter();
        } else if (ActionEvent.STOP.equals(event.getAction())) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.SUBTITLE) mPlayers.setSub(Sub.from(event.getPath()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                setTrackVisible(false);
                mClock.setCallback(this);
                setState(RenderState.PREPARING);
                break;
            case Player.STATE_IDLE:
                setState(RenderState.IDLE);
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                setState(RenderState.PREPARING);
                break;
            case Player.STATE_READY:
                setMetadata();
                hideProgress();
                mPlayers.reset();
                setTrackVisible(true);
                setState(RenderState.PLAYING);
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
            case Player.STATE_ENDED:
                showControl();
                setState(RenderState.STOPPED);
                break;
        }
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.text.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_TEXT) ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setMetadata() {
        String title = mBinding.widget.title.getText().toString();
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, getIjk().getDefaultArtwork());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mPlayers.getDuration());
        mPlayers.setMetadata(builder.build());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (mPlayers.addRetry() > event.getRetry()) onError(event);
        else onReset();
    }

    private void onError(ErrorEvent event) {
        showError(event.getMsg());
        onStopped();
    }

    private void onPaused(boolean visible) {
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(0));
        setState(RenderState.PAUSED);
        if (visible) showInfo();
        else hideInfo();
        mPlayers.pause();
    }

    private void onPlay() {
        setState(RenderState.PLAYING);
        mPlayers.play();
        hideCenter();
    }

    private void onStopped() {
        setState(RenderState.STOPPED);
        mPlayers.stop();
    }

    private void setState(RenderState state) {
        if (mService != null) mService.notifyAvTransportLastChange(this.mState = state);
    }

    @NonNull
    @Override
    public RenderState getState() {
        return mState;
    }

    @Override
    public void onTrackClick(Track item) {
    }

    @Override
    public void onTimeChanged() {
        position = mPlayers.getPosition();
        duration = mPlayers.getDuration();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        (mService = ((RendererServiceBinder) service).getService()).bindRealPlayer(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public long getCurrentPosition() {
        return position;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public void seek(long time) {
        App.post(() -> mPlayers.seekTo(time, true));
    }

    @Override
    public void pause() {
        App.post(() -> onPaused(true));
    }

    @Override
    public void play(@Nullable Double speed) {
        App.post(this::onPlay);
    }

    @Override
    public void stop() {
        App.post(this::finish);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) onToggle();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (isGone(mBinding.control.getRoot()) && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeeking(int time) {
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.center.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public void onSeekTo(int time) {
        mKeyDown.resetTime();
        mPlayers.seekTo(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isPlaying()) return;
        mBinding.control.speed.setText(mPlayers.setSpeed(mPlayers.getSpeed() < 3 ? 3 : 5));
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.widget.speed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSpeedEnd() {
        mBinding.control.speed.setText(mPlayers.setSpeed(1.0f));
        mBinding.widget.speed.setVisibility(View.GONE);
        mBinding.widget.speed.clearAnimation();
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
        if (mPlayers.isPlaying()) onPaused(true);
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
    public void setSubtitle(int size) {
        getExo().getSubtitleView().setFixedTextSize(Dimension.SP, size);
        getIjk().getSubtitleView().setFixedTextSize(Dimension.SP, size);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        onPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        onPaused(false);
        mClock.stop();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.center)) {
            hideCenter();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClock.release();
        mPlayers.release();
        Source.get().stop();
        unbindService(this);
        mService.bindRealPlayer(null);
        App.removeCallbacks(mR1, mR2);
    }
}
