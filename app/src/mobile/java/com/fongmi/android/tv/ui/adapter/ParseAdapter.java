package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.databinding.AdapterParseBinding;

import java.util.List;

public class ParseAdapter extends RecyclerView.Adapter<ParseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Parse> mItems;

    public ParseAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = ApiConfig.get().getParses();
    }

    public interface OnClickListener {

        void onItemClick(Parse item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterParseBinding binding;

        ViewHolder(@NonNull AdapterParseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public Parse getActivated() {
        for (Parse item : mItems) if (item.isActivated()) return item;
        return null;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterParseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Parse item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }
}