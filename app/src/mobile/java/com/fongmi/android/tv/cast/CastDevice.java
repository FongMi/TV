package com.fongmi.android.tv.cast;

import org.fourthline.cling.model.meta.Device;

import java.util.ArrayList;
import java.util.List;

public class CastDevice {

    private final List<Device<?, ?, ?>> devices;

    private static class Loader {
        static volatile CastDevice INSTANCE = new CastDevice();
    }

    public static CastDevice get() {
        return Loader.INSTANCE;
    }

    public CastDevice() {
        this.devices = new ArrayList<>();
    }

    public boolean isEmpty() {
        return devices.isEmpty();
    }

    private com.fongmi.android.tv.bean.Device create(Device<?, ?, ?> item) {
        com.fongmi.android.tv.bean.Device device = new com.fongmi.android.tv.bean.Device();
        device.setUuid(item.getIdentity().getUdn().getIdentifierString());
        device.setName(item.getDetails().getFriendlyName());
        device.setType(2);
        return device;
    }

    public List<com.fongmi.android.tv.bean.Device> getAll() {
        List<com.fongmi.android.tv.bean.Device> items = new ArrayList<>();
        for (Device<?, ?, ?> item : devices) items.add(create(item));
        return items;
    }

    public List<com.fongmi.android.tv.bean.Device> add(Device<?, ?, ?> item) {
        devices.remove(item);
        devices.add(item);
        return getAll();
    }

    public com.fongmi.android.tv.bean.Device remove(Device<?, ?, ?> device) {
        devices.remove(device);
        return create(device);
    }

    public Device<?, ?, ?> find(com.fongmi.android.tv.bean.Device item) {
        for (Device<?, ?, ?> device : devices) if (device.getIdentity().getUdn().getIdentifierString().equals(item.getUuid())) return device;
        return null;
    }
}
