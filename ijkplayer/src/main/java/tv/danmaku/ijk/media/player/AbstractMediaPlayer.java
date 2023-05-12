package tv.danmaku.ijk.media.player;

import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

public abstract class AbstractMediaPlayer implements IMediaPlayer {

    private Listener mListener;

    public final void setListener(Listener listener) {
        mListener = listener;
    }

    public void resetListeners() {
        mListener = null;
    }

    protected final void notifyOnPrepared() {
        if (mListener != null) mListener.onPrepared(this);
    }

    protected final void notifyOnCompletion() {
        if (mListener != null) mListener.onCompletion(this);
    }

    protected final void notifyOnBufferingUpdate(long position) {
        if (mListener != null) mListener.onBufferingUpdate(this, position);
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mListener != null) mListener.onBufferingUpdate(this, percent);
    }

    protected final void notifyOnSeekComplete() {
        if (mListener != null) mListener.onSeekComplete(this);
    }

    protected final void notifyOnVideoSizeChanged(int width, int height, int sarNum, int sarDen) {
        if (mListener != null) mListener.onVideoSizeChanged(this, width, height, sarNum, sarDen);
    }

    protected final boolean notifyOnError(int what, int extra) {
        return mListener != null && mListener.onError(this, what, extra);
    }

    protected final void notifyOnInfo(int what, int extra) {
        if (mListener != null) mListener.onInfo(this, what, extra);
    }

    protected final void notifyOnTimedText(IjkTimedText text) {
        if (mListener != null) mListener.onTimedText(this, text);
    }

    public void setDataSource(IMediaDataSource mediaDataSource) {
        throw new UnsupportedOperationException();
    }
}
