package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.fragment.CollectFragment;
import com.fongmi.android.tv.ui.presenter.CollectPresenter;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectActivity extends BaseActivity {

    private ActivityCollectBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ExecutorService mService;
    private SiteViewModel mViewModel;
    private PageAdapter mPageAdapter;
    private List<Site> mSites;
    private View mOldView;

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("keyword", keyword);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        setPager();
        setSite();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.pager.setCurrentItem(position);
                if (mOldView != null) mOldView.setActivated(false);
                if (child == null) return;
                mOldView = child.itemView;
                mOldView.setActivated(true);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new CollectPresenter())));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            mAdapter.add(Collect.create(result.getList()));
            getFragment().addVideo(result.getList());
            mPageAdapter.notifyDataSetChanged();
        });
    }

    private void setPager() {
        mBinding.pager.setAdapter(mPageAdapter = new PageAdapter(getSupportFragmentManager()));
    }

    private void setSite() {
        mSites = new ArrayList<>();
        for (Site site : ApiConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        Site home = ApiConfig.get().getHome();
        if (!mSites.contains(home)) return;
        mSites.remove(home);
        mSites.add(0, home);
    }

    private void search() {
        mAdapter.add(Collect.all());
        mPageAdapter.notifyDataSetChanged();
        mService = Executors.newFixedThreadPool(5);
        mBinding.hint.setText(getString(R.string.search_result, getKeyword()));
        for (Site site : mSites) mService.execute(() -> mViewModel.searchContent(site, getKeyword()));
    }

    private CollectFragment getFragment() {
        return (CollectFragment) mPageAdapter.instantiateItem(mBinding.pager, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService == null) return;
        mService.shutdownNow();
        mService = null;
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mAdapter.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return CollectFragment.newInstance(((Collect) mAdapter.get(position)).getList());
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
