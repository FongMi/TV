package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeGridBinding;
import com.fongmi.android.tv.databinding.AdapterEpisodeListBinding;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final List<Episode> mItems;
    private final OnClickListener mListener;
    private final int viewType;

    public EpisodeAdapter(OnClickListener listener, int viewType) {
        this(listener, viewType, new ArrayList<>());
    }

    public EpisodeAdapter(OnClickListener listener, int viewType, ArrayList<Episode> items) {
        this.mListener = listener;
        this.viewType = viewType;
        this.mItems = items;
    }

    public interface OnClickListener {

        void onItemClick(Episode item);
    }

    public void addAll(List<Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Episode getActivated() {
        return mItems.get(getPosition());
    }

    public Episode getNext() {
        int current = getPosition();
        int max = getItemCount() - 1;
        current = ++current > max ? max : current;
        return mItems.get(current);
    }

    public Episode getPrev() {
        int current = getPosition();
        current = --current < 0 ? 0 : current;
        return mItems.get(current);
    }

    public List<Episode> getItems() {
        return mItems;
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) return new ViewHolder(AdapterEpisodeListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        return new ViewHolder(AdapterEpisodeGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode item = mItems.get(position);
        if (holder.gridBinding != null) holder.initView(holder.gridBinding.text, item, false);
        if (holder.listBinding != null) holder.initView(holder.listBinding.text, item, true);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private AdapterEpisodeListBinding listBinding;
        private AdapterEpisodeGridBinding gridBinding;

        ViewHolder(@NonNull AdapterEpisodeListBinding binding) {
            super(binding.getRoot());
            this.listBinding = binding;
        }

        ViewHolder(@NonNull AdapterEpisodeGridBinding binding) {
            super(binding.getRoot());
            this.gridBinding = binding;
        }

        void initView(TextView view, Episode item, boolean ems) {
            view.setSelected(item.isActivated());
            view.setActivated(item.isActivated());
            if (ems) view.setMaxEms(Product.getEms());
            view.setText(item.getDesc().concat(item.getName()));
            view.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }
}