package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;

public class FileChooser {

    public static final int REQUEST_PICK_FILE = 9999;

    private final Fragment fragment;

    public static FileChooser from(Fragment fragment) {
        return new FileChooser(fragment);
    }

    private FileChooser(Fragment fragment) {
        this.fragment = fragment;
    }

    public void show() {
        show("*/*");
    }

    public void show(String mimeType) {
        show(mimeType, REQUEST_PICK_FILE);
    }

    public void show(String mimeType, int code) {
        String[] mimeTypes = mimeType.split(" ");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(mimeTypes[0]);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        Intent destIntent = Intent.createChooser(intent, "");
        if (fragment != null) fragment.startActivityForResult(destIntent, code);
    }

    public static String getPathFromUri(Context context, Uri uri) {
        return uri.toString();
    }
}
