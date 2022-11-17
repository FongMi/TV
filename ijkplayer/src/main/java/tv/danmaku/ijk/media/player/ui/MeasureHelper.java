package tv.danmaku.ijk.media.player.ui;

import android.view.View;

import java.lang.ref.WeakReference;

public final class MeasureHelper {

    private final WeakReference<View> mWeakView;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;

    private int mVideoRotationDegree;

    private int mMeasuredWidth;
    private int mMeasuredHeight;

    private int mCurrentAspectRatio = IRenderView.AR_ASPECT_FIT_PARENT;

    public MeasureHelper(View view) {
        mWeakView = new WeakReference<>(view);
    }

    public View getView() {
        return mWeakView.get();
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
    }

    public void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen) {
        mVideoSarNum = videoSarNum;
        mVideoSarDen = videoSarDen;
    }

    public void setVideoRotation(int videoRotationDegree) {
        mVideoRotationDegree = videoRotationDegree;
    }

    public void doMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
            int tempSpec = widthMeasureSpec;
            widthMeasureSpec = heightMeasureSpec;
            heightMeasureSpec = tempSpec;
        }
        int width = View.getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = View.getDefaultSize(mVideoHeight, heightMeasureSpec);
        if (mCurrentAspectRatio == IRenderView.AR_MATCH_PARENT) {
            width = widthMeasureSpec;
            height = heightMeasureSpec;
        } else if (mVideoWidth > 0 && mVideoHeight > 0) {
            int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                float specAspectRatio = (float) widthSpecSize / (float) heightSpecSize;
                float displayAspectRatio;
                switch (mCurrentAspectRatio) {
                    case IRenderView.AR_16_9_FIT_PARENT:
                        displayAspectRatio = 16.0f / 9.0f;
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) displayAspectRatio = 1.0f / displayAspectRatio;
                        break;
                    case IRenderView.AR_4_3_FIT_PARENT:
                        displayAspectRatio = 4.0f / 3.0f;
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) displayAspectRatio = 1.0f / displayAspectRatio;
                        break;
                    case IRenderView.AR_ASPECT_FIT_PARENT:
                    case IRenderView.AR_ASPECT_FILL_PARENT:
                    case IRenderView.AR_ASPECT_WRAP_CONTENT:
                    default:
                        displayAspectRatio = (float) mVideoWidth / (float) mVideoHeight;
                        if (mVideoSarNum > 0 && mVideoSarDen > 0) displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen;
                        break;
                }
                boolean shouldBeWider = displayAspectRatio > specAspectRatio;
                switch (mCurrentAspectRatio) {
                    case IRenderView.AR_ASPECT_FIT_PARENT:
                    case IRenderView.AR_16_9_FIT_PARENT:
                    case IRenderView.AR_4_3_FIT_PARENT:
                        if (shouldBeWider) {
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio);
                        } else {
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio);
                        }
                        break;
                    case IRenderView.AR_ASPECT_FILL_PARENT:
                        if (shouldBeWider) {
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio);
                        } else {
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio);
                        }
                        break;
                    case IRenderView.AR_ASPECT_WRAP_CONTENT:
                    default:
                        if (shouldBeWider) {
                            width = Math.min(mVideoWidth, widthSpecSize);
                            height = (int) (width / displayAspectRatio);
                        } else {
                            height = Math.min(mVideoHeight, heightSpecSize);
                            width = (int) (height * displayAspectRatio);
                        }
                        break;
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize;
                height = heightSpecSize;
                if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize;
                }
            } else {
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        }
        mMeasuredWidth = width;
        mMeasuredHeight = height;
    }

    public int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    public int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    public void setAspectRatio(int aspectRatio) {
        mCurrentAspectRatio = aspectRatio;
    }
}