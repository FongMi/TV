package com.fongmi.android.tv.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.receiver.ActionReceiver;
import com.fongmi.android.tv.utils.Notify;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlaybackService extends Service {

    private static Players players;
    private final int ID = 9527;

    public static void start(Players players) {
        ContextCompat.startForegroundService(App.get(), new Intent(App.get(), PlaybackService.class));
        PlaybackService.players = players;
    }

    public static void stop() {
        App.get().stopService(new Intent(App.get(), PlaybackService.class));
    }

    private NotificationManager getManager() {
        return (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private NotificationCompat.Action buildNotificationAction(@DrawableRes int icon, @StringRes int title, String action) {
        return new NotificationCompat.Action(icon, getString(title), ActionReceiver.getPendingIntent(this, action));
    }

    private NotificationCompat.Action getPlayPauseAction() {
        if (players != null && players.isPlaying()) return buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_pause, androidx.media3.ui.R.string.exo_controls_pause_description, ActionEvent.PAUSE);
        return buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_play, androidx.media3.ui.R.string.exo_controls_play_description, ActionEvent.PLAY);
    }

    private MediaMetadataCompat getMetadata() {
        return players.getSession().getController().getMetadata();
    }

    private String getTitle() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE).isEmpty() ? null : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
    }

    private String getArtist() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST).isEmpty() ? null : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
    }

    private Bitmap getArt() {
        return getMetadata() == null ? null : getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
    }

    private void setLargeIcon(NotificationCompat.Builder builder, Bitmap art) {
        Bitmap b1 = Bitmap.createScaledBitmap(art, 16, 16, true);
        Bitmap b2 = Bitmap.createScaledBitmap(b1, 1, 1, true);
        builder.setColor(b2.getPixel(0, 0));
        builder.setLargeIcon(art);
        b2.recycle();
        b1.recycle();
    }

    private void setAction(NotificationCompat.Builder builder) {
        builder.addAction(buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_previous, androidx.media3.ui.R.string.exo_controls_previous_description, ActionEvent.PREV));
        builder.addAction(getPlayPauseAction());
        builder.addAction(buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_next, androidx.media3.ui.R.string.exo_controls_next_description, ActionEvent.NEXT));
    }

    private void setStyle(NotificationCompat.Builder builder) {
        MediaStyle style = new MediaStyle();
        style.setShowCancelButton(true);
        style.setCancelButtonIntent(ActionReceiver.getPendingIntent(this, ActionEvent.STOP));
        builder.setStyle(style);
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Notify.DEFAULT);
        setStyle(builder);
        setAction(builder);
        Bitmap art = getArt();
        builder.setOngoing(false);
        builder.setColorized(true);
        builder.setOnlyAlertOnce(true);
        builder.setContentText(getArtist());
        builder.setContentTitle(getTitle());
        builder.setSmallIcon(R.drawable.ic_logo);
        if (art != null) setLargeIcon(builder, art);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setDeleteIntent(ActionReceiver.getPendingIntent(this, ActionEvent.STOP));
        builder.setContentIntent(players.getSession().getController().getSessionActivity());
        return builder.build();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (event.isUpdate()) getManager().notify(ID, buildNotification());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ID, buildNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        getManager().cancel(ID);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
