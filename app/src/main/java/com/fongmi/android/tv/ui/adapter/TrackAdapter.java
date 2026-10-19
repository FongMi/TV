package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.AdapterTrackBinding;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<TrackItem> mItems;

    public TrackAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Track item);
    }

    public TrackAdapter addAll(List<TrackItem> items) {
        mItems.addAll(items);
        notifyDataSetChanged();
        return this;
    }

    public int getSelected() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).track().isSelected()) return i;
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
        TrackItem item = mItems.get(position);
        boolean selected = item.track().isSelected();
        holder.binding.getRoot().setSelected(selected);
        holder.binding.text.setText(item.track().getName());
        holder.binding.text.setSelected(selected || holder.itemView.hasFocus());
        bindRole(holder.binding.role, item.role());
    }

    private static void bindRole(MaterialTextView view, @StringRes int role) {
        view.setVisibility(role == 0 ? View.GONE : View.VISIBLE);
        if (role == 0) return;
        boolean primary = role == R.string.subtitle_primary_role;
        int container = primary ? com.google.android.material.R.attr.colorPrimaryContainer : com.google.android.material.R.attr.colorTertiaryContainer;
        int content = primary ? com.google.android.material.R.attr.colorOnPrimaryContainer : com.google.android.material.R.attr.colorOnTertiaryContainer;
        view.setText(role);
        view.setTextColor(MaterialColors.getColor(view, content));
        view.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(view, container)));
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterTrackBinding binding;

        public ViewHolder(@NonNull AdapterTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnFocusChangeListener((view, hasFocus) -> binding.text.setSelected(hasFocus || view.isSelected()));
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(mItems.get(getLayoutPosition()).track().toggle());
        }
    }

    public record TrackItem(Track track, @StringRes int role) {
    }
}
