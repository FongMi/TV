package com.fongmi.quickjs.host;

import com.github.catvod.net.Net;
import com.github.catvod.utils.Task;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface Host extends Net.Cookies {

    Host NONE = new Host() {
    };

    default long getCallTimeout() {
        return 0;
    }

    default boolean openWeb(String options) {
        return false;
    }

    default CompletableFuture<String> dialog(String options) {
        return Task.failedFuture(new UnsupportedOperationException("Dialog requires a compatible App"));
    }

    default CompletableFuture<String> dialog(String options, Function<String, CompletableFuture<String>> callback) {
        return Task.failedFuture(new UnsupportedOperationException("Dialog actions require a compatible App"));
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
