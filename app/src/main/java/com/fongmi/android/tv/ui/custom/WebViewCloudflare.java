package com.fongmi.android.tv.ui.custom;

import android.net.Uri;
import android.webkit.WebResourceResponse;

import java.util.Map;

final class WebViewCloudflare {

    static final String CHECK_SCRIPT = "Boolean(window._cf_chl_opt && window._cf_chl_opt.cType)";

    private static final String CHALLENGE_PATH = "/cdn-cgi/challenge-platform/";
    private static final String TURNSTILE_HOST = "challenges.cloudflare.com";

    static boolean isResource(Uri uri) {
        return isTurnstileHost(uri) || uri.getPath() != null && uri.getPath().contains(CHALLENGE_PATH);
    }

    static boolean isTurnstile(Uri uri) {
        return isTurnstileHost(uri) && uri.getPath() != null && uri.getPath().startsWith("/turnstile/");
    }

    static boolean isChallenge(WebResourceResponse response) {
        if (response == null || response.getResponseHeaders() == null) return false;
        for (Map.Entry<String, String> entry : response.getResponseHeaders().entrySet()) {
            if ("cf-mitigated".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && "challenge".equalsIgnoreCase(entry.getValue().trim())) return true;
        }
        return false;
    }

    private static boolean isTurnstileHost(Uri uri) {
        return TURNSTILE_HOST.equalsIgnoreCase(uri.getHost());
    }

    private WebViewCloudflare() {
    }
}
