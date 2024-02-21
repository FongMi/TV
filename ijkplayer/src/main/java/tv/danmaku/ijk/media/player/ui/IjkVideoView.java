package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.ui.SubtitleView;

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

    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_ENDED = 5;

    private static final int codec = IjkMediaPlayer.OPT_CATEGORY_CODEC;
    private static final int format = IjkMediaPlayer.OPT_CATEGORY_FORMAT;
    private static final int player = IjkMediaPlayer.OPT_CATEGORY_PLAYER;

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
    private long mStartPosition;

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
    private final ImageView mArtworkView;
    private Drawable mDefaultArtwork;
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
        mArtworkView = findViewById(R.id.ijk_artwork);
        mCurrentPlayer = PLAYER_NONE;
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        mCurrentSpeed = 1.0f;
        setSubtitleView();
    }

    private void setSubtitleView() {
        if (mSubtitleView == null) return;
        mSubtitleView.setUserDefaultStyle();
        mSubtitleView.setUserDefaultTextSize();
        mSubtitleView.setApplyEmbeddedFontSizes(false);
    }

    private void initAttr(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IjkVideoView, defStyleAttr, 0);
        try {
            mDefaultArtwork = context.getDrawable(a.getResourceId(R.styleable.IjkVideoView_default_artwork, 0));
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

    public void setWakeMode(int mode) {
        mPlayer.setWakeMode(getContext(), mode);
    }

    public void setMediaSource(MediaSource source) {
        setMediaSource(source, 0);
    }

    public void setMediaSource(MediaSource source, long position) {
        setVideoURI(source.getUri(), source.getHeaders());
        mStartPosition = position;
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
        mSubtitleView.setCues(null);
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
        seekTo((long) positionMs);
    }

    public void seekTo(long positionMs) {
        onInfo(mPlayer, IMediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mPlayer.seekTo(positionMs);
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

    public int getPlaybackState() {
        return mCurrentState;
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
        return mPlayer.getAudioSessionId();
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
        long position = getCurrentPosition();
        List<ITrackInfo> trackInfos = getTrackInfo();
        for (int index = 0; index < trackInfos.size(); index++) {
            ITrackInfo trackInfo = trackInfos.get(index);
            if (trackInfo.getTrackType() != type) continue;
            if (index == track && selected != track) {
                mSubtitleView.setCues(null);
                mPlayer.selectTrack(index);
                updateForCurrentTrackSelections();
                if (position > 0) seekTo(position);
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
                mSubtitleView.setCues(null);
                mPlayer.deselectTrack(track);
                updateForCurrentTrackSelections();
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

    public void setDefaultArtwork(@Nullable Drawable defaultArtwork) {
        if (this.mDefaultArtwork != defaultArtwork) {
            this.mDefaultArtwork = defaultArtwork;
            updateForCurrentTrackSelections();
        }
    }

    public Bitmap getDefaultArtwork() {
        return ((BitmapDrawable) mDefaultArtwork).getBitmap();
    }

    private void updateForCurrentTrackSelections() {
        if (mPlayer == null || mPlayer.getTrackInfo().isEmpty()) return;
        int select = getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        if (select >= 0) {
            mArtworkView.setVisibility(GONE);
            setRenderView(mCurrentRender);
        } else {
            removeRenderView();
            setDrawableArtwork(mDefaultArtwork);
        }
    }

    private void setDrawableArtwork(Drawable drawable) {
        if (drawable == null) return;
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth == 0 || drawableHeight == 0) return;
        mArtworkView.setImageDrawable(drawable);
        mArtworkView.setVisibility(VISIBLE);
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
        updateForCurrentTrackSelections();
        if (mCurrentSpeed > 0) setSpeed(mCurrentSpeed);
        if (mStartPosition > 0) seekTo(mStartPosition);
        mListener.onPrepared(mPlayer);
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        if (mTargetState == STATE_PLAYING) start();
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mCurrentState = STATE_ENDED;
        mTargetState = STATE_ENDED;
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
        mSubtitleView.setCues(SubtitleParser.parse(text.getText()));
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        mListener.onBufferingUpdate(mp, percent);
        mCurrentBufferPercentage = percent;
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, long position) {
        mListener.onBufferingUpdate(mp, position);
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