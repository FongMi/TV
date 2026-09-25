package com.fongmi.android.tv.ui.custom;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TapSeekFeedback extends LinearLayout {

    private static final int FEEDBACK_DURATION_MS = 460;
    private static final int FADE_DURATION_MS = 160;
    private static final int ENTER_DURATION_MS = 90;
    private static final int ARROW_MOVE_DURATION_MS = 280;
    private static final int ARROW_SIZE_DP = 20;
    private static final int ARROW_TRAVEL_DP = 12;
    private static final LinearInterpolator ARROW_INTERPOLATOR = new LinearInterpolator();

    private final FrameLayout video;
    private final FrameLayout arrows;
    private final Runnable onHidden;
    private final AppCompatTextView secondsView;
    private final AppCompatImageView firstArrow;
    private final AppCompatImageView secondArrow;
    private final Rect textBounds = new Rect();
    private final Runnable hideFirstArrow;
    private final Runnable hideSecondArrow;
    private final Runnable hide;
    private int direction;
    private int count;
    private int generation;

    public TapSeekFeedback(FrameLayout video, Runnable onHidden) {
        super(video.getContext());
        this.video = video;
        this.onHidden = onHidden;
        this.arrows = new FrameLayout(getContext());
        this.secondsView = new AppCompatTextView(getContext());
        this.firstArrow = createArrow();
        this.secondArrow = createArrow();
        this.hideFirstArrow = () -> firstArrow.setVisibility(View.GONE);
        this.hideSecondArrow = () -> secondArrow.setVisibility(View.GONE);
        this.hide = () -> {
            int currentGeneration = generation;
            animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction(() -> {
                if (generation != currentGeneration) return;
                setVisibility(View.GONE);
                onHidden.run();
            }).start();
        };
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        setVisibility(View.GONE);
        secondsView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        secondsView.setTextSize(18);
        secondsView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        int shadowRadius = ResUtil.dp2px(3);
        int shadowOffset = ResUtil.dp2px(1);
        secondsView.setShadowLayer(shadowRadius, 0, shadowOffset, Color.BLACK);
        int shadowPadding = shadowRadius + shadowOffset;
        secondsView.setPadding(shadowPadding, shadowPadding, shadowPadding, shadowPadding);
        secondsView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int arrowSize = ResUtil.dp2px(ARROW_SIZE_DP);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(arrowSize, arrowSize, Gravity.CENTER);
        arrows.addView(firstArrow, arrowParams);
        arrows.addView(secondArrow, new FrameLayout.LayoutParams(arrowSize, arrowSize, Gravity.CENTER));
        firstArrow.setVisibility(View.GONE);
        secondArrow.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
        video.addView(this, params);
    }

    private AppCompatImageView createArrow() {
        AppCompatImageView arrow = new AppCompatImageView(getContext());
        arrow.setImageResource(R.drawable.ic_seek_chevron);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return arrow;
    }

    public int directionAt(float x) {
        float fifth = video.getWidth() / 5f;
        return x < fifth ? -1 : x >= fifth * 4 ? 1 : 0;
    }

    public long show(float x) {
        int nextDirection = directionAt(x);
        if (nextDirection == 0) return 0;
        boolean entering = direction != nextDirection || getVisibility() != View.VISIBLE;
        if (entering) count = 0;
        count++;
        generation++;
        removeCallbacks(hide);
        animate().cancel();
        if (entering) resetArrows();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        int inset = video.getWidth() / 6;
        if (direction != nextDirection || (nextDirection > 0 ? params.rightMargin : params.leftMargin) != inset) arrange(nextDirection, inset);
        direction = nextDirection;
        int seconds = direction * (int) (TimeUnit.MILLISECONDS.toSeconds(Constant.INTERVAL_SEEK) * count);
        secondsView.setText(String.format(Locale.ROOT, "%+d", seconds));
        setContentDescription(getContext().getString(R.string.seek_seconds, seconds));
        setVisibility(View.VISIBLE);
        setAlpha(entering ? 0f : 1f);
        if (entering) animate().alpha(1f).setDuration(ENTER_DURATION_MS).start();
        animateArrow();
        postDelayed(hide, FEEDBACK_DURATION_MS - FADE_DURATION_MS);
        return direction * Constant.INTERVAL_SEEK;
    }

    private void animateArrow() {
        AppCompatImageView arrow = (count & 1) == 1 ? firstArrow : secondArrow;
        Runnable hideArrow = arrow == firstArrow ? hideFirstArrow : hideSecondArrow;
        int arrowOffset = direction * ResUtil.dp2px(ARROW_TRAVEL_DP);
        arrow.removeCallbacks(hideArrow);
        arrow.animate().cancel();
        arrow.setVisibility(View.VISIBLE);
        arrow.setAlpha(1f);
        arrow.setTranslationX(0f);
        arrow.animate().translationX(arrowOffset).setInterpolator(ARROW_INTERPOLATOR).setDuration(ARROW_MOVE_DURATION_MS).start();
        arrow.postDelayed(hideArrow, FEEDBACK_DURATION_MS);
    }

    private void arrange(int direction, int inset) {
        removeAllViews();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER_VERTICAL | (direction > 0 ? Gravity.END : Gravity.START);
        params.leftMargin = direction < 0 ? inset : 0;
        params.rightMargin = direction > 0 ? inset : 0;
        setLayoutParams(params);
        firstArrow.setRotation(direction > 0 ? 0f : 180f);
        secondArrow.setRotation(direction > 0 ? 0f : 180f);
        int arrowSize = ResUtil.dp2px(ARROW_SIZE_DP);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(arrowSize + ResUtil.dp2px(ARROW_TRAVEL_DP) * 2, arrowSize);
        if (direction > 0) {
            addView(secondsView);
            arrowParams.leftMargin = ResUtil.dp2px(16);
            addView(arrows, arrowParams);
        } else {
            arrowParams.rightMargin = ResUtil.dp2px(16);
            addView(arrows, arrowParams);
            addView(secondsView);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        alignArrows();
    }

    private void alignArrows() {
        String text = secondsView.getText().toString();
        if (text.isEmpty()) return;
        secondsView.getPaint().getTextBounds(text, 0, text.length(), textBounds);
        float textCenter = secondsView.getTop() + secondsView.getBaseline() + (textBounds.top + textBounds.bottom) / 2f;
        float arrowCenter = arrows.getTop() + arrows.getHeight() / 2f;
        arrows.setTranslationY(textCenter - arrowCenter);
    }

    public void clear() {
        generation++;
        removeCallbacks(hide);
        animate().cancel();
        resetArrows();
        setVisibility(View.GONE);
        onHidden.run();
        direction = 0;
        count = 0;
    }

    private void resetArrows() {
        firstArrow.removeCallbacks(hideFirstArrow);
        secondArrow.removeCallbacks(hideSecondArrow);
        firstArrow.animate().cancel();
        secondArrow.animate().cancel();
        firstArrow.setVisibility(View.GONE);
        secondArrow.setVisibility(View.GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        clear();
        super.onDetachedFromWindow();
    }
}
