package com.fongmi.android.tv.cast;

import org.greenrobot.eventbus.EventBus;

public class ScanEvent {

    private final String address;

    public static void post(String address) {
        EventBus.getDefault().post(new ScanEvent(address));
    }

    public ScanEvent(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
