package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.ui.dialog.RestoreDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback {

    private FragmentSettingBinding mBinding;
    private String[] size;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    private int getDohIndex() {
        return Math.max(0, VodConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : VodConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    private HomeActivity getRoot() {
        return (HomeActivity) requireActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        setOtherText();
        setCacheText();
    }

    private void setOtherText() {
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.backup.setOnClickListener(this::onBackup);
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.restore.setOnClickListener(this::onRestore);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.vod.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.live.setOnLongClickListener(this::onLiveEdit);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
        mBinding.wall.setOnLongClickListener(this::onWallEdit);
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.incognito.setOnClickListener(this::setIncognito);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.doh.setOnClickListener(this::setDoh);
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file")) {
            PermissionUtil.requestFile(this, allGranted -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(requireActivity());
                VodConfig.load(config, getCallback(0));
                mBinding.vodUrl.setText(config.getDesc());
                break;
            case 1:
                Notify.progress(requireActivity());
                LiveConfig.load(config, getCallback(1));
                mBinding.liveUrl.setText(config.getDesc());
                break;
            case 2:
                Notify.progress(requireActivity());
                WallConfig.load(config, getCallback(2));
                mBinding.wallUrl.setText(config.getDesc());
                break;
        }
    }

    private Callback getCallback(int type) {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                setConfig(type);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                setConfig(type);
            }
        };
    }

    private void setConfig(int type) {
        switch (type) {
            case 0:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                mBinding.vodUrl.setText(VodConfig.getDesc());
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
            case 1:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.config();
                mBinding.liveUrl.setText(LiveConfig.getDesc());
                break;
            case 2:
                setCacheText();
                Notify.dismiss();
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void onVod(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 0).show();
    }

    private void onLive(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 2).show();
    }

    private boolean onVodEdit(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 0).edit().show();
        return true;
    }

    private boolean onLiveEdit(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 1).edit().show();
        return true;
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create(this).launcher(launcher).type(type = 2).edit().show();
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).all().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).action().show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }

    private void onPlayer(View view) {
        getRoot().change(2);
    }

    private void onVersion(View view) {
        Updater.create().force().release().start(requireActivity());
    }

    private boolean onVersionDev(View view) {
        Updater.create().force().dev().start(requireActivity());
        return true;
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 4 ? 1 : Setting.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(requireActivity());
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        });
    }

    private void setIncognito(View view) {
        Setting.putIncognito(!Setting.isIncognito());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.setting_doh).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(VodConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        Source.get().stop();
        OkHttp.get().setDoh(doh);
        Notify.progress(requireActivity());
        Setting.putDoh(doh.toString());
        mBinding.dohText.setText(doh.getName());
        VodConfig.load(Config.vod(), getCallback(0));
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }

    private void onBackup(View view) {
        PermissionUtil.requestFile(this, allGranted -> AppDatabase.backup(new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.backup_success);
            }

            @Override
            public void error() {
                Notify.show(R.string.backup_fail);
            }
        }));
    }

    private void onRestore(View view) {
        PermissionUtil.requestFile(this, allGranted -> RestoreDialog.create().show(requireActivity(), new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.restore_success);
                Notify.progress(requireActivity());
                setOtherText();
                initConfig();
            }

            @Override
            public void error() {
                Notify.show(R.string.restore_fail);
            }
        }));
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback(0));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) return;
        mBinding.vodUrl.setText(VodConfig.getDesc());
        mBinding.liveUrl.setText(LiveConfig.getDesc());
        mBinding.wallUrl.setText(WallConfig.getDesc());
        setCacheText();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
        setConfig(Config.find("file:/" + FileChooser.getPathFromUri(result.getData().getData()).replace(Path.rootPath(), ""), type));
    });
}
