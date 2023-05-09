package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.MediaController;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.player.R;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

public class IjkVideoView extends FrameLayout implements MediaController.MediaPlayerControl {

    private final String TAG = IjkVideoView.class.getSimpleName();

    private static final int codec = IjkMediaPlayer.OPT_CATEGORY_CODEC;
    private static final int format = IjkMediaPlayer.OPT_CATEGORY_FORMAT;
    private static final int player = IjkMediaPlayer.OPT_CATEGORY_PLAYER;

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int RENDER_SURFACE_VIEW = 0;
    private static final int RENDER_TEXTURE_VIEW = 1;

    private int mVideoRotationDegree;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;

    private int mTargetState;
    private int mCurrentState;
    private int mCurrentDecode;
    private int mCurrentRender;
    private int mCurrentAspectRatio;
    private int mStartPosition;

    private int mCurrentBufferPercentage;
    private long mCurrentBufferPosition;
    private float mCurrentSpeed;

    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;

    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private IRenderView mRenderView;

    private final SubtitleView mSubtitleView;
    private final FrameLayout mContentFrame;
    private final Context mContext;
    private IjkMediaPlayer mPlayer;

    public IjkVideoView(Context context) {
        this(context, null);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context.getApplicationContext();
        LayoutInflater.from(context).inflate(R.layout.ijk_player_view, this);
        mContentFrame = findViewById(R.id.ijk_content_frame);
        mSubtitleView = findViewById(R.id.ijk_subtitle);
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        mCurrentSpeed = 1;
    }

    public void setRender(int render) {
        mCurrentRender = render;
        if (mPlayer == null) return;
        switch (render) {
            case RENDER_TEXTURE_VIEW:
                setRenderView(new TextureRenderView(getContext()));
                break;
            case RENDER_SURFACE_VIEW:
                setRenderView(new SurfaceRenderView(getContext()));
                break;
        }
    }

