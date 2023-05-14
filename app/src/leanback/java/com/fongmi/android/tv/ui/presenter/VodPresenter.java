package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodGridBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.VodFolderHolder;
import com.fongmi.android.tv.ui.holder.VodGridHolder;
import com.fongmi.android.tv.utils.ResUtil;

public class VodPresenter extends Presenter {

    private final OnClickListener mListener;
    private final int viewType;
    private int width, height;

    public VodPresenter(OnClickListener listener) {
        this(listener, ViewType.GRID);
    }

    public VodPresenter(OnClickListener listener, int viewType) {
        this.mListener = listener;
        this.viewType = viewType;
        setLayoutSize();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    private void setLayoutSize() {
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (Product.getColumn() - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / Product.getColumn();
        height = (int) (width / 0.75f);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        if (viewType == ViewType.FOLDER) return new VodFolderHolder(AdapterVodFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        VodGridHolder holder = new VodGridHolder(AdapterVodGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        switch (viewType) {
            case ViewType.GRID:
                ((VodGridHolder) viewHolder).initView((Vod) object);
                break;
            case ViewType.FOLDER:
                ((VodFolderHolder) viewHolder).initView((Vod) object);
                break;
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }
}