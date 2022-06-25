package com.fongmi.bear.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.databinding.AdapterFuncBinding;

import java.util.ArrayList;
import java.util.List;

public class FuncAdapter extends RecyclerView.Adapter<FuncAdapter.ViewHolder> {

    private OnItemClickListener mListener;
    private List<Func> mItems;

    public FuncAdapter() {
        addAll();
    }

    public interface OnItemClickListener {
        void onItemClick(Func item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterFuncBinding binding;

        public ViewHolder(@NonNull AdapterFuncBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }

    public void addAll() {
        mItems = new ArrayList<>();
        mItems.add(Func.create(R.string.home_vod));
        mItems.add(Func.create(R.string.home_live));
        mItems.add(Func.create(R.string.home_search));
        mItems.add(Func.create(R.string.home_push));
        mItems.add(Func.create(R.string.home_setting));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public FuncAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFuncBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FuncAdapter.ViewHolder holder, int position) {
        Func item = mItems.get(position);
        holder.binding.icon.setImageResource(item.getDrawable());
        holder.binding.text.setText(item.getText());
    }
}
