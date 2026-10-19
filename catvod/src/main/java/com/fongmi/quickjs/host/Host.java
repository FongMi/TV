package com.fongmi.quickjs.host;

import com.github.catvod.net.Net;

public interface Host extends Net.Cookies {

    Host NONE = new Host() {
    };

    default long getCallTimeout() {
        return 0;
    }

    default boolean openWeb(String options) {
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
