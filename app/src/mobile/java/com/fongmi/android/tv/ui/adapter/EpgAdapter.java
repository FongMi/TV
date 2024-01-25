package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.databinding.AdapterEpgBinding;

import java.util.ArrayList;
import java.util.List;

public class EpgAdapter extends RecyclerView.Adapter<EpgAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Epg> mItems;

    public EpgAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Epg item);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Epg> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isInRange()) return i;
        return 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpgBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Epg item = mItems.get(position);
        holder.binding.time.setText(item.getTime());
        holder.binding.title.setText(item.getTitle());
        holder.binding.getRoot().setSelected(item.isInRange());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpgBinding binding;

        ViewHolder(@NonNull AdapterEpgBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
