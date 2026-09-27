package com.fongmi.android.tv.api.parser;

import android.util.Log;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Tv;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Formatters;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EpgParser {

    private static final String TAG = EpgParser.class.getSimpleName();
    private static final Pattern XML_TIME = Pattern.compile("(\\d{4}(?:\\d{2}){0,5})(?:\\s*(Z|UTC|GMT|[+-]\\d{2}:?\\d{2}))?");
    private static final DateTimeFormatter XML_DATE = DateTimeFormatter.ofPattern("uuuuMMddHHmmss", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private static OffsetDateTime parseFull(String source, ZoneId zoneId) {
        try {
            Matcher matcher = XML_TIME.matcher(source.trim());
            if (!matcher.matches()) return null;
            String time = matcher.group(1);
            LocalDateTime date = LocalDateTime.parse(time + "0101000000".substring(time.length() - 4), XML_DATE);
            String offset = matcher.group(2);
            // Keep source-zone fallback for existing feeds without an explicit offset.
            if (offset == null) return date.atZone(zoneId).toOffsetDateTime();
            return date.atOffset(offset.equals("UTC") || offset.equals("GMT") ? ZoneOffset.UTC : ZoneOffset.of(offset));
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized void start(Live live, String url) throws Exception {
        checkInterrupted();
        long t0 = System.currentTimeMillis();
        XmlData data = loadXml(url, Path.epg(Crypto.md5(url) + ".xml"));
        ProgrammeResult result = processProgramme(data, prepareLiveChannels(live), live.getZoneId());
        checkInterrupted();
        bindResultsToLive(live, result);
        Log.i(TAG, "start done elapsed=" + (System.currentTimeMillis() - t0) + "ms");
    }

    public static Epg getEpg(String xml, String key, ZoneId zoneId) {
        return getEpg(xml, key, null, zoneId);
    }

    public static Epg getEpg(String xml, String key, String date, ZoneId zoneId) {
        try {
            XmlData data = parseXmlData(NodeBuilder.read(new StringReader(xml)));
            Map<String, Set<String>> channels = new HashMap<>();
            channels.put(key, Set.of(key));
            boolean named = data.channels.values().stream().flatMap(List::stream).flatMap(channel -> channel.getDisplayName().stream()).anyMatch(name -> name.getText().equals(key));
            Set<String> programmeIds = data.programmes.keySet();
            if (!data.ids.contains(key) && !named && programmeIds.size() == 1) channels.put(programmeIds.iterator().next(), Set.of(key));
            Map<String, Epg> days = processProgramme(data, channels, zoneId).epgMap.get(key);
            if (days == null || days.isEmpty()) return new Epg();
            return date == null ? days.values().iterator().next() : days.getOrDefault(date, new Epg());
        } catch (Exception e) {
            Log.w(TAG, "getEpg parse failed key=" + key + ": " + e.getMessage());
            return new Epg();
        }
    }

    private static XmlData loadXml(String url, File file) throws Exception {
        if (isFresh(file)) {
            try {
                return parseXmlData(file);
            } catch (Exception e) {
                checkInterrupted();
                Log.w(TAG, "Invalid EPG cache; retrying download");
            }
        }
        File download = File.createTempFile("epg-", ".download", Path.epg());
        File xml = null;
        try {
            Download.create(url, download).get();
            checkInterrupted();
            File source = download;
            if (isGzip(download)) {
                xml = File.createTempFile("epg-", ".xml", Path.epg());
                if (!FileUtil.gzipDecompress(download, xml)) throw new IOException("Invalid EPG gzip");
                source = xml;
            }
            XmlData data = parseXmlData(source);
            checkInterrupted();
            Path.move(source, file);
            return data;
        } finally {
            Path.clear(download);
            Path.clear(xml);
        }
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    }

    private static boolean isFresh(File file) {
        return Path.exists(file) && isToday(file.lastModified()) && System.currentTimeMillis() - file.lastModified() <= TimeUnit.HOURS.toMillis(6);
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

    private static Map<String, Set<String>> prepareLiveChannels(Live live) {
        Map<String, Set<String>> map = new HashMap<>();
        List<Channel> channels = live.getGroups().stream().flatMap(group -> group.getChannel().stream()).toList();
        for (Channel channel : channels) {
            if (!channel.getTvgId().isEmpty()) map.put(channel.getTvgId(), Set.of(channel.getTvgId()));
        }
        Set<String> ids = new HashSet<>(map.keySet());
        for (Channel channel : channels) {
            for (String alias : List.of(channel.getTvgName(), channel.getName())) {
                if (!alias.isEmpty() && !ids.contains(alias)) map.computeIfAbsent(alias, key -> new LinkedHashSet<>()).add(channel.getTvgId());
            }
        }
        return map;
    }

    private static XmlData parseXmlData(File file) throws Exception {
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return parseXmlData(NodeBuilder.read(input));
        }
    }

    private static XmlData parseXmlData(InputNode root) throws Exception {
        if (root == null || !"tv".equals(root.getName())) throw new IOException("Invalid XMLTV root");
        return new XmlData(new Persister().read(Tv.class, root, false));
    }

    private static ProgrammeResult processProgramme(XmlData data, Map<String, Set<String>> liveChannelMap, ZoneId zoneId) throws InterruptedIOException {
        Map<String, Map<String, Epg>> epgMap = new HashMap<>();
        Map<String, String> srcMap = new HashMap<>();
        for (String xmlId : data.ids) {
            checkInterrupted();
            Set<String> targets = findTargetChannels(xmlId, liveChannelMap, data);
            if (targets.isEmpty()) continue;
            List<EpgData> entries = getProgrammeData(data.programmes.getOrDefault(xmlId, List.of()), zoneId);
            String src = "";
            for (Tv.Channel channel : data.channels.getOrDefault(xmlId, List.of())) {
                if (!channel.hasSrc()) continue;
                src = channel.getSrc();
                break;
            }
            for (String id : targets) {
                if (!src.isEmpty()) srcMap.putIfAbsent(id, src);
                for (EpgData entry : entries) {
                    String date = Instant.ofEpochMilli(entry.getStartTime()).atZone(zoneId).format(Formatters.DATE);
                    List<EpgData> list = epgMap.computeIfAbsent(id, k -> new TreeMap<>()).computeIfAbsent(date, d -> Epg.create(id, d)).getList();
                    boolean duplicate = list.stream().anyMatch(item -> item.getStartTime() == entry.getStartTime() && item.getEndTime() == entry.getEndTime() && item.getTitle().equals(entry.getTitle()));
                    if (!duplicate) list.add(targets.size() == 1 ? entry : copyEntry(entry));
                }
            }
        }
        epgMap.values().forEach(days -> days.values().forEach(epg -> epg.getList().sort(Comparator.comparingLong(EpgData::getStartTime))));
        return new ProgrammeResult(epgMap, srcMap);
    }

    private static EpgData copyEntry(EpgData entry) {
        EpgData copy = new EpgData();
        copy.setTitle(entry.getTitle());
        copy.setStart(entry.getStart());
        copy.setEnd(entry.getEnd());
        copy.setStartTime(entry.getStartTime());
        copy.setEndTime(entry.getEndTime());
        return copy;
    }

    private static Set<String> findTargetChannels(String xmlId, Map<String, Set<String>> liveChannels, XmlData data) {
        Set<String> direct = liveChannels.get(xmlId);
        if (direct != null && direct.contains(xmlId)) return direct;
        Set<String> targets = new LinkedHashSet<>();
        if (direct != null) targets.addAll(direct);
        for (Tv.Channel channel : data.channels.getOrDefault(xmlId, List.of())) {
            for (Tv.DisplayName name : channel.getDisplayName()) targets.addAll(liveChannels.getOrDefault(name.getText(), Set.of()));
        }
        targets.removeIf(data.ids::contains);
        return targets;
    }

    private static void bindResultsToLive(Live live, ProgrammeResult result) {
        int withEpg = 0;
        int withoutEpg = 0;
        for (Group group : live.getGroups()) {
            for (Channel channel : group.getChannel()) {
                String tvgId = channel.getTvgId();
                Map<String, Epg> dateMap = result.epgMap.get(tvgId);
                if (dateMap != null && !channel.hasEpgOverride()) {
                    dateMap.values().forEach(channel::setData);
                    channel.getDataList().sort(Comparator.comparing(Epg::getDate));
                    withEpg++;
                } else {
                    withoutEpg++;
                }
                if (channel.getLogo().isEmpty()) {
                    String src = result.srcMap.get(tvgId);
                    if (src != null) channel.setLogo(src);
                }
            }
        }
        Log.i(TAG, "bindResultsToLive with-epg=" + withEpg + " without-epg=" + withoutEpg);
    }

    private static List<EpgData> getProgrammeData(List<Tv.Programme> programmes, ZoneId zoneId) throws InterruptedIOException {
        List<EpgData> entries = new ArrayList<>();
        for (Tv.Programme programme : programmes) {
            checkInterrupted();
            OffsetDateTime start = parseFull(programme.getStart(), zoneId);
            OffsetDateTime end = parseFull(programme.getStop(), zoneId);
            if (start == null || (!programme.getStop().isEmpty() && (end == null || !end.isAfter(start)))) continue;
            EpgData epgData = new EpgData();
            epgData.setTitle(programme.getTitle());
            epgData.setStart(start.atZoneSameInstant(zoneId).format(Formatters.TIME));
            epgData.setStartTime(start.toInstant().toEpochMilli());
            if (end != null) epgData.setEndTime(end.toInstant().toEpochMilli());
            epgData.trans();
            entries.add(epgData);
        }
        entries.sort(Comparator.comparingLong(EpgData::getStartTime));
        long nextStart = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            EpgData entry = entries.get(i);
            if (i + 1 < entries.size() && entry.getStartTime() < entries.get(i + 1).getStartTime()) nextStart = entries.get(i + 1).getStartTime();
            if (entry.getEndTime() == 0) entry.setEndTime(nextStart);
            if (entry.getEndTime() > entry.getStartTime()) entry.setEnd(Instant.ofEpochMilli(entry.getEndTime()).atZone(zoneId).format(Formatters.TIME));
        }
        entries.removeIf(entry -> entry.getEndTime() <= entry.getStartTime());
        return entries;
    }

    private static class XmlData {

        final Map<String, List<Tv.Channel>> channels;
        final Map<String, List<Tv.Programme>> programmes;
        final Set<String> ids;

        public XmlData(Tv tv) {
            this.channels = tv.getChannel().stream().collect(Collectors.groupingBy(Tv.Channel::getId, LinkedHashMap::new, Collectors.toList()));
            this.programmes = tv.getProgramme().stream().collect(Collectors.groupingBy(Tv.Programme::getChannel, LinkedHashMap::new, Collectors.toList()));
            this.ids = new LinkedHashSet<>(channels.keySet());
            this.ids.addAll(programmes.keySet());
        }
    }

    private record ProgrammeResult(Map<String, Map<String, Epg>> epgMap, Map<String, String> srcMap) {}
}
