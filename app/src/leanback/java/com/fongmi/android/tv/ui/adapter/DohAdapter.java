package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.databinding.AdapterDohBinding;
import com.github.catvod.bean.Doh;

import java.util.List;

public class DohAdapter extends RecyclerView.Adapter<DohAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Doh> mItems;
    private int select;

    public DohAdapter(OnClickListener listener) {
        this.mItems = ApiConfig.get().getDoh();
        this.mListener = listener;
    }

    public void setSelect(int select) {
        this.select = select;
    }

    public int getSelect() {
        return select;
    }

    public interface OnClickListener {

        void onItemClick(Doh item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterDohBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doh item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setActivated(select == position);
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterDohBinding binding;

        public ViewHolder(@NonNull AdapterDohBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
