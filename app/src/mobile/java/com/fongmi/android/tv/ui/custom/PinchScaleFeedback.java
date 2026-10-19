package com.fongmi.android.tv.ui.custom;

import android.view.View;
import android.widget.TextView;

import com.fongmi.android.tv.R;

public final class PinchScaleFeedback {

    private final TextView view;
    private PinchScale.Axis axis;
    private int widthPercent;
    private int heightPercent;
    private boolean active;

    public PinchScaleFeedback(TextView view) {
        this.view = view;
    }

    public boolean isActive() {
        return active;
    }

    public void show(PinchScale.Axis axis, float scaleX, float scaleY) {
        if (!active) {
            active = true;
            view.animate().cancel();
            if (view.getVisibility() != View.VISIBLE) view.setAlpha(0.0f);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1.0f).setStartDelay(0).setDuration(90).start();
        }
        int width = Math.round(scaleX * 100);
        int height = Math.round(scaleY * 100);
        if (this.axis == axis && widthPercent == width && heightPercent == height) return;
        this.axis = axis;
        widthPercent = width;
        heightPercent = height;
        if (axis == PinchScale.Axis.HORIZONTAL) view.setText(view.getContext().getString(R.string.pinch_width, width));
        else if (axis == PinchScale.Axis.VERTICAL) view.setText(view.getContext().getString(R.string.pinch_height, height));
        else if (width == height) view.setText(view.getContext().getString(R.string.pinch_zoom, width));
        else view.setText(view.getContext().getString(R.string.pinch_size, width, height));
    }

    public void hide() {
        if (!active) return;
        active = false;
        view.animate().cancel();
        view.animate().alpha(0.0f).setStartDelay(180).setDuration(160).withEndAction(() -> view.setVisibility(View.GONE)).start();
    }
}
