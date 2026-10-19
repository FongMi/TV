package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.databinding.AdapterDeviceBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends BaseDiffAdapter<Device, DeviceAdapter.ViewHolder> {

    private final OnClickListener listener;

    public DeviceAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Device item);

        boolean onLongClick(Device item);
    }

    public interface OnConnectListener extends OnClickListener {

        void onLogsClick(Device item);
    }

    public List<String> getIps() {
        List<String> ips = new ArrayList<>();
        for (Device item : getItems()) if (item.isApp()) ips.add(item.getIp());
        return ips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterDeviceBinding binding = AdapterDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        if (listener instanceof OnConnectListener) binding.host.setTextColor(MaterialColors.getColor(binding.host, com.google.android.material.R.attr.colorOnSurfaceVariant));
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device item = getItem(position);
        AdapterDeviceBinding binding = holder.binding;
        binding.name.setText(item.getDisplayName());
        binding.host.setText(item.getHost());
        binding.type.setImageResource(getIcon(item));
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        binding.actions.setVisibility(listener instanceof OnConnectListener ? View.VISIBLE : View.GONE);
        if (listener instanceof OnConnectListener connection) {
            binding.web.setOnClickListener(v -> listener.onItemClick(item));
            binding.logs.setOnClickListener(v -> connection.onLogsClick(item));
            binding.web.setContentDescription(binding.web.getContext().getString(R.string.device_open_web) + ": " + item.getDisplayName());
            binding.logs.setContentDescription(binding.logs.getContext().getString(R.string.device_view_logs) + ": " + item.getDisplayName());
        }
    }

    private int getIcon(Device item) {
        return item.isMobile() ? R.drawable.ic_cast_mobile : R.drawable.ic_cast_tv;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterDeviceBinding binding;

        ViewHolder(@NonNull AdapterDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
