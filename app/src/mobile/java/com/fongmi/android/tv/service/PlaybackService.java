package com.fongmi.android.tv.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.impl.CustomTarget;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.receiver.ActionReceiver;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlaybackService extends Service {

    private static Class<?> classes;
    private static Players players;
    private final int ID = 9527;
    private Bitmap bitmap;

    public static void start(Activity activity, Players players) {
        ContextCompat.startForegroundService(activity, new Intent(activity, PlaybackService.class));
        PlaybackService.classes = activity.getClass();
        PlaybackService.players = players;
    }

    public static void stop() {
        App.get().stopService(new Intent(App.get(), PlaybackService.class));
    }

    private NotificationCompat.Action buildNotificationAction(@DrawableRes int icon, @StringRes int title, String type) {
        return new NotificationCompat.Action.Builder(icon, getString(title), ActionReceiver.getPendingIntent(this, type)).setContextual(false).setShowsUserInterface(false).build();
    }

    private NotificationCompat.Action getPlayPauseAction() {
        if (players != null && players.isPlaying()) return buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_pause, androidx.media3.ui.R.string.exo_controls_pause_description, ActionEvent.PAUSE);
        return buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_play, androidx.media3.ui.R.string.exo_controls_play_description, ActionEvent.PLAY);
    }

    private void setLargeIcon(NotificationCompat.Builder builder) {
        Bitmap b1 = Bitmap.createScaledBitmap(bitmap, 16, 16, true);
        Bitmap b2 = Bitmap.createScaledBitmap(b1, 1, 1, true);
        builder.setColor(b2.getPixel(0, 0));
        builder.setLargeIcon(bitmap);
        b2.recycle();
        b1.recycle();
    }

    private CharSequence getTitle() {
        if (players.getMetadata() == null || TextUtils.isEmpty(players.getMetadata().title)) return null;
        return players.getMetadata().title;
    }

    private String getText() {
        if (players.getMetadata() == null || TextUtils.isEmpty(players.getMetadata().artist)) return null;
        if (players.getMetadata().artist.equals(players.getMetadata().title)) return null;
        return getString(R.string.live_epg_now, players.getMetadata().artist);
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, classes);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Utils.getPendingFlag());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Notify.DEFAULT);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setSmallIcon(R.drawable.ic_logo);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getTitle());
        builder.setContentText(getText());
        builder.setOngoing(false);
        builder.setColorized(true);
        builder.setOnlyAlertOnce(true);
        builder.addAction(buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_previous, androidx.media3.ui.R.string.exo_controls_previous_description, ActionEvent.PREV));
        builder.addAction(getPlayPauseAction());
        builder.addAction(buildNotificationAction(androidx.media3.ui.R.drawable.exo_icon_next, androidx.media3.ui.R.string.exo_controls_next_description, ActionEvent.NEXT));
        MediaStyle mediaStyle = new MediaStyle();
        mediaStyle.setShowActionsInCompactView(0, 2);
        mediaStyle.setShowCancelButton(true);
        //mediaStyle.setMediaSession(players.getSession().getSessionToken());
        mediaStyle.setCancelButtonIntent(ActionReceiver.getPendingIntent(this, ActionEvent.CANCEL));
        builder.setDeleteIntent(ActionReceiver.getPendingIntent(this, ActionEvent.CANCEL));
        if (bitmap != null) setLargeIcon(builder);
        builder.setStyle(mediaStyle);
        return builder.build();
    }

    private void setNotification() {
        NotificationManager notifyMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMgr.notify(ID, buildNotification());
    }

    private void setArtwork() {
        ImgUtil.load(players.getMetadata().artworkUri, R.drawable.ic_img_error, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                bitmap = resource;
                setNotification();
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                bitmap = null;
                setNotification();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (!event.getType().equals(ActionEvent.UPDATE)) return;
        if (players != null && players.getMetadata() != null) {
            setArtwork();
        } else {
            setNotification();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (players != null && players.getMetadata() != null) setArtwork();
        startForeground(ID, buildNotification());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
