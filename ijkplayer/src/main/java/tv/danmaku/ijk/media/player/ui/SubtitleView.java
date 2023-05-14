package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class SubtitleView extends TextView {

    private final float strokeWidth;
    private boolean isDrawing;

    public SubtitleView(Context context) {
        this(context, null);
    }

    public SubtitleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        strokeWidth = Utils.dp2px(getContext(), 0.6f);
    }

    public void onSubtitleChanged(String text) {
        if (TextUtils.isEmpty(text)) {
            setText("");
        } else {
            setText(Html.fromHtml(text.replaceAll("\\{\\\\.*?\\}", "")));
        }
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
