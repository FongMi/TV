package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.bean.Result;

public record PlaybackResult<T>(T request, Result result) {
}
