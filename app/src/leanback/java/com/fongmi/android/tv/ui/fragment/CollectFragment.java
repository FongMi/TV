package com.fongmi.android.tv.ui.fragment;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.activity.VodActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private FragmentVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<Vod> mItems;
    private String mKeyword;
    private Site mSite;

    public static CollectFragment newInstance(String keyword, Site site, List<Vod> items) {
        return new CollectFragment().setKeyword(keyword).setSite(site).setItems(items);
    }

    private CollectFragment setKeyword(String keyword) {
        this.mKeyword = keyword;
        return this;
    }

    private CollectFragment setSite(Site site) {
        this.mSite = site;
        return this;
    }

    private CollectFragment setItems(List<Vod> items) {
        this.mItems = items;
        return this;
    }

    private boolean isAll() {
        return mSite.getKey().equals("all");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mScroller = new CustomScroller(this);
        setRecyclerView();
        setViewModel();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        if (!isAll()) mBinding.recycler.addOnScrollListener(mScroller);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setHeader(getActivity().findViewById(R.id.result), getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            int size = result.getList().size();
            mScroller.endLoading(size == 0);
            addVideo(result.getList());
        });
    }

    @Override
    protected void initData() {
        addVideo(mItems);
    }

    private boolean checkLastSize(List<Vod> items) {
        if (mLast == null || items.size() == 0) return false;
        int size = Product.getColumn() - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addVideo(new ArrayList<>(items.subList(size, items.size())));
        return true;
    }

    public void addVideo(List<Vod> items) {
        if (checkLastSize(items) || getActivity() == null || getActivity().isFinishing()) return;
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, Product.getColumn())) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    @Override
    public void onItemClick(Vod item) {
        getActivity().setResult(Activity.RESULT_OK);
        if (item.isFolder()) VodActivity.start(getActivity(), item.getSiteKey(), Result.folder(item));
        else DetailActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }

    @Override
    public void onLoadMore(String page) {
        mViewModel.searchContent(mSite, mKeyword, page);
        mScroller.setLoading(true);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
