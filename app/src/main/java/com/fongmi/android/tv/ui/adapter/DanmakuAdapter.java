package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.databinding.AdapterDanmakuBinding;

import java.util.ArrayList;
import java.util.List;

public class DanmakuAdapter extends RecyclerView.Adapter<DanmakuAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Danmaku> mItems;

    public DanmakuAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Danmaku item);
    }

    public void clear() {
        int size = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, size);
    }

    public DanmakuAdapter addAll(List<Danmaku> items) {
        if (items == null || items.isEmpty()) return this;
        mItems.addAll(items);
        notifyItemRangeInserted(0, mItems.size());
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isSelected()) return i;
        return 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterDanmakuBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Danmaku item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setSelected(item.isSelected());
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterDanmakuBinding binding;

        public ViewHolder(@NonNull AdapterDanmakuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }
}
