package com.fongmi.android.tv.api;

import android.net.Uri;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Tv;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Trans;

import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EpgParser {

    private static final SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat formatFull = new SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault());

    public static boolean start(Live live, String url) throws Exception {
        File file = Path.epg(Uri.parse(url).getLastPathSegment());
        if (shouldDownload(file)) Download.create(url, file).start();
        if (file.getName().endsWith(".gz")) readGzip(live, file);
        else readXml(live, file);
        return true;
    }

    private static boolean shouldDownload(File file) {
        return !file.exists() || !isToday(file.lastModified()) || System.currentTimeMillis() - file.lastModified() > TimeUnit.HOURS.toMillis(6);
    }

    private static boolean isToday(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    private static void readGzip(Live live, File file) throws Exception {
        File xml = Path.epg(file.getName().replace(".gz", ""));
        if (!xml.exists()) FileUtil.gzipDecompress(file, xml);
        readXml(live, xml);
    }

    private static void readXml(Live live, File file) throws Exception {
        Set<String> exist = new HashSet<>();
        Map<String, Epg> epgMap = new HashMap<>();
        Map<String, String> srcMap = new HashMap<>();
        Map<String, Tv.Channel> mapping = new HashMap<>();
        String today = formatDate.format(new Date());
        Tv tv = new Persister().read(Tv.class, Path.read(file), false);
        for (Group group : live.getGroups()) for (Channel channel : group.getChannel()) exist.add(channel.getTvgId());
        for (Tv.Channel channel : tv.getChannel()) mapping.put(channel.getId(), channel);
        for (Tv.Programme programme : tv.getProgramme()) {
            String key = programme.getChannel();
            Tv.Channel channel = mapping.get(key);
            if (!exist.contains(key)) key = find(exist, channel);
            Date startDate = parse(formatFull, programme.getStart());
            Date endDate = parse(formatFull, programme.getStop());
            if (!exist.contains(key) || !isToday(startDate.getTime())) continue;
            if (!epgMap.containsKey(key)) epgMap.put(key, Epg.create(key, today));
            epgMap.get(key).getList().add(getEpgData(startDate, endDate, programme));
            if (channel != null && channel.hasSrc()) srcMap.put(key, channel.getSrc());
        }
        for (Group group : live.getGroups()) {
            for (Channel channel : group.getChannel()) {
                if (epgMap.containsKey(channel.getTvgId())) channel.setData(epgMap.get(channel.getTvgId()));
                if (srcMap.containsKey(channel.getTvgId())) channel.setLogo(srcMap.get(channel.getTvgId()));
            }
        }
    }

    private static String find(Set<String> exist, Tv.Channel channel) {
        if (channel == null) return "";
        for (Tv.DisplayName name : channel.getDisplayName()) if (exist.contains(name.getText())) return name.getText();
        return "";
    }

    public static Epg getEpg(String xml, String key) throws Exception {
        Tv tv = new Persister().read(Tv.class, xml, false);
        Epg epg = Epg.create(key, formatDate.format(parse(formatFull, tv.getDate())));
        for (Tv.Programme programme : tv.getProgramme()) epg.getList().add(getEpgData(programme));
        return epg;
    }

    private static EpgData getEpgData(Tv.Programme programme) {
        Date startDate = parse(formatFull, programme.getStart());
        Date endDate = parse(formatFull, programme.getStop());
        return getEpgData(startDate, endDate, programme);
    }

    private static EpgData getEpgData(Date startDate, Date endDate, Tv.Programme programme) {
        try {
            EpgData epgData = new EpgData();
            epgData.setTitle(Trans.s2t(programme.getTitle()));
            epgData.setStart(formatTime.format(startDate));
            epgData.setEnd(formatTime.format(endDate));
            epgData.setStartTime(startDate.getTime());
            epgData.setEndTime(endDate.getTime());
            return epgData;
        } catch (Exception e) {
            return new EpgData();
        }
    }

    private static Date parse(SimpleDateFormat format, String source) {
        try {
            return format.parse(source);
        } catch (Exception e) {
            Date date = new Date();
            date.setTime(0);
            return date;
        }
    }
}