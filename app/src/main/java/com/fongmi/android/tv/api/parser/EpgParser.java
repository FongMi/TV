package com.fongmi.android.tv.api.parser;

import android.util.Log;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Tv;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Formatters;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Path;

import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EpgParser {

    private static final String TAG = EpgParser.class.getSimpleName();

    private static OffsetDateTime parseFull(String source, ZoneId zoneId) {
        String s = source.trim();
        try {
            String time = s.length() > 14 ? s.substring(0, 14) : s;
            String offset = s.length() > 14 ? s.substring(14).trim() : "";
            if (!offset.isEmpty()) return parseOffset(time + " " + offset);
            return LocalDateTime.parse(time, Formatters.EPG_FULL_NO_TZ).atZone(zoneId).toOffsetDateTime();
        } catch (Exception e) {
            Log.w(TAG, "parseFull failed: " + s + " -> " + e.getMessage());
            return OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        }
    }

    private static OffsetDateTime parseOffset(String source) {
        try {
            return OffsetDateTime.parse(source, Formatters.EPG_FULL);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(source, Formatters.EPG_FULL_COLON);
        }
    }

    public static void start(Live live, String url) throws Exception {
        long t0 = System.currentTimeMillis();
        File file = Path.epg(UrlUtil.path(url));
        String reason = refreshReason(file);
        boolean refresh = reason != null;
        Log.i(TAG, "start url=" + url + " file=" + file.getName() + " refresh=" + refresh + (refresh ? " reason=" + reason : ""));
        if (refresh) Download.create(url, file).get();
        boolean gzip = isGzip(file);
        if (gzip) readGzip(live, file, refresh);
        else readXml(live, file);
        Log.i(TAG, "start done elapsed=" + (System.currentTimeMillis() - t0) + "ms");
    }

    public static Epg getEpg(String xml, String key, ZoneId zoneId) {
        try {
            Tv tv = new Persister().read(Tv.class, xml, false);
            String rawDate = tv.getDate();
            String date = rawDate.isEmpty() ? LocalDate.now(zoneId).format(Formatters.DATE) : parseFull(rawDate, zoneId).atZoneSameInstant(zoneId).format(Formatters.DATE);
            Epg epg = Epg.create(key, date);
            tv.getProgramme().forEach(programme -> epg.getList().add(getEpgData(programme, zoneId)));
            return epg;
        } catch (Exception e) {
            Log.w(TAG, "getEpg parse failed key=" + key + ": " + e.getMessage());
            return new Epg();
        }
    }

    private static String refreshReason(File file) {
        if (!Path.exists(file)) return "file-missing";
        if (!isToday(file.lastModified())) return "not-today";
        if (System.currentTimeMillis() - file.lastModified() > TimeUnit.HOURS.toMillis(6)) return "older-than-6h";
        return null;
    }

    private static boolean isGzip(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return (fis.read() | (fis.read() << 8)) == 0x8B1F;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isToday(long millis) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).equals(LocalDate.now());
    }

    private static void readGzip(Live live, File file, boolean refresh) throws Exception {
        File xml = Path.epg(file.getName() + ".xml");
        boolean needsDecompression = refresh || !Path.exists(xml);
        if (needsDecompression && !FileUtil.gzipDecompress(file, xml)) Log.w(TAG, "gzip decompress failed file=" + file);
        readXml(live, xml);
    }

    private static void readXml(Live live, File file) throws Exception {
        ZoneId zoneId = live.getZoneId();
        Map<String, Channel> liveChannelMap = prepareLiveChannels(live);
        XmlData xmlData = parseXmlData(file);
        ProgrammeResult result = processProgramme(xmlData, liveChannelMap, zoneId);
        bindResultsToLive(live, result);
    }

    private static Map<String, Channel> prepareLiveChannels(Live live) {
        Map<String, Channel> map = new HashMap<>();
        live.getGroups().stream()
                .flatMap(group -> group.getChannel().stream())
                .forEach(channel -> {
                    if (!channel.getTvgId().isEmpty()) map.putIfAbsent(channel.getTvgId(), channel);
                    if (!channel.getTvgName().isEmpty()) map.putIfAbsent(channel.getTvgName(), channel);
                    if (!channel.getName().isEmpty()) map.putIfAbsent(channel.getName(), channel);
                });
        return map;
    }

    private static XmlData parseXmlData(File file) throws Exception {
        Tv tv = new Persister().read(Tv.class, file, false);
        Map<String, List<Tv.Channel>> map = tv.getChannel().stream().collect(Collectors.groupingBy(Tv.Channel::getId));
        return new XmlData(tv, map);
    }

    private static ProgrammeResult processProgramme(XmlData data, Map<String, Channel> liveChannelMap, ZoneId zoneId) {
        Map<String, Map<String, Epg>> epgMap = new HashMap<>();
        Map<String, String> srcMap = new HashMap<>();
        Map<String, Channel> channelCache = new HashMap<>();
        Set<String> channelMiss = new HashSet<>();
        int skipped = 0;
        for (Tv.Programme programme : data.tv.getProgramme()) {
            String xmlChannelId = programme.getChannel();
            Channel targetChannel;
            if (channelCache.containsKey(xmlChannelId)) {
                targetChannel = channelCache.get(xmlChannelId);
            } else if (channelMiss.contains(xmlChannelId)) {
                targetChannel = null;
            } else {
                targetChannel = findTargetChannel(xmlChannelId, liveChannelMap, data.map);
                if (targetChannel != null) channelCache.put(xmlChannelId, targetChannel);
                else channelMiss.add(xmlChannelId);
            }
            if (targetChannel == null) {
                skipped++;
                continue;
            }
            OffsetDateTime startDate = parseFull(programme.getStart(), zoneId);
            OffsetDateTime endDate = parseFull(programme.getStop(), zoneId);
            String liveTvgId = targetChannel.getTvgId();
            String programmeDate = startDate.atZoneSameInstant(zoneId).format(Formatters.DATE);
            epgMap.computeIfAbsent(liveTvgId, k -> new HashMap<>())
                    .computeIfAbsent(programmeDate, d -> Epg.create(liveTvgId, d))
                    .getList().add(getEpgData(startDate, endDate, zoneId, programme));
            if (!srcMap.containsKey(liveTvgId)) {
                List<Tv.Channel> xmlChannels = data.map.get(xmlChannelId);
                if (xmlChannels != null) {
                    for (Tv.Channel ch : xmlChannels) {
                        if (ch.hasSrc()) {
                            srcMap.put(liveTvgId, ch.getSrc());
                            break;
                        }
                    }
                }
            }
        }
        Log.i(TAG, "processProgramme skipped(no match)=" + skipped + " matched channels=" + epgMap.size());
        return new ProgrammeResult(epgMap, srcMap);
    }

    private static Channel findTargetChannel(String xmlChannelId, Map<String, Channel> liveChannelMap, Map<String, List<Tv.Channel>> xmlChannelIdMap) {
        Channel targetChannel = liveChannelMap.get(xmlChannelId);
        if (targetChannel != null) return targetChannel;
        List<Tv.Channel> channels = xmlChannelIdMap.get(xmlChannelId);
        if (channels == null) return null;
        return channels.stream().flatMap(xmlChannel -> xmlChannel.getDisplayName().stream()).map(Tv.DisplayName::getText).filter(name -> !name.isEmpty()).filter(liveChannelMap::containsKey).findFirst().map(liveChannelMap::get).orElse(null);
    }

    private static void bindResultsToLive(Live live, ProgrammeResult result) {
        int[] counts = {0, 0};
        live.getGroups().stream()
                .flatMap(group -> group.getChannel().stream())
                .forEach(channel -> {
                    String tvgId = channel.getTvgId();
                    Map<String, Epg> dateMap = result.epgMap.get(tvgId);
                    if (dateMap != null) {
                        channel.setDataList(new ArrayList<>(dateMap.values()));
                        counts[0]++;
                    } else {
                        counts[1]++;
                    }
                    if (channel.getLogo().isEmpty()) {
                        String src = result.srcMap.get(tvgId);
                        if (src != null) channel.setLogo(src);
                    }
                });
        Log.i(TAG, "bindResultsToLive with-epg=" + counts[0] + " without-epg=" + counts[1]);
    }

    private static EpgData getEpgData(Tv.Programme programme, ZoneId zoneId) {
        OffsetDateTime startDate = parseFull(programme.getStart(), zoneId);
        OffsetDateTime endDate = parseFull(programme.getStop(), zoneId);
        return getEpgData(startDate, endDate, zoneId, programme);
    }

    private static EpgData getEpgData(OffsetDateTime startDate, OffsetDateTime endDate, ZoneId zoneId, Tv.Programme programme) {
        try {
            EpgData epgData = new EpgData();
            epgData.setTitle(programme.getTitle());
            epgData.setStart(startDate.atZoneSameInstant(zoneId).format(Formatters.TIME));
            epgData.setEnd(endDate.atZoneSameInstant(zoneId).format(Formatters.TIME));
            epgData.setStartTime(startDate.toInstant().toEpochMilli());
            epgData.setEndTime(endDate.toInstant().toEpochMilli());
            epgData.trans();
            return epgData;
        } catch (Exception e) {
            return new EpgData();
        }
    }

    private static class XmlData {

        Tv tv;
        Map<String, List<Tv.Channel>> map;

        public XmlData(Tv tv, Map<String, List<Tv.Channel>> map) {
            this.tv = tv;
            this.map = map;
        }
    }

    private static class ProgrammeResult {

        Map<String, Map<String, Epg>> epgMap;
        Map<String, String> srcMap;

        public ProgrammeResult(Map<String, Map<String, Epg>> epgMap, Map<String, String> srcMap) {
            this.epgMap = epgMap;
            this.srcMap = srcMap;
        }
    }
}
