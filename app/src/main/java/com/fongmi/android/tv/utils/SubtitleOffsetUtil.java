package com.fongmi.android.tv.utils;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.text.SubtitleOffsets;

public final class SubtitleOffsetUtil {

    public static final long MAX_OFFSET_MS = 300_000;

    private SubtitleOffsetUtil() {
    }

    /** Shifts the requested roles together, or rejects the entire change if either exceeds the UI range. */
    @Nullable
    public static SubtitleOffsets shift(SubtitleOffsets previous, long deltaMs, boolean primary, boolean secondary) {
        long main = primary ? shift(previous.primaryMs, deltaMs) : previous.primaryMs;
        long sub = secondary ? shift(previous.secondaryMs, deltaMs) : previous.secondaryMs;
        return main == C.TIME_UNSET || sub == C.TIME_UNSET ? null : new SubtitleOffsets(main, sub);
    }

    /** Returns the shifted delay, or {@link C#TIME_UNSET} if it overflows or exceeds the UI range. */
    public static long shift(long previousMs, long deltaMs) {
        try {
            long result = Math.addExact(previousMs, deltaMs);
            return result < -MAX_OFFSET_MS || result > MAX_OFFSET_MS ? C.TIME_UNSET : result;
        } catch (ArithmeticException e) {
            return C.TIME_UNSET;
        }
    }
}
