package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.SkipSegment;

final class VodSkipPolicy {

    private VodSkipPolicy() {
    }

    static long startPositionMs(History history, Result result, long resumePositionMs) {
        long opening = history == null ? C.TIME_UNSET : history.getOpening();
        boolean autoSkip = History.isAutoSkip(opening);
        long position = Math.max(autoSkip ? C.TIME_UNSET : opening, resumePositionMs);
        if (result.hasPosition()) position = Math.max(position, result.getPosition());
        if (!autoSkip) return position;
        for (SkipSegment segment : result.getSkipSegments()) {
            if (segment != null && segment.isOpening() && segment.isValid() && segment.getStart() == 0) position = Math.max(position, segment.getEnd());
        }
        return position;
    }

    static SkipSegment activeSegment(History history, Result result, long positionMs) {
        if (positionMs < 0) return null;
        long opening = history == null ? C.TIME_UNSET : history.getOpening();
        long ending = history == null ? C.TIME_UNSET : history.getEnding();
        for (SkipSegment segment : result.getSkipSegments()) {
            if (segment == null || !segment.contains(positionMs)) continue;
            if (segment.isMiddle()) return segment;
            if (segment.isOpening() && History.isAutoSkip(opening)) return segment;
            if (segment.isEnding() && History.isAutoSkip(ending)) return segment;
        }
        return null;
    }
}
