package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivityPlayBinding;
import com.fongmi.bear.impl.KeyDownImpl;
import com.fongmi.bear.player.Player;
import com.fongmi.bear.utils.KeyDown;
import com.fongmi.bear.utils.Utils;

public class PlayActivity extends BaseActivity implements Player.Callback, KeyDownImpl {

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
        mBinding.video.setPlayer(Player.get().callback(this).exo());
    }

    @Override
    protected void initEvent() {

    }

    @Override
    public void onBuffering() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    @Override
    public void onReady() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!mBinding.video.isControllerVisible() && Utils.hasEvent(event)) return mKeyDown.onKeyDown(event);
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
        mBinding.video.showController();
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
        if (mBinding.video.isControllerVisible()) {
            mBinding.video.hideController();
        } else {
            super.onBackPressed();
        }
    }
}
