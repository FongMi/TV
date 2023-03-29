package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivitySettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.permissionx.guolindev.PermissionX;

public class SettingActivity extends BaseActivity implements ConfigCallback, SiteCallback, LiveCallback {

    private ActivitySettingBinding mBinding;
    private String[] quality;
    private String[] render;
    private String[] decode;
    private String[] player;
    private String[] scale;
    private String[] size;
    private int type;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(ApiConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Prefers.getSize()]);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Prefers.getScale()]);
        mBinding.playerText.setText((player = ResUtil.getStringArray(R.array.select_player))[Prefers.getPlayer()]);
        mBinding.decodeText.setText((decode = ResUtil.getStringArray(R.array.select_decode))[Prefers.getDecode()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Prefers.getRender()]);
        mBinding.qualityText.setText((quality = ResUtil.getStringArray(R.array.select_quality))[Prefers.getQuality()]);
    }

    @Override
    protected void initEvent() {
        mBinding.vodHome.setOnClickListener(view -> SiteDialog.create(this).all().show());
        mBinding.liveHome.setOnClickListener(view -> LiveDialog.create(this).show());
        mBinding.vod.setOnClickListener(view -> ConfigDialog.create(this).type(type = 0).show());
        mBinding.live.setOnClickListener(view -> ConfigDialog.create(this).type(type = 1).show());
        mBinding.wall.setOnClickListener(view -> ConfigDialog.create(this).type(type = 2).show());
        mBinding.vodHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 0).show());
        mBinding.liveHistory.setOnClickListener(view -> HistoryDialog.create(this).type(type = 1).show());
        mBinding.wallDefault.setOnClickListener(view -> setWallDefault());
        mBinding.wallRefresh.setOnClickListener(view -> setWallRefresh());
        mBinding.version.setOnLongClickListener(view -> onVersion(true));
        mBinding.version.setOnClickListener(view -> onVersion(false));
        mBinding.quality.setOnClickListener(view -> setQuality());
        mBinding.player.setOnClickListener(view -> setPlayer());
        mBinding.decode.setOnClickListener(view -> setDecode());
        mBinding.render.setOnClickListener(view -> setRender());
        mBinding.scale.setOnClickListener(view -> setScale());
        mBinding.size.setOnClickListener(view -> setSize());
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !Utils.hasPermission(this)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(this);
                mBinding.vodUrl.setText(config.getDesc());
                ApiConfig.get().clear().config(config).load(getCallback(config));
                break;
            case 1:
                Notify.progress(this);
                mBinding.liveUrl.setText(config.getDesc());
                LiveConfig.get().clear().config(config).load(getCallback(config));
                break;
            case 2:
                mBinding.wallUrl.setText(config.getDesc());
                WallConfig.get().clear().config(config).load(getCallback(config));
                break;
        }
    }

    private Callback getCallback(Config config) {
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
        switch (type) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.history();
                mBinding.vodUrl.setText(ApiConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
            case 1:
                Notify.dismiss();
                mBinding.liveUrl.setText(LiveConfig.getUrl());
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
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private boolean onVersion(boolean dev) {
        if (dev) Updater.get().force().dev().start();
        else Updater.get().force().start();
        return true;
    }

    private void setQuality() {
        int index = Prefers.getQuality();
        Prefers.putQuality(index = index == quality.length - 1 ? 0 : ++index);
        mBinding.qualityText.setText(quality[index]);
        RefreshEvent.image();
    }

    private void setPlayer() {
        int index = Prefers.getPlayer();
        Prefers.putPlayer(index = index == player.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(player[index]);
    }

    private void setDecode() {
        int index = Prefers.getDecode();
        Prefers.putDecode(index = index == decode.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(decode[index]);
    }

    private void setRender() {
        int index = Prefers.getRender();
        Prefers.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
    }

    private void setScale() {
        int index = Prefers.getScale();
        Prefers.putScale(index = index == scale.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(scale[index]);
    }

    private void setSize() {
        int index = Prefers.getSize();
        Prefers.putSize(index = index == size.length - 2 ? 0 : ++index);
        mBinding.sizeText.setText(size[index]);
        RefreshEvent.size();
    }

    private void setWallDefault() {
        WallConfig.refresh(Prefers.getWall() == 4 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh() {
        WallConfig.get().load();
    }
}
