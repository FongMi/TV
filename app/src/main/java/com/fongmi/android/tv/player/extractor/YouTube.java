package com.fongmi.android.tv.player.extractor;

import android.net.Uri;
import android.util.Base64;

import androidx.media3.common.MimeTypes;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.NewPipeImpl;
import com.fongmi.android.tv.utils.UrlUtil;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class YouTube implements Source.Extractor {

    private static final String DASH_NAMESPACE = "urn:mpeg:dash:schema:mpd:2011";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String DASH_PROFILE = "urn:mpeg:dash:profile:isoff-on-demand:2011";

    public YouTube() {
        NewPipe.init(NewPipeImpl.get(), Localization.fromLocale(Locale.getDefault()));
    }

    @Override
    public boolean match(Uri uri) {
        String host = UrlUtil.host(uri);
        return host.contains("youtube.com") || host.contains("youtu.be");
    }

    @Override
    public String fetch(String url) throws Exception {
        return fetch(StreamInfo.getInfo(url));
    }

    @Override
    public String fetch(Result result) throws Exception {
        StreamInfo info = StreamInfo.getInfo(result.getUrl().v());
        result.setSubs(getSubtitles(info.getSubtitles()));
        return fetch(info);
    }

    private String fetch(StreamInfo info) throws Exception {
        RefreshEvent.vod(convert(info).trans());
        return getPlayUrl(info);
    }

    private String getPlayUrl(StreamInfo info) throws Exception {
        return isLive(info) ? getLive(info) : getMpd(info);
    }

    private Vod convert(StreamInfo info) {
        try {
            Vod vod = new Vod();
            vod.setName(info.getName());
            vod.setDirector(info.getUploaderName());
            vod.setContent(info.getDescription().getContent());
            vod.setPic(info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl());
            return vod;
        } catch (Exception e) {
            Vod vod = new Vod();
            vod.setName(info.getName());
            vod.setContent(info.getDescription().getContent());
            return vod;
        }
    }

    private boolean isLive(StreamInfo info) {
        return StreamType.LIVE_STREAM.equals(info.getStreamType());
    }

    private String getLive(StreamInfo info) {
        return !info.getHlsUrl().isEmpty() ? info.getHlsUrl() : info.getDashMpdUrl();
    }

    private String getMpd(StreamInfo info) throws Exception {
        List<AudioStream> audioFormats = getSegmentStreams(info.getAudioStreams());
        List<VideoStream> videoFormats = getSegmentStreams(info.getVideoOnlyStreams());
        if (audioFormats.isEmpty() && videoFormats.isEmpty()) return getProgressive(info.getVideoStreams());
        return toDataUri(MimeTypes.APPLICATION_MPD, documentToXml(createMpd(info, videoFormats, audioFormats)));
    }

    private String getProgressive(List<VideoStream> formats) {
        return formats.stream().filter(format -> !format.getContent().isEmpty()).max(Comparator.comparingInt(VideoStream::getHeight).thenComparingInt(VideoStream::getBitrate)).map(VideoStream::getContent).orElse("");
    }

    private Document createMpd(StreamInfo info, List<VideoStream> videoFormats, List<AudioStream> audioFormats) throws Exception {
        String duration = "PT" + Math.max(0, info.getDuration()) + "S";
        Document doc = newDocument();
        Element mpd = append(doc, doc, "MPD");
        attr(mpd, "xmlns:xsi", XSI_NAMESPACE);
        attr(mpd, "xmlns", DASH_NAMESPACE);
        attr(mpd, "xsi:schemaLocation", DASH_NAMESPACE + " DASH-MPD.xsd");
        attr(mpd, "type", "static");
        attr(mpd, "mediaPresentationDuration", duration);
        attr(mpd, "minBufferTime", "PT1.500S");
        attr(mpd, "profiles", DASH_PROFILE);
        Element period = append(doc, mpd, "Period");
        attr(period, "duration", duration);
        attr(period, "start", "PT0S");
        for (VideoStream format : videoFormats) addVideo(doc, period, format);
        for (AudioStream format : audioFormats) addAudio(doc, period, format);
        return doc;
    }

    private void addVideo(Document doc, Element period, VideoStream format) {
        Element representation = addRepresentation(doc, period, "video", format.getFormat().getMimeType(), format.getItag(), format.getBitrate(), format.getCodec(), format.getContent());
        attr(representation, "height", format.getHeight());
        attr(representation, "width", format.getWidth());
        attr(representation, "frameRate", format.getFps());
        attr(representation, "maxPlayoutRate", "1");
        attr(representation, "startWithSAP", "1");
        addSegmentBase(doc, representation, format.getIndexStart(), format.getIndexEnd(), format.getInitStart(), format.getInitEnd());
    }

    private void addAudio(Document doc, Element period, AudioStream format) {
        Element representation = addRepresentation(doc, period, "audio", format.getFormat().getMimeType(), format.getItag(), format.getBitrate(), format.getCodec(), format.getContent());
        if (format.getItagItem() != null) attr(representation, "audioSamplingRate", format.getItagItem().getSampleRate());
        addSegmentBase(doc, representation, format.getIndexStart(), format.getIndexEnd(), format.getInitStart(), format.getInitEnd());
    }

    private Element addRepresentation(Document doc, Element period, String contentType, String mimeType, int id, int bandwidth, String codecs, String url) {
        Element adaptationSet = append(doc, period, "AdaptationSet");
        attr(adaptationSet, "contentType", contentType);
        attr(adaptationSet, "mimeType", mimeType);
        attr(adaptationSet, "subsegmentAlignment", "true");
        Element content = append(doc, adaptationSet, "ContentComponent");
        attr(content, "contentType", contentType);
        Element representation = append(doc, adaptationSet, "Representation");
        attr(representation, "id", id);
        attr(representation, "bandwidth", bandwidth);
        attr(representation, "codecs", codecs);
        attr(representation, "mimeType", mimeType);
        append(doc, representation, "BaseURL").setTextContent(url);
        return representation;
    }

    private void addSegmentBase(Document doc, Element representation, long indexStart, long indexEnd, long initStart, long initEnd) {
        Element segmentBase = append(doc, representation, "SegmentBase");
        attr(segmentBase, "indexRange", range(indexStart, indexEnd));
        Element initialization = append(doc, segmentBase, "Initialization");
        attr(initialization, "range", range(initStart, initEnd));
    }

    private List<Sub> getSubtitles(List<SubtitlesStream> subtitles) {
        return subtitles == null ? Collections.emptyList() : subtitles.stream().map(this::toSub).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Sub toSub(SubtitlesStream subtitle) {
        String url = getSubtitleUrl(subtitle);
        if (url == null) return null;
        String label = subtitle.getDisplayLanguageName();
        String language = subtitle.getLanguageTag();
        if (label == null || label.isEmpty()) label = language;
        return Sub.from(label, url, language, MimeTypes.TEXT_VTT);
    }

    private String getSubtitleUrl(SubtitlesStream subtitle) {
        MediaFormat format = subtitle.getFormat();
        if (format == null || subtitle.getContent().isEmpty()) return null;
        if (subtitle.isUrl() && MimeTypes.APPLICATION_TTML.equals(format.getMimeType())) return getVttSubtitleUrl(UrlUtil.uri(subtitle.getContent()));
        if (!MimeTypes.TEXT_VTT.equals(format.getMimeType())) return null;
        if (subtitle.isUrl()) return subtitle.getContent();
        return toDataUri(MimeTypes.TEXT_VTT, subtitle.getContent());
    }

    private String getVttSubtitleUrl(Uri uri) {
        Uri.Builder builder = uri.buildUpon().clearQuery();
        for (String name : uri.getQueryParameterNames()) {
            if ("fmt".equalsIgnoreCase(name)) continue;
            for (String value : uri.getQueryParameters(name)) builder.appendQueryParameter(name, value);
        }
        builder.appendQueryParameter("fmt", "vtt");
        return builder.build().toString();
    }

    private <T extends Stream> List<T> getSegmentStreams(List<T> formats) {
        return formats.stream().filter(this::hasSegmentRanges).collect(Collectors.toList());
    }

    private boolean hasSegmentRanges(Stream format) {
        if (format instanceof VideoStream video) return hasRange(video.getIndexStart(), video.getIndexEnd()) && hasRange(video.getInitStart(), video.getInitEnd());
        if (format instanceof AudioStream audio) return hasRange(audio.getIndexStart(), audio.getIndexEnd()) && hasRange(audio.getInitStart(), audio.getInitEnd());
        return false;
    }

    private boolean hasRange(long start, long end) {
        return start >= 0 && end >= start;
    }

    private String range(long start, long end) {
        return start + "-" + end;
    }

    private String toDataUri(String mimeType, String content) {
        return "data:" + mimeType + ";base64," + Base64.encodeToString(content.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().newDocument();
    }

    private String documentToXml(Document doc) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        StringWriter result = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(result));
        return result.toString();
    }

    private Element append(Document doc, org.w3c.dom.Node parent, String name) {
        Element child = doc.createElement(name);
        parent.appendChild(child);
        return child;
    }

    private void attr(Element element, String name, String value) {
        if (value != null && !value.isEmpty()) element.setAttribute(name, value);
    }

    private void attr(Element element, String name, int value) {
        if (value > 0) attr(element, name, String.valueOf(value));
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }

    public record Parser(String url) implements Callable<List<Episode>> {

        private static final Pattern PATTERN = Pattern.compile("(youtube\\.com|youtu\\.be).*list=");

        public static boolean match(String url) {
            return PATTERN.matcher(url).find();
        }

        public static Parser get(String url) {
            return new Parser(url);
        }

        @Override
        public List<Episode> call() {
            try {
                ListLinkHandler handler = YoutubePlaylistLinkHandlerFactory.getInstance().fromUrl(url);
                YoutubePlaylistExtractor extractor = new YoutubePlaylistExtractor(ServiceList.YouTube, handler);
                extractor.forceLocalization(NewPipe.getPreferredLocalization());
                extractor.fetchPage();
                List<Episode> episodes = new ArrayList<>();
                add(extractor, episodes, extractor.getInitialPage());
                return episodes;
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        private void add(YoutubePlaylistExtractor extractor, List<Episode> episodes, ListExtractor.InfoItemsPage<StreamInfoItem> page) {
            for (StreamInfoItem item : page.getItems()) {
                if (item.getDuration() == -1) continue;
                episodes.add(Episode.create(item.getName(), item.getUrl()));
            }
            if (page.hasNextPage()) {
                try {
                    add(extractor, episodes, extractor.getPage(page.getNextPage()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
