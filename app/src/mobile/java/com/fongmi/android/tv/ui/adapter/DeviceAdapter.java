package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.databinding.AdapterDeviceBinding;

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

    @Override
    public void addItem(Device item) {
        super.addItem(item);
        sort(new Device.Sorter());
    }

    @Override
    public void addItems(List<Device> items) {
        if (items.isEmpty()) return;
        super.addItems(items);
        sort(new Device.Sorter());
    }

    @Override
    public void clear() {
        super.clear();
        Device.delete();
    }

    public List<String> getIps() {
        List<String> ips = new ArrayList<>();
        for (Device item : getItems()) if (item.isApp()) ips.add(item.getIp());
        return ips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device item = getItem(position);
        holder.binding.name.setText(item.getName());
        holder.binding.host.setText(item.getHost());
        holder.binding.type.setImageResource(getIcon(item));
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
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
