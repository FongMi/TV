package com.fongmi.android.tv.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Foreground refresh scheduling; old completions cannot release a newer request. */
public final class EpgRefreshPolicy {
    private record Key(Object source, Object channel, LocalDate date, LocalDate today, ZoneId zone, String endpoint) {
        @Override public boolean equals(Object other) {
            return other instanceof Key item && source == item.source && channel == item.channel
                    && date.equals(item.date) && today.equals(item.today) && zone.equals(item.zone) && endpoint.equals(item.endpoint);
        }
        @Override public int hashCode() {
            return Objects.hash(System.identityHashCode(source), System.identityHashCode(channel), date, today, zone, endpoint);
        }
    }

    private static final class State {
        long attempted;
        long succeeded;
        int missing;
    }

    private final Map<Key, State> states = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, State> item) { return size() > 64; }
    };
    private Object xmlSource;
    private LocalDate xmlDay;
    private long xmlAttempt;
    private long issued;
    private long pending;
    private State active;

    public long begin(Object source, Object channel, LocalDate date, LocalDate today, ZoneId zone,
                      String endpoint, boolean xml, boolean missing, long now) {
        if (pending != 0) return 0;
        if (xml && source == xmlSource && today.equals(xmlDay) && now >= xmlAttempt && now - xmlAttempt < 60000) return 0;
        Key key = new Key(source, xml ? null : channel, date, today, zone, endpoint);
        State state = states.computeIfAbsent(key, ignored -> new State());
        long delay = missing ? Math.min(900000L, 60000L << Math.max(0, Math.min(4, state.missing - 1))) : 60000L;
        if (state.attempted != 0 && now >= state.attempted && now - state.attempted < delay) return 0;
        if (!missing && state.succeeded != 0 && now >= state.succeeded && now - state.succeeded < 3600000) return 0;
        state.attempted = now;
        state.missing = missing ? Math.min(5, state.missing + 1) : 0;
        if (xml) { xmlSource = source; xmlDay = today; xmlAttempt = now; }
        active = state;
        return pending = ++issued;
    }

    public void complete(long token, boolean success, long now) {
        if (token == 0 || token != pending) return;
        if (success) active.succeeded = now;
        pending = 0;
        active = null;
    }

    public void reset() {
        states.clear(); active = null; pending = 0;
        xmlSource = null; xmlDay = null; xmlAttempt = 0;
    }
}
