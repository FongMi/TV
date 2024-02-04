package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingCustomBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingCustomActivity extends BaseActivity {

    private ActivitySettingCustomBinding mBinding;
    private String[] quality;
    private String[] size;
    private String[] episode;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingCustomBinding.inflate(getLayoutInflater());
    }

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingCustomActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected void initView() {
        mBinding.quality.requestFocus();
        mBinding.qualityText.setText((quality = ResUtil.getStringArray(R.array.select_quality))[Setting.getQuality()]);
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        mBinding.episodeText.setText((episode = ResUtil.getStringArray(R.array.select_episode))[Setting.getEpisode()]);
        mBinding.displayTimeText.setText(getSwitch(Setting.isDisplayTime()));
        mBinding.displayNetspeedText.setText(getSwitch(Setting.isDisplaySpeed()));
        mBinding.displayDurationText.setText(getSwitch(Setting.isDisplayDuration()));
        mBinding.speedText.setText(Setting.getPlaySpeed() + "x");
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
    }

    @Override
    protected void initEvent() {
        mBinding.quality.setOnClickListener(this::setQuality);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.episode.setOnClickListener(this::setEpisode);
        mBinding.displayTime.setOnClickListener(this::setDisplayTime);
        mBinding.displayNetspeed.setOnClickListener(this::setDisplaySpeed);
        mBinding.displayDuration.setOnClickListener(this::setDisplayDuration);
        mBinding.speed.setOnClickListener(this::setSpeed);
        mBinding.speed.setOnLongClickListener(this::resetSpeed);
        mBinding.aggregatedSearch.setOnClickListener(this::setAggregatedSearch);
    }

    private void setQuality(View view) {
        int index = Setting.getQuality();
        Setting.putQuality(index = index == quality.length - 1 ? 0 : ++index);
        mBinding.qualityText.setText(quality[index]);
        RefreshEvent.image();
    }

    private void setSize(View view) {
        int index = Setting.getSize();
        Setting.putSize(index = index == size.length - 1 ? 0 : ++index);
        mBinding.sizeText.setText(size[index]);
        RefreshEvent.size();
    }

    private void setEpisode(View view) {
        int index = Setting.getEpisode();
        Setting.putEpisode(index = index == episode.length - 1 ? 0 : ++index);
        mBinding.episodeText.setText(episode[index]);
    }

    private void setDisplayTime(View view) {
        Setting.putDisplayTime(!Setting.isDisplayTime());
        mBinding.displayTimeText.setText(getSwitch(Setting.isDisplayTime()));
    }

    private void setDisplaySpeed(View view) {
        Setting.putDisplaySpeed(!Setting.isDisplaySpeed());
        mBinding.displayNetspeedText.setText(getSwitch(Setting.isDisplaySpeed()));
    }

    private void setDisplayDuration(View view) {
        Setting.putDisplayDuration(!Setting.isDisplayDuration());
        mBinding.displayDurationText.setText(getSwitch(Setting.isDisplayDuration()));
    }

    private void setSpeed(View view) {
        float speed = Setting.getPlaySpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.25f : Math.min(speed + addon, 5.0f);
        Setting.putPlaySpeed(speed);
        mBinding.speedText.setText(Setting.getPlaySpeed() + "x");
    }

    private boolean resetSpeed(View view) {
        Setting.putPlaySpeed(1.0f);
        mBinding.speedText.setText(Setting.getPlaySpeed() + "x");
        return true;
    }

    private void setAggregatedSearch(View view) {
        Setting.putAggregatedSearch(!Setting.isAggregatedSearch());
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
    }

}
