package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.databinding.AdapterParseDarkBinding;
import com.fongmi.android.tv.databinding.AdapterParseLightBinding;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.List;

public class ParseAdapter extends RecyclerView.Adapter<ParseAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Parse> mItems;
    private final int viewType;

    public ParseAdapter(OnClickListener listener, int viewType) {
        this.mItems = ApiConfig.get().getParses();
        this.mListener = listener;
        this.viewType = viewType;
    }

    public interface OnClickListener {

        void onItemClick(Parse item);
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public Parse get(int position) {
        return mItems.get(position);
    }

    public Parse first() {
        return mItems.get(0);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.DARK) return new ViewHolder(AdapterParseDarkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        return new ViewHolder(AdapterParseLightBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Parse item = mItems.get(position);
        if (holder.darkBinding != null) holder.initView(holder.darkBinding.text, item);
        if (holder.lightBinding != null) holder.initView(holder.lightBinding.text, item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private AdapterParseDarkBinding darkBinding;
        private AdapterParseLightBinding lightBinding;

        ViewHolder(@NonNull AdapterParseDarkBinding binding) {
            super(binding.getRoot());
            this.darkBinding = binding;
        }

        ViewHolder(@NonNull AdapterParseLightBinding binding) {
            super(binding.getRoot());
            this.lightBinding = binding;
        }

        void initView(TextView view, Parse item) {
            view.setText(item.getName());
            view.setActivated(item.isActivated());
            view.setOnClickListener(v -> mListener.onItemClick(item));
        }
    }
}