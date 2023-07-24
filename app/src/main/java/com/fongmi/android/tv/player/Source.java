package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.player.extractor.BiliBili;
import com.fongmi.android.tv.player.extractor.Force;
import com.fongmi.android.tv.player.extractor.JianPian;
import com.fongmi.android.tv.player.extractor.TVBus;
import com.fongmi.android.tv.player.extractor.Youtube;
import com.fongmi.android.tv.player.extractor.ZLive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Source {

    private final List<Extractor> extractors;

    private static class Loader {
        static volatile Source INSTANCE = new Source();
    }

    public static Source get() {
        return Loader.INSTANCE;
    }

    public Source() {
        extractors = new ArrayList<>();
        extractors.add(new BiliBili());
        extractors.add(new Force());
        extractors.add(new JianPian());
        extractors.add(new TVBus());
        extractors.add(new Youtube());
        extractors.add(new ZLive());
    }

    public String fetch(String url) throws Exception {
        Uri uri = Uri.parse(url);
        String host = Objects.requireNonNullElse(uri.getHost(), "");
        String scheme = Objects.requireNonNullElse(uri.getScheme(), "");
        for (Extractor extractor : extractors) if (extractor.match(scheme, host)) return extractor.fetch(url);
        return url;
    }

    public void stop() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.stop();
    }

    public void destroy() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.destroy();
    }

    public void release() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.release();
    }

    public interface Extractor {

        boolean match(String scheme, String host);

        String fetch(String url) throws Exception;

        void stop();

        void destroy();

        void release();
    }
}
