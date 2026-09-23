package com.fongmi.quickjs.host;

public interface Host {

    Host NONE = new Host() {
    };

    default boolean openWeb(String url, String cookieName) {
        return false;
    }

    default String getCookie(String url) {
        return "";
    }

    default boolean setCookie(String url, String cookie) {
        return false;
    }

    default String getUserAgent() {
        return "";
    }
}
