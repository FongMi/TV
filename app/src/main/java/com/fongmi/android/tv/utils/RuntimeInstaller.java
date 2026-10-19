package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.MainThread;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.exoplayer.offline.Downloader;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.github.catvod.net.Download;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class RuntimeInstaller extends BroadcastReceiver {

    public static final String RESULT = "com.fongmi.android.tv.RUNTIME_INSTALL_RESULT";
    private static final String CONFIRM_SESSION = "runtimeConfirmSession";
    private static final String CONFIRM_INTENT = "runtimeConfirmIntent";
    private static final String CONFIRM_PACKAGE = "runtimeConfirmPackage";
    private static final String REMOVE_PACKAGE = "runtimeRemovePackage";
    private static final Set<Integer> finishedSessions = new HashSet<>();
    private static int confirmationSession = -1;
    private static boolean removing;

    public static String getAbi() {
        return Process.is64Bit() ? "arm64-v8a" : "armeabi-v7a";
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
        for (int i = 0; i < artifacts.length(); i++) {
            JSONObject artifact = artifacts.getJSONObject(i);
            if (!getAbi().equals(artifact.getString("abi"))) continue;
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

    public static boolean hasPendingSession() {
        PackageInstaller installer = App.get().getBaseContext().getPackageManager().getPackageInstaller();
        return getPendingSessions(installer).stream().anyMatch(info -> getRuntime(info.getAppPackageName()) != null) || getRuntime(Prefers.getPrefers().getString(REMOVE_PACKAGE, "")) != null;
    }

    public static boolean hasPendingSession(RuntimeModule module) {
        String target = module.getPackageName();
        PackageInstaller installer = App.get().getBaseContext().getPackageManager().getPackageInstaller();
        return getPendingSessions(installer).stream().anyMatch(info -> target.equals(info.getAppPackageName())) || target.equals(Prefers.getPrefers().getString(REMOVE_PACKAGE, ""));
    }

    private static synchronized List<PackageInstaller.SessionInfo> getPendingSessions(PackageInstaller installer) {
        List<PackageInstaller.SessionInfo> sessions = new ArrayList<>(installer.getMySessions());
        Set<Integer> existing = new HashSet<>();
        for (PackageInstaller.SessionInfo info : sessions) existing.add(info.getSessionId());
        finishedSessions.retainAll(existing);
        sessions.removeIf(info -> finishedSessions.contains(info.getSessionId()));
        return sessions;
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
        if (hasPendingSession()) throw new IOException("A runtime installation is already in progress");
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

    private static void saveConfirmation(int id, String target, Intent confirm) throws IOException {
        if (confirm == null) throw new IOException("Runtime confirmation is unavailable");
        if (id < 0 || confirm.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != id) throw new IOException("Invalid runtime confirmation session");
        if (getRuntime(target) == null) throw new IOException("Runtime confirmation package mismatch");
        if (!Prefers.getPrefers().edit().putInt(CONFIRM_SESSION, id).putString(CONFIRM_PACKAGE, target).putString(CONFIRM_INTENT, confirm.toUri(Intent.URI_INTENT_SCHEME)).commit()) throw new IOException("Unable to save runtime confirmation");
    }

    private static void clearConfirmation(int id) throws IOException {
        var prefs = Prefers.getPrefers();
        if (prefs.getInt(CONFIRM_SESSION, -1) != id) return;
        if (!prefs.edit().remove(CONFIRM_SESSION).remove(CONFIRM_INTENT).remove(CONFIRM_PACKAGE).commit()) throw new IOException("Unable to clear runtime confirmation");
        if (confirmationSession == id) confirmationSession = -1;
    }

    public static void confirmInstallation(Activity activity) {
        try {
            resumeConfirmation(activity, true);
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    @MainThread
    public static Runnable cancelInstallation(RuntimeModule module, BiConsumer<Boolean, String> callback) {
        String target = module.getPackageName();
        PackageInstaller installer = App.get().getBaseContext().getPackageManager().getPackageInstaller();
        CancelCallback observer = new CancelCallback(target, installer, callback);
        observer.start();
        return observer.completed ? null : () -> observer.complete(null);
    }

    private static class CancelCallback extends PackageInstaller.SessionCallback {

        private final String target;
        private final PackageInstaller installer;
        private final BiConsumer<Boolean, String> callback;
        private final List<Integer> pending = new ArrayList<>();
        private boolean requestsFinished;
        private boolean completed;
        private boolean success = true;

        private CancelCallback(String target, PackageInstaller installer, BiConsumer<Boolean, String> callback) {
            this.target = target;
            this.installer = installer;
            this.callback = callback;
        }

        private void complete(String error) {
            if (completed) return;
            completed = true;
            try {
                installer.unregisterSessionCallback(this);
            } catch (RuntimeException e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            callback.accept(error == null && success, error);
        }

        private void start() {
            try {
                installer.registerSessionCallback(this, new Handler(Looper.getMainLooper()));
                for (PackageInstaller.SessionInfo info : getPendingSessions(installer)) if (target.equals(info.getAppPackageName())) pending.add(info.getSessionId());
                if (pending.isEmpty()) {
                    finishMissingSession();
                    return;
                }
                List<Integer> sessions = List.copyOf(pending);
                Task.execute(() -> abandonSessions(sessions));
            } catch (IOException | RuntimeException e) {
                complete(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }

        private void finishMissingSession() throws IOException {
            success = false;
            var prefs = Prefers.getPrefers();
            int id = prefs.getInt(CONFIRM_SESSION, -1);
            if (target.equals(prefs.getString(CONFIRM_PACKAGE, "")) && installer.getSessionInfo(id) == null) clearConfirmation(id);
            complete(null);
        }

        private void abandonSessions(List<Integer> sessions) {
            String error = null;
            try {
                for (int id : sessions) installer.abandonSession(id);
            } catch (RuntimeException e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            String message = error;
            App.post(() -> finishRequests(message));
        }

        private void finishRequests(String error) {
            requestsFinished = true;
            if (error != null || pending.isEmpty()) complete(error);
        }

        @Override
        public void onCreated(int sessionId) {
        }

        @Override
        public void onBadgingChanged(int sessionId) {
        }

        @Override
        public void onActiveChanged(int sessionId, boolean active) {
        }

        @Override
        public void onProgressChanged(int sessionId, float progress) {
        }

        @Override
        public void onFinished(int sessionId, boolean succeeded) {
            if (completed || !pending.remove(Integer.valueOf(sessionId))) return;
            finishSession(sessionId);
            success &= succeeded;
            String error = null;
            try {
                clearConfirmation(sessionId);
            } catch (IOException | RuntimeException e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            if (error != null || requestsFinished && pending.isEmpty()) complete(error);
        }
    }

    private static void resumeConfirmation(Activity activity, boolean requested) throws IOException {
        var prefs = Prefers.getPrefers();
        String uri = prefs.getString(CONFIRM_INTENT, "");
        if (uri.isEmpty()) return;
        int id = prefs.getInt(CONFIRM_SESSION, -1);
        String target = prefs.getString(CONFIRM_PACKAGE, "");
        PackageInstaller installer = App.get().getBaseContext().getPackageManager().getPackageInstaller();
        PackageInstaller.SessionInfo info = installer.getSessionInfo(id);
        if (info == null) {
            clearConfirmation(id);
            return;
        }
        if (getRuntime(target) == null || !target.equals(info.getAppPackageName())) throw new IOException("Runtime confirmation package mismatch");
        if (!requested && confirmationSession == id) return;
        try {
            Intent confirm = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            if (confirm.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != id) throw new IOException("Runtime confirmation session mismatch");
            activity.startActivity(confirm);
            confirmationSession = id;
        } catch (URISyntaxException e) {
            throw new IOException("Invalid runtime confirmation", e);
        } catch (ActivityNotFoundException | SecurityException e) {
            failConfirmation(installer, id, target, e);
        }
    }

    private static void failConfirmation(PackageInstaller installer, int session, String target, Exception error) throws IOException {
        Context context = App.get().getBaseContext();
        try {
            Intent result = new Intent(context, RuntimeInstaller.class).setAction(RESULT);
            PendingIntent pending = PendingIntent.getBroadcast(context, session, result, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_MUTABLE);
            if (pending != null) pending.cancel();
        } finally {
            try {
                installer.abandonSession(session);
            } finally {
                finishInstallation(context, session, target, PackageInstaller.STATUS_FAILURE, error.getMessage() == null ? error.toString() : error.getMessage());
            }
        }
    }

    private static void finishInstallation(Context context, int session, String target, int status, String message) throws IOException {
        try {
            clearConfirmation(session);
        } finally {
            finishSession(session);
            if (message == null || message.isEmpty()) message = context.getString(R.string.module_failed_title);
            if (status != PackageInstaller.STATUS_SUCCESS) Notify.show(status == PackageInstaller.STATUS_FAILURE_ABORTED ? context.getString(R.string.module_cancelled) : message);
            context.sendBroadcast(new Intent(RESULT).setPackage(context.getPackageName()).setData(Uri.fromParts("package", target, null)).putExtra(PackageInstaller.EXTRA_STATUS, status).putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message));
        }
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
        signal.throwIfCanceled();
        module.validate(file);
        signal.throwIfCanceled();
        String target = module.getPackageName();
        Path.requireSpace(file, file.length());
        PackageInstaller installer = App.get().getBaseContext().getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(target);
        int id = createSession(installer, params, signal);
        boolean committed = false;
        Throwable failure = null;
        try (PackageInstaller.Session session = installer.openSession(id)) {
            writeSession(session, file, signal);
            commitSession(session, id, target, file, signal);
            committed = true;
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            if (!committed) abandonSession(installer, id, failure);
        }
    }

    private static void abandonSession(PackageInstaller installer, int id, Throwable failure) {
        try {
            installer.abandonSession(id);
        } catch (RuntimeException | Error e) {
            if (failure == null) throw e;
            if (failure != e) failure.addSuppressed(e);
        }
    }

    private static void writeSession(PackageInstaller.Session session, File file, CancellationSignal signal) throws IOException {
        try (InputStream input = new FileInputStream(file); OutputStream output = session.openWrite(file.getName(), 0, file.length())) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                signal.throwIfCanceled();
                output.write(buffer, 0, count);
            }
            session.fsync(output);
        }
    }

    private static void commitSession(PackageInstaller.Session session, int id, String target, File file, CancellationSignal signal) throws IOException {
        Context context = App.get().getBaseContext();
        Intent result = new Intent(context, RuntimeInstaller.class).setAction(RESULT).putExtra("runtimePackage", target);
        if (file.getCanonicalFile().getParentFile().equals(new File(Path.cache(), ApkCache.MODULE_DIRECTORY).getCanonicalFile())) result.putExtra("runtimeFile", file.getName());
        PendingIntent pending = PendingIntent.getBroadcast(context, id, result, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        signal.throwIfCanceled();
        session.commit(pending.getIntentSender());
    }

    private static int createSession(PackageInstaller installer, PackageInstaller.SessionParams params, CancellationSignal signal) throws IOException {
        PackageInstaller.SessionCallback callback = new PackageInstaller.SessionCallback() {
            @Override
            public void onCreated(int sessionId) {
            }

            @Override
            public void onBadgingChanged(int sessionId) {
            }

            @Override
            public void onActiveChanged(int sessionId, boolean active) {
            }

            @Override
            public void onProgressChanged(int sessionId, float progress) {
            }

            @Override
            public void onFinished(int sessionId, boolean success) {
                finishSession(sessionId);
            }
        };
        installer.registerSessionCallback(callback, new Handler(Looper.getMainLooper()));
        signal.setOnCancelListener(RuntimeInstaller::wakeInstallers);
        try {
            synchronized (RuntimeInstaller.class) {
                awaitPendingSessions(signal);
                signal.throwIfCanceled();
                return installer.createSession(params);
            }
        } finally {
            signal.setOnCancelListener(null);
            installer.unregisterSessionCallback(callback);
        }
    }

    private static void awaitPendingSessions(CancellationSignal signal) throws IOException {
        while (hasPendingSession()) {
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

    private static synchronized void finishSession(int id) {
        finishedSessions.add(id);
        RuntimeInstaller.class.notifyAll();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!RESULT.equals(intent.getAction())) return;
        try {
            handleResult(context, intent);
        } catch (IOException | RuntimeException e) {
            Notify.show(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void handleResult(Context context, Intent intent) throws IOException {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        int session = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        String target = intent.getStringExtra("runtimePackage");
        RuntimeModule module = getRuntime(target);
        if (module == null) throw new IOException("Runtime result package mismatch");
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            requestConfirmation(context, session, target, intent.getParcelableExtra(Intent.EXTRA_INTENT));
            return;
        }
        try {
            if (status == PackageInstaller.STATUS_SUCCESS) clearInstalledApk(module, intent);
        } finally {
            finishInstallation(context, session, target, status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        }
    }

    private static void clearInstalledApk(RuntimeModule module, Intent intent) throws IOException {
        File directory = new File(Path.cache(), ApkCache.MODULE_DIRECTORY);
        ApkCache.installed(directory, module.id, intent.getStringExtra("runtimeFile"));
    }

    private static void requestConfirmation(Context context, int session, String target, Intent confirm) throws IOException {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionInfo info = installer.getSessionInfo(session);
        if (info == null || !target.equals(info.getAppPackageName())) throw new IOException("Runtime confirmation package mismatch");
        try {
            saveConfirmation(session, target, confirm);
        } catch (IOException | RuntimeException e) {
            failConfirmation(installer, session, target, e);
            return;
        }
        Activity activity = App.activity();
        if (activity != null) resumeConfirmation(activity, true);
        else notifyConfirmation(false);
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
