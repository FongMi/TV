package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
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
import com.fongmi.android.tv.databinding.ActivityCastBinding;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.dialog.SubtitleDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

public class CastActivity extends BaseActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, RenderControl, ServiceConnection, Clock.Callback {

    private ActivityCastBinding mBinding;
    private DLNARendererService mService;
    private CustomKeyDownVod mKeyDown;
    private RenderState mState;
    private CastAction mAction;
    private Players mPlayers;
    private Runnable mR1;
    private Runnable mR2;
    private Clock mClock;
    private boolean redirect;
    private long position;
    private long duration;
    private String tag;
    private int scale;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, CastActivity.class));
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCastBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(RendererInterfaceKt.keyExtraCastAction)) setAction(intent);
        else finish();
    }

    @Override
    protected void initView() {
        bindService(new Intent(this, DLNARendererService.class), this, Context.BIND_AUTO_CREATE);
        mClock = Clock.create(mBinding.widget.clock);
        mKeyDown = CustomKeyDownVod.create(this);
        mPlayers = Players.create(this);
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mKeyDown.setFull(true);
        setVideoView();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.control.speed.setUpListener(this::onSpeedAdd);
        mBinding.control.speed.setDownListener(this::onSpeedSub);
        mBinding.control.text.setUpListener(this::onSubtitleClick);
        mBinding.control.text.setDownListener(this::onSubtitleClick);
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.reset.setOnClickListener(view -> onReset());
        mBinding.control.player.setOnClickListener(view -> onChoose());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
    }

    private String getName() {
        try {
            return new DIDLParser().parse(mAction.getCurrentURIMetaData()).getItems().get(0).getId();
        } catch (Exception e) {
            return mAction.getCurrentURI();
        }
    }

    private void setAction(Intent intent) {
        mAction = intent.getParcelableExtra(RendererInterfaceKt.keyExtraCastAction);
        mBinding.widget.waiting.setVisibility(View.GONE);
        mBinding.widget.title.setText(getName());
        mBinding.widget.title.setSelected(true);
        position = duration = C.TIME_UNSET;
        mService.bindRealPlayer(this);
        start();
    }

    private void start() {
        mPlayers.setMediaItem(mAction.getCurrentURI());
        showProgress();
        setMetadata();
        hideCenter();
    }

    private void setVideoView() {
        mPlayers.init(mBinding.exo);
        PlaybackService.start(mPlayers);
        setScale(scale = Setting.getScale());
        ExoUtil.setSubtitleView(mBinding.exo);
        mPlayers.setTag(tag = UUID.randomUUID().toString());
        findViewById(R.id.timeBar).setNextFocusUpId(R.id.reset);
        mBinding.control.speed.setText(mPlayers.getSpeedText());
        mBinding.control.decode.setText(mPlayers.getDecodeText());
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[0]);
    }

    private void setDecode() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setScale(int scale) {
        mBinding.exo.setResizeMode(scale);
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
        position = duration = C.TIME_UNSET;
        if (mPlayers.isEmpty()) return;
        start();
    }

    private void onChoose() {
        mPlayers.choose(this, mBinding.widget.title.getText());
        setRedirect(true);
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        setDecode();
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
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
        mBinding.widget.top.setVisibility(View.VISIBLE);
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.duration.setText(mPlayers.getDurationTime());
        mBinding.widget.position.setText(mPlayers.getPositionTime(0));
    }

    private void hideInfo() {
        mBinding.widget.top.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
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

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
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
        if (event.getType() == RefreshEvent.Type.PLAYER) onReset();
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) mPlayers.setSub(Sub.from(event.getPath()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        if (!event.getTag().equals(tag)) return;
        switch (event.getState()) {
            case PlayerEvent.PREPARE:
                setDecode();
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
                hideProgress();
                mPlayers.reset();
                setState(RenderState.PLAYING);
                break;
            case Player.STATE_ENDED:
                showControl();
                setState(RenderState.STOPPED);
                break;
            case PlayerEvent.TRACK:
                setMetadata();
                setTrackVisible();
                mClock.setCallback(this);
                break;
            case PlayerEvent.SIZE:
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
        }
    }

    private void setTrackVisible() {
        mBinding.control.text.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_TEXT) || mPlayers.isVod() ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setMetadata() {
        mPlayers.setMetadata(mBinding.widget.title.getText().toString(), "", "", mBinding.exo.getDefaultArtwork());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (!event.getTag().equals(tag)) return;
        if (mPlayers.retried()) onError(event);
        else onReset();
    }

    private void onError(ErrorEvent event) {
        showError(event.getMsg());
        mPlayers.resetTrack();
        onStopped();
    }

    private void onPaused() {
        setState(RenderState.PAUSED);
        mPlayers.pause();
        showInfo();
    }

    private void onPlay() {
        if (mPlayers.isEmpty()) return;
        setState(RenderState.PLAYING);
        mPlayers.play();
        hideCenter();
    }

    private void onStopped() {
        setState(RenderState.STOPPED);
        mPlayers.clearMediaItems();
        mPlayers.reset();
        mPlayers.stop();
    }

    private void setState(RenderState state) {
        if (mService != null) mService.notifyAvTransportLastChange(mState = state);
    }

    @NonNull
    @Override
    public RenderState getState() {
        return mState;
    }

    @Override
    public void onSubtitleClick() {
        App.post(this::hideControl, 200);
        App.post(() -> SubtitleDialog.create().view(mBinding.exo.getSubtitleView()).full(true).show(this), 200);
    }

    @Override
    public void onTimeChanged() {
        position = mPlayers.getPosition();
        duration = mPlayers.getDuration();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((RendererServiceBinder) service).getService();
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
        App.post(() -> mPlayers.seekTo(time));
    }

    @Override
    public void pause() {
        App.post(this::onPaused);
    }

    @Override
    public void play(@Nullable Double speed) {
        App.post(this::onPlay);
    }

    @Override
    public void stop() {
        App.post(this::onStopped);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) onToggle();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (isGone(mBinding.control.getRoot()) && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeeking(long time) {
        if (mPlayers.isEmpty()) return;
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.duration.setText(mPlayers.getDurationTime());
        mBinding.widget.position.setText(mPlayers.getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        hideProgress();
    }

    @Override
    public void onSeekTo(long time) {
        if (mPlayers.isEmpty()) return;
        mKeyDown.resetTime();
        mPlayers.seek(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isPlaying()) return;
        mBinding.control.speed.setText(mPlayers.setSpeed(Setting.getSpeed()));
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
        if (mPlayers.isPlaying()) onPaused();
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
        mBinding.exo.setPlayer(mPlayers.get());
        mClock.stop().start();
        onPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRedirect()) onPlay();
        setRedirect(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRedirect()) onPaused();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Setting.isBackgroundOff()) onPaused();
        if (Setting.isBackgroundOff()) mClock.stop();
        mBinding.exo.setPlayer(null);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClock.release();
        mPlayers.release();
        unbindService(this);
        PlaybackService.stop();
        mService.bindRealPlayer(null);
        App.removeCallbacks(mR1, mR2);
    }
}
