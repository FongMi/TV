package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class VodAdapter extends RecyclerView.Adapter<VodAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Vod> mItems;
    private int width, height;

    public VodAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        setLayoutSize(3);
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    private void setLayoutSize(int spanCount) {
        int space = ResUtil.dp2px(32 + ((spanCount - 1) * 16));
        int base = ResUtil.getScreenWidthPx() - space;
        width = base / spanCount;
        height = (int) (width / 0.75f);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }

    public void addAll(List<Vod> items) {
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
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.year.setText(item.getVodYear());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.year.setVisibility(item.getYearVisible());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
    }
}
