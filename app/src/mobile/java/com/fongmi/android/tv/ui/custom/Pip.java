package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.util.Rational;

import com.fongmi.android.tv.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Pip {

    public static final String ACTION_MEDIA_CONTROL = "media_control";
    public static final String EXTRA_CONTROL_TYPE = "control_type";

    public static final int CONTROL_TYPE_PREV = 1;
    public static final int CONTROL_TYPE_NEXT = 2;
    public static final int CONTROL_TYPE_PLAY = 3;

    private PictureInPictureParams.Builder builder;

    public Pip() {
        if (!Utils.hasPIP()) return;
        this.builder = new PictureInPictureParams.Builder();
    }

    public void update(Activity activity, boolean play) {
        if (!Utils.hasPIP()) return;
        List<RemoteAction> actions = new ArrayList<>();
        actions.add(new RemoteAction(Icon.createWithResource(activity, com.google.android.exoplayer2.ui.R.drawable.exo_icon_previous), "", "", PendingIntent.getBroadcast(activity, CONTROL_TYPE_PREV, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PREV), 0)));
        actions.add(new RemoteAction(Icon.createWithResource(activity, play ? com.google.android.exoplayer2.ui.R.drawable.exo_icon_pause : com.google.android.exoplayer2.ui.R.drawable.exo_icon_play), "", "", PendingIntent.getBroadcast(activity, CONTROL_TYPE_PLAY, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY), 0)));
        actions.add(new RemoteAction(Icon.createWithResource(activity, com.google.android.exoplayer2.ui.R.drawable.exo_icon_next), "", "", PendingIntent.getBroadcast(activity, CONTROL_TYPE_NEXT, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_NEXT), 0)));
        builder.setActions(actions);
        activity.setPictureInPictureParams(builder.build());
    }

    public void enter(Activity activity, boolean four) {
        try {
            if (!Utils.hasPIP() || activity.isInPictureInPictureMode()) return;
            builder.setAspectRatio(new Rational(four ? 4 : 16, four ? 3 : 9)).build();
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
