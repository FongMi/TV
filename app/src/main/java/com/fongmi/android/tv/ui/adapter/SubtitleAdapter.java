package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.SubtitleSearchItem;
import com.fongmi.android.tv.databinding.AdapterSubtitleBinding;

import java.util.ArrayList;
import java.util.List;

public class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<SubtitleSearchItem> mItems;

    public SubtitleAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(SubtitleSearchItem item);
    }

    public void clear() {
        int size = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, size);
    }

    public List<SubtitleSearchItem> getItems() {
        return new ArrayList<>(mItems);
    }

    public void setItems(List<SubtitleSearchItem> items) {
        clear();
        addAll(items);
    }

    public void addAll(List<SubtitleSearchItem> items) {
        if (items == null || items.isEmpty()) return;
        int start = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSubtitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubtitleSearchItem item = mItems.get(position);
        holder.binding.text.setText(item.getText());
        holder.binding.text.setSelected(false);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterSubtitleBinding binding;

        public ViewHolder(@NonNull AdapterSubtitleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) listener.onItemClick(mItems.get(position));
        }
    }
}
