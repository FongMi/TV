package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodFullBinding;
import com.fongmi.android.tv.databinding.AdapterVodGridBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.databinding.AdapterVodOvalBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.VodFolderHolder;
import com.fongmi.android.tv.ui.holder.VodFullHolder;
import com.fongmi.android.tv.ui.holder.VodGridHolder;
import com.fongmi.android.tv.ui.holder.VodListHolder;
import com.fongmi.android.tv.ui.holder.VodOvalHolder;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<BaseVodHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private int viewType;
    private int[] size;

    public VodAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.viewType = ViewType.GRID;
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    public void setSize(int[] size) {
        this.size = size;
    }

    public int getWidth() {
        return size[0];
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public boolean isList() {
        return viewType == ViewType.LIST || viewType == ViewType.FOLDER || viewType == ViewType.FULL;
    }

    public boolean isGrid() {
        return viewType == ViewType.GRID || viewType == ViewType.LAND || viewType == ViewType.OVAL;
    }

    public void addAll(List<Vod> items) {
        int position = mItems.size() + 1;
        mItems.addAll(items);
        notifyItemRangeInserted(position, items.size());
    }

    public VodAdapter clear() {
        mItems.clear();
        notifyDataSetChanged();
        return this;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseVodHolder holder, int position) {
        holder.initView(mItems.get(position));
    }

    @NonNull
    @Override
    public BaseVodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ViewType.FOLDER:
                return new VodFolderHolder(AdapterVodFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.LIST:
                return new VodListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.FULL:
                return new VodFullHolder(AdapterVodFullBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case ViewType.OVAL:
                return new VodOvalHolder(AdapterVodOvalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
            default:
                return new VodGridHolder(AdapterVodGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
        }
    }
}
