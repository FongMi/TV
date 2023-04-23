package com.fongmi.android.tv.cast;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter;

import org.fourthline.cling.model.meta.Device;

import java.util.ArrayList;
import java.util.List;

public class CastDevice {

    private final List<Device<?, ?, ?>> devices;
    private final List<MediaRouter.RouteInfo> routers;

    private static class Loader {
        static volatile CastDevice INSTANCE = new CastDevice();
    }

    public static CastDevice get() {
        return Loader.INSTANCE;
    }

    public CastDevice() {
        this.devices = new ArrayList<>();
        this.routers = new ArrayList<>();
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

    private com.fongmi.android.tv.bean.Device create(MediaRouter.RouteInfo item) {
        com.fongmi.android.tv.bean.Device device = new com.fongmi.android.tv.bean.Device();
        device.setName(item.getName());
        device.setUuid(item.getId());
        device.setType(3);
        return device;
    }

    public List<com.fongmi.android.tv.bean.Device> getDLNA() {
        List<com.fongmi.android.tv.bean.Device> items = new ArrayList<>();
        for (Device<?, ?, ?> item : devices) items.add(create(item));
        return items;
    }

    public List<com.fongmi.android.tv.bean.Device> getCast() {
        List<com.fongmi.android.tv.bean.Device> items = new ArrayList<>();
        for (MediaRouter.RouteInfo item : routers) items.add(create(item));
        return items;
    }

    public List<com.fongmi.android.tv.bean.Device> add(Device<?, ?, ?> item) {
        devices.remove(item);
        devices.add(item);
        return getDLNA();
    }

    @SuppressLint("RestrictedApi")
    public List<com.fongmi.android.tv.bean.Device> add(List<MediaRouter.RouteInfo> items) {
        ArrayList<MediaRouter.RouteInfo> routes = new ArrayList<>(items);
        onFilterRoutes(routes);
        routers.clear();
        routers.addAll(routes);
        return getCast();
    }

    public com.fongmi.android.tv.bean.Device remove(Device<?, ?, ?> device) {
        devices.remove(device);
        return create(device);
    }

    public Device<?, ?, ?> findDLNA(com.fongmi.android.tv.bean.Device item) {
        for (Device<?, ?, ?> device : devices) if (device.getIdentity().getUdn().getIdentifierString().equals(item.getUuid())) return device;
        return null;
    }

    public MediaRouter.RouteInfo findCast(com.fongmi.android.tv.bean.Device item) {
        for (MediaRouter.RouteInfo device : routers) if (device.getId().equals(item.getUuid())) return device;
        return null;
    }

    private void onFilterRoutes(@NonNull List<MediaRouter.RouteInfo> routes) {
        for (int i = routes.size(); i-- > 0; ) if (!onFilterRoute(routes.get(i))) routes.remove(i);
    }

    @SuppressLint("RestrictedApi")
    private boolean onFilterRoute(@NonNull MediaRouter.RouteInfo route) {
        return !route.isDefaultOrBluetooth() && route.isEnabled();
    }
}
