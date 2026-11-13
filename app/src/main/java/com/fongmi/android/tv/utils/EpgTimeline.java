package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.EpgProgramme;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Source-independent day coverage; fallback intervals are at most one hour. */
public final class EpgTimeline {
    public static final long SLOT_MILLIS = 60 * 60 * 1000L;
    private static final Pattern MISSING=Pattern.compile("(?:暂无|暫無|没有|沒有|无|無)(?:节目|節目|数据|資料|信息|資訊).*" );
    private static final Pattern ALL_DAY=Pattern.compile("全天直播|全天节目|全天節目|精彩节目|精彩節目|电视节目|電視節目|节目预告|節目預告");
    public static final class Entry {
        public final EpgData source;
        public final long start, end;
        public final String title;
        public final boolean adjusted;
        public Entry(EpgData source, long start, long end, String title) {
            this(source, start, end, title, false);
        }
        private Entry(EpgData source, long start, long end, String title, boolean adjusted) {
            this.source=source; this.start=start; this.end=end; this.title=title; this.adjusted=adjusted;
        }
        Entry bounds(long from, long to) { return new Entry(source, from, to, title, true); }
        public boolean fallback() { return source == null; }
    }
    private EpgTimeline() {}

    public static boolean unknown(String title) {
        String value = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() || MISSING.matcher(value).matches()
                || value.startsWith("节目单缺失") || value.startsWith("節目單缺失")
                || value.equals("no information") || value.equals("no program information")
                || value.equals("no programme information") || value.equals("no epg") || value.equals("no data")
                || value.equals("精彩节目") || value.equals("精彩節目")
                || value.equals("unknown") || value.equals("n/a") || value.equals("-") || value.equals("--");
    }

    public static boolean unknown(String title,long duration) {
        return unknown(title) || (duration>=12*3600000L && ALL_DAY.matcher(title.trim()).matches());
    }

    private static boolean distinct(String title) {
        return title.length() >= 4 && !title.matches("电影|電影|纪录片|紀錄片|电视剧|電視劇|晚间剧场|晚間劇場|活力影院|精彩节目|精彩節目|精彩回放|全天节目|全天節目|全天直播");
    }

    public static List<Entry> plan(List<Entry> input, long dayStart, long dayEnd, ZoneId zone, String placeholderTitle) {
        List<Entry> valid = new ArrayList<>();
        for (Entry item : input) if (item != null && item.start > 0 && item.end > item.start && !unknown(item.title,item.end-item.start)) valid.add(item);
        Collections.sort(valid, Comparator.comparingLong((Entry item) -> item.start).thenComparingLong(item -> item.end));
        List<Entry> joined = new ArrayList<>();
        for (Entry item : valid) {
            if (!joined.isEmpty()) {
                Entry previous = joined.get(joined.size()-1);
                if (previous.start == item.start) continue;
                boolean midnight = Instant.ofEpochMilli(item.start).atZone(zone).toLocalTime().equals(LocalTime.MIDNIGHT);
                long gap = item.start - previous.end;
                if (midnight && gap >= 0 && gap <= 60000 && previous.title.trim().equals(item.title.trim())
                        && distinct(item.title.trim()) && item.end - previous.start <= 6*3600000L) {
                    joined.set(joined.size()-1, previous.bounds(previous.start,item.end));
                    continue;
                }
                if (midnight && gap > 0 && gap <= 1000)
                    joined.set(joined.size()-1, previous.bounds(previous.start,item.start));
            }
            joined.add(item);
        }
        List<Entry> output = new ArrayList<>();
        long cursor=dayStart;
        for (int index=0;index<joined.size();index++) {
            Entry item=joined.get(index);
            if (index+1<joined.size() && item.end>joined.get(index+1).start)
                item=item.bounds(item.start,joined.get(index+1).start);
            if (item.end<=dayStart || item.start>=dayEnd || item.end<=item.start) continue;
            long visibleStart=Math.max(dayStart,item.start);
            if (visibleStart>cursor) fill(output,cursor,visibleStart,dayStart,placeholderTitle);
            output.add(item); cursor=Math.max(cursor,Math.min(dayEnd,item.end));
        }
        if (cursor<dayEnd) fill(output,cursor,dayEnd,dayStart,placeholderTitle);
        return output;
    }

    private static void fill(List<Entry> output,long from,long to,long midnight,String placeholderTitle) {
        while (from<to) {
            long boundary=midnight+((from-midnight)/SLOT_MILLIS+1)*SLOT_MILLIS;
            long end=Math.min(to,boundary);
            output.add(new Entry(null,from,end,placeholderTitle));
            from=end;
        }
    }

