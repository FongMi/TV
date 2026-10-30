package com.fongmi.android.tv.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Calendar days in the EPG picker; the live clock never follows its selection. */
public final class EpgHistory {
    public static final int PREVIOUS_DAYS = 7;
    public static final int FUTURE_DAYS = 2;

    private EpgHistory() {}

    public static List<LocalDate> dates(LocalDate today) {
        List<LocalDate> result = new ArrayList<>();
        for (int offset = FUTURE_DAYS; offset >= -PREVIOUS_DAYS; offset--) result.add(today.plusDays(offset));
        return result;
    }

    public static LocalDate clamp(LocalDate selected, LocalDate today) {
        if (selected == null || selected.isAfter(today.plusDays(FUTURE_DAYS))) return today;
        LocalDate first = today.minusDays(PREVIOUS_DAYS);
        return selected.isBefore(first) ? first : selected;
    }
}
