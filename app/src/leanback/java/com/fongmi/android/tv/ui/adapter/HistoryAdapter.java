package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
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
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        setLayoutSize();
    }

    private void setLayoutSize() {
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (Product.getColumn() - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / Product.getColumn();
        height = (int) (width / 0.75f);
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
        notifyItemRangeChanged(0, mItems.size());
    }

    public void addAll(List<History> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        setDelete(false);
        notifyDataSetChanged();
        History.delete(VodConfig.getCid());
    }

    public int delete(History item) {
        int index = mItems.indexOf(item);
        if (index == -1) return index;
        mItems.remove(index);
        notifyItemRemoved(index);
        return index;
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
        setFocusListener(holder.binding);
        setClickListener(holder.itemView, item);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
        ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
    }

    private void setFocusListener(AdapterVodBinding binding) {
        binding.getRoot().setOnFocusChangeListener((v, hasFocus) -> binding.name.setSelected(hasFocus));
    }

    private void setClickListener(View root, History item) {
        root.setOnLongClickListener(view -> mListener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
