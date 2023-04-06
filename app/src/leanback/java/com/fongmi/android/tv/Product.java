package com.fongmi.android.tv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

import java.io.ByteArrayOutputStream;

public class Product {

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 7);
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(24), 35);
    }

    public static byte[] resize(byte[] bytes) {
        int width = ResUtil.getScreenWidth();
        int height = ResUtil.getScreenHeight();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap.getWidth() < width && bitmap.getHeight() < height) return bytes;
        Matrix matrix = new Matrix();
        matrix.postScale((float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }
}
