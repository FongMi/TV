package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public class QRCode {

    private static final int FINDER_MODULES = 7;
    private static final int MIN_MARGIN = 4;

    public static Bitmap getBitmap(String content, int size, int margin) {
        if (size < 0) return null;
        try {
            margin = Math.max(MIN_MARGIN, margin);
            return render(encode(content, margin), ResUtil.dp2px(size), margin);
        } catch (Exception e) {
            return null;
        }
    }

    private static BitMatrix encode(String content, int margin) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, margin);
        return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
    }

    private static Bitmap render(BitMatrix matrix, int size, int margin) {
        int width = Math.max(size, matrix.getWidth());
        int module = width / matrix.getWidth();
        int offset = (width - matrix.getWidth() * module) / 2;
        int modules = matrix.getWidth() - 2 * margin;
        Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        drawDataModules(canvas, paint, matrix, margin, modules, offset, module);
        int start = offset + margin * module;
        int end = start + (modules - FINDER_MODULES) * module;
        drawFinder(canvas, paint, start, start, module);
        drawFinder(canvas, paint, end, start, module);
        drawFinder(canvas, paint, start, end, module);
        return bitmap;
    }

    private static void drawDataModules(Canvas canvas, Paint paint, BitMatrix matrix, int margin, int modules, int offset, int module) {
        for (int y = margin; y < margin + modules; y++) {
            for (int x = margin; x < margin + modules; x++) {
                if (!matrix.get(x, y) || isFinderRegion(x - margin, y - margin, modules)) continue;
                canvas.drawCircle(offset + (x + 0.5f) * module, offset + (y + 0.5f) * module, 0.45f * module, paint);
            }
        }
    }

    private static boolean isFinderRegion(int x, int y, int modules) {
        return x < FINDER_MODULES && y < FINDER_MODULES || x >= modules - FINDER_MODULES && y < FINDER_MODULES || x < FINDER_MODULES && y >= modules - FINDER_MODULES;
    }

    private static void drawFinder(Canvas canvas, Paint paint, int left, int top, int module) {
        int right = left + FINDER_MODULES * module;
        int bottom = top + FINDER_MODULES * module;
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(left, top, right, bottom, 2 * module, 2 * module, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(left + module, top + module, right - module, bottom - module, module, module, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(left + 2 * module, top + 2 * module, right - 2 * module, bottom - 2 * module, module / 2f, module / 2f, paint);
    }
}
