package com.github.catvod.runtime;

import com.github.catvod.crawler.Spider;
import com.github.catvod.net.Net;

import java.util.function.Supplier;

public interface PythonRuntime extends RuntimePlugin {

    void initialize();

    default String format(String source) {
        throw new UnsupportedOperationException("Update the Python module to use YAPF formatting");
    }

    Spider spider(String api, String siteKey, Net.Cookies cookies, Supplier<String> userAgent);
}
