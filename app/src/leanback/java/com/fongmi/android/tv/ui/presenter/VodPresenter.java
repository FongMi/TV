package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodFullBinding;
import com.fongmi.android.tv.databinding.AdapterVodGridBinding;
import com.fongmi.android.tv.databinding.AdapterVodOvalBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.VodFolderHolder;
import com.fongmi.android.tv.ui.holder.VodFullHolder;
import com.fongmi.android.tv.ui.holder.VodGridHolder;
import com.fongmi.android.tv.ui.holder.VodOvalHolder;

public class VodPresenter extends Presenter {

    private final OnClickListener mListener;
    private final int viewType;
    private final int[] size;

    public VodPresenter(OnClickListener listener) {
        this(listener, ViewType.GRID);
    }

    public VodPresenter(OnClickListener listener, int viewType) {
        this.mListener = listener;
        this.viewType = viewType;
        this.size = Product.getSpec(viewType);
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        switch (viewType) {
            case ViewType.FOLDER:
                return new VodFolderHolder(AdapterVodFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.FULL:
                return new VodFullHolder(AdapterVodFullBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)).size(size);
            case ViewType.OVAL:
                return new VodOvalHolder(AdapterVodOvalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
            default:
                return new VodGridHolder(AdapterVodGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ((BaseVodHolder) viewHolder).initView((Vod) object);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }
}