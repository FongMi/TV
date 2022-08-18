package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<History> mItems;
    private int width, height;
    private boolean delete;

    public HistoryAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        setLayoutSize(3);
    }

    public interface OnClickListener {

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    private void setLayoutSize(int spanCount) {
        int space = ResUtil.dp2px(32 + ((spanCount - 1) * 16));
        int base = ResUtil.getScreenWidthPx() - space;
        width = base / spanCount;
        height = (int) (width / 0.75f);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
            binding.getRoot().setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            History item = mItems.get(getLayoutPosition());
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        }

        @Override
        public boolean onLongClick(View v) {
            return mListener.onLongClick();
        }
    }

    public void addAll(List<History> items) {
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
        History item = mItems.get(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(ApiConfig.getSiteName(item.getSiteKey()));
        holder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
        holder.binding.name.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.site.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        ImgUtil.load(item.getVodPic(), holder.binding.image);
    }
}
