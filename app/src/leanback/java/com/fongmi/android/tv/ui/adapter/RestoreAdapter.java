package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterRestoreBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.github.catvod.utils.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RestoreAdapter extends RecyclerView.Adapter<RestoreAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<File> mItems;

    public RestoreAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.listener = listener;
        this.addAll();
    }

    public interface OnClickListener {

        void onItemClick(File item);

        void onDeleteClick(File item);
    }

    public void addAll() {
        mItems.addAll(BackupManager.getFiles());
        notifyDataSetChanged();
    }

    public int remove(File item) {
        int position = mItems.indexOf(item);
        if (position == -1) return -1;
        Path.clear(item);
        mItems.remove(position);
        notifyItemRemoved(position);
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterRestoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setOnClickListener(v -> listener.onItemClick(item));
        holder.binding.delete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterRestoreBinding binding;

        public ViewHolder(@NonNull AdapterRestoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
