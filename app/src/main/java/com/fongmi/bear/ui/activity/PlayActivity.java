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
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.impl.KeyDownImpl;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.player.Players;
import com.fongmi.bear.utils.KeyDown;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.ResUtil;
import com.fongmi.bear.utils.Utils;
import com.google.android.exoplayer2.Player;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlayActivity extends BaseActivity implements KeyDownImpl {

    private ActivityPlayBinding mBinding;
    private SiteViewModel mSiteViewModel;
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
        mKeyDown = KeyDown.create(this);
        mVodFlag = Vod.Flag.objectFrom(getFlag());
        mBinding.video.setPlayer(Players.get().callback(this).exo());
        setViewModel();
        findCurrent();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mBinding.video.findViewById(R.id.next).setOnClickListener(view -> {
            int max = mVodFlag.getEpisodes().size() - 1;
            mCurrent = ++mCurrent > max ? max : mCurrent;
            if (mVodFlag.getEpisodes().get(mCurrent).isActivated()) Notify.show(R.string.error_play_next);
            else getPlayer();
        });
        mBinding.video.findViewById(R.id.prev).setOnClickListener(view -> {
            mCurrent = --mCurrent < 0 ? 0 : mCurrent;
            if (mVodFlag.getEpisodes().get(mCurrent).isActivated()) Notify.show(R.string.error_play_prev);
            else getPlayer();
        });
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.player.observe(this, object -> Players.get().setMediaSource(object));
    }

    private void findCurrent() {
        for (int i = 0; i < mVodFlag.getEpisodes().size(); i++) {
            if (mVodFlag.getEpisodes().get(i).isActivated()) {
                mCurrent = i;
                break;
            }
        }
    }

    private void getPlayer() {
        Vod.Flag.Episode episode = mVodFlag.getEpisodes().get(mCurrent);
        mVodFlag.setActivated(episode);
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        mSiteViewModel.playerContent(mVodFlag.getFlag(), episode.getUrl());
        Notify.show(ResUtil.getString(R.string.play_ready, episode.getName()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackStateChanged(PlayerEvent event) {
        mBinding.progress.getRoot().setVisibility(event.getState() == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!mBinding.video.isControllerFullyVisible() && Utils.hasEvent(event)) return mKeyDown.onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeek(boolean forward) {

    }

    @Override
    public void onKeyUp() {

    }

    @Override
    public void onKeyDown() {
        mBinding.video.showController();
    }

    @Override
    public void onKeyLeft() {

    }

    @Override
    public void onKeyRight() {

    }

    @Override
    public void onKeyCenter() {
        Players.get().toggle();
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

    @Override
    public void onBackPressed() {
        if (mBinding.video.isControllerFullyVisible()) {
            mBinding.video.hideController();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
