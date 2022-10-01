package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityKeepBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.KeepPresenter;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class KeepActivity extends BaseActivity implements KeepPresenter.OnClickListener {

    private ActivityKeepBinding mBinding;
    private ArrayObjectAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, KeepActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityKeepBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        getVideo();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), KeepPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setHeader(mBinding.toolbar);
    }

    private void getVideo() {
        List<ListRow> rows = new ArrayList<>();
        for (List<Keep> part : Lists.partition(Keep.getAll(), Prefers.getColumn())) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new KeepPresenter(this));
            adapter.setItems(part, null);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(0, rows);
    }

    private void loadConfig(Config config, Keep item) {
        Prefers.putUrl(config.getUrl());
        ApiConfig.get().setCid(config.update().getId());
        ApiConfig.get().clear().loadConfig(true, new Callback() {
            @Override
            public void success(String json) {
                DetailActivity.start(getActivity(), item.getSiteKey(), item.getVodId());
                RefreshEvent.history();
                RefreshEvent.video();
            }

            @Override
            public void error(int resId) {
                CollectActivity.start(getActivity(), item.getVodName());
            }
        });
    }

    @Override
    public void onItemClick(Keep item) {
        Config config = Config.find(item.getCid());
        if (item.getCid() == ApiConfig.getCid()) {
            DetailActivity.start(this, item.getSiteKey(), item.getVodId());
        } else if (config == null) {
            CollectActivity.start(this, item.getVodName());
        } else {
            loadConfig(config, item);
        }
    }

    @Override
    public void onItemDelete(Keep item) {

    }

    @Override
    public boolean onLongClick() {
        return true;
    }
}
