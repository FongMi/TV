package com.fongmi.android.tv.cast;

import com.android.cast.dlna.dmc.DLNACastManager;
import com.fongmi.android.tv.bean.Device;

import java.util.ArrayList;
import java.util.List;

public class CastDevice {

    private final List<org.fourthline.cling.model.meta.Device<?, ?, ?>> devices;

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

    private Device create(org.fourthline.cling.model.meta.Device<?, ?, ?> item) {
        Device device = new Device();
        device.setUuid(item.getIdentity().getUdn().getIdentifierString());
        device.setName(item.getDetails().getFriendlyName());
        device.setType(2);
        return device;
    }

    public List<com.fongmi.android.tv.bean.Device> getAll() {
        List<com.fongmi.android.tv.bean.Device> items = new ArrayList<>();
        for (org.fourthline.cling.model.meta.Device<?, ?, ?> item : devices) items.add(create(item));
        return items;
    }

    public List<com.fongmi.android.tv.bean.Device> add(org.fourthline.cling.model.meta.Device<?, ?, ?> item) {
        devices.remove(item);
        devices.add(item);
        return getAll();
    }

    public Device remove(org.fourthline.cling.model.meta.Device<?, ?, ?> device) {
        devices.remove(device);
        return create(device);
    }

    public void disconnect() {
        for (org.fourthline.cling.model.meta.Device<?, ?, ?> device : devices) DLNACastManager.INSTANCE.disconnectDevice(device);
    }

    public org.fourthline.cling.model.meta.Device<?, ?, ?> find(com.fongmi.android.tv.bean.Device item) {
        for (org.fourthline.cling.model.meta.Device<?, ?, ?> device : devices) if (device.getIdentity().getUdn().getIdentifierString().equals(item.getUuid())) return device;
        return null;
    }
}
