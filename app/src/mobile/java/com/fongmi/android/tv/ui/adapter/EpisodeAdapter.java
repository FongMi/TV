package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod.Flag.Episode> mItems;

    public EpisodeAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Vod.Flag.Episode item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpisodeBinding binding;

        ViewHolder(@NonNull AdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void addAll(List<Vod.Flag.Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
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

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod.Flag.Episode item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        //holder.binding.text.setMaxEms(ResUtil.getEms());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }
}