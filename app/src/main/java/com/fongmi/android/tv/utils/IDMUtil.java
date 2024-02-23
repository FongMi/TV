package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;

import java.util.ArrayList;
import java.util.Map;

public class IDMUtil {

    public static final String PACKAGE_NAME_1DM_PLUS = "idm.internet.download.manager.plus";
    public static final String PACKAGE_NAME_1DM_NORMAL = "idm.internet.download.manager";
    public static final String PACKAGE_NAME_1DM_LITE = "idm.internet.download.manager.adm.lite";
    public static final String DOWNLOADER_ACTIVITY_NAME_1DM = "idm.internet.download.manager.Downloader";
    public static final int SECURE_URI_1DM_SUPPORT_MIN_VERSION_CODE = 169;
    public static final int HEADERS_AND_MULTIPLE_LINKS_1DM_SUPPORT_MIN_VERSION_CODE = 157;
    public static final String EXTRA_SECURE_URI = "secure_uri";
    public static final String EXTRA_COOKIES = "extra_cookies";
    public static final String EXTRA_USERAGENT = "extra_useragent";
    public static final String EXTRA_REFERER = "extra_referer";
    public static final String EXTRA_HEADERS = "extra_headers";
    public static final String EXTRA_FILENAME = "extra_filename";
    public static final String EXTRA_URL_LIST = "url_list";
    public static final String EXTRA_URL_FILENAME_LIST = "url_list.filename";
    public static final String MESSAGE_INSTALL_1DM = "To download content install 1DM";
    public static final String MESSAGE_UPDATE_1DM = "To download content update 1DM";

    public enum AppState {OK, UPDATE_REQUIRED, NOT_INSTALLED}

    public static boolean downloadFile(@NonNull Activity activity, @NonNull String url, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, null, url, null, null, null, null, null, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFile(@NonNull Activity activity, @NonNull String url, @Nullable Map<String, String> headers, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, null, url, null, null, null, null, headers, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFile(@NonNull Activity activity, @NonNull String url, String fileName, @Nullable Map<String, String> headers, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, null, url, null, fileName, null, null, headers, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFile(@NonNull Activity activity, @NonNull String url, @Nullable String referer, @Nullable String fileName, @Nullable String userAgent, @Nullable String cookies, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, null, url, referer, fileName, userAgent, cookies, null, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFile(@NonNull Activity activity, @NonNull String url, @Nullable String referer, @Nullable String fileName, @Nullable String userAgent, @Nullable String cookies, @Nullable Map<String, String> headers, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, null, url, referer, fileName, userAgent, cookies, headers, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFiles(@NonNull Activity activity, @NonNull Map<String, String> urlAndFileNames, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, urlAndFileNames, null, null, null, null, null, null, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    public static boolean downloadFiles(@NonNull Activity activity, @NonNull Map<String, String> urlAndFileNames, @Nullable Map<String, String> headers, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled) {
        return downloadFilesInternal(activity, urlAndFileNames, null, null, null, null, null, headers, secureUri, askUserToInstall1DMIfNotInstalled);
    }

    private static boolean downloadFilesInternal(@NonNull Activity activity, @Nullable Map<String, String> urlAndFileNames, @Nullable String url, @Nullable String referer, @Nullable String fileName, @Nullable String userAgent, @Nullable String cookies, @Nullable Map<String, String> headers, boolean secureUri, boolean askUserToInstall1DMIfNotInstalled){
        int requiredVersionCode = secureUri ? SECURE_URI_1DM_SUPPORT_MIN_VERSION_CODE : !isEmpty(urlAndFileNames) || !isEmpty(headers) ? HEADERS_AND_MULTIPLE_LINKS_1DM_SUPPORT_MIN_VERSION_CODE : 0;
        String packageName = "";
        try {
            packageName = get1DMInstalledPackageName(requiredVersionCode, askUserToInstall1DMIfNotInstalled);
        } catch (Exception e) {
            return false;
        }
        if (TextUtils.isEmpty(packageName)) return false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName(packageName, DOWNLOADER_ACTIVITY_NAME_1DM));
        intent.putExtra(EXTRA_SECURE_URI, secureUri);
        if (isEmpty(urlAndFileNames)) {
            intent.setData(Uri.parse(url));
            if (!TextUtils.isEmpty(referer)) intent.putExtra(EXTRA_REFERER, referer);
            if (!TextUtils.isEmpty(userAgent)) intent.putExtra(EXTRA_USERAGENT, userAgent);
            if (!TextUtils.isEmpty(cookies)) intent.putExtra(EXTRA_COOKIES, cookies);
            if (!TextUtils.isEmpty(fileName)) intent.putExtra(EXTRA_FILENAME, fileName);
        } else {
            ArrayList<String> urls = new ArrayList<>(urlAndFileNames.size());
            ArrayList<String> names = new ArrayList<>(urlAndFileNames.size());
            for (Map.Entry<String, String> entry : urlAndFileNames.entrySet()) {
                if (TextUtils.isEmpty(entry.getKey())) continue;
                urls.add(entry.getKey());
                names.add(entry.getValue());
            }
            if (urls.size() > 0) {
                intent.putExtra(EXTRA_URL_LIST, urls);
                intent.putExtra(EXTRA_URL_FILENAME_LIST, names);
                intent.setData(Uri.parse(urls.get(0)));
            }
        }
        if (!isEmpty(headers)) {
            Bundle extra = new Bundle();
            for (Map.Entry<String, String> entry : headers.entrySet()) extra.putString(entry.getKey(), entry.getValue());
            intent.putExtra(EXTRA_HEADERS, extra);
        }
        activity.startActivity(intent);
        return true;
    }

    public static void install1DM(String packageName, boolean update) {
        
    }

    private static <S, T> boolean isEmpty(Map<S, T> map) {
        return map == null || map.size() == 0;
    }

    private static String get1DMInstalledPackageName(int requiredVersionCode, boolean askUserToInstall1DMIfNotInstalled) throws Exception {
        PackageManager packageManager = App.get().getPackageManager();
        String packageName = PACKAGE_NAME_1DM_PLUS;
        AppState state = get1DMAppState(packageManager, packageName, requiredVersionCode);
        if (state == AppState.NOT_INSTALLED) {
            packageName = PACKAGE_NAME_1DM_NORMAL;
            state = get1DMAppState(packageManager, packageName, requiredVersionCode);
            if (state == AppState.NOT_INSTALLED) {
                packageName = PACKAGE_NAME_1DM_LITE;
                state = get1DMAppState(packageManager, packageName, requiredVersionCode);
                if (state == AppState.NOT_INSTALLED) {
                    if (!askUserToInstall1DMIfNotInstalled) throw new Exception(MESSAGE_INSTALL_1DM);
                    install1DM(PACKAGE_NAME_1DM_NORMAL, false);
                    return null;
                }
            }
        }
        if (state == AppState.UPDATE_REQUIRED) {
            if (!askUserToInstall1DMIfNotInstalled) throw new Exception(MESSAGE_UPDATE_1DM);
            install1DM(packageName, true);
            return null;
        }
        return packageName;
    }

    private static AppState get1DMAppState(@NonNull PackageManager packageManager, @NonNull String packageName, int requiredVersion) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (requiredVersion <= 0 || packageInfo.versionCode >= requiredVersion) return AppState.OK;
            return AppState.UPDATE_REQUIRED;
        } catch (PackageManager.NameNotFoundException ignore) {
            return AppState.NOT_INSTALLED;
        }
    }
}
