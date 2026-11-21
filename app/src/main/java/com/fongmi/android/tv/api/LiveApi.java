package com.fongmi.android.tv.api;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.api.parser.EpgParser;
import com.fongmi.android.tv.api.parser.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.EpgProgramme;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.utils.Formatters;
import com.fongmi.android.tv.utils.EpgTimeline;
import com.github.catvod.net.OkHttp;

import java.time.LocalDate;
import java.time.ZoneId;

public class LiveApi {

    public static void parse(@NonNull Live item) throws Exception {
        LiveParser.start(item.recent());
        item.getGroups().removeIf(Group::isEmpty);
        if (item.getGroups().isEmpty() || item.getGroups().get(0).isKeep()) return;
        item.getGroups().add(0, Group.create(R.string.keep));
        LiveConfig.get().applyKeepsToGroups(item.getGroups());
    }

    public static boolean parseXml(@NonNull Live item) {
        boolean parsed = false;
        for (String url : item.getEpgXml()) parsed |= startXml(item, url);
        return parsed;
    }

    @NonNull
    public static Epg getEpg(@NonNull Channel item, @NonNull ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        for (int offset = -1; offset <= 1; offset++) fetchEpgDay(item, zoneId, today.plusDays(offset));
        return item.getData(zoneId, today).selected();
    }

    @NonNull
    public static Epg getEpg(@NonNull Channel item, @NonNull ZoneId zoneId, @NonNull LocalDate day) {
        // Fetch only the selected day and its midnight carry-over. Browsing
        // history must not refetch today's neighbours or change live metadata.
        fetchEpgDay(item, zoneId, day);
        try {
            if (!Thread.currentThread().isInterrupted()) fetchEpgDay(item, zoneId, day.minusDays(1));
        } catch (Exception ignored) {
            // An unavailable previous day must not hide the requested schedule.
        }
        return item.getData(zoneId, day);
    }

    @NonNull
    public static Result getUrl(@NonNull Channel item) throws Exception {
        Result result = resolveUrl(item);
        item.applyAuth(result);
        return result;
    }

    @NonNull
    public static Result getUrl(@NonNull Channel item, @NonNull EpgData data) throws Exception {
        data = EpgProgramme.forRequest(data, System.currentTimeMillis());
        Result result = resolveUrl(item);
        if (Source.isPush(result.getUrl().v())) return result;
        result.setTimeshift(null);
        result.setUrl(item.getCatchup().format(result.getRealUrl(), data));
        BaseLoader.get().requireProxy(result.getRealUrl());
        if (item.isRtsp()) result.getHeader().put("rtsp_range", data.getRange());
        item.applyAuth(result);
        return result;
    }

    private static Result resolveUrl(Channel item) throws Exception {
        Source.get().stop();
        Result result = item.result();
        result.setUrl(Source.get().fetch(result));
        return result;
    }

    private static boolean startXml(Live item, String url) {
        try {
            EpgParser.start(item, url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Foreground recovery for a requested day; missing data never overwrites a valid cache. */
    public static boolean refreshEpg(@NonNull Live live, @NonNull Channel item, @NonNull ZoneId zone, @NonNull LocalDate day) throws Exception {
        if (item.getEpg().startsWith("http")) {
            boolean fetched = fetchEpgDay(item, zone, day, true);
            LocalDate previous = day.minusDays(1);
            if (!Thread.currentThread().isInterrupted() && !hasRealDay(item, previous)) {
                try { fetched |= fetchEpgDay(item, zone, previous, true); }
                catch (Exception ignored) { }
            }
            return fetched;
        }
        boolean parsed = false;
        for (String url : live.getEpgXml()) {
            if (Thread.currentThread().isInterrupted()) break;
            try { EpgParser.start(live, url, true); parsed = true; }
            catch (Exception ignored) { }
        }
        return parsed;
    }

    public static boolean hasRealDay(Channel channel, LocalDate day) {
        return channel.getDataList().stream().anyMatch(epg -> epg.equal(day.format(Formatters.DATE)) && validDay(epg, day));
    }

    public static boolean validDay(Epg epg, LocalDate day) {
        return epg.equal(day.format(Formatters.DATE)) && epg.getList().stream().anyMatch(item -> item != null
                && item.getStartTime() > 0 && item.getEndTime() > item.getStartTime()
                && !EpgTimeline.unknown(item.getTitle(), item.getEndTime() - item.getStartTime()));
    }

    private static void fetchEpgDay(@NonNull Channel item, @NonNull ZoneId zoneId, @NonNull LocalDate day) {
        fetchEpgDay(item, zoneId, day, false);
    }

    private static boolean fetchEpgDay(@NonNull Channel item, @NonNull ZoneId zoneId, @NonNull LocalDate day, boolean force) {
        String date = day.format(Formatters.DATE);
        String url = item.getEpg().replace("{date}", date);
        boolean need = url.startsWith("http") && (force || item.getDataList().stream().noneMatch(epg -> epg.equal(date)));
        if (!need) return false;
        Epg epg = Epg.objectFrom(OkHttp.string(url), item.getTvgId(), date, zoneId);
        if (Thread.currentThread().isInterrupted() || !epg.equal(date) || (force && !validDay(epg, day))) return false;
        item.setData(epg);
        return true;
    }
}
