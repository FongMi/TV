package com.fongmi.android.tv.ui.adapter.holder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.AdapterChannelBinding;
import com.fongmi.android.tv.ui.adapter.ChannelAdapter;

public class ChannelHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

	private final AdapterChannelBinding binding;
	private final ChannelAdapter adapter;

	public ChannelHolder(ChannelAdapter adapter, @NonNull AdapterChannelBinding binding) {
		super(binding.getRoot());
		this.binding = binding;
		this.adapter = adapter;
		itemView.setOnClickListener(this);
		itemView.setOnLongClickListener(this);
	}

	@Override
	public void onClick(View view) {
		adapter.setPosition(getLayoutPosition());
		adapter.setChannel();
	}

	@Override
	public boolean onLongClick(View view) {
		adapter.setPosition(getLayoutPosition());
		return false;
	}

	public void setView(Channel item) {
		itemView.setSelected(item.isSelect());
		binding.name.setText(item.getName());
		binding.number.setText(item.getNumber());
		binding.icon.setVisibility(item.getVisible());
	}
}
