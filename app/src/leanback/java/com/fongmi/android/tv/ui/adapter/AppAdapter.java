package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Info;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.AdapterAppBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Info> mItems;
    private int[] size;

    public AppAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        setSize();
    }

    public interface OnClickListener {

        void onItemClick(Info item);

        boolean onLongClick(Info item);
    }

    private void setSize() {
        int column = 5;
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        this.size = Product.getSpec(space, column, Style.rect());
    }

    public void add(Info item) {
        if (item.getPack().equals(App.get().getPackageName())) {
            mItems.add(0, item);
            notifyItemInserted(0);
        } else {
            mItems.add(item);
            notifyItemInserted(mItems.size() - 1);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)).size(size);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Info item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.icon.setImageDrawable(item.getIcon());
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(v -> mListener.onLongClick(item));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterAppBinding binding;

        public ViewHolder(@NonNull AdapterAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ViewHolder size(int[] size) {
            binding.getRoot().getLayoutParams().width = size[0];
            return this;
        }
    }
}
