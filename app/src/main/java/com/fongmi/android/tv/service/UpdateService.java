package com.fongmi.android.tv.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.utils.ApkCache;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UpdatePackage;
import com.github.catvod.net.Download;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Task;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Future;

public class UpdateService extends Service implements Download.Callback {

    private static final String CANCEL = "cancel";
    private static final String PAUSE = "pause";
    private static final int ID = 9529;

    private UpdatePackage update;
    private Download download;
    private Future<?> verification;
    private Closeable retained;
    private File file;
    private boolean finished;
    private boolean paused;
    private long lastNotification;

    public static void start(UpdatePackage update) {
        Intent intent = new Intent(App.get(), UpdateService.class);
        intent.putExtra("version", update.version()).putExtra("url", update.url()).putExtra("size", update.size()).putExtra("digest", update.digest());
        ContextCompat.startForegroundService(App.get(), intent);
    }

    public static void pause() {
        App.get().startService(new Intent(App.get(), UpdateService.class).setAction(PAUSE));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceCompat.startForeground(this, ID, downloadNotification(0), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        lastNotification = SystemClock.elapsedRealtime();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && PAUSE.equals(intent.getAction())) {
            if (!finished && download != null) download.pause();
            return START_NOT_STICKY;
        }
        if (finished) return START_NOT_STICKY;
        if (intent == null || CANCEL.equals(intent.getAction())) {
            error(getString(R.string.update_interrupted));
            return START_NOT_STICKY;
        }
        if (download != null || verification != null) return START_NOT_STICKY;
        try {
            prepareUpdate(intent);
        } catch (Exception e) {
            error(e.getMessage());
        }
        return START_NOT_STICKY;
    }

    private void prepareUpdate(Intent intent) throws IOException {
        update = new UpdatePackage(intent.getStringExtra("version"), intent.getStringExtra("url"), intent.getLongExtra("size", 0), intent.getStringExtra("digest"));
        File directory = ApkCache.app();
        retained = Download.retain(directory);
        file = update.file(directory);
        if (file.isFile()) success(file);
        else startDownload();
    }

    private void startDownload() {
        if (finished) return;
        download = Download.create(update.url(), file).previousDirectory(new File(getCacheDir(), ".downloads")).size(update.size());
        download.start(this);
    }

    private PendingIntent launchIntent() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        return PendingIntent.getActivity(this, ID, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private NotificationCompat.Builder notification(String text, PendingIntent content) {
        return new NotificationCompat.Builder(this, Notify.DEFAULT).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.app_name)).setContentText(text).setContentIntent(content);
    }

    private Notification downloadNotification(int progress) {
        PendingIntent cancel = PendingIntent.getService(this, ID, new Intent(this, UpdateService.class).setAction(CANCEL), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return notification(getString(progress < 0 ? R.string.update_verifying : R.string.update_downloading), launchIntent()).setOnlyAlertOnce(true).setOngoing(true).setProgress(100, Math.max(0, progress), progress < 0).addAction(0, getString(R.string.dialog_negative), cancel).build();
    }

    private void notify(Notification notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) getSystemService(NotificationManager.class).notify(ID, notification);
    }

    @Override
    public void progress(int progress) {
        if (finished) return;
        long now = SystemClock.elapsedRealtime();
        if (progress < 0 || now - lastNotification >= 1000) {
            lastNotification = now;
            notify(downloadNotification(progress));
        }
        Updater.get().progress(progress);
    }

    @Override
    public void paused() {
        if (finished) return;
        finished = true;
        paused = true;
        download = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        notify(notification(getString(R.string.transfer_paused), launchIntent()).setAutoCancel(true).build());
        stopSelf();
    }

    @Override
    public void success(File file) {
        if (finished) return;
        progress(-1);
        verification = Task.submit(() -> verify(file));
    }

    private void verify(File file) {
        try {
            update.verifyApk(this, file);
            App.post(() -> complete(file));
        } catch (Exception e) {
            if (restartCachedDownload(file, e)) return;
            App.post(() -> error(Notify.getError(R.string.update_verify_failed, e)));
        }
    }

    private boolean restartCachedDownload(File file, Exception error) {
        if (!(error instanceof IOException) || download != null || Thread.currentThread().isInterrupted()) return false;
        try {
            Files.deleteIfExists(file.toPath());
            App.post(this::startDownload);
            return true;
        } catch (IOException cleanup) {
            error.addSuppressed(cleanup);
            return false;
        }
    }

    private void complete(File file) {
        if (finished) return;
        finished = true;
        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(FileUtil.getShareUri(file), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent content = PendingIntent.getActivity(this, ID + 1, install, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        stopForeground(STOP_FOREGROUND_REMOVE);
        notify(notification(getString(R.string.update_ready), content).setContentTitle(getString(R.string.update_version, update.version())).setAutoCancel(true).build());
        Updater.get().success(file);
        stopSelf();
    }

    @Override
    public void error(String msg) {
        if (finished) return;
        finished = true;
        cancelTasks();
        Updater.get().error(msg);
        stopForeground(STOP_FOREGROUND_REMOVE);
        notify(notification(msg, launchIntent()).setAutoCancel(true).build());
        stopSelf();
    }

    private void cancelTasks() {
        if (download != null) download.cancel();
        if (verification != null) verification.cancel(true);
        if (file != null) Path.clear(file);
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        error(getString(R.string.update_interrupted));
    }

    @Override
    public void onDestroy() {
        if (paused) {
            Updater.get().paused();
        } else if (!finished) {
            finished = true;
            cancelTasks();
            Updater.get().error(getString(R.string.update_interrupted));
        }
        try {
            if (retained != null) retained.close();
        } catch (IOException e) {
            Notify.show(e.getMessage());
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
