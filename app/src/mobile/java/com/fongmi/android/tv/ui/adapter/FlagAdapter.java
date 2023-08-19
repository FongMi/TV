package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.databinding.AdapterFlagBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlagAdapter extends RecyclerView.Adapter<FlagAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Flag> mItems;

    public FlagAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Flag item, boolean force);
    }

    public void addAll(List<Flag> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Flag get(int position) {
        return mItems.get(position);
    }

    public Flag getActivated() {
        return mItems.get(getPosition());
    }

    public void setActivated(Flag flag) {
        for (Flag item : mItems) item.setActivated(flag);
        notifyItemRangeChanged(0, getItemCount());
    }

    public void toggle(Episode episode) {
        for (Flag item : mItems) item.toggle(item.isActivated(), episode);
    }

    public void reverse() {
        for (Flag item : mItems) Collections.reverse(item.getEpisodes());
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
        Flag item = mItems.get(position);
        holder.binding.text.setText(item.getShow());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item, false));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFlagBinding binding;

        ViewHolder(@NonNull AdapterFlagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}