package com.fongmi.bear.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.bear.bean.Site;
import com.fongmi.bear.databinding.AdapterSiteBinding;

import java.util.ArrayList;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private OnItemClickListener mListener;
    private List<Site> mItems;

    public SiteAdapter() {
        mItems = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Site item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterSiteBinding binding;

        public ViewHolder(@NonNull AdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Site item = mItems.get(getLayoutPosition());
            setHome(getLayoutPosition());
            mListener.onItemClick(item);
        }
    }

    private void setHome(int position) {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setHome(i == position);
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<Site> items) {
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
    public SiteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SiteAdapter.ViewHolder holder, int position) {
        Site item = mItems.get(position);
        holder.itemView.setSelected(item.isHome());
        holder.binding.name.setText(item.getName());
        if (item.isHome()) holder.itemView.requestFocus();
        else holder.itemView.clearFocus();
    }
}
