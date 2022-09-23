package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

    private OnClickListener mListener;
    private final List<Class> mItems;

    public TypeAdapter() {
        this.mItems = new ArrayList<>();
    }

    public void setListener(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Class item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterTypeBinding binding;

        ViewHolder(@NonNull AdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void addAll(List<Class> items) {
        if (items.isEmpty()) return;
        mItems.clear();
        mItems.addAll(items);
        mItems.get(0).setActivated(true);
        notifyDataSetChanged();
    }

    public int setActivated(Class item) {
        int position = mItems.indexOf(item);
        setActivated(position);
        return position;
    }

    public void setActivated(int position) {
        for (Class item : mItems) item.setActivated(false);
        mItems.get(position).setActivated(true);
        notifyItemRangeChanged(0, mItems.size());
    }

    public List<Class> getTypes() {
        return mItems;
    }

    public Class get(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = mItems.get(position);
        holder.binding.text.setText(item.getTypeName());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setCompoundDrawablePadding(ResUtil.dp2px(4));
        holder.binding.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, item.getIcon(), 0);
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
    }
}