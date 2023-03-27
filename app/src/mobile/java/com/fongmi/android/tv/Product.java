package com.fongmi.android.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

import java.io.ByteArrayOutputStream;

public class Product {

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
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            return Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0, bitmap.getHeight(), bitmap.getHeight());
        } else {
            return Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2 - bitmap.getWidth() / 2, bitmap.getWidth(), bitmap.getWidth());
        }
    }
}
