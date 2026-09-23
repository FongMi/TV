package com.fongmi.android.tv.ui.custom;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.fongmi.android.tv.api.config.RuleConfig;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.utils.Util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class WebViewRules {

    static void apply(WebView view, Uri uri, String script, BooleanSupplier current) {
        List<String> scripts = new ArrayList<>(Sniffer.getScript(uri));
        if (!TextUtils.isEmpty(script) && !scripts.contains(script)) scripts.add(0, script);
        evaluate(view, scripts, 0, current, () -> WebViewClicker.click(view, Sniffer.getClick(uri), current));
    }

    static boolean isAd(Uri uri) {
        String host = uri.getHost();
        if (TextUtils.isEmpty(host)) return false;
        for (String ad : RuleConfig.get().getAds()) if (Util.containOrMatch(host, ad)) return true;
        return false;
    }

    static WebResourceResponse empty() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }

    private static void evaluate(WebView view, List<String> scripts, int index, BooleanSupplier current, Runnable complete) {
        if (!current.getAsBoolean()) return;
        if (index >= scripts.size()) {
            complete.run();
            return;
        }
        String script = scripts.get(index);
        if (TextUtils.isEmpty(script)) evaluate(view, scripts, index + 1, current, complete);
        else view.evaluateJavascript(script, value -> evaluate(view, scripts, index + 1, current, complete));
    }

    private WebViewRules() {
    }
}
