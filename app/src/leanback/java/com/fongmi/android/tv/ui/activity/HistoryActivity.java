package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.OnClickListener {

    private ActivityHistoryBinding mBinding;

    private HistoryAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        getHistory();
    }

    @Override
    protected void initEvent() {
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(Product.getColumn(), 16));
    }

    private void getHistory() {
        mBinding.delete.setFocusable(false);
        mAdapter.addAll(History.get());
        App.post(() -> {
            mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
            mBinding.delete.setFocusable(true);
        }, 500);
        mBinding.recycler.requestFocus();
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_history).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mBinding.delete.setFocusable(false);
        int index = mAdapter.delete(item.delete());
        if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
        App.post(() -> {
            mBinding.delete.setFocusable(true);
        }, 300);
        if (mAdapter.getItemCount() > 0) {
            int nextIndex = index + 1;
            if (index == mAdapter.getItemCount()) nextIndex = index - 1;
            View view  = mBinding.recycler.getLayoutManager().findViewByPosition(nextIndex);
            if (view != null) view.requestFocus();
        }
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(true);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        switch (event.getType()) {
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getHistory();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RefreshEvent.history();
    }

}
