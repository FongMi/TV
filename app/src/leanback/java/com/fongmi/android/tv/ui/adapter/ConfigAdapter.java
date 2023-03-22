package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.AdapterConfigBinding;

import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private List<Config> mItems;

    public ConfigAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onTextClick(Config item);

        void onDeleteClick(Config item);
    }

    public ConfigAdapter addAll(int type) {
        mItems = Config.getAll(type);
        mItems.remove(type == 0 ? ApiConfig.get().getConfig() : LiveConfig.get().getConfig());
        return this;
    }

    public int remove(Config item) {
        item.delete();
        mItems.remove(item);
        notifyDataSetChanged();
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterConfigBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mItems.get(position);
        holder.binding.text.setText(item.getDesc());
        holder.binding.text.setOnClickListener(v -> mListener.onTextClick(item));
        holder.binding.delete.setOnClickListener(v -> mListener.onDeleteClick(item));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterConfigBinding binding;

        public ViewHolder(@NonNull AdapterConfigBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
