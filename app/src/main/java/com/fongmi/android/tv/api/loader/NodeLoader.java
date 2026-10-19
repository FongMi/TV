package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.api.node.NodeRuntime;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.utils.RuntimeModule;

import java.util.concurrent.ConcurrentHashMap;

public final class NodeLoader {

    private final ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();

    public Spider getSpider(String key, String api, String ext) {
        if (!api.startsWith("node:")) return new SpiderNull();
        RuntimeModule.NODEJS.require();
        Spider spider = spiders.computeIfAbsent(key + '\u0000' + api, ignored -> createSpider(key, api, ext));
        return spider == null ? new SpiderNull() : spider;
    }

    private Spider createSpider(String key, String api, String ext) {
        try {
            Spider created = NodeRuntime.get().client().spider(api);
            created.siteKey = key;
            created.init(null, ext);
            return created;
        } catch (Throwable e) {
            RuntimeModule.MissingException missing = RuntimeModule.MissingException.find(e);
            if (missing != null) throw missing;
            SpiderDebug.log(e);
            return null;
        }
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        spiders.clear();
    }
}
