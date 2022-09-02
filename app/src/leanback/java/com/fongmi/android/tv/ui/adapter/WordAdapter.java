package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterWordBinding;

import java.util.ArrayList;
import java.util.List;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<String> mItems;

    public WordAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(String item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterWordBinding binding;

        public ViewHolder(@NonNull AdapterWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }

    public void addAll(List<String> items) {
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterWordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.text.setText(mItems.get(position));
    }
}
