package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterLiveChannelBinding;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    private final List<Channel> mItems;
    private Group group;

    public ChannelAdapter() {
        this.mItems = new ArrayList<>();
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void addAll(Group group) {
        setGroup(group);
        mItems.clear();
        mItems.addAll(group.getChannel());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterLiveChannelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.number.setText(item.getNumber());
        holder.binding.icon.setVisibility(item.getVisible());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterLiveChannelBinding binding;

        public ViewHolder(@NonNull AdapterLiveChannelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}