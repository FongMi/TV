package com.fongmi.android.tv.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;

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
        show(mimeType, new String[]{"*/*"}, REQUEST_PICK_FILE);
    }

    public void show(String[] mimeTypes) {
        show("*/*", mimeTypes, REQUEST_PICK_FILE);
    }

    public void show(String mimeType, String[] mimeTypes, int code) {
        Intent intent = new Intent(Util.isTvBox() ? Intent.ACTION_GET_CONTENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        if (intent.resolveActivity(App.get().getPackageManager()) == null) return;
        if (fragment != null) fragment.startActivityForResult(Intent.createChooser(intent, ""), code);
    }

    public static boolean isValid(Context context, Uri uri) {
        try {
            return DocumentsContract.isDocumentUri(context, uri) || ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) || ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme());
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        String path = null;
        if (DocumentsContract.isDocumentUri(context, uri)) path = getPathFromDocumentUri(context, uri);
        else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) path = getDataColumn(context, uri);
        else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) path = uri.getPath();
        return path != null ? URLDecoder.decode(path) : createFileFromUri(context, uri);
    }

    private static String getPathFromDocumentUri(Context context, Uri uri) {
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        if (isExternalStorageDocument(uri)) return getPath(docId, split);
        else if (isDownloadsDocument(uri)) return getPath(context, uri, docId);
        else if (isMediaDocument(uri)) return getPath(context, split);
        else return null;
    }

    private static String getPath(String docId, String[] split) {
        if ("primary".equalsIgnoreCase(split[0])) {
            return split.length > 1 ? Environment.getExternalStorageDirectory() + "/" + split[1] : Environment.getExternalStorageDirectory() + "/";
        } else {
            return "/storage/" + docId.replace(":", "/");
        }
    }

    private static String getPath(Context context, Uri uri, String docId) {
        String fileName = getNameColumn(context, uri);
        if (docId.startsWith("raw:")) {
            return docId.replaceFirst("raw:", "");
        } else if (fileName != null) {
            return Environment.getExternalStorageDirectory() + "/Download/" + fileName;
        } else {
            return getDataColumn(context, ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId)));
        }
    }

    private static String getPath(Context context, String[] split) {
        switch (split[0]) {
            case "image":
                return getDataColumn(context, ContentUris.withAppendedId(getImageUri(), Long.parseLong(split[1])));
            case "video":
                return getDataColumn(context, ContentUris.withAppendedId(getVideoUri(), Long.parseLong(split[1])));
            case "audio":
                return getDataColumn(context, ContentUris.withAppendedId(getAudioUri(), Long.parseLong(split[1])));
            default:
                return getDataColumn(context, ContentUris.withAppendedId(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), Long.parseLong(split[1])));
        }
    }

    private static String createFileFromUri(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        try (cursor) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            int column = cursor.getColumnIndexOrThrow(projection[0]);
            File file = Path.cache(cursor.getString(column));
            Path.copy(is, file);
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getDataColumn(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        try (cursor) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return cursor.getString(cursor.getColumnIndexOrThrow(projection[0]));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getNameColumn(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        try (cursor) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return cursor.getString(cursor.getColumnIndexOrThrow(projection[0]));
        } catch (Exception e) {
            return null;
        }
    }

    private static Uri getImageUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
    }

    private static Uri getVideoUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
    }

    private static Uri getAudioUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}