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

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Parse get(int position) {
        return mItems.get(position);
    }

    public Parse first() {
        return mItems.get(0);
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterParseBinding binding;

        ViewHolder(@NonNull AdapterParseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}