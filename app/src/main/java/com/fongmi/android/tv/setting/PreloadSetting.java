package com.fongmi.android.tv.setting;

import com.github.catvod.utils.Prefers;

public class PreloadSetting {

    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 10;
    public static final int MIN_SIZE_MB = 128;
    public static final int MAX_SIZE_MB = 4096;
    public static final int STEP_SIZE_MB = 128;
    public static final int MIN_TIME_SECONDS = 20;
    public static final int MAX_TIME_SECONDS = 120;
    public static final int STEP_TIME_SECONDS = 10;

    public static boolean isEnabled() {
        return Prefers.getBoolean("preload");
    }

    public static void putEnabled(boolean preload) {
        Prefers.put("preload", preload);
    }

    public static boolean isNextEpisodeEnabled() {
        return Prefers.getBoolean("preload_next_episode");
    }

    public static void putNextEpisodeEnabled(boolean enabled) {
        Prefers.put("preload_next_episode", enabled);
    }

    public static int getThreads() {
        return Math.clamp(Prefers.getInt("preload_threads", MIN_THREADS), MIN_THREADS, MAX_THREADS);
    }

    public static void putThreads(int threads) {
        Prefers.put("preload_threads", Math.clamp(threads, MIN_THREADS, MAX_THREADS));
    }

    public static int getSizeMb() {
        int size = Math.clamp(Prefers.getInt("preload_size", MIN_SIZE_MB), MIN_SIZE_MB, MAX_SIZE_MB);
        return Math.clamp(MIN_SIZE_MB + (long) Math.round((float) (size - MIN_SIZE_MB) / STEP_SIZE_MB) * STEP_SIZE_MB, MIN_SIZE_MB, MAX_SIZE_MB);
    }

    public static void putSizeMb(int size) {
        Prefers.put("preload_size", Math.clamp(size, MIN_SIZE_MB, MAX_SIZE_MB));
    }

    public static long getSizeBytes() {
        return getSizeMb() * 1024L * 1024L;
    }

    public static int getTimeSeconds() {
        int seconds = Math.clamp(Prefers.getInt("preload_time", MAX_TIME_SECONDS), MIN_TIME_SECONDS, MAX_TIME_SECONDS);
        return Math.clamp(MIN_TIME_SECONDS + (long) Math.round((float) (seconds - MIN_TIME_SECONDS) / STEP_TIME_SECONDS) * STEP_TIME_SECONDS, MIN_TIME_SECONDS, MAX_TIME_SECONDS);
    }

    public static void putTimeSeconds(int seconds) {
        Prefers.put("preload_time", Math.clamp(seconds, MIN_TIME_SECONDS, MAX_TIME_SECONDS));
    }

    public static long getDurationMs() {
        return getTimeSeconds() * 1000L;
    }
}
