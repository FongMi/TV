package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.api.node.NodeRuntime;
import com.github.catvod.crawler.Spider;

public final class NodeLoader extends SpiderLoader {

    public Spider getSpider(String key, String api, String ext) {
        return load(key + '\u0000' + api, null, ext, () -> newSpider(key, api));
    }

    Spider newSpider(String key, String api) {
        Spider spider = NodeRuntime.get().client().spider(api);
        spider.siteKey = key;
        return spider;
    }
}
