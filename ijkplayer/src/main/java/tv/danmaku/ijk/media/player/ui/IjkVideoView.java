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

    private final String TAG = "IjkVideoView";

    private Uri mUri;
    private Map<String, String> mHeaders;

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int codec = IjkMediaPlayer.OPT_CATEGORY_CODEC;
    private static final int format = IjkMediaPlayer.OPT_CATEGORY_FORMAT;
    private static final int player = IjkMediaPlayer.OPT_CATEGORY_PLAYER;

    public static final int RENDER_SURFACE_VIEW = 0;
    public static final int RENDER_TEXTURE_VIEW = 1;

    private float mCurrentSpeed = 1;
    private int mCurrentAspectRatio;
    private int mCurrentRender;
    private int mCurrentDecode;
    private int mStartPosition;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private int mCurrentBufferPercentage;
    private long mCurrentBufferPosition;

    private IRenderView.ISurfaceHolder mSurfaceHolder = null;
    private IjkMediaPlayer mIjkPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoRotationDegree;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;

    private Context mAppContext;
    private IRenderView mRenderView;
    private int mVideoSarNum;
    private int mVideoSarDen;

    private FrameLayout mContentFrame;
    private SubtitleView mSubtitleView;

    public IjkVideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.ijk_player_view, this);
        mAppContext = context.getApplicationContext();
        mContentFrame = findViewById(R.id.ijk_content_frame);
        mSubtitleView = findViewById(R.id.ijk_subtitle);
        mVideoWidth = 0;
        mVideoHeight = 0;
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    public void setRender(int render) {
        mCurrentRender = render;
        if (mIjkPlayer == null) return;
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
        mUri = uri;
        mHeaders = headers;
        openVideo();
        requestLayout();
        invalidate();
    }

    private void openVideo() {
        if (mUri == null) return;
        release(false);
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        try {
            createPlayer();
            fixUserAgent();
            setSpeed(mCurrentSpeed);
            setRender(mCurrentRender);
            mCurrentBufferPosition = 0;
            mCurrentBufferPercentage = 0;
            mIjkPlayer.setDataSource(mAppContext, mUri, mHeaders);
            bindSurfaceHolder(mIjkPlayer, mSurfaceHolder);
            mIjkPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mIjkPlayer.setScreenOnWhilePlaying(true);
            mIjkPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to open content: " + mUri, e);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mIjkPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void fixUserAgent() {
        if (!mHeaders.containsKey(Utils.USER_AGENT)) mHeaders.put(Utils.USER_AGENT, Utils.CHROME);
        mIjkPlayer.setOption(format, "user_agent", mHeaders.get(Utils.USER_AGENT));
        mHeaders.remove(Utils.USER_AGENT);
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
            if (mOnPreparedListener != null) mOnPreparedListener.onPrepared(mIjkPlayer);
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
                mOnCompletionListener.onCompletion(mIjkPlayer);
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
                if (mOnErrorListener.onError(mIjkPlayer, framework_err, impl_err)) {
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
            if (mIjkPlayer != null && isValidState && hasValidSize) {
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (mIjkPlayer != null) bindSurfaceHolder(mIjkPlayer, holder);
            mSurfaceHolder = holder;
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (mIjkPlayer != null) mIjkPlayer.setDisplay(null);
            mSurfaceHolder = null;
        }
    };

    public void release() {
        release(true);
    }

    public void release(boolean clearState) {
        if (mIjkPlayer == null) return;
        mSubtitleView.setText("");
        removeRenderView();
        mIjkPlayer.reset();
        mIjkPlayer.release();
        mIjkPlayer = null;
        mCurrentState = STATE_IDLE;
        if (clearState) mTargetState = STATE_IDLE;
        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mIjkPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mIjkPlayer.isPlaying()) {
                mIjkPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) return (int) mIjkPlayer.getDuration();
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) return (int) mIjkPlayer.getCurrentPosition();
        return 0;
    }

    @Override
    public void seekTo(int positionMs) {
        if (!isInPlaybackState()) return;
        mInfoListener.onInfo(mIjkPlayer, IMediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mIjkPlayer.seekTo(positionMs);
        mStartPosition = 0;
    }

    public void seekTo(long positionMs) {
        mStartPosition = (int) positionMs;
        seekTo(mStartPosition);
    }

    public void setSpeed(float speed) {
        if (mIjkPlayer != null) mIjkPlayer.setSpeed(speed);
        mCurrentSpeed = speed;
    }

    public float getSpeed() {
        if (mIjkPlayer != null) return mIjkPlayer.getSpeed();
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
        return isInPlaybackState() && mIjkPlayer.isPlaying();
    }

    public long getBufferedPosition() {
        if (mIjkPlayer != null) return mCurrentBufferPosition;
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        if (mIjkPlayer != null) return mCurrentBufferPercentage;
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mIjkPlayer != null && mCurrentState != STATE_ERROR && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
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
        if (mIjkPlayer == null) return false;
        for (IjkTrackInfo trackInfo : getTrackInfo()) if (trackInfo.getTrackType() == type) ++count;
        return count > 0;
    }

    public List<IjkTrackInfo> getTrackInfo() {
        return mIjkPlayer.getTrackInfo();
    }

    public int getSelectedTrack(int type) {
        return mIjkPlayer.getSelectedTrack(type);
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
                mIjkPlayer.selectTrack(index);
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
                mIjkPlayer.deselectTrack(track);
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
                mIjkPlayer.selectTrack(index);
                break;
            }
        }
    }

    private void createPlayer() {
        String url = mUri.toString();
        mIjkPlayer = new IjkMediaPlayer();
        mIjkPlayer.setOnPreparedListener(mPreparedListener);
        mIjkPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mIjkPlayer.setOnCompletionListener(mCompletionListener);
        mIjkPlayer.setOnErrorListener(mErrorListener);
        mIjkPlayer.setOnInfoListener(mInfoListener);
        mIjkPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
        mIjkPlayer.setOnTimedTextListener(mOnTimedTextListener);
        mIjkPlayer.setOption(codec, "skip_loop_filter", 48);
        mIjkPlayer.setOption(format, "dns_cache_clear", 1);
        mIjkPlayer.setOption(format, "dns_cache_timeout", -1);
        mIjkPlayer.setOption(format, "fflags", "fastseek");
        mIjkPlayer.setOption(format, "http-detect-range-support", 0);
        mIjkPlayer.setOption(player, "enable-accurate-seek", 0);
        mIjkPlayer.setOption(player, "framedrop", 1);
        mIjkPlayer.setOption(player, "max-buffer-size", 15 * 1024 * 1024);
        mIjkPlayer.setOption(player, "mediacodec", mCurrentDecode);
        mIjkPlayer.setOption(player, "mediacodec-hevc", mCurrentDecode);
        mIjkPlayer.setOption(player, "mediacodec-all-videos", mCurrentDecode);
        mIjkPlayer.setOption(player, "mediacodec-auto-rotate", mCurrentDecode);
        mIjkPlayer.setOption(player, "mediacodec-handle-resolution-change", mCurrentDecode);
        mIjkPlayer.setOption(player, "opensles", 0);
        mIjkPlayer.setOption(player, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        mIjkPlayer.setOption(player, "reconnect", 1);
        mIjkPlayer.setOption(player, "soundtouch", 1);
        mIjkPlayer.setOption(player, "start-on-prepared", 1);
        mIjkPlayer.setOption(player, "subtitle", 1);
        mIjkPlayer.setOption(format, "protocol_whitelist", "async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtmp,rtp,tcp,tls,udp,ijkurlhook,data");
        if (url.contains("rtsp") || url.contains("udp") || url.contains("rtp")) {
            mIjkPlayer.setOption(format, "infbuf", 1);
            mIjkPlayer.setOption(format, "rtsp_transport", "tcp");
            mIjkPlayer.setOption(format, "rtsp_flags", "prefer_tcp");
            mIjkPlayer.setOption(format, "probesize", 512 * 1000);
            mIjkPlayer.setOption(format, "analyzeduration", 2 * 1000 * 1000);
        }
    }
}