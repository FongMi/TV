package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.Updater;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.permissionx.guolindev.PermissionX;

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback {

    private FragmentSettingBinding mBinding;
    private Config config;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(ApiConfig.getUrl());
        mBinding.liveUrl.setText(LiveConfig.getUrl());
        mBinding.wallUrl.setText(WallConfig.getUrl());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.sizeText.setText(ResUtil.getStringArray(R.array.select_size)[Prefers.getSize()]);
        mBinding.scaleText.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getScale()]);
        mBinding.playerText.setText(ResUtil.getStringArray(R.array.select_player)[Prefers.getPlayer()]);
        mBinding.decodeText.setText(ResUtil.getStringArray(R.array.select_decode)[Prefers.getDecode()]);
        mBinding.renderText.setText(ResUtil.getStringArray(R.array.select_render)[Prefers.getRender()]);
    }

    @Override
    protected void initEvent() {
        mBinding.vodHome.setOnClickListener(view -> SiteDialog.create(this).all().show());
        mBinding.liveHome.setOnClickListener(view -> LiveDialog.create(this).show());
        mBinding.vod.setOnClickListener(view -> ConfigDialog.create(this).type(0).show());
        mBinding.live.setOnClickListener(view -> ConfigDialog.create(this).type(1).show());
        mBinding.wall.setOnClickListener(view -> ConfigDialog.create(this).type(2).show());
        mBinding.vodHistory.setOnClickListener(view -> HistoryDialog.create(this).type(0).show());
        mBinding.liveHistory.setOnClickListener(view -> HistoryDialog.create(this).type(1).show());
        mBinding.version.setOnClickListener(view -> Updater.get().force().start());
        mBinding.wallDefault.setOnClickListener(view -> setWallDefault());
        mBinding.wallRefresh.setOnClickListener(view -> setWallRefresh());
        mBinding.player.setOnClickListener(view -> setPlayer());
        mBinding.decode.setOnClickListener(view -> setDecode());
        mBinding.render.setOnClickListener(view -> setRender());
        mBinding.scale.setOnClickListener(view -> setScale());
        mBinding.size.setOnClickListener(view -> setSize());
        mBinding.version.setOnLongClickListener(v -> {
            Updater.get().force().branch("dev").start();
            return true;
        });
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
        checkPermission();
    }

    private void checkPermission() {
        if (config.getUrl().startsWith("file") && !Utils.hasPermission(getActivity())) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> loadConfig());
        } else {
            loadConfig();
        }
    }

    private void loadConfig() {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity());
                mBinding.vodUrl.setText(config.getUrl());
                ApiConfig.get().clear().config(config).load(getCallback());
                break;
            case 1:
                Notify.progress(getActivity());
                mBinding.liveUrl.setText(config.getUrl());
                LiveConfig.get().clear().config(config).load(getCallback());
                break;
            case 2:
                mBinding.wallUrl.setText(config.getUrl());
                WallConfig.get().clear().config(config).load(getCallback());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                config.delete();
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (config.getType()) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                mBinding.liveUrl.setText(LiveConfig.getUrl());
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
            case 1:
                Notify.dismiss();
                break;
            case 2:
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void setPlayer() {
        int index = Prefers.getPlayer();
        String[] array = ResUtil.getStringArray(R.array.select_player);
        Prefers.putPlayer(index = index == array.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(array[index]);
    }

    private void setDecode() {
        int index = Prefers.getDecode();
        String[] array = ResUtil.getStringArray(R.array.select_decode);
        Prefers.putDecode(index = index == array.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(array[index]);
    }

    private void setRender() {
        int index = Prefers.getRender();
        String[] array = ResUtil.getStringArray(R.array.select_render);
        Prefers.putRender(index = index == array.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(array[index]);
    }

    private void setScale() {
        int index = Prefers.getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        Prefers.putScale(index = index == array.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(array[index]);
    }

    private void setSize() {
        int index = Prefers.getSize();
        String[] array = ResUtil.getStringArray(R.array.select_size);
        Prefers.putSize(index = index == array.length - 1 ? 0 : ++index);
        mBinding.sizeText.setText(array[index]);
        RefreshEvent.size();
    }

    private void setWallDefault() {
        WallConfig.refresh(Prefers.getWall() == 4 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh() {
        WallConfig.get().load();
    }
}
