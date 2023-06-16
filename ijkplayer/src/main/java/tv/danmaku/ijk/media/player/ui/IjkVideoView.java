package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.content.res.TypedArray;
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

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.player.MediaSource;
import tv.danmaku.ijk.media.player.R;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class IjkVideoView extends FrameLayout implements MediaController.MediaPlayerControl, IMediaPlayer.Listener, IRenderView.IRenderCallback {

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

    private static final int PLAYER_NONE = -1;
    private static final int PLAYER_SYS = 0;
    private static final int PLAYER_IJK = 1;

    private static final int RENDER_SURFACE_VIEW = 0;
    private static final int RENDER_TEXTURE_VIEW = 1;

    private int mVideoWidth;
    private int mVideoHeight;

    private int mTargetState;
    private int mCurrentState;
    private int mCurrentDecode;
    private int mCurrentRender;
    private int mCurrentPlayer;
    private int mCurrentAspectRatio;
    private int mStartPosition;

    private int mCurrentBufferPercentage;
    private long mCurrentBufferPosition;
    private float mCurrentSpeed;

    private boolean mKeepContentOnPlayerReset;

    private IRenderView.ISurfaceHolder mSurfaceHolder;
    private IMediaPlayer.Listener mListener;
    private IRenderView mRenderView;

    private final SubtitleView mSubtitleView;
    private final AudioManager mAudioManager;
    private final FrameLayout mContentFrame;
    private IMediaPlayer mPlayer;

    public IjkVideoView(Context context) {
        this(context, null);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.ijk_player_view, this);
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (attrs != null) initAttr(context, attrs, defStyleAttr);
        mContentFrame = findViewById(R.id.ijk_content_frame);
        mSubtitleView = findViewById(R.id.ijk_subtitle);
        mCurrentPlayer = PLAYER_NONE;
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        mCurrentSpeed = 1.0f;
    }

    private void initAttr(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IjkVideoView, defStyleAttr, 0);
        try {
            mKeepContentOnPlayerReset = a.getBoolean(R.styleable.IjkVideoView_keep_content_on_player_reset, mKeepContentOnPlayerReset);
        } finally {
            a.recycle();
        }
    }

    public IjkVideoView decode(int decode) {
        mCurrentDecode = decode;
        return this;
    }

    public IjkVideoView render(int render) {
        setRender(render);
        return this;
    }

    public void setPlayer(int type) {
        if (mCurrentPlayer == type) return;
        if (mPlayer != null) release();
        mCurrentPlayer = type;
        switch (type) {
            case PLAYER_SYS:
                mPlayer = new AndroidMediaPlayer().setListener(this);
                break;
            case PLAYER_IJK:
                mPlayer = new IjkMediaPlayer().setListener(this);
                break;
        }
    }

    public void addListener(IMediaPlayer.Listener listener) {
        mListener = listener;
    }

    public void setRender(int render) {
        mCurrentRender = render;
    }

    private void setRenderView(int render) {
        if (mRenderView != null) {
            bindSurfaceHolder(mPlayer, mSurfaceHolder);
            return;
        }
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
        mRenderView = renderView;
        setResizeMode(mCurrentAspectRatio);
        mContentFrame.addView(mRenderView.getView(), 0, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        mRenderView.addRenderCallback(this);
    }

    private void removeRenderView() {
        if (mRenderView == null) return;
        mContentFrame.removeView(mRenderView.getView());
        mRenderView.removeRenderCallback(this);
        mRenderView = null;
    }

    public void setResizeMode(int resizeMode) {
        mCurrentAspectRatio = resizeMode;
        if (mRenderView != null) mRenderView.setAspectRatio(resizeMode);
    }

    public void setMediaSource(MediaSource source) {
        setVideoURI(source.getUri(), source.getHeaders());
    }

    private void setVideoURI(Uri uri, Map<String, String> headers) {
        if (!mKeepContentOnPlayerReset) removeRenderView();
        openVideo(uri, headers);
        requestLayout();
        invalidate();
    }

    private void openVideo(Uri uri, Map<String, String> headers) {
        try {
            mPlayer.reset();
            setOptions(uri);
            setRenderView(mCurrentRender);
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            mPlayer.setDataSource(getContext(), uri, headers);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setScreenOnWhilePlaying(true);
            mPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to open content: " + uri, e);
            onError(mPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
        }
    }

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null || holder == null) return;
        holder.bindToMediaPlayer(mp);
    }

    public void stop() {
        if (mPlayer == null) return;
        mPlayer.stop();
        reset();
    }

    public void release() {
        if (mPlayer == null) return;
        mCurrentPlayer = PLAYER_NONE;
        mPlayer.release();
        mPlayer = null;
        reset();
    }

    private void reset() {
        removeRenderView();
        mSubtitleView.setText("");
        mTargetState = STATE_IDLE;
        mCurrentState = STATE_IDLE;
        mCurrentBufferPosition = 0;
        mCurrentBufferPercentage = 0;
        mAudioManager.abandonAudioFocus(null);
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
        if (isInPlaybackState() && mPlayer.isPlaying()) {
            mPlayer.pause();
            mCurrentState = STATE_PAUSED;
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
        onInfo(mPlayer, IMediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mPlayer.seekTo(positionMs);
        mStartPosition = 0;
    }

    public void seekTo(long positionMs) {
        mStartPosition = (int) positionMs;
        seekTo(mStartPosition);
    }

    public void setSpeed(float speed) {
        mCurrentSpeed = speed;
        if (isInPlaybackState()) mPlayer.setSpeed(speed);
    }

    public float getSpeed() {
        if (isInPlaybackState()) return mPlayer.getSpeed();
        return mCurrentSpeed;
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
        for (ITrackInfo trackInfo : getTrackInfo()) if (trackInfo.getTrackType() == type) ++count;
        return count > 0;
    }

    public List<ITrackInfo> getTrackInfo() {
        return mPlayer.getTrackInfo();
    }

    public int getSelectedTrack(int type) {
        return mPlayer.getSelectedTrack(type);
    }

    public void selectTrack(int type, int track) {
        int selected = getSelectedTrack(type);
        List<ITrackInfo> trackInfos = getTrackInfo();
        for (int index = 0; index < trackInfos.size(); index++) {
            ITrackInfo trackInfo = trackInfos.get(index);
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
        List<ITrackInfo> trackInfos = getTrackInfo();
        for (int index = 0; index < trackInfos.size(); index++) {
            ITrackInfo trackInfo = trackInfos.get(index);
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
        List<ITrackInfo> trackInfos = getTrackInfo();
        int selected = getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TEXT);
        for (int index = 0; index < trackInfos.size(); index++) {
            ITrackInfo trackInfo = trackInfos.get(index);
            if (trackInfo.getTrackType() != ITrackInfo.MEDIA_TRACK_TYPE_TEXT) continue;
            if (trackInfo.getLanguage().equals("zh") && index != selected) {
                mPlayer.selectTrack(index);
                break;
            }
        }
    }

    private void setOptions(Uri uri) {
        String url = uri.toString();
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
        mPlayer.setOption(format, "protocol_whitelist", "async,cache,crypto,file,http,https,pipe,rtmp,rtp,tcp,tls,udp,data,ijkinject,ijklongurl,ijksegment,ijkhttphook,ijklivehook,ijktcphook,ijkurlhook,ijkmediadatasource");
        if (url.contains("rtsp") || url.contains("udp") || url.contains("rtp")) {
            mPlayer.setOption(format, "infbuf", 1);
            mPlayer.setOption(format, "rtsp_transport", "tcp");
            mPlayer.setOption(format, "rtsp_flags", "prefer_tcp");
            mPlayer.setOption(format, "probesize", 512 * 1000);
            mPlayer.setOption(format, "analyzeduration", 2 * 1000 * 1000);
        }
    }

    @Override
    public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
        if (mPlayer != null) bindSurfaceHolder(mPlayer, mSurfaceHolder = holder);
    }

    @Override
    public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int width, int height) {
        boolean isValidState = mTargetState == STATE_PLAYING;
        boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == width && mVideoHeight == height);
        if (mPlayer != null && isValidState && hasValidSize) start();
    }

    @Override
    public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
        if (mPlayer != null) mPlayer.setDisplay(null);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        setPreferredTextLanguage();
        mCurrentState = STATE_PREPARED;
        if (mCurrentSpeed > 0) setSpeed(mCurrentSpeed);
        if (mStartPosition > 0) seekTo(mStartPosition);
        mListener.onPrepared(mPlayer);
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mTargetState == STATE_PLAYING) start();
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        mTargetState = STATE_PLAYBACK_COMPLETED;
        mListener.onCompletion(mPlayer);
    }

    @Override
    public void onInfo(IMediaPlayer mp, int what, int extra) {
        if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED && mRenderView != null) mRenderView.setVideoRotation(extra);
        mListener.onInfo(mp, what, extra);
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        mCurrentState = STATE_ERROR;
        mTargetState = STATE_ERROR;
        return mListener.onError(mPlayer, what, extra);
    }

    @Override
    public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
        mSubtitleView.onSubtitleChanged(text.getText());
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        mCurrentBufferPercentage = percent;
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, long position) {
        mCurrentBufferPosition = position;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mVideoWidth != 0 && mVideoHeight != 0 && mRenderView != null) {
            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
            mRenderView.setVideoSampleAspectRatio(sar_num, sar_den);
            requestLayout();
        }
    }
}