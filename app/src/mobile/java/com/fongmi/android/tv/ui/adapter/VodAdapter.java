package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFullBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.databinding.AdapterVodOvalBinding;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.VodFullHolder;
import com.fongmi.android.tv.ui.holder.VodListHolder;
import com.fongmi.android.tv.ui.holder.VodOvalHolder;
import com.fongmi.android.tv.ui.holder.VodRectHolder;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<BaseVodHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private Vod.Style style;
    private int[] size;

    public VodAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.style = Vod.Style.rect();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        boolean onLongClick(Vod item);
    }

    public void setSize(int[] size) {
        this.size = size;
    }

    public Vod.Style getStyle() {
        return style;
    }

    public void setStyle(Vod.Style style) {
        this.style = style;
    }

    public boolean isLinear() {
        return style.isList() || style.isFull();
    }

    public void addAll(List<Vod> items) {
        int position = mItems.size() + 1;
        mItems.addAll(items);
        notifyItemRangeInserted(position, items.size());
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return style.getViewType();
    }

    @Override
    public void onBindViewHolder(@NonNull BaseVodHolder holder, int position) {
        holder.initView(mItems.get(position));
    }

    @NonNull
    @Override
    public BaseVodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ViewType.LIST:
                return new VodListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.FULL:
                return new VodFullHolder(AdapterVodFullBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case ViewType.OVAL:
                return new VodOvalHolder(AdapterVodOvalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
            default:
                return new VodRectHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener).size(size);
        }
    }
}
