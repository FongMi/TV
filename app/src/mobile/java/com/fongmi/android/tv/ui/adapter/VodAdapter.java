package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodGridBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.VodFolderHolder;
import com.fongmi.android.tv.ui.holder.VodGridHolder;
import com.fongmi.android.tv.ui.holder.VodListHolder;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private int width, height;
    private int viewType;

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
        this.width = size[0];
        this.height = size[1];
    }

    public int getWidth() {
        return width;
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.FOLDER) return new VodFolderHolder(AdapterVodFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        if (viewType == ViewType.LIST) return new VodListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        VodGridHolder holder = new VodGridHolder(AdapterVodGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        holder.itemView.getLayoutParams().width = width;
        holder.itemView.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (viewType) {
            case ViewType.GRID:
                ((VodGridHolder) holder).initView(mItems.get(position));
                break;
            case ViewType.LIST:
                ((VodListHolder) holder).initView(mItems.get(position));
                break;
            case ViewType.FOLDER:
                ((VodFolderHolder) holder).initView(mItems.get(position));
                break;
        }
    }
}
