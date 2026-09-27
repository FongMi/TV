package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.nodejs.NodeSpider;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.crawler.SpiderNull;

import java.util.concurrent.ConcurrentHashMap;

public final class NodeLoader {

    private final ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();

    public Spider getSpider(String key, String api, String ext) {
        if (!api.startsWith("node:")) return new SpiderNull();
        Spider spider = spiders.computeIfAbsent(key + '\u0000' + api, ignored -> {
            try {
                NodeSpider created = new NodeSpider(NodeRuntime.get().client(), api);
                created.siteKey = key;
                created.init(null, ext);
                return created;
            } catch (Throwable e) {
                SpiderDebug.log(e);
                return null;
            }
        });
        return spider == null ? new SpiderNull() : spider;
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        spiders.clear();
    }
}
