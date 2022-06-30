package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityHomeBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.custom.CustomSelector;
import com.fongmi.bear.ui.presenter.FuncPresenter;
import com.fongmi.bear.ui.presenter.FuncRowPresenter;
import com.fongmi.bear.ui.presenter.TitlePresenter;
import com.fongmi.bear.ui.presenter.VodPresenter;
import com.fongmi.bear.ui.presenter.VodRowPresenter;
import com.fongmi.bear.utils.ResUtil;
import com.fongmi.bear.utils.Utils;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private FuncPresenter mFuncPresenter;
    private ArrayObjectAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        setAdapter();
        getContent();
    }

    @Override
    protected void initEvent() {
        mFuncPresenter.setOnClickListener(this::onFuncClick);
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(String.class, new TitlePresenter());
        selector.addPresenter(ListRow.class, new VodRowPresenter(), VodPresenter.class);
        selector.addPresenter(ListRow.class, new FuncRowPresenter(), FuncPresenter.class);
        ItemBridgeAdapter adapter = new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(24));
        mBinding.recycler.setAdapter(adapter);
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(this, result -> {
            for (List<Vod> items : Utils.chunkList(result.getList(), 5)) {
                ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter());
                adapter.addAll(0, items);
                mAdapter.add(new ListRow(adapter));
            }
        });
    }

    private void setAdapter() {
        mAdapter.add(ResUtil.getString(R.string.app_name));
        mAdapter.add(getFuncRow());
        mAdapter.add(ResUtil.getString(R.string.home_recent));
        mAdapter.add(ResUtil.getString(R.string.home_recommend));
    }

    private void getContent() {
        if (mAdapter.size() > 4) mAdapter.removeItems(4, mAdapter.size() - 4);
        mSiteViewModel.homeContent();
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(mFuncPresenter = new FuncPresenter());
        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        return new ListRow(adapter);
    }

    private void onFuncClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_vod:
                Result result = mSiteViewModel.getResult().getValue();
                if (result != null) VodActivity.start(this, result);
                break;
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        getContent();
    }
}