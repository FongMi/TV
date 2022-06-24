package com.fongmi.bear.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.AdapterVodBinding;
import com.fongmi.bear.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<VodAdapter.VodHolder> {

    private OnItemClickListener listener;
    private List<Vod> items;

    public VodAdapter() {
        items = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Vod item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    class VodHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterVodBinding binding;

        public VodHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(items.get(getLayoutPosition()));
        }
    }

    public void addAll(List<Vod> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public VodAdapter.VodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VodHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VodAdapter.VodHolder holder, int position) {
        Vod item = items.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        Utils.loadImage(item.getVodPic(), holder.binding.image);
    }
}
