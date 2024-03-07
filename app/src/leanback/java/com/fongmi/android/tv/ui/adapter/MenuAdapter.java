package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.databinding.AdapterMenuBinding;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private final MenuAdapter.OnClickListener mListener;
    private List<String> mItems;

    public MenuAdapter(OnClickListener listener, List<String> items) {
        this.mListener = listener;
        this.mItems = items;
    }

    public interface OnClickListener {

        void onItemClick(int position);

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterMenuBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.text.setText(mItems.get(position));
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterMenuBinding binding;

        public ViewHolder(@NonNull AdapterMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
