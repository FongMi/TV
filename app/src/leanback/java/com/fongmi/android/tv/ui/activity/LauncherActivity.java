package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import androidx.viewbinding.ViewBinding;

import com.android.cast.dlna.dmr.DLNARendererService;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Info;
import com.fongmi.android.tv.databinding.ActivityLauncherBinding;
import com.fongmi.android.tv.ui.adapter.AppAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

public class LauncherActivity extends BaseActivity implements AppAdapter.OnClickListener {

    private ActivityLauncherBinding mBinding;
    private AppAdapter mAdapter;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLauncherBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        DLNARendererService.Companion.start(this, R.drawable.ic_logo);
        setRecyclerView();
        getApps();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new AppAdapter(this));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(5, 16));
    }

    private void getApps() {
        for (ApplicationInfo info : getPackageManager().getInstalledApplications(0)) {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            mAdapter.add(Info.get(info));
        }
    }

    @Override
    public void onItemClick(Info item) {
        try {
            startActivity(getPackageManager().getLaunchIntentForPackage(item.getPack()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(Info item) {
        if (item.getPack().equals(getPackageName())) return false;
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + item.getPack()));
        startActivity(intent);
        return true;
    }
}
