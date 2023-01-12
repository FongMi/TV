package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.AdapterTrackBinding;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Track> mItems;

    public TrackAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Track item);
    }

    public TrackAdapter addAll(List<Track> items) {
        mItems.addAll(items);
        notifyDataSetChanged();
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
        return new ViewHolder(AdapterTrackBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setActivated(item.isSelected());
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterTrackBinding binding;

        public ViewHolder(@NonNull AdapterTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()).toggle());
        }
    }
}
