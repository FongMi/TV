package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class SubtitleView extends TextView {

    private boolean isDrawing;
    private float strokeWidth;

    public SubtitleView(Context context) {
        super(context);
        init();
    }

    public SubtitleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SubtitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidth = Utils.dp2px(getContext(), 0.6f);
        setTypeface(Typeface.DEFAULT_BOLD);
        setGravity(Gravity.CENTER);
        setTextSize(16);
        setZ(99);
    }

    @Override
    public void invalidate() {
        if (isDrawing) return;
        super.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        isDrawing = true;
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.FILL);
        setTextColor(Color.WHITE);
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(strokeWidth);
        setTextColor(Color.BLACK);
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        setTextColor(Color.WHITE);
        isDrawing = false;
    }
}
