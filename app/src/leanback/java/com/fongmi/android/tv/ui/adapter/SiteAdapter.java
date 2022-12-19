package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSearchSiteBinding;

import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private final List<Site> mItems;

    public SiteAdapter() {
        this.mItems = ApiConfig.get().getSites();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSearchSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        holder.binding.site.setText((item.isSearchable() ? "√ " : "").concat(item.getName()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final AdapterSearchSiteBinding binding;

        public ViewHolder(@NonNull AdapterSearchSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Site item = mItems.get(getLayoutPosition());
            item.setSearchable(!item.isSearchable()).save();
            notifyItemChanged(getLayoutPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            Site item = mItems.get(getLayoutPosition());
            boolean result = !item.isSearchable();
            for (Site site : mItems) site.setSearchable(result).save();
            notifyItemRangeChanged(0, mItems.size());
            return true;
        }
    }
}
