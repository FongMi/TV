package com.fongmi.android.tv.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.AdapterDisplayBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class DisplayAdapter extends RecyclerView.Adapter<DisplayAdapter.ViewHolder> {

    private List<String> mItems;

    public DisplayAdapter() {
        mItems = new ArrayList<>();
        mItems.add(ResUtil.getString(R.string.play_time));
        mItems.add(ResUtil.getString(R.string.play_netspeed));
        mItems.add(ResUtil.getString(R.string.play_duration));
        mItems.add(ResUtil.getString(R.string.play_video_title));
        mItems.add(ResUtil.getString(R.string.play_mini_progress));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterDisplayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mItems.get(position);
        holder.binding.text.setText(name);
        holder.binding.check.setChecked(getChecked(position));
        holder.binding.select.setOnLongClickListener(v -> onItemLongClick(position));
        holder.binding.select.setOnClickListener(v -> onItemClick(position));
        holder.binding.text.setGravity(Gravity.CENTER);
    }

    private boolean getChecked(int position) {
        if (position == 0) return Setting.isDisplayTime();
        else if (position == 1) return Setting.isDisplaySpeed();
        else if (position == 2) return Setting.isDisplayDuration();
        else if (position == 3) return Setting.isDisplayVideoTitle();
        else if (position == 4) return Setting.isDisplayMiniProgress();
        return false;
    }

    private void onItemClick(int position) {
        if (position == 0) Setting.putDisplayTime(!Setting.isDisplayTime());
        else if (position == 1) Setting.putDisplaySpeed(!Setting.isDisplaySpeed());
        else if (position == 2) Setting.putDisplayDuration(!Setting.isDisplayDuration());
        else if (position == 3) Setting.putDisplayVideoTitle(!Setting.isDisplayVideoTitle());
        else if (position == 4) Setting.putDisplayMiniProgress(!Setting.isDisplayMiniProgress());
        notifyItemRangeChanged(0, getItemCount());
    }

    private boolean onItemLongClick(int position) {
        boolean checked = false;
        if (position == 0) checked = Setting.isDisplayTime();
        else if (position == 1) checked = Setting.isDisplaySpeed();
        else if (position == 2) checked = Setting.isDisplayDuration();
        else if (position == 3) checked = Setting.isDisplayVideoTitle();
        else if (position == 4) checked = Setting.isDisplayMiniProgress();
        Setting.putDisplayTime(!checked);
        Setting.putDisplaySpeed(!checked);
        Setting.putDisplayDuration(!checked);
        Setting.putDisplayVideoTitle(!checked);
        Setting.putDisplayMiniProgress(!checked);
        notifyItemRangeChanged(0, getItemCount());
        return true;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterDisplayBinding binding;

        ViewHolder(@NonNull AdapterDisplayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
