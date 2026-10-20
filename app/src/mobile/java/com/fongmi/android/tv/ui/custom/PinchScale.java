package com.fongmi.android.tv.ui.custom;

public final class PinchScale {

    private static final float AXIS_DOMINANCE_RATIO = 2.0f;
    private static final float CENTER_REGION_HALF_FRACTION = 0.15f;
    private static final float EDGE_CONTACT_SLOP_PX = 1.0f;
    private static final float EDGE_ELASTIC_SCALE = 0.12f;
    private static final float MIN_ELASTIC_SCALE = 0.12f;
    private static final float MAX_ELASTIC_SCALE = 0.20f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_AXIS_SCALE = 2.0f;
    private static final float MAX_UNIFORM_SCALE = 5.0f;

    public enum Axis {HORIZONTAL, VERTICAL, BOTH}

    private Axis axis = Axis.BOTH;
    private Axis activeAxis;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float visualScaleX = 1.0f;
    private float visualScaleY = 1.0f;
    private float baseScaleX = 1.0f;
    private float baseScaleY = 1.0f;
    private float edgeScaleX = Float.POSITIVE_INFINITY;
    private float edgeScaleY = Float.POSITIVE_INFINITY;
    private float rawFactor = 1.0f;

    void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        captureBaseline();
    }

    void begin(float spanX, float spanY, float edgeScaleX, float edgeScaleY) {
        axis = axisForSpans(spanX, spanY);
        this.edgeScaleX = edgeScaleX;
        this.edgeScaleY = edgeScaleY;
        captureBaseline();
    }

    static Axis axisForSpans(float spanX, float spanY) {
        if (spanX >= spanY * AXIS_DOMINANCE_RATIO) return Axis.HORIZONTAL;
        if (spanY >= spanX * AXIS_DOMINANCE_RATIO) return Axis.VERTICAL;
        return Axis.BOTH;
    }

    static boolean useCenterPivot(boolean multiTouch, float spanX, float spanY, float focusX, float focusY, float width, float height) {
        if (!multiTouch || width <= 0 || height <= 0) return false;
        if (axisForSpans(spanX, spanY) != Axis.BOTH) return true;
        return Math.abs(focusX - width / 2.0f) <= width * CENTER_REGION_HALF_FRACTION && Math.abs(focusY - height / 2.0f) <= height * CENTER_REGION_HALF_FRACTION;
    }

    Axis update(float factor, boolean multiTouch) {
        Axis current = multiTouch ? axis : Axis.BOTH;
        if (activeAxis != current) captureBaseline();
        activeAxis = current;
        rawFactor *= factor;
        float minBase = current == Axis.HORIZONTAL ? baseScaleX : current == Axis.VERTICAL ? baseScaleY : Math.min(baseScaleX, baseScaleY);
        float maxBase = current == Axis.HORIZONTAL ? baseScaleX : current == Axis.VERTICAL ? baseScaleY : Math.max(baseScaleX, baseScaleY);
        float minScaleX = getMinScale(baseScaleX);
        float minScaleY = getMinScale(baseScaleY);
        float minFactor = current == Axis.HORIZONTAL ? minScaleX / baseScaleX : current == Axis.VERTICAL ? minScaleY / baseScaleY : Math.max(minScaleX / baseScaleX, minScaleY / baseScaleY);
        float maxScale = Math.max(current == Axis.BOTH ? MAX_UNIFORM_SCALE : MAX_AXIS_SCALE, maxBase);
        float maxScaleX = getMaxScale(baseScaleX, maxScale);
        float maxScaleY = getMaxScale(baseScaleY, maxScale);
        float maxFactor = current == Axis.HORIZONTAL ? maxScaleX / baseScaleX : current == Axis.VERTICAL ? maxScaleY / baseScaleY : Math.min(maxScaleX / baseScaleX, maxScaleY / baseScaleY);
        float edgeFactorX = edgeScaleX / baseScaleX;
        float edgeFactorY = edgeScaleY / baseScaleY;
        float edgeFactor = current == Axis.HORIZONTAL ? edgeFactorX : current == Axis.VERTICAL ? edgeFactorY : Math.min(edgeFactorX, edgeFactorY);
        float edgeBase = current == Axis.HORIZONTAL ? baseScaleX : current == Axis.VERTICAL ? baseScaleY : edgeFactorX <= edgeFactorY ? baseScaleX : baseScaleY;
        float limitFactor = Math.min(edgeFactor, maxFactor);
        float elasticDistance = edgeFactor < maxFactor ? EDGE_ELASTIC_SCALE / edgeBase : MAX_ELASTIC_SCALE / maxBase;
        float committedFactor = Math.clamp(rawFactor, minFactor, limitFactor);
        float visualFactor = resist(rawFactor, minFactor, limitFactor, MIN_ELASTIC_SCALE / minBase, elasticDistance);
        if (current != Axis.VERTICAL) {
            scaleX = Math.clamp(baseScaleX * committedFactor, minScaleX, maxScaleX);
            visualScaleX = baseScaleX * visualFactor;
        }
        if (current != Axis.HORIZONTAL) {
            scaleY = Math.clamp(baseScaleY * committedFactor, minScaleY, maxScaleY);
            visualScaleY = baseScaleY * visualFactor;
        }
        return current;
    }

    private static float getMinScale(float baseScale) {
        return baseScale > 1.0f ? 1.0f : Math.min(MIN_SCALE, baseScale);
    }

    private static float getMaxScale(float baseScale, float maxScale) {
        return baseScale < 1.0f ? 1.0f : maxScale;
    }

    private void captureBaseline() {
        activeAxis = null;
        baseScaleX = scaleX;
        baseScaleY = scaleY;
        rawFactor = 1.0f;
        visualScaleX = scaleX;
        visualScaleY = scaleY;
    }

    private static float resist(float factor, float min, float max, float minDistance, float maxDistance) {
        if (factor < min) return min - (min - factor) * minDistance / (min - factor + minDistance);
        if (factor > max) return max + (factor - max) * maxDistance / (factor - max + maxDistance);
        return factor;
    }

    static float edgeContactScale(float start, float end, float focus, float viewport, float translation, float scale) {
        if (!Float.isFinite(start) || !Float.isFinite(end) || start >= end || viewport <= 0) return Float.POSITIVE_INFINITY;
        float leading = focus + translation + scale * (start - focus);
        float trailing = focus + translation + scale * (end - focus);
        if (leading <= EDGE_CONTACT_SLOP_PX || trailing >= viewport - EDGE_CONTACT_SLOP_PX) return Float.POSITIVE_INFINITY;
        float left = focus > start ? (focus + translation) / (focus - start) : Float.POSITIVE_INFINITY;
        float right = focus < end ? (viewport - focus - translation) / (end - focus) : Float.POSITIVE_INFINITY;
        return Math.min(left, right);
    }

    static float centeredEdgeScale(float start, float end, float focus, float viewport, float translation, float scale) {
        float edge = edgeContactScale(start, end, focus, viewport, translation, scale);
        return Float.isFinite(edge) ? viewport / (end - start) : edge;
    }

    static float localFocus(float focus, float translation, float pivot, float scale) {
        return pivot + (focus - translation - pivot) / scale;
    }

    static boolean shouldConstrainTranslation(float previousScale, float previousVisualScale, float scale, float visualScale) {
        return scale < previousScale || previousVisualScale < previousScale || visualScale < scale;
    }

    static float releaseTranslation(float translation, float start, float end, float pivot, float scale, float visualScale, float viewport) {
        if (scale == 1.0f) return 0f;
        return visualScale < scale ? constrainTranslation(translation, start, end, pivot, scale, viewport) : translation;
    }

    static float constrainTranslation(float translation, float start, float end, float pivot, float scale, float viewport) {
        if (!Float.isFinite(start) || !Float.isFinite(end) || start >= end || viewport <= 0) return translation;
        float leading = pivot + scale * (start - pivot);
        float trailing = pivot + scale * (end - pivot);
        if (trailing - leading <= viewport) return (viewport - leading - trailing) / 2.0f;
        return Math.clamp(translation, viewport - trailing, -leading);
    }

    boolean end() {
        boolean elastic = Math.abs(visualScaleX - scaleX) > 0.0001f || Math.abs(visualScaleY - scaleY) > 0.0001f;
        captureBaseline();
        return elastic;
    }

    float getScaleX() {
        return scaleX;
    }

    float getScaleY() {
        return scaleY;
    }

    float getVisualScaleX() {
        return visualScaleX;
    }

    float getVisualScaleY() {
        return visualScaleY;
    }

    void reset() {
        scaleX = 1.0f;
        scaleY = 1.0f;
        captureBaseline();
    }
}
