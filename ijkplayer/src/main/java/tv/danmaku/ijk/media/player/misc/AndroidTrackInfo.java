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

import android.media.MediaFormat;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndroidTrackInfo implements ITrackInfo {

    private final MediaPlayer.TrackInfo mTrackInfo;
    private int mTrackType = MEDIA_TRACK_TYPE_UNKNOWN;

    public static List<ITrackInfo> fromMediaPlayer(MediaPlayer mp) {
        return fromTrackInfo(mp.getTrackInfo());
    }

    private static List<ITrackInfo> fromTrackInfo(MediaPlayer.TrackInfo[] items) {
        if (items == null) return Collections.emptyList();
        List<ITrackInfo> trackInfos = new ArrayList<>();
        for (MediaPlayer.TrackInfo item : items) trackInfos.add(new AndroidTrackInfo(item));
        return trackInfos;
    }

    private AndroidTrackInfo(MediaPlayer.TrackInfo trackInfo) {
        initTrackType(mTrackInfo = trackInfo);
    }

    private void initTrackType(MediaPlayer.TrackInfo trackInfo) {
        if (trackInfo == null) {
            setTrackType(MEDIA_TRACK_TYPE_UNKNOWN);
        } else if (trackInfo.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        } else if (trackInfo.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        } else if (trackInfo.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT || trackInfo.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
            setTrackType(ITrackInfo.MEDIA_TRACK_TYPE_TEXT);
        }
    }

    @Override
    public String getLanguage() {
        if (mTrackInfo == null) return "und";
        return mTrackInfo.getLanguage();
    }

    @Override
    public int getChannelCount() {
        if (mTrackInfo == null || mTrackInfo.getFormat() == null) return 0;
        return mTrackInfo.getFormat().getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    }

    @Override
    public int getBitrate() {
        if (mTrackInfo == null || mTrackInfo.getFormat() == null) return 0;
        return mTrackInfo.getFormat().getInteger(MediaFormat.KEY_BIT_RATE);
    }

    @Override
    public int getWidth() {
        if (mTrackInfo == null || mTrackInfo.getFormat() == null) return 0;
        return mTrackInfo.getFormat().getInteger(MediaFormat.KEY_WIDTH);
    }

    @Override
    public int getHeight() {
        if (mTrackInfo == null || mTrackInfo.getFormat() == null) return 0;
        return mTrackInfo.getFormat().getInteger(MediaFormat.KEY_HEIGHT);
    }

    @Override
    public int getTrackType() {
        return mTrackType;
    }

    public void setTrackType(int trackType) {
        mTrackType = trackType;
    }
}