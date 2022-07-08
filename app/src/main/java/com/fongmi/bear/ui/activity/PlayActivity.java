package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivityPlayBinding;
import com.fongmi.bear.player.Player;

public class PlayActivity extends BaseActivity {

    private ActivityPlayBinding mBinding;

    public static void newInstance(Activity activity) {
        activity.startActivityForResult(new Intent(activity, PlayActivity.class), 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityPlayBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.video.setControllerAutoShow(false);
        mBinding.video.setPlayer(Player.get().exo());
    }

    @Override
    protected void initEvent() {
    }
}
