package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterSubtitleBinding;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Arrays;
import java.util.List;

public class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<String> mItems;

    public SubtitleAdapter(OnClickListener listener) {
        this.mItems = Arrays.asList(ResUtil.getStringArray(R.array.select_subtitle));
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(int position);
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
        String item = mItems.get(position);
        holder.binding.text.setText(item);
        holder.binding.text.setActivated(Prefers.getSubtitle() == position);
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSubtitleBinding binding;

        public ViewHolder(@NonNull AdapterSubtitleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
