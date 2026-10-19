package com.github.catvod.runtime;

import android.content.Context;
import android.content.Intent;

import com.github.catvod.crawler.Spider;
import com.google.gson.JsonObject;

import java.io.IOException;

public interface NodeRuntimeApi extends RuntimePlugin {

    String SERVICE_CLASS = "com.fongmi.android.tv.api.node.NodeService";
    String SERVICE_ACTION = "com.fongmi.nodejs.START";

    Client create(Context context, Callback callback);

    void startProcess(Context context, Intent intent);

    interface Client {

        LoadedConfig loadConfig(String source) throws Exception;

        void check() throws Exception;

        void clear();

        void accept(LoadedConfig loaded) throws IOException;

        void prune(LoadedConfig loaded, Iterable<String> sources, String currentSource) throws IOException;

        boolean fail(LoadedConfig loaded);

        void cancelLoad();

        Spider spider(String api);
    }

    record LoadedConfig(String json, long session) {
    }

    interface Callback {

        String onMessage(String action, JsonObject options);

        void onPlayInfo(String flag, String id);
    }
}
