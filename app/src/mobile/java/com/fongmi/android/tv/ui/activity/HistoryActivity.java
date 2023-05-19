package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.dialog.SyncDialog;
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
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        getHistory();
    }

    @Override
    protected void initEvent() {
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        mAdapter.setSize(Product.getSpec(getActivity()));
    }

    private void getHistory() {
        mAdapter.addAll(History.get());
        mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void onSync(View view) {
        SyncDialog.create().history().show(this);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.HISTORY)) getHistory();
    }

    @Override
    public void onItemClick(History item) {
        DetailActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName());
    }

    @Override
    public void onItemDelete(History item) {
        mAdapter.remove(item.delete());
        if (mAdapter.getItemCount() > 0) return;
        mBinding.delete.setVisibility(View.GONE);
        mAdapter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
    }
}
