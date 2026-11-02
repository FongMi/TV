package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.utils.Formatters;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Locale;

/** Local programme view and bounded replay request; never inserted into the source cache. */
public final class EpgProgramme extends EpgData {
    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private final transient EpgData origin;
    private final transient LocalDate day;
    private final transient ZoneId zone;
    private transient volatile long requestedEnd;

    public EpgProgramme(EpgData origin, long start, long end, String title, LocalDate day, ZoneId zone) {
        this.origin = origin; this.day = day; this.zone = zone;
        setTitle(title); setStartTime(start); setEndTime(end);
        setStart(Instant.ofEpochMilli(start).atZone(zone).format(SECONDS));
        setEnd(Instant.ofEpochMilli(end).atZone(zone).format(SECONDS));
    }

    public boolean isPlaceholder() {
        return origin == null;
    }

    public EpgData getOrigin() {
        return origin;
    }

    public EpgProgramme from(long start) {
        return new EpgProgramme(origin, start, getEndTime(), getTitle(), day, zone);
    }

    public static long playbackEnd(EpgData item) {
        return item instanceof EpgProgramme && ((EpgProgramme) item).requestedEnd > 0
                ? ((EpgProgramme) item).requestedEnd : item.getEndTime();
    }

    public static EpgData forRequest(EpgData item, long now) {
        if (!(item instanceof EpgProgramme)) return item;
        EpgProgramme programme = (EpgProgramme) item;
        long end = Math.min(item.getEndTime(), now / 1000 * 1000);
        if (end <= item.getStartTime()) throw new IllegalArgumentException("No recorded content is available for this interval");
        programme.requestedEnd = end;
        return new EpgProgramme(programme.origin, item.getStartTime(), end, item.getTitle(), programme.day, programme.zone);
    }

    @Override
    public String getTime() {
        return time(getEndTime());
    }

    public String playbackFormat() {
        return time(playbackEnd(this)) + "  " + getTitle();
    }

    private String time(long end) {
        boolean seconds = end - getStartTime() < 60000;
        DateTimeFormatter format = seconds ? SECONDS : Formatters.TIME;
        var startTime = Instant.ofEpochMilli(getStartTime()).atZone(zone);
        var endTime = Instant.ofEpochMilli(end).atZone(zone);
        String from = startTime.format(format), to = endTime.format(format);
        if (startTime.toLocalDate().isBefore(day)) from = startTime.format(Formatters.DATE) + " " + from;
        if (end == day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()) to = seconds ? "24:00:00" : "24:00";
        else if (endTime.toLocalDate().isAfter(day)) to = endTime.format(Formatters.DATE) + " " + to;
        return from + " ~ " + to;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EpgData item)) return false;
        return getStartTime() == item.getStartTime() && getEndTime() == item.getEndTime() && getTitle().equals(item.getTitle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartTime(), getEndTime(), getTitle());
    }
}
