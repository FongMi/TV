package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterRestoreBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.fongmi.android.tv.utils.Formatters;
import com.github.catvod.utils.Path;

import java.io.File;
import java.time.Instant;
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

    private void addAll() {
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
        holder.binding.name.setText(item.getName());
        holder.binding.time.setText(Formatters.TIME_SEC.format(Instant.ofEpochMilli(item.lastModified())));
        holder.binding.delete.setOnClickListener(v -> listener.onDeleteClick(item));
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterRestoreBinding binding;

        ViewHolder(@NonNull AdapterRestoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
