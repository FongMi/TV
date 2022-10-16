package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterLiveGroupBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private final List<Group> mItems;
    private final List<Group> mHides;

    public GroupAdapter() {
        this.mItems = new ArrayList<>();
        this.mHides = new ArrayList<>();
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        addGroup(items);
        notifyDataSetChanged();
    }

    private void addGroup(List<Group> items) {
        for (Group item : items) if (item.isHidden()) mHides.add(item);else mItems.add(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterLiveGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.icon.setVisibility(item.getVisible());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterLiveGroupBinding binding;

        public ViewHolder(@NonNull AdapterLiveGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
