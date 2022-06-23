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

public class FuncAdapter extends RecyclerView.Adapter<FuncAdapter.FuncHolder> {

    private List<Func> items;

    public FuncAdapter() {
        addAll();
    }

    static class FuncHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterFuncBinding binding;

        public FuncHolder(@NonNull AdapterFuncBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        }
    }

    public void addAll() {
        items = new ArrayList<>();
        items.add(Func.create(R.string.home_vod));
        items.add(Func.create(R.string.home_live));
        items.add(Func.create(R.string.home_search));
        items.add(Func.create(R.string.home_push));
        items.add(Func.create(R.string.home_setting));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public FuncAdapter.FuncHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FuncHolder(AdapterFuncBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FuncAdapter.FuncHolder holder, int position) {
        Func item = items.get(position);
        holder.binding.icon.setImageResource(item.getDrawable());
        holder.binding.text.setText(item.getText());
    }
}
