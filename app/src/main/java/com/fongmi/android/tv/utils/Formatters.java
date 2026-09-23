package com.fongmi.android.tv.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatters {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    public static final DateTimeFormatter TIME_SEC = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter LOCAL_DATETIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter EPG_DT_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm", Locale.ROOT);
    public static final DateTimeFormatter EPG_DT_LONG = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter EPG_RANGE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
}
