package com.fongmi.android.tv.cast;

import androidx.annotation.NonNull;

import com.android.cast.dlna.core.ICast;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.FileUtil;

import java.util.UUID;

public class CastVideo implements ICast {

    private final String name;
    private final String url;

    public static CastVideo get(String name, String url) {
        return new CastVideo(name, url);
    }

    private CastVideo(String name, String url) {
        if (url.startsWith("file")) url = Server.get().getAddress() + "/" + url.replace(FileUtil.getRootPath(), "");
        this.name = name;
        this.url = url;
    }

    @NonNull
    @Override
    public String getId() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    @Override
    public String getUri() {
        return url;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }
}