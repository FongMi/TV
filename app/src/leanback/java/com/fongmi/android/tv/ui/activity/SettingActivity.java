package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.View;

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
import com.fongmi.android.tv.utils.FileUtil;
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
        setCacheText();
    }

    @Override
    protected void initEvent() {
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.quality.setOnClickListener(this::setQuality);
        mBinding.player.setOnClickListener(this::setPlayer);
        mBinding.decode.setOnClickListener(this::setDecode);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.scale.setOnClickListener(this::setScale);
        mBinding.size.setOnClickListener(this::setSize);
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
                setCacheText();
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.history();
                mBinding.vodUrl.setText(ApiConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
            case 1:
                setCacheText();
                Notify.dismiss();
                mBinding.liveUrl.setText(LiveConfig.getUrl());
                break;
            case 2:
                setCacheText();
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

    private void onVod(View view) {
        ConfigDialog.create(this).type(type = 0).show();
    }

    private void onLive(View view) {
        ConfigDialog.create(this).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).all().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }

    private void onVersion(View view) {
        Updater.get().force().start();
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Prefers.getWall() == 4 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(this);
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setQuality(View view) {
        int index = Prefers.getQuality();
        Prefers.putQuality(index = index == quality.length - 1 ? 0 : ++index);
        mBinding.qualityText.setText(quality[index]);
        RefreshEvent.image();
    }

    private void setPlayer(View view) {
        int index = Prefers.getPlayer();
        Prefers.putPlayer(index = index == player.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(player[index]);
    }

    private void setDecode(View view) {
        int index = Prefers.getDecode();
        Prefers.putDecode(index = index == decode.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(decode[index]);
    }

    private void setRender(View view) {
        int index = Prefers.getRender();
        Prefers.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
    }

    private void setScale(View view) {
        int index = Prefers.getScale();
        Prefers.putScale(index = index == scale.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(scale[index]);
    }

    private void setSize(View view) {
        int index = Prefers.getSize();
        Prefers.putSize(index = index == size.length - 2 ? 0 : ++index);
        mBinding.sizeText.setText(size[index]);
        RefreshEvent.size();
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }
}
