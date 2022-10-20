package com.fongmi.android.tv.ui.adapter.holder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterLiveGroupBinding;
import com.fongmi.android.tv.ui.adapter.GroupAdapter;

public class GroupHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final AdapterLiveGroupBinding binding;
    private final GroupAdapter adapter;

    public GroupHolder(GroupAdapter adapter, AdapterLiveGroupBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        this.adapter = adapter;
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        adapter.setPosition(getLayoutPosition());
        adapter.setSelected();
        adapter.setType();
    }

    public void setView(Group item) {
        itemView.setSelected(item.isSelect());
        binding.name.setText(item.getName());
        binding.icon.setVisibility(item.getVisible());
    }
}
