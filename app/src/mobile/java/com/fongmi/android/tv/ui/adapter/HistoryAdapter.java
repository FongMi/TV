package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<History> mItems;
    private int width, height;

    public HistoryAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        setLayoutSize(3);
    }

    public interface OnClickListener {

        void onItemClick(History item);

        boolean onLongClick(History item);
    }

    private void setLayoutSize(int spanCount) {
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (spanCount - 1));
        int base = ResUtil.getScreenWidthPx() - space;
        width = base / spanCount;
        height = (int) (width / 0.75f);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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
        holder.binding.getRoot().setOnClickListener(view -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(view -> mListener.onLongClick(item));
        Glide.with(App.get()).load(item.getVodPic()).centerCrop().error(R.drawable.ic_img_error).placeholder(R.drawable.ic_img_loading).into(holder.binding.image);
    }
}
