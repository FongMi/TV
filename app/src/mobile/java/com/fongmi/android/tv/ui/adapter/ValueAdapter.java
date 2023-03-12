package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.databinding.AdapterValueBinding;
import com.fongmi.android.tv.impl.FilterCallback;

import java.util.List;

public class ValueAdapter extends RecyclerView.Adapter<ValueAdapter.ViewHolder> {

    private final FilterCallback mListener;
    private final List<Filter.Value> mItems;
    private final String mKey;

    public ValueAdapter(FilterCallback listener, Filter filter) {
        this.mListener = listener;
        this.mItems = filter.getValue();
        this.mKey = filter.getKey();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterValueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Filter.Value item = mItems.get(position);
        holder.binding.text.setText(item.getN());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> onItemClick(item));
    }

    private void onItemClick(Filter.Value value) {
        for (Filter.Value item : mItems) item.setActivated(value);
        notifyItemRangeChanged(0, getItemCount());
        mListener.setFilter(mKey, value.getV());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterValueBinding binding;

        ViewHolder(@NonNull AdapterValueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}