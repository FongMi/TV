package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.databinding.AdapterFilterBinding;
import com.fongmi.android.tv.impl.FilterCallback;

import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    private final FilterCallback mListener;
    private final List<Filter> mItems;

    public FilterAdapter(FilterCallback listener, List<Filter> items) {
        this.mListener = listener;
        this.mItems = items;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Filter item = mItems.get(position);
        holder.binding.recycler.setHasFixedSize(true);
        holder.binding.recycler.setItemAnimator(null);
        holder.binding.recycler.setAdapter(new ValueAdapter(mListener, item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFilterBinding binding;

        ViewHolder(@NonNull AdapterFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}