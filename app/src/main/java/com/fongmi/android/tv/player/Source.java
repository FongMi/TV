package com.fongmi.android.tv.player;

import com.fongmi.android.tv.player.extractor.BiliBili;
import com.fongmi.android.tv.player.extractor.Force;
import com.fongmi.android.tv.player.extractor.JianPian;
import com.fongmi.android.tv.player.extractor.Push;
import com.fongmi.android.tv.player.extractor.TVBus;
import com.fongmi.android.tv.player.extractor.Thunder;
import com.fongmi.android.tv.player.extractor.Youtube;
import com.fongmi.android.tv.player.extractor.ZLive;
import com.github.catvod.utils.Util;

import java.util.ArrayList;
import java.util.List;

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
        extractors.add(new Push());
        extractors.add(new Thunder());
        extractors.add(new TVBus());
        extractors.add(new Youtube());
        extractors.add(new ZLive());
    }

    public String fetch(String url) throws Exception {
        String host = Util.host(url);
        String scheme = Util.scheme(url);
        for (Extractor extractor : extractors) if (extractor.match(scheme, host)) return extractor.fetch(url.trim());
        return url;
    }

    public void stop() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.stop();
    }

    public void exit() {
        if (extractors == null) return;
        for (Extractor extractor : extractors) extractor.exit();
    }

    public interface Extractor {

        boolean match(String scheme, String host);

        String fetch(String url) throws Exception;

        void stop();

        void exit();
    }
}
