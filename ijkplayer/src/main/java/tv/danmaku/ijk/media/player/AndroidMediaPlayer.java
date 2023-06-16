/*
 * Copyright (C) 2006 Bilibili
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 Zhang Rui <bbcallen@gmail.com>
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

package tv.danmaku.ijk.media.player;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.misc.AndroidTrackInfo;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class AndroidMediaPlayer extends AbstractMediaPlayer implements MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnTimedTextListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnVideoSizeChangedListener {

    private final MediaPlayer mMediaPlayer;

    public AndroidMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnTimedTextListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        mMediaPlayer.setDisplay(sh);
    }

    @Override
    public void setSurface(Surface surface) {
        mMediaPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            setDataSource(uri.getPath());
        } else {
            mMediaPlayer.setDataSource(context, uri, headers);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaPlayer.setDataSource(path);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mMediaPlayer.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        mMediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mMediaPlayer.pause();
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        mMediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public List<ITrackInfo> getTrackInfo() {
        try {
            return AndroidTrackInfo.fromMediaPlayer(mMediaPlayer);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public int getVideoSarNum() {
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        return 1;
    }

    @Override
    public boolean isPlaying() {
        try {
            return mMediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mMediaPlayer.seekTo((int) msec);
    }

    @Override
    public boolean getCurrentFrame(Bitmap bitmap) {
        return false;
    }

    @Override
    public long getCurrentPosition() {
        try {
            return mMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public long getDuration() {
        try {
            return mMediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public void release() {
        resetListeners();
        mMediaPlayer.release();
    }

    @Override
    public void reset() {
        mMediaPlayer.reset();
    }

    @Override
    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    @Override
    public boolean isLooping() {
        return mMediaPlayer.isLooping();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
            mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public float getSpeed() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 1.0f;
            return mMediaPlayer.getPlaybackParams().getSpeed();
        } catch (Exception e) {
            return 1.0f;
        }
    }

    @Override
    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    @Override
    public int getSelectedTrack(int type) {
        try {
            return mMediaPlayer.getSelectedTrack(type);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void selectTrack(int track) {
        try {
            mMediaPlayer.selectTrack(track);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deselectTrack(int track) {
        try {
            mMediaPlayer.deselectTrack(track);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOption(int category, String name, String value) {
    }

    @Override
    public void setOption(int category, String name, long value) {
    }

    @Override
    public void setLogEnabled(boolean enable) {
    }

    @Override
    public boolean isPlayable() {
        return true;
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        mMediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        mMediaPlayer.setAudioStreamType(streamtype);
    }

    @Override
    public void setKeepInBackground(boolean keepInBackground) {
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        notifyOnBufferingUpdate(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        notifyOnCompletion();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return notifyOnError(what, extra);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        notifyOnInfo(what, extra);
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        notifyOnPrepared();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        notifyOnInfo(MEDIA_INFO_VIDEO_SEEK_RENDERING_START, 0);
    }

    @Override
    public void onTimedText(MediaPlayer mp, TimedText text) {
        if (text != null) notifyOnTimedText(new IjkTimedText(text.getBounds(), text.getText()));
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        notifyOnVideoSizeChanged(width, height, 1, 1);
    }
}