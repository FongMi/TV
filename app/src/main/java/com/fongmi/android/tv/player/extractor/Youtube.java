package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.impl.NewPipeImpl;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;

public class Youtube implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return host.contains("youtube.com") || host.contains("youtu.be");
    }

    public Youtube() {
        NewPipe.init(new NewPipeImpl());
    }

    @Override
    public String fetch(String url) throws Exception {
        String html = OkHttp.newCall(url, Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
        Matcher matcher = Pattern.compile("hlsManifestUrl\\S*?(https\\S*?\\.m3u8)").matcher(html);
        if (matcher.find()) {
            html = OkHttp.newCall(matcher.group(1), Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
            return find(html);
        } else {
            LinkHandler handler = YoutubeStreamLinkHandlerFactory.getInstance().fromUrl(url);
            YoutubeStreamExtractor extractor = new YoutubeStreamExtractor(ServiceList.YouTube, handler);
            extractor.fetchPage();
            return find(extractor);
        }
    }

    private String find(YoutubeStreamExtractor extractor) throws ExtractionException {
        VideoStream item = extractor.getVideoStreams().get(0);
        for (VideoStream stream : extractor.getVideoStreams()) if (!stream.isVideoOnly() && stream.getHeight() >= item.getHeight()) item = stream;
        return item.getContent();
    }

    private String find(String html) {
        String url = "";
        List<String> items = Arrays.asList("301", "300", "96", "95", "94");
        for (String item : items) if (!(url = find(html, "https:/.*/" + item + "/.*index.m3u8")).isEmpty()) break;
        return url;
    }

    private String find(String html, String rule) {
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) return matcher.group();
        return "";
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
