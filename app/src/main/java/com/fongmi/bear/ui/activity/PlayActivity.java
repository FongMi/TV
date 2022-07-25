package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.R;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityPlayBinding;
import com.fongmi.bear.databinding.ViewControllerBottomBinding;
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.impl.KeyDownImpl;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.player.Players;
import com.fongmi.bear.utils.KeyDown;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;
import com.fongmi.bear.utils.ResUtil;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Formatter;
import java.util.Locale;

public class PlayActivity extends BaseActivity implements KeyDownImpl {

    private ViewControllerBottomBinding mControl;
    private ActivityPlayBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private StringBuilder mBuilder;
    private Formatter mFormatter;
    private Vod.Flag mVodFlag;
    private KeyDown mKeyDown;
    private int mCurrent;

    private String getFlag() {
        return getIntent().getStringExtra("flag");
    }

    public static void newInstance(Activity activity, Vod.Flag flag) {
        Intent intent = new Intent(activity, PlayActivity.class);
        intent.putExtra("flag", flag.toString());
        activity.startActivityForResult(intent, 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityPlayBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBuilder = new StringBuilder();
        mKeyDown = KeyDown.create(this);
        mVodFlag = Vod.Flag.objectFrom(getFlag());
        mFormatter = new Formatter(mBuilder, Locale.getDefault());
        mControl = ViewControllerBottomBinding.bind(mBinding.video.findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller));
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getScale()]);
        mControl.speed.setText(Players.get().getSpeed());
        mBinding.video.setResizeMode(Prefers.getScale());
        mBinding.video.setControllerShowTimeoutMs(3000);
        mBinding.video.setPlayer(Players.get().exo());
        if (Players.get().isIdle()) showProgress();
        setViewModel();
        findCurrent();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mControl.next.setOnClickListener(view -> onNext());
        mControl.prev.setOnClickListener(view -> onPrev());
        mControl.replay.setOnClickListener(view -> getPlayer());
        mControl.speed.setOnClickListener(view -> mControl.speed.setText(Players.get().addSpeed()));
        mControl.scale.setOnClickListener(view -> onScale());
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.player.observe(this, object -> {
            if (object != null) Players.get().setMediaSource(object);
        });
    }

    private void findCurrent() {
        for (int i = 0; i < mVodFlag.getEpisodes().size(); i++) {
            if (mVodFlag.getEpisodes().get(i).isActivated()) {
                mCurrent = i;
                break;
            }
        }
    }

    private void showProgress() {
        if (mBinding.progress.getRoot().getVisibility() == View.GONE) {
            mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        if (mBinding.progress.getRoot().getVisibility() == View.VISIBLE) {
            mBinding.progress.getRoot().setVisibility(View.GONE);
        }
    }

    private void getPlayer() {
        Vod.Flag.Episode episode = mVodFlag.getEpisodes().get(mCurrent);
        mSiteViewModel.playerContent(mVodFlag.getFlag(), episode.getUrl());
        Notify.show(ResUtil.getString(R.string.play_ready, episode.getName()));
        mVodFlag.setActivated(episode);
        showProgress();
    }

    private void onNext() {
        int max = mVodFlag.getEpisodes().size() - 1;
        mCurrent = ++mCurrent > max ? max : mCurrent;
        if (mVodFlag.getEpisodes().get(mCurrent).isActivated()) Notify.show(R.string.error_play_next);
        else getPlayer();
    }

    private void onPrev() {
        mCurrent = --mCurrent < 0 ? 0 : mCurrent;
        if (mVodFlag.getEpisodes().get(mCurrent).isActivated()) Notify.show(R.string.error_play_prev);
        else getPlayer();
    }

    private void onScale() {
        int scale = mBinding.video.getResizeMode();
        mBinding.video.setResizeMode(scale = scale >= 4 ? 0 : scale + 1);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
        Prefers.putScale(scale);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackStateChanged(PlayerEvent event) {
        if (event.getState() == -1) Notify.show(R.string.error_play_parse);
        if (event.getState() == Player.STATE_BUFFERING) showProgress();
        else hideProgress();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!mBinding.video.isControllerFullyVisible() && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeek(boolean forward) {
        long time = Players.get().exo().getCurrentPosition() + (forward ? 10000 : -10000);
        mBinding.center.exoDuration.setText(mControl.exoDuration.getText());
        mBinding.center.exoPosition.setText(Util.getStringForTime(mBuilder, mFormatter, time));
        mBinding.center.action.setImageResource(forward ? R.drawable.ic_forward : R.drawable.ic_rewind);
        mBinding.center.getRoot().setVisibility(View.VISIBLE);
        Players.get().exo().seekTo(time);
    }

    @Override
    public void onKeyUp() {

    }

    @Override
    public void onKeyDown() {
        mBinding.video.showController();
        mControl.next.requestFocus();
    }

    @Override
    public void onKeyLeft() {
        mBinding.center.getRoot().setVisibility(View.GONE);
        mBinding.center.action.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onKeyRight() {
        mBinding.center.getRoot().setVisibility(View.GONE);
        mBinding.center.action.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onKeyCenter() {
        if (Players.get().isPlaying()) {
            Players.get().pause();
            mBinding.center.getRoot().setVisibility(View.VISIBLE);
            mBinding.center.exoDuration.setText(mControl.exoDuration.getText());
            mBinding.center.exoPosition.setText(Util.getStringForTime(mBuilder, mFormatter, Players.get().exo().getCurrentPosition()));
        } else {
            Players.get().play();
            mBinding.center.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    public void onKeyMenu() {

    }

    @Override
    public void onKeyBack() {
        onBackPressed();
    }

    @Override
    public void onLongPress() {

    }

    private void setResult() {
        Intent intent = new Intent();
        intent.putExtra("current", mCurrent);
        setResult(RESULT_OK, intent);
    }

    @Override
    public void onBackPressed() {
        if (mBinding.video.isControllerFullyVisible()) {
            mBinding.video.hideController();
        } else {
            setResult();
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
