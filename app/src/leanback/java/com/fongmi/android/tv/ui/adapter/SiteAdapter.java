package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSiteBinding;

import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Site> mItems;
    private int type;

    public SiteAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = ApiConfig.get().getSites();
    }

    public interface OnClickListener {

        void onItemClick(Site item);
    }

    public void setType(int type) {
        this.type = type;
        notifyDataSetChanged();
    }

    public void selectAll() {
        setEnable(true, true);
    }

    public void cancelAll() {
        setEnable(false, true);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.check.setChecked(getChecked(item));
        holder.binding.text.setSelected(item.isActivated());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.check.setVisibility(getVisible(item));
        holder.binding.getRoot().setOnLongClickListener(v -> setLongListener(item));
        holder.binding.getRoot().setOnClickListener(v -> setListener(item, position));
    }

    private int getVisible(Site item) {
        return type == 0 || (type == 1 && item.getSearchable() == 0) ? View.GONE : View.VISIBLE;
    }

    private boolean getChecked(Site item) {
        if (type == 1) return item.isSearchable();
        if (type == 2) return item.isChangeable();
        if (type == 3) return !item.isRecordable();
        return false;
    }

    private void setListener(Site item, int position) {
        if (type == 0) mListener.onItemClick(item);
        if (type == 1) item.setSearchable(!item.isSearchable());
        if (type == 2) item.setChangeable(!item.isChangeable());
        if (type == 3) item.setRecordable(!item.isRecordable());
        if (type != 0) notifyItemChanged(position);
    }

    private boolean setLongListener(Site item) {
        if (type == 1) setEnable(!item.isSearchable(), false);
        if (type == 2) setEnable(!item.isChangeable(), false);
        if (type == 3) setEnable(!item.isRecordable(), false);
        return true;
    }

    private void setEnable(boolean enable, boolean dynamic) {
        if (type == 1) for (Site site : ApiConfig.get().getSites()) site.setSearchable(enable).save();
        if (type == 2) for (Site site : ApiConfig.get().getSites()) site.setChangeable(enable).save();
        if (type == 3) for (Site site : ApiConfig.get().getSites()) site.setRecordable(enable).save();
        if (dynamic) notifyItemRangeChanged(0, getItemCount());
        else notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSiteBinding binding;

        ViewHolder(@NonNull AdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
