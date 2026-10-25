package com.fongmi.quickjs.host;

import com.github.catvod.net.Net;

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
        CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new UnsupportedOperationException("Dialog requires a compatible App"));
        return result;
    }

    default CompletableFuture<String> dialog(String options, Function<String, CompletableFuture<String>> onAction) {
        CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new UnsupportedOperationException("Dialog actions require a compatible App"));
        return result;
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