    private static List<Entry> entries(List<Epg> days) {
        List<Entry> result = new ArrayList<>();
        for (Epg day : days) {
            if (day == null) continue;
            for (EpgData item : new ArrayList<>(day.getList())) {
                if (item == null) continue;
                long start = item.getStartTime(), end = item.getEndTime();
                if (start > 0 && end > start && !unknown(item.getTitle(), end - start))
                    result.add(new Entry(item, start, end, item.getTitle()));
            }
        }
        return result;
    }

    public static boolean hasRealProgrammes(Epg epg) {
        return epg.getList().stream().anyMatch(item -> !isPlaceholder(item));
    }

    public static boolean isPlaceholder(EpgData item) {
        return item instanceof EpgProgramme && ((EpgProgramme) item).isPlaceholder();
    }

    public static EpgData current(Epg epg, long now) {
        return epg.getList().stream().filter(item -> item.getStartTime() <= now && now < item.getEndTime())
                .findFirst().orElse(null);
    }

    public static boolean select(Epg epg, EpgData playing, boolean catchup, long now) {
        boolean changed = false;
        for (EpgData item : epg.getList()) {
            boolean selected = catchup && playing != null
                    ? item.getStartTime() <= playing.getStartTime() && playing.getStartTime() < item.getEndTime()
                    : item.getStartTime() <= now && now < item.getEndTime();
            if (selected == item.isSelected()) continue;
            item.setSelected(selected);
            changed = true;
        }
        return changed;
    }

    private static Epg build(Channel channel, List<Epg> days, LocalDate date, ZoneId zone, String placeholderTitle) {
        long start = date.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        List<EpgData> rows = new ArrayList<>();
        for (Entry entry : plan(entries(days), start, end, zone, placeholderTitle))
            rows.add(new EpgProgramme(entry.source, entry.start, entry.end, entry.title, date, zone));
        Epg result = Epg.create(channel.getTvgId(), date.format(Formatters.DATE));
        result.setList(rows);
        return result;
    }

    public static EpgData next(Channel channel, EpgData playing, ZoneId zone, long now, String placeholderTitle) {
        if (channel == null || playing == null) return null;
        long after = EpgProgramme.playbackEnd(playing);
        if (after <= playing.getStartTime() || after + 1000 > now) return null;
        LocalDate day = Instant.ofEpochMilli(playing.getStartTime()).atZone(zone).toLocalDate();
        List<Epg> snapshot = channel.getDataList();
        for (int offset = 0; offset <= EpgHistory.PREVIOUS_DAYS + 2; offset++) {
            LocalDate date = day.plusDays(offset);
            if (date.atStartOfDay(zone).toInstant().toEpochMilli() > now) break;
            for (EpgData item : build(channel, snapshot, date, zone, placeholderTitle).getList()) {
                if (item.getStartTime() < after && after < item.getEndTime())
                    return ((EpgProgramme) item).from(after);
                if (item.getStartTime() >= after && item.getStartTime() + 1000 <= now) return item;
            }
        }
        return null;
    }

    /** Cached local view; no selection or synthesized row is written into Channel. */
    public static final class Cache {
        private Channel channel;
        private LocalDate date;
        private ZoneId zone;
        private String title;
        private List<Epg> days;
        private List<List<EpgData>> lists;
        private List<Integer> sizes;
        private Epg view;

        public Epg get(Channel channel, LocalDate date, ZoneId zone, String placeholderTitle) {
            List<Epg> snapshot = channel == null ? Collections.emptyList() : channel.getDataList();
            boolean same = this.channel == channel && date.equals(this.date) && zone.equals(this.zone)
                    && placeholderTitle.equals(title) && days != null && days.size() == snapshot.size();
            for (int i = 0; same && i < snapshot.size(); i++) {
                Epg day = snapshot.get(i);
                List<EpgData> list = day == null ? Collections.emptyList() : day.getList();
                same = day == days.get(i) && list == lists.get(i) && list.size() == sizes.get(i);
            }
            if (same) return view;
            this.channel = channel; this.date = date; this.zone = zone; title = placeholderTitle;
            days = snapshot; lists = new ArrayList<>(); sizes = new ArrayList<>();
            for (Epg day : days) {
                List<EpgData> list = day == null ? Collections.emptyList() : day.getList();
                lists.add(list); sizes.add(list.size());
            }
            view = build(channel == null ? new Channel() : channel, snapshot, date, zone, placeholderTitle);
            return view;
        }

        public void clear() {
            channel = null; date = null; zone = null; title = null; days = null; lists = null; sizes = null; view = null;
        }
    }
}
