package com.fongmi.android.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

import java.io.ByteArrayOutputStream;

public class Product {

    public static int getDeviceType() {
        return 1;
    }

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 5);
    }

    public static void bootLive() {
    }

    public static int[] getSpec(Context context) {
        return getSpec(context, ResUtil.dp2px(32) + ResUtil.dp2px(16 * (getColumn() - 1)), getColumn());
    }

    public static int[] getSpec(Context context, int space, int column) {
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        int height = (int) (width / 0.75f);
        return new int[]{width, height};
    }

    public static byte[] resize(byte[] bytes) {
        Bitmap bitmap = crop(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private static Bitmap crop(Bitmap bitmap) {
        int width = ResUtil.getScreenWidth();
        int height = ResUtil.getScreenHeight();
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            return Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0, bitmap.getHeight(), bitmap.getHeight());
        } else if (bitmap.getWidth() < width && bitmap.getHeight() < height) {
            return bitmap;
        } else {
            Matrix matrix = new Matrix();
            matrix.postScale((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        }
    }
}
