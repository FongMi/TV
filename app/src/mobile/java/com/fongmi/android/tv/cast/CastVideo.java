package com.fongmi.android.tv.cast;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;

public class CastVideo {

    private final String name;
    private final String url;

    public static CastVideo get(String name, String url) {
        return new CastVideo(name, url);
    }

    private CastVideo(String name, String url) {
        if (url.startsWith("file")) url = Server.get().getAddress() + "/" + url.replace(Path.rootPath(), "");
        if (url.contains("127.0.0.1")) url = url.replace("127.0.0.1", Util.getIp());
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}