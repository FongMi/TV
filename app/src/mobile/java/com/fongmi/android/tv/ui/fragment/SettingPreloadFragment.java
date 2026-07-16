package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentSettingPreloadBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.PreloadDialog;
import com.fongmi.android.tv.utils.FileUtil;

public class SettingPreloadFragment extends BaseFragment {

    private FragmentSettingPreloadBinding mBinding;

    public static SettingPreloadFragment newInstance() {
        return new SettingPreloadFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPreloadBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        refresh();
    }

    @Override
    protected void initEvent() {
        mBinding.preload.setOnClickListener(this::setPreload);
        mBinding.preloadNext.setOnClickListener(this::setPreloadNext);
        mBinding.preloadSize.setOnClickListener(view -> PreloadDialog.show(this, PreloadDialog.SIZE));
        mBinding.preloadTime.setOnClickListener(view -> PreloadDialog.show(this, PreloadDialog.TIME));
        mBinding.preloadThread.setOnClickListener(view -> PreloadDialog.show(this, PreloadDialog.THREADS));
    }

    private void refresh() {
        mBinding.preloadText.setText(Setting.getSwitch(PreloadSetting.isEnabled()));
        mBinding.preloadNextText.setText(Setting.getSwitch(PreloadSetting.isNextEpisodeEnabled()));
        setPreloadThreadsText();
        setPreloadSizeText();
        setPreloadTimeText();
        setVisible();
    }

    private void setVisible() {
        boolean exo = PlayerSetting.isExo();
        boolean preload = PreloadSetting.isEnabled();
        mBinding.preloadTime.setVisibility(preload ? View.VISIBLE : View.GONE);
        mBinding.preloadNext.setVisibility(preload && exo ? View.VISIBLE : View.GONE);
        mBinding.preloadSize.setVisibility(preload && exo ? View.VISIBLE : View.GONE);
        mBinding.preloadThread.setVisibility(preload && exo ? View.VISIBLE : View.GONE);
    }

    private void setPreload(View view) {
        PreloadSetting.putEnabled(!PreloadSetting.isEnabled());
        mBinding.preloadText.setText(Setting.getSwitch(PreloadSetting.isEnabled()));
        setVisible();
    }

    private void setPreloadNext(View view) {
        PreloadSetting.putNextEpisodeEnabled(!PreloadSetting.isNextEpisodeEnabled());
        mBinding.preloadNextText.setText(Setting.getSwitch(PreloadSetting.isNextEpisodeEnabled()));
    }

    public void setPreload(int type, int value) {
        if (type == PreloadDialog.THREADS) {
            PreloadSetting.putThreads(value);
            setPreloadThreadsText();
        } else if (type == PreloadDialog.SIZE) {
            PreloadSetting.putSizeMb(value);
            setPreloadSizeText();
        } else if (type == PreloadDialog.TIME) {
            PreloadSetting.putTimeSeconds(value);
            setPreloadTimeText();
        }
    }

    private void setPreloadSizeText() {
        mBinding.preloadSizeText.setText(FileUtil.byteCountToDisplaySize(PreloadSetting.getSizeBytes()));
    }

    private void setPreloadTimeText() {
        mBinding.preloadTimeText.setText(getString(R.string.player_preload_time_value, PreloadSetting.getTimeSeconds()));
    }

    private void setPreloadThreadsText() {
        mBinding.preloadThreadText.setText(getString(R.string.player_preload_threads_value, PreloadSetting.getThreads()));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) refresh();
    }
}
