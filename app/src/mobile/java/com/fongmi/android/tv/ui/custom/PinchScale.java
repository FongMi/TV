package com.fongmi.android.tv.ui.custom;

final class PinchScale {

    private static final float CENTER_REGION_HALF_FRACTION = 0.15f;
    private static final float EDGE_CONTACT_SLOP_PX = 1.0f;
    private static final float EDGE_ELASTIC_SCALE = 0.12f;
    private static final float MIN_ELASTIC_SCALE = 0.12f;
    private static final float MAX_ELASTIC_SCALE = 0.20f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_UNIFORM_SCALE = 5.0f;

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

    void begin(float edgeScaleX, float edgeScaleY) {
        this.edgeScaleX = edgeScaleX;
        this.edgeScaleY = edgeScaleY;
        captureBaseline();
    }

    static boolean useCenterPivot(boolean multiTouch, float focusX, float focusY, float width, float height) {
        if (!multiTouch || width <= 0 || height <= 0) return false;
        return Math.abs(focusX - width / 2.0f) <= width * CENTER_REGION_HALF_FRACTION && Math.abs(focusY - height / 2.0f) <= height * CENTER_REGION_HALF_FRACTION;
    }

    void update(float factor) {
        rawFactor *= factor;
        float minBase = Math.min(baseScaleX, baseScaleY);
        float maxBase = Math.max(baseScaleX, baseScaleY);
        float minScaleX = getMinScale(baseScaleX);
        float minScaleY = getMinScale(baseScaleY);
        float minFactor = Math.max(minScaleX / baseScaleX, minScaleY / baseScaleY);
        float maxScale = Math.max(MAX_UNIFORM_SCALE, maxBase);
        float maxScaleX = getMaxScale(baseScaleX, maxScale);
        float maxScaleY = getMaxScale(baseScaleY, maxScale);
        float maxFactor = Math.min(maxScaleX / baseScaleX, maxScaleY / baseScaleY);
        float edgeFactorX = edgeScaleX / baseScaleX;
        float edgeFactorY = edgeScaleY / baseScaleY;
        float edgeFactor = Math.min(edgeFactorX, edgeFactorY);
        float edgeBase = edgeFactorX <= edgeFactorY ? baseScaleX : baseScaleY;
        float limitFactor = Math.min(edgeFactor, maxFactor);
        float elasticDistance = edgeFactor < maxFactor ? EDGE_ELASTIC_SCALE / edgeBase : MAX_ELASTIC_SCALE / maxBase;
        float committedFactor = Math.clamp(rawFactor, minFactor, limitFactor);
        float visualFactor = resist(rawFactor, minFactor, limitFactor, MIN_ELASTIC_SCALE / minBase, elasticDistance);
        scaleX = Math.clamp(baseScaleX * committedFactor, minScaleX, maxScaleX);
        visualScaleX = baseScaleX * visualFactor;
        scaleY = Math.clamp(baseScaleY * committedFactor, minScaleY, maxScaleY);
        visualScaleY = baseScaleY * visualFactor;
    }

    private static float getMinScale(float baseScale) {
        return baseScale > 1.0f ? 1.0f : Math.min(MIN_SCALE, baseScale);
    }

    private static float getMaxScale(float baseScale, float maxScale) {
        return baseScale < 1.0f ? 1.0f : maxScale;
    }

    private void captureBaseline() {
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
