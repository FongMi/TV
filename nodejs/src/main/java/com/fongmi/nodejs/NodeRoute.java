package com.fongmi.nodejs;

public final class NodeRoute {

    private static final String PREFIX = "node:";

    private NodeRoute() {
    }

    public static String append(String api, String endpoint) {
        if (api == null || !api.startsWith(PREFIX)) throw new IllegalArgumentException("Invalid Node API");
        String value = api.substring(PREFIX.length());
        int fragment = value.indexOf('#');
        if (fragment >= 0) value = value.substring(0, fragment);
        int query = value.indexOf('?');
        String suffix = query < 0 ? "" : value.substring(query);
        String path = query < 0 ? value : value.substring(0, query);
        if (!path.startsWith("/")) path = "/" + path;
        while (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        String child = endpoint == null ? "" : endpoint.trim();
        if (!child.isEmpty() && !child.startsWith("/")) child = "/" + child;
        if (path.equals("/") && !child.isEmpty()) path = "";
        return PREFIX + path + child + suffix;
    }

    public static String path(String api) {
        if (api == null || !api.startsWith(PREFIX)) throw new IllegalArgumentException("Invalid Node API");
        String route = api.substring(PREFIX.length());
        return route.startsWith("/") ? route : "/" + route;
    }
}
