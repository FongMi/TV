package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterEpisodeGridBinding;
import com.fongmi.android.tv.databinding.AdapterEpisodeListBinding;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final List<Vod.Flag.Episode> mItems;
    private final OnClickListener mListener;
    private final int viewType;

    public EpisodeAdapter(OnClickListener listener, int viewType) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        this.viewType = viewType;
    }

    public interface OnClickListener {

        void onItemClick(Vod.Flag.Episode item);
    }

    public void addAll(List<Vod.Flag.Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Vod.Flag.Episode getActivated() {
        return mItems.get(getPosition());
    }

    public Vod.Flag.Episode getNext() {
        int current = getPosition();
        int max = getItemCount() - 1;
        current = ++current > max ? max : current;
        return mItems.get(current);
    }

    public Vod.Flag.Episode getPrev() {
        int current = getPosition();
        current = --current < 0 ? 0 : current;
        return mItems.get(current);
    }

    public List<Vod.Flag.Episode> getItems() {
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
        Vod.Flag.Episode item = mItems.get(position);
        if (holder.gridBinding != null) holder.initView(holder.gridBinding.text, item);
        if (holder.listBinding != null) holder.initView(holder.listBinding.text, item);
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

        void initView(TextView view, Vod.Flag.Episode item) {
            view.setText(item.getName());
            view.setSelected(item.isActivated());
            view.setActivated(item.isActivated());
            view.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }
}