package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.media3.exoplayer.offline.Downloader;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.github.catvod.net.Download;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.PackageAbi;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class RuntimeInstaller {

    public static final String RESULT = "com.fongmi.android.tv.RUNTIME_INSTALL_RESULT";
    private static final String INSTALL_REQUEST = "runtimeInstallRequest";
    private static final String INSTALL_PACKAGE = "runtimeInstallPackage";
    private static final String INSTALL_VERSION = "runtimeInstallVersion";
    private static final String INSTALL_OPENED = "runtimeInstallOpened";
    private static final String REMOVE_PACKAGE = "runtimeRemovePackage";
    private static String confirmationRequest = "";
    private static Closeable retainedApk;
    private static boolean removing;

    public static String getAbi() {
        return PackageAbi.get();
    }

    public static JSONObject getAsset(RuntimeModule module, CancellationSignal signal) throws IOException {
        JSONObject catalog = getCatalog(signal);
        JSONObject asset = catalog == null ? null : getExternalAsset(module, catalog);
        signal.throwIfCanceled();
        return asset;
    }

    public static JSONObject getCatalog(CancellationSignal signal) throws IOException {
        signal.throwIfCanceled();
        String url = getCatalogUrl();
        return url.isEmpty() ? null : getCatalog(url, signal);
    }

    private static String getCatalogUrl() throws IOException {
        try {
            var info = App.get().getBaseContext().getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA);
            return info.metaData == null ? "" : info.metaData.getString("com.fongmi.runtime.catalog", "");
        } catch (PackageManager.NameNotFoundException e) {
            throw new IOException("Unable to read runtime catalog configuration", e);
        }
    }

    public static JSONObject getExternalAsset(RuntimeModule module, String url, CancellationSignal signal) throws IOException {
        JSONObject asset = getExternalAsset(module, getCatalog(url, signal));
        signal.throwIfCanceled();
        return asset;
    }

    private static JSONObject getCatalog(String url, CancellationSignal signal) throws IOException {
        signal.throwIfCanceled();
        Call call = OkHttp.client().newCall(new Request.Builder().url(url).build());
        signal.setOnCancelListener(call::cancel);
        try (Response response = call.execute()) {
            signal.throwIfCanceled();
            if (!response.isSuccessful()) throw new IOException("Runtime catalog HTTP " + response.code());
            JSONObject catalog = new JSONObject(response.body().string());
            signal.throwIfCanceled();
            return catalog;
        } catch (JSONException e) {
            throw new IOException("Invalid runtime catalog", e);
        } finally {
            signal.setOnCancelListener(null);
        }
    }

    public static JSONObject getExternalAsset(RuntimeModule module, JSONObject catalog) throws IOException {
        try {
            if (catalog.getInt("schema") != 1) throw new IOException("Unsupported runtime catalog");
            return selectAsset(module, catalog.getJSONArray("modules"));
        } catch (JSONException | IllegalArgumentException e) {
            throw new IOException("Invalid runtime catalog", e);
        }
    }

    private static JSONObject selectAsset(RuntimeModule module, JSONArray modules) throws IOException, JSONException {
        JSONObject selected = null;
        for (int i = 0; i < modules.length(); i++) selected = selectAsset(module, modules.getJSONObject(i), selected);
        return selected;
    }

    private static JSONObject selectAsset(RuntimeModule module, JSONObject candidate, JSONObject selected) throws IOException, JSONException {
        if (!module.id.equals(candidate.getString("id"))) return selected;
        if (!module.getPackageName().equals(candidate.getString("packageName"))) throw new IOException("Runtime catalog package mismatch");
        if (!module.getCompatibilityId().equals(candidate.getString("compatibilityId"))) return selected;
        long code = candidate.getLong("versionCode");
        String version = candidate.getString("versionName");
        if (code <= 0 || version.isEmpty()) throw new IOException("Invalid runtime catalog version");
        return selectArtifact(candidate.getJSONArray("artifacts"), selected, code, version);
    }

    private static JSONObject selectArtifact(JSONArray artifacts, JSONObject selected, long code, String version) throws JSONException {
        String abi = getAbi();
        for (int i = 0; i < artifacts.length(); i++) {
            JSONObject artifact = artifacts.getJSONObject(i);
            if (!abi.equals(artifact.getString("abi"))) continue;
            if (artifact.optInt("minSdk", 24) > Build.VERSION.SDK_INT) continue;
            if (selected != null && selected.getLong("versionCode") >= code) continue;
            selected = createAsset(artifact, code, version);
        }
        return selected;
    }

    private static JSONObject createAsset(JSONObject artifact, long code, String version) throws JSONException {
        JSONObject asset = new JSONObject().put("versionCode", code).put("versionName", version).put("browser_download_url", artifact.getString("url")).put("size", artifact.getLong("size")).put("digest", "sha256:" + artifact.getString("sha256"));
        new UpdatePackage(version, asset.getString("browser_download_url"), asset.getLong("size"), asset.getString("digest"));
        return asset;
    }

    private static String getFileName(RuntimeModule module) {
        return "runtime-" + module.id + "-" + getAbi().replace('-', '_') + ".apk";
    }

    private static File getInstallationFile(RuntimeModule module, long version) {
        return new File(new File(Path.cache(), ApkCache.MODULE_DIRECTORY), version + "-" + getFileName(module));
    }

    public static File download(RuntimeModule module, File directory, CancellationSignal signal, Downloader.ProgressListener progress) throws IOException {
        JSONObject asset = getAsset(module, signal);
        if (asset == null) throw new IOException(module.title + " APK is not published");
        return download(module, asset, directory, signal, progress);
    }

    public static File download(RuntimeModule module, JSONObject asset, File directory, CancellationSignal signal, Downloader.ProgressListener progress) throws IOException {
        signal.throwIfCanceled();
        UpdatePackage update = getUpdatePackage(asset);
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Unable to create runtime download directory");
        File file = new File(directory, asset.optLong("versionCode") + "-" + getFileName(module));
        if (file.isFile()) {
            try {
                verifyDownload(module, asset, update, file);
                signal.throwIfCanceled();
                return file;
            } catch (IOException e) {
                if (!file.delete()) throw new IOException("Unable to remove invalid runtime APK", e);
            }
        }
        boolean complete = false;
        Throwable failure = null;
        try {
            Download.create(update.url(), file).size(update.size()).get(signal, progress == null ? null : (total, downloaded, percent) -> progress.onProgress(update.size(), downloaded, 100f * downloaded / update.size()));
            signal.throwIfCanceled();
            verifyDownload(module, asset, update, file);
            signal.throwIfCanceled();
            complete = true;
            return file;
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            if (!complete) deleteIncompleteApk(file, failure);
        }
    }

    private static void deleteIncompleteApk(File file, Throwable failure) throws IOException {
        try {
            if (file.exists() && !file.delete()) throw new IOException("Unable to remove incomplete runtime APK");
        } catch (IOException | RuntimeException | Error e) {
            if (failure == null) throw e;
            if (failure != e) failure.addSuppressed(e);
        }
    }

    private static UpdatePackage getUpdatePackage(JSONObject asset) throws IOException {
        if (asset.optLong("versionCode") <= 0) throw new IOException("Invalid runtime asset version");
        UpdatePackage update;
        try {
            update = new UpdatePackage(asset.optString("versionName"), asset.optString("browser_download_url"), asset.optLong("size"), asset.isNull("digest") ? "" : asset.optString("digest"));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid runtime asset", e);
        }
        if (update.digest().isEmpty()) throw new IOException("Runtime APK checksum is missing");
        return update;
    }

    private static void verifyDownload(RuntimeModule module, JSONObject asset, UpdatePackage update, File file) throws IOException {
        update.verifyFile(file);
        PackageInfo info = module.validate(file);
        long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        if (code != asset.optLong("versionCode") || !update.version().equals(info.versionName)) throw new IOException("Runtime asset version mismatch");
    }

    public static boolean hasPendingInstallation() {
        var prefs = Prefers.getPrefers();
        return getRuntime(prefs.getString(INSTALL_PACKAGE, "")) != null || getRuntime(prefs.getString(REMOVE_PACKAGE, "")) != null;
    }

    public static boolean hasPendingInstallation(RuntimeModule module) {
        var prefs = Prefers.getPrefers();
        String target = module.getPackageName();
        return target.equals(prefs.getString(INSTALL_PACKAGE, "")) || target.equals(prefs.getString(REMOVE_PACKAGE, ""));
    }

    private static RuntimeModule getRuntime(String target) {
        for (RuntimeModule module : RuntimeModule.values()) if (module.getPackageName().equals(target)) return module;
        return null;
    }

    public static boolean isRemovable(RuntimeModule module) throws IOException {
        return module.getVersionName() != null;
    }

    public static synchronized void remove(RuntimeModule module, CancellationSignal signal) throws IOException {
        signal.throwIfCanceled();
        if (!isRemovable(module)) throw new IOException(module.title + " is not removable");
        if (hasPendingInstallation()) throw new IOException("A runtime installation is already in progress");
        signal.throwIfCanceled();
        if (!Prefers.getPrefers().edit().putString(REMOVE_PACKAGE, module.getPackageName()).commit()) throw new IOException("Unable to save runtime removal");
        App.post(RuntimeInstaller::requestRemoval);
    }

    private static void requestRemoval() {
        if (App.activity() != null) {
            resumeUpdate(App.activity());
        } else {
            try {
                notifyConfirmation(true);
            } catch (RuntimeException e) {
                Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }
    }

    public static void resumeUpdate(Activity activity) {
        try {
            resumeConfirmation(activity, false);
            resumeRemoval();
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public static void confirmInstallation(Activity activity) {
        resumeConfirmation(activity, true);
    }

    public static synchronized void cancelInstallation(RuntimeModule module) throws IOException {
        var prefs = Prefers.getPrefers();
        if (module.getPackageName().equals(prefs.getString(INSTALL_PACKAGE, ""))) clearInstallation(prefs.getString(INSTALL_REQUEST, ""));
    }

    private static synchronized boolean clearInstallation(String request) throws IOException {
        var prefs = Prefers.getPrefers();
        if (request.isEmpty() || !request.equals(prefs.getString(INSTALL_REQUEST, ""))) return false;
        if (!prefs.edit().remove(INSTALL_REQUEST).remove(INSTALL_PACKAGE).remove(INSTALL_VERSION).remove(INSTALL_OPENED).commit()) throw new IOException("Unable to clear runtime installation");
        releaseInstallation();
        return true;
    }

    private static void releaseInstallation() throws IOException {
        try {
            if (retainedApk != null) retainedApk.close();
        } finally {
            retainedApk = null;
            wakeInstallers();
        }
    }

    private static synchronized void retainInstallation(String request, File file) throws IOException {
        if (request.equals(Prefers.getPrefers().getString(INSTALL_REQUEST, "")) && retainedApk == null) retainedApk = Download.retain(file);
    }

    private static void resumeConfirmation(Activity activity, boolean requested) {
        var prefs = Prefers.getPrefers();
        String request = prefs.getString(INSTALL_REQUEST, "");
        if (request.isEmpty() || request.equals(confirmationRequest)) return;
        String target = prefs.getString(INSTALL_PACKAGE, "");
        long version = prefs.getLong(INSTALL_VERSION, 0);
        boolean returned = prefs.getBoolean(INSTALL_OPENED, false) && !requested;
        confirmationRequest = request;
        Task.execute(() -> checkInstallation(activity, request, target, version, returned));
    }

    private static void checkInstallation(Activity activity, String request, String target, long version, boolean returned) {
        try {
            boolean installed = checkInstalledVersion(request, target, version, returned);
            App.post(() -> finishConfirmation(activity, request, target, version, installed, returned));
        } catch (IOException | RuntimeException e) {
            App.post(() -> failConfirmation(request, target, e));
        }
    }

    private static boolean checkInstalledVersion(String request, String target, long version, boolean returned) throws IOException {
        RuntimeModule module = getRuntime(target);
        if (module == null || version <= 0) throw new IOException("Invalid runtime installation");
        File file = getInstallationFile(module, version);
        retainInstallation(request, file);
        RuntimeModule.refresh();
        boolean installed = module.getVersionCode() >= version;
        if (!installed && !returned) validateInstallationVersion(module, file, version);
        return installed;
    }

    private static void validateInstallationVersion(RuntimeModule module, File file, long version) throws IOException {
        if (PackageInfoCompat.getLongVersionCode(module.validate(file)) != version) throw new IOException("Runtime installation version mismatch");
    }

    private static boolean completeConfirmation(String request) {
        if (request.equals(confirmationRequest)) confirmationRequest = "";
        return request.equals(Prefers.getPrefers().getString(INSTALL_REQUEST, ""));
    }

    private static void finishConfirmation(Activity activity, String request, String target, long version, boolean installed, boolean returned) {
        if (!completeConfirmation(request)) return;
        try {
            if (installed) finishInstallation(request, target, PackageInstaller.STATUS_SUCCESS, null);
            else if (returned) finishInstallation(request, target, null, null);
            else requestConfirmation(activity, getInstallationFile(getRuntime(target), version));
        } catch (IOException | RuntimeException e) {
            failInstallation(request, target, e);
        }
    }

    private static void failConfirmation(String request, String target, Exception error) {
        if (completeConfirmation(request)) failInstallation(request, target, error);
    }

    private static void failInstallation(String request, String target, Exception error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        Notify.show(message);
        try {
            finishInstallation(request, target, PackageInstaller.STATUS_FAILURE, message);
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void requestConfirmation(Activity activity, File file) throws IOException {
        if (activity.isFinishing() || activity.isDestroyed() || activity != App.activity()) notifyConfirmation(false);
        else openInstallation(file);
    }

    private static void openInstallation(File file) throws IOException {
        if (!Prefers.getPrefers().edit().putBoolean(INSTALL_OPENED, true).commit()) throw new IOException("Unable to save runtime installation");
        FileUtil.openFile(file, "application/vnd.android.package-archive");
    }

    private static void finishInstallation(String request, String target, Integer status, String message) throws IOException {
        long version = Prefers.getPrefers().getLong(INSTALL_VERSION, 0);
        if (!clearInstallation(request)) return;
        Context context = App.get().getBaseContext();
        Intent result = new Intent(RESULT).setPackage(context.getPackageName()).setData(Uri.fromParts("package", target, null));
        if (status != null) result.putExtra(PackageInstaller.EXTRA_STATUS, status);
        if (message != null) result.putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message);
        try {
            if (status != null && status == PackageInstaller.STATUS_SUCCESS) clearInstalledApk(target, version);
        } finally {
            context.sendBroadcast(result);
        }
    }

    private static void clearInstalledApk(String target, long version) throws IOException {
        RuntimeModule module = getRuntime(target);
        File file = getInstallationFile(module, version);
        ApkCache.installed(file.getParentFile(), module.id, file.getName());
    }

    private static void resumeRemoval() throws IOException {
        var prefs = Prefers.getPrefers();
        String target = prefs.getString(REMOVE_PACKAGE, "");
        if (target.isEmpty() || removing) return;
        RuntimeModule module = getRuntime(target);
        if (module == null) {
            if (!prefs.edit().remove(REMOVE_PACKAGE).commit()) throw new IOException("Unable to clear runtime removal");
            wakeInstallers();
            throw new IOException("Runtime removal package mismatch");
        }
        removing = true;
        Task.execute(() -> {
            try {
                RuntimeModule.refresh();
                boolean installed = isRemovable(module);
                App.post(() -> finishRemoval(target, installed, null));
            } catch (IOException | RuntimeException e) {
                String error = e.getMessage() == null ? e.toString() : e.getMessage();
                App.post(() -> finishRemoval(target, false, error));
            }
        });
    }

    private static void finishRemoval(String target, boolean installed, String error) {
        removing = false;
        var prefs = Prefers.getPrefers();
        if (!target.equals(prefs.getString(REMOVE_PACKAGE, ""))) return;
        Activity activity = App.activity();
        if (installed && (activity == null || activity.isFinishing() || activity.isDestroyed())) return;
        try {
            if (!prefs.edit().remove(REMOVE_PACKAGE).commit()) throw new IOException("Unable to clear runtime removal");
            wakeInstallers();
            if (error != null) Notify.show(error);
            else if (installed) activity.startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", target, null)));
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public static void install(RuntimeModule module, File file, CancellationSignal signal) throws IOException {
        long version = validateInstallation(module, file, signal);
        String request = UUID.randomUUID().toString();
        queueInstallation(request, module, file, version, signal);
        App.post(() -> requestInstallation(request, module.getPackageName(), signal));
    }

    private static long validateInstallation(RuntimeModule module, File file, CancellationSignal signal) throws IOException {
        signal.throwIfCanceled();
        long version = PackageInfoCompat.getLongVersionCode(module.validate(file));
        File directory = new File(Path.cache(), ApkCache.MODULE_DIRECTORY).getCanonicalFile();
        if (!directory.equals(file.getCanonicalFile().getParentFile()) || !file.getName().equals(getInstallationFile(module, version).getName())) throw new IOException("Invalid runtime installation file");
        Path.requireSpace(file, file.length());
        return version;
    }

    private static void queueInstallation(String request, RuntimeModule module, File file, long version, CancellationSignal signal) throws IOException {
        signal.setOnCancelListener(RuntimeInstaller::wakeInstallers);
        try {
            synchronized (RuntimeInstaller.class) {
                awaitPendingInstallations(signal);
                signal.throwIfCanceled();
                saveInstallation(request, module, file, version);
            }
        } finally {
            signal.setOnCancelListener(null);
        }
    }

    private static void saveInstallation(String request, RuntimeModule module, File file, long version) throws IOException {
        Closeable retained = Download.retain(file);
        boolean saved = false;
        try {
            if (!Prefers.getPrefers().edit().putString(INSTALL_REQUEST, request).putString(INSTALL_PACKAGE, module.getPackageName()).putLong(INSTALL_VERSION, version).putBoolean(INSTALL_OPENED, false).commit()) throw new IOException("Unable to save runtime installation");
            retainedApk = retained;
            saved = true;
        } finally {
            if (!saved) retained.close();
        }
    }

    private static void requestInstallation(String request, String target, CancellationSignal signal) {
        if (!request.equals(Prefers.getPrefers().getString(INSTALL_REQUEST, ""))) return;
        try {
            if (signal.isCanceled()) finishInstallation(request, target, PackageInstaller.STATUS_FAILURE_ABORTED, null);
            else if (App.activity() != null) resumeConfirmation(App.activity(), false);
            else notifyConfirmation(false);
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void awaitPendingInstallations(CancellationSignal signal) throws IOException {
        while (hasPendingInstallation()) {
            signal.throwIfCanceled();
            try {
                RuntimeInstaller.class.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Runtime installation interrupted", e);
            }
        }
    }

    private static synchronized void wakeInstallers() {
        RuntimeInstaller.class.notifyAll();
    }

    private static void notifyConfirmation(boolean remove) {
        Context context = App.get().getBaseContext();
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        int id = 9530;
        int text = remove ? R.string.module_confirm_remove : R.string.module_confirm_install;
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        PendingIntent content = PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        context.getSystemService(NotificationManager.class).notify(id, new NotificationCompat.Builder(context, Notify.DEFAULT).setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(text)).setContentIntent(content).setAutoCancel(true).build());
    }
}
