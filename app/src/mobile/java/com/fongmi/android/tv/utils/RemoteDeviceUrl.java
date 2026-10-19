package com.fongmi.android.tv.utils;

import okhttp3.HttpUrl;

public final class RemoteDeviceUrl {

    private RemoteDeviceUrl() {
    }

    public static String origin(String address) {
        return parse(address).newBuilder().encodedPath("/").query(null).fragment(null).build().toString();
    }

    public static String page(String address, String path) {
        return parse(address).newBuilder().encodedPath(path).query(null).fragment(null).build().toString();
    }

    public static boolean isLogPage(String address) {
        return parse(address).encodedPath().equals("/logs.html");
    }

    private static HttpUrl parse(String address) {
        HttpUrl url = address == null ? null : HttpUrl.parse(address);
        if (url == null || !url.username().isEmpty() || !url.password().isEmpty()) throw new IllegalArgumentException("Invalid device address");
        return url;
    }
}
