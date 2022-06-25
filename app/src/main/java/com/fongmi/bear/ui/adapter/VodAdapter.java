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

public class VodAdapter extends RecyclerView.Adapter<VodAdapter.ViewHolder> {

    private OnItemClickListener mListener;
    private List<Vod> mItems;

    public VodAdapter() {
        mItems = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Vod item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }

    public void addAll(List<Vod> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public VodAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VodAdapter.ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        Utils.loadImage(item.getVodPic(), holder.binding.image);
    }
}