    private void setRenderView(IRenderView renderView) {
        removeRenderView();
        mRenderView = renderView;
        setResizeMode(mCurrentAspectRatio);
        mContentFrame.addView(mRenderView.getView(), 0, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private void removeRenderView() {
        if (mRenderView == null) return;
        mContentFrame.removeView(mRenderView.getView());
        mRenderView.removeRenderCallback(mSHCallback);
        mRenderView = null;
    }

    public void setResizeMode(int resizeMode) {
        mCurrentAspectRatio = resizeMode;
        if (mRenderView != null) mRenderView.setAspectRatio(resizeMode);
    }

    public void setMediaSource(String path, Map<String, String> headers) {
        setVideoURI(Uri.parse(path.trim().replace("\\", "")), headers);
    }

    public void setVideoURI(Uri uri, Map<String, String> headers) {
        openVideo(uri, headers);
        requestLayout();
        invalidate();
    }

    private void openVideo(Uri uri, Map<String, String> headers) {
        release(false);
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        try {
            createPlayer(uri);
            fixUserAgent(headers);
            setSpeed(mCurrentSpeed);
            setRender(mCurrentRender);
            mCurrentBufferPosition = 0;
            mCurrentBufferPercentage = 0;
            mPlayer.setDataSource(mContext, uri, headers);
            bindSurfaceHolder(mPlayer, mSurfaceHolder);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setScreenOnWhilePlaying(true);
            mPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to open content: " + uri, e);
            mErrorListener.onError(mPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
        }
    }

    private void fixUserAgent(Map<String, String> headers) {
        if (!headers.containsKey(Utils.USER_AGENT)) headers.put(Utils.USER_AGENT, Utils.CHROME);
        mPlayer.setOption(format, "user_agent", headers.get(Utils.USER_AGENT));
        headers.remove(Utils.USER_AGENT);
    }

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mVideoSarNum = mp.getVideoSarNum();
            mVideoSarDen = mp.getVideoSarDen();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                }
                requestLayout();
            }
        }
    };

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            setPreferredTextLanguage();
            mCurrentState = STATE_PREPARED;
            if (mStartPosition > 0) seekTo(mStartPosition);
            if (mOnPreparedListener != null) mOnPreparedListener.onPrepared(mPlayer);
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        if (mTargetState == STATE_PLAYING) start();
                    }
                }
            } else {
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private final IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mPlayer);
            }
        }
    };

    private final IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, what, extra);
            }
            if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
                mVideoRotationDegree = extra;
                if (mRenderView != null) mRenderView.setVideoRotation(extra);
            }
            return true;
        }
    };

    private final IMediaPlayer.OnErrorListener mErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
            Log.d(TAG, "Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mPlayer, framework_err, impl_err)) {
                    return true;
                }
            }
            return true;
        }
    };

    private final IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, long position) {
            mCurrentBufferPosition = position;
        }

        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    private final IMediaPlayer.OnTimedTextListener mOnTimedTextListener = new IMediaPlayer.OnTimedTextListener() {
        @Override
        public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
            mSubtitleView.onSubtitleChanged(text.getText());
        }
    };

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null || holder == null) return;
        holder.bindToMediaPlayer(mp);
    }

    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mPlayer != null && isValidState && hasValidSize) {
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (mPlayer != null) bindSurfaceHolder(mPlayer, holder);
            mSurfaceHolder = holder;
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (mPlayer != null) mPlayer.setDisplay(null);
            mSurfaceHolder = null;
        }
    };

    public void release() {
        release(true);
    }

    public void release(boolean clearState) {
        if (mPlayer == null) return;
        mSubtitleView.setText("");
        removeRenderView();
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;
        mCurrentState = STATE_IDLE;
        if (clearState) mTargetState = STATE_IDLE;
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) return (int) mPlayer.getDuration();
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) return (int) mPlayer.getCurrentPosition();
        return 0;
    }

    @Override
    public void seekTo(int positionMs) {
        if (!isInPlaybackState()) return;
        mInfoListener.onInfo(mPlayer, IMediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mPlayer.seekTo(positionMs);
        mStartPosition = 0;
    }

    public void seekTo(long positionMs) {
        mStartPosition = (int) positionMs;
        seekTo(mStartPosition);
    }

    public void setSpeed(float speed) {
        if (mPlayer != null) mPlayer.setSpeed(speed);
        mCurrentSpeed = speed;
    }

    public float getSpeed() {
        if (mPlayer != null) return mPlayer.getSpeed();
        return mCurrentSpeed;
    }

    public void setDecode(int decode) {
        this.mCurrentDecode = decode;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public SubtitleView getSubtitleView() {
        return mSubtitleView;
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mPlayer.isPlaying();
    }

    public long getBufferedPosition() {
        if (mPlayer != null) return mCurrentBufferPosition;
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        if (mPlayer != null) return mCurrentBufferPercentage;
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mPlayer != null && mCurrentState != STATE_ERROR && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public boolean haveTrack(int type) {
        int count = 0;
        if (mPlayer == null) return false;
        for (IjkTrackInfo trackInfo : getTrackInfo()) if (trackInfo.getTrackType() == type) ++count;
        return count > 0;
    }

    public List<IjkTrackInfo> getTrackInfo() {
        return mPlayer.getTrackInfo();
    }

    public int getSelectedTrack(int type) {
        return mPlayer.getSelectedTrack(type);
    }

    public void selectTrack(int type, int track) {
        int selected = getSelectedTrack(type);
        List<IjkTrackInfo> trackInfos = getTrackInfo();
        for (int index = 0; index < trackInfos.size(); index++) {
            IjkTrackInfo trackInfo = trackInfos.get(index);
            if (trackInfo.getTrackType() != type) continue;
            if (index == track && selected != track) {
                long position = getCurrentPosition();
                mSubtitleView.setText("");
                mPlayer.selectTrack(index);
                if (position != 0) seekTo(position);
            }
        }
    }

    public void deselectTrack(int type, int track) {
        int selected = getSelectedTrack(type);
        List<IjkTrackInfo> trackInfos = getTrackInfo();
        for (int index = 0; index < trackInfos.size(); index++) {
            IjkTrackInfo trackInfo = trackInfos.get(index);
            if (trackInfo.getTrackType() != type) continue;
            if (index == track && selected == track) {
                long position = getCurrentPosition();
                mSubtitleView.setText("");
                mPlayer.deselectTrack(track);
                if (position != 0) seekTo(position);
            }
        }
    }

    private void setPreferredTextLanguage() {
        List<IjkTrackInfo> trackInfos = getTrackInfo();
        int selected = getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TEXT);
        for (int index = 0; index < trackInfos.size(); index++) {
            IjkTrackInfo trackInfo = trackInfos.get(index);
            if (trackInfo.getTrackType() != ITrackInfo.MEDIA_TRACK_TYPE_TEXT) continue;
            if (trackInfo.getLanguage().equals("zh") && index != selected) {
                mPlayer.selectTrack(index);
                break;
            }
        }
    }

    private void createPlayer(Uri uri) {
        String url = uri.toString();
        mPlayer = new IjkMediaPlayer();
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mPlayer.setOnCompletionListener(mCompletionListener);
        mPlayer.setOnErrorListener(mErrorListener);
        mPlayer.setOnInfoListener(mInfoListener);
        mPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
        mPlayer.setOnTimedTextListener(mOnTimedTextListener);
        mPlayer.setOption(codec, "skip_loop_filter", 48);
        mPlayer.setOption(format, "dns_cache_clear", 1);
        mPlayer.setOption(format, "dns_cache_timeout", -1);
        mPlayer.setOption(format, "fflags", "fastseek");
        mPlayer.setOption(format, "http-detect-range-support", 0);
        mPlayer.setOption(player, "enable-accurate-seek", 0);
        mPlayer.setOption(player, "framedrop", 1);
        mPlayer.setOption(player, "max-buffer-size", 15 * 1024 * 1024);
        mPlayer.setOption(player, "mediacodec", mCurrentDecode);
        mPlayer.setOption(player, "mediacodec-hevc", mCurrentDecode);
        mPlayer.setOption(player, "mediacodec-all-videos", mCurrentDecode);
        mPlayer.setOption(player, "mediacodec-auto-rotate", mCurrentDecode);
        mPlayer.setOption(player, "mediacodec-handle-resolution-change", mCurrentDecode);
        mPlayer.setOption(player, "opensles", 0);
        mPlayer.setOption(player, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        mPlayer.setOption(player, "reconnect", 1);
        mPlayer.setOption(player, "soundtouch", 1);
        mPlayer.setOption(player, "start-on-prepared", 1);
        mPlayer.setOption(player, "subtitle", 1);
        mPlayer.setOption(format, "protocol_whitelist", "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtmp,rtp,tcp,tls,udp,ijkurlhook,data");
        if (url.contains("rtsp") || url.contains("udp") || url.contains("rtp")) {
            mPlayer.setOption(format, "infbuf", 1);
            mPlayer.setOption(format, "rtsp_transport", "tcp");
            mPlayer.setOption(format, "rtsp_flags", "prefer_tcp");
            mPlayer.setOption(format, "probesize", 512 * 1000);
            mPlayer.setOption(format, "analyzeduration", 2 * 1000 * 1000);
        }
    }
}