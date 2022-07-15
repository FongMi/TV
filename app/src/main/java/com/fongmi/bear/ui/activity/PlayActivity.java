package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivityPlayBinding;
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.impl.KeyDownImpl;
import com.fongmi.bear.player.Players;
import com.fongmi.bear.utils.KeyDown;
import com.fongmi.bear.utils.Utils;
import com.google.android.exoplayer2.Player;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlayActivity extends BaseActivity implements KeyDownImpl {

    private ActivityPlayBinding mBinding;
    private KeyDown mKeyDown;

    public static void newInstance(Activity activity) {
        activity.startActivityForResult(new Intent(activity, PlayActivity.class), 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityPlayBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKeyDown = KeyDown.create(this);
        mBinding.video.setPlayer(Players.get().callback(this).exo());
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
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
    public void onKeyVertical(boolean up) {

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
