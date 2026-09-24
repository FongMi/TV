package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.iso.IsoNavigationSession;
import androidx.media3.ui.PlayerView;

/** Draws ExoPlayer disc menu graphics above the video surface. */
public final class DiscMenuOverlayView extends View {

    private final PlayerView playerView;
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF videoRect = new RectF();
    private final int[] surfaceLocation = new int[2];
    private final int[] overlayLocation = new int[2];
    private final OnLayoutChangeListener surfaceLayoutListener = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> invalidate();
    @Nullable private View observedSurface;
    @Nullable private Bitmap bitmap;
    @Nullable private IsoNavigationSession.MenuHighlight highlight;
    private int version;

    public DiscMenuOverlayView(Context context, PlayerView playerView) {
        super(context);
        this.playerView = playerView;
        highlightPaint.setColor(0x88ffcc33);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
        setClickable(false);
    }

    public int getVersion() {
        return version;
    }

    public void setOverlay(IsoNavigationSession.MenuOverlay overlay) {
        version = overlay.version;
        if (overlay.argbPixels == null) {
            if (bitmap != null) bitmap.recycle();
            bitmap = null;
        } else {
            if (bitmap == null || bitmap.getWidth() != overlay.width || bitmap.getHeight() != overlay.height) {
                if (bitmap != null) bitmap.recycle();
                bitmap = Bitmap.createBitmap(overlay.width, overlay.height, Bitmap.Config.ARGB_8888);
            }
            bitmap.setPixels(overlay.argbPixels, 0, overlay.width, 0, 0, overlay.width, overlay.height);
        }
        invalidate();
    }

    public void setHighlight(@Nullable IsoNavigationSession.MenuHighlight highlight) {
        observeSurface();
        if (sameHighlight(this.highlight, highlight)) return;
        this.highlight = highlight;
        invalidate();
    }

    public void clear() {
        if (bitmap != null) bitmap.recycle();
        bitmap = null;
        highlight = null;
        version = 0;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        observeSurface();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (observedSurface != null) observedSurface.removeOnLayoutChangeListener(surfaceLayoutListener);
        observedSurface = null;
        super.onDetachedFromWindow();
    }

    private void observeSurface() {
        View surface = playerView.getVideoSurfaceView();
        if (surface == observedSurface) return;
        if (observedSurface != null) observedSurface.removeOnLayoutChangeListener(surfaceLayoutListener);
        observedSurface = surface;
        if (surface != null) surface.addOnLayoutChangeListener(surfaceLayoutListener);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        View surface = playerView.getVideoSurfaceView();
        if (surface == null || surface.getWidth() == 0 || surface.getHeight() == 0) return;
        surface.getLocationOnScreen(surfaceLocation);
        getLocationOnScreen(overlayLocation);
        videoRect.set(surfaceLocation[0] - overlayLocation[0], surfaceLocation[1] - overlayLocation[1], surfaceLocation[0] - overlayLocation[0] + surface.getWidth(), surfaceLocation[1] - overlayLocation[1] + surface.getHeight());
        if (bitmap != null) canvas.drawBitmap(bitmap, null, videoRect, imagePaint);
        if (highlight != null) {
            Player player = playerView.getPlayer();
            VideoSize size = player != null ? player.getVideoSize() : VideoSize.UNKNOWN;
            int width = highlight.videoWidth > 0 ? highlight.videoWidth : size.width > 0 ? size.width : 720;
            int height = highlight.videoHeight > 0 ? highlight.videoHeight : size.height > 0 ? size.height : 480;
            float sx = videoRect.width() / width;
            float sy = videoRect.height() / height;
            canvas.drawRect(videoRect.left + highlight.left * sx, videoRect.top + highlight.top * sy, videoRect.left + highlight.right * sx, videoRect.top + highlight.bottom * sy, highlightPaint);
        }
    }

    private static boolean sameHighlight(@Nullable IsoNavigationSession.MenuHighlight a, @Nullable IsoNavigationSession.MenuHighlight b) {
        return a == b || a != null && b != null && a.button == b.button && a.left == b.left && a.top == b.top && a.right == b.right && a.bottom == b.bottom && a.palette == b.palette && a.videoWidth == b.videoWidth && a.videoHeight == b.videoHeight;
    }
}
