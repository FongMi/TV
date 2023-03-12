package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterFlagBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlagAdapter extends RecyclerView.Adapter<FlagAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod.Flag> mItems;

    public FlagAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Vod.Flag item);
    }

    public void addAll(List<Vod.Flag> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Vod.Flag get(int position) {
        return mItems.get(position);
    }

    public Vod.Flag getActivated() {
        return mItems.get(getPosition());
    }

    public void setActivated(Vod.Flag flag) {
        for (Vod.Flag item : mItems) item.setActivated(flag);
        notifyItemRangeChanged(0, getItemCount());
    }

    public void toggle(Vod.Flag.Episode episode) {
        for (Vod.Flag item : mItems) item.toggle(item.isActivated(), episode);
    }

    public void reverse() {
        for (Vod.Flag item : mItems) Collections.reverse(item.getEpisodes());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFlagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod.Flag item = mItems.get(position);
        holder.binding.text.setText(item.getFlag());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFlagBinding binding;

        ViewHolder(@NonNull AdapterFlagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}