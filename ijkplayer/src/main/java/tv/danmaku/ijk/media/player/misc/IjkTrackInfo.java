/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.player.misc;

import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaMeta;

public class IjkTrackInfo implements ITrackInfo {

    private final IjkMediaMeta.IjkStreamMeta mStreamMeta;
    private int mTrackType = MEDIA_TRACK_TYPE_UNKNOWN;

    public static List<ITrackInfo> fromMediaMeta(Bundle bundle) {
        if (bundle == null) return Collections.emptyList();
        return fromMediaMeta(IjkMediaMeta.parse(bundle));
    }

    private static List<ITrackInfo> fromMediaMeta(IjkMediaMeta mediaMeta) {
        if (mediaMeta == null) return Collections.emptyList();
        List<ITrackInfo> trackInfos = new ArrayList<>();
        for (IjkMediaMeta.IjkStreamMeta streamMeta : mediaMeta.mStreams) trackInfos.add(new IjkTrackInfo(streamMeta));
        return trackInfos;
    }

    private IjkTrackInfo(IjkMediaMeta.IjkStreamMeta streamMeta) {
        initTrackType(mStreamMeta = streamMeta);
    }

    private void initTrackType(IjkMediaMeta.IjkStreamMeta streamMeta) {
        if (streamMeta.mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__VIDEO)) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        } else if (streamMeta.mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__AUDIO)) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        } else if (streamMeta.mType.equalsIgnoreCase(IjkMediaMeta.IJKM_VAL_TYPE__TIMEDTEXT)) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_TEXT);
        }
    }

    @Override
    public String getLanguage() {
        if (mStreamMeta == null || TextUtils.isEmpty(mStreamMeta.mLanguage)) return "und";
        return mStreamMeta.mLanguage;
    }

    @Override
    public int getChannelCount() {
        return mStreamMeta.getChannelCount();
    }

    @Override
    public int getBitrate() {
        return (int) mStreamMeta.mBitrate;
    }

    @Override
    public int getWidth() {
        return mStreamMeta.mWidth;
    }

    @Override
    public int getHeight() {
        return mStreamMeta.mHeight;
    }

    @Override
    public int getTrackType() {
        return mTrackType;
    }

    public void setTrackType(int trackType) {
        mTrackType = trackType;
    }
}
