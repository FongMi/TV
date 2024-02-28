package com.github.catvod.bean;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class WrappedCookie {

    private final Cookie cookie;

    public static WrappedCookie wrap(Cookie cookie) {
        return new WrappedCookie(cookie);
    }

    private WrappedCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Cookie unwrap() {
        return cookie;
    }

    public boolean isExpired() {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    public boolean matches(HttpUrl url) {
        return cookie.matches(url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WrappedCookie)) return false;
        WrappedCookie it = (WrappedCookie) obj;
        return cookie.name().equals(it.cookie.name()) && cookie.domain().equals(it.cookie.domain()) && cookie.path().equals(it.cookie.path()) && cookie.secure() == it.cookie.secure() && cookie.hostOnly() == it.cookie.hostOnly();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + cookie.name().hashCode();
        result = 31 * result + cookie.domain().hashCode();
        result = 31 * result + cookie.path().hashCode();
        result = 31 * result + (cookie.secure() ? 0 : 1);
        result = 31 * result + (cookie.hostOnly() ? 0 : 1);
        return result;
    }
}
