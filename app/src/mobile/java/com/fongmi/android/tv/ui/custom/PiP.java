package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Rational;
import android.view.View;

import androidx.media3.ui.R;

import com.fongmi.android.tv.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class PiP {

    public static final String ACTION_MEDIA_CONTROL = "media_control";
    public static final String EXTRA_CONTROL_TYPE = "control_type";

    public static final int CONTROL_TYPE_PREV = 1;
    public static final int CONTROL_TYPE_NEXT = 2;
    public static final int CONTROL_TYPE_PLAY = 3;
    public static final int CONTROL_TYPE_PAUSE = 4;

    private PictureInPictureParams.Builder builder;

    public PiP() {
        if (!Utils.hasPiP()) return;
        this.builder = new PictureInPictureParams.Builder();
    }

    public void update(Activity activity, View view) {
        if (!Utils.hasPiP()) return;
        Rect sourceRectHint = new Rect();
        view.getGlobalVisibleRect(sourceRectHint);
        builder.setSourceRectHint(sourceRectHint);
        activity.setPictureInPictureParams(builder.build());
    }

    public void update(Activity activity, boolean play) {
        if (!Utils.hasPiP()) return;
        List<RemoteAction> actions = new ArrayList<>();
        int icon = play ? R.drawable.exo_icon_pause : R.drawable.exo_icon_play;
        actions.add(new RemoteAction(Icon.createWithResource(activity, R.drawable.exo_icon_previous), "", "", PendingIntent.getBroadcast(activity, CONTROL_TYPE_PREV, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PREV), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)));
        actions.add(new RemoteAction(Icon.createWithResource(activity, icon), "", "", PendingIntent.getBroadcast(activity, play ? CONTROL_TYPE_PAUSE : CONTROL_TYPE_PLAY, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, play ? CONTROL_TYPE_PAUSE : CONTROL_TYPE_PLAY), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)));
        actions.add(new RemoteAction(Icon.createWithResource(activity, R.drawable.exo_icon_next), "", "", PendingIntent.getBroadcast(activity, CONTROL_TYPE_NEXT, new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)));
        activity.setPictureInPictureParams(builder.setActions(actions).build());
    }

    public void enter(Activity activity, boolean four) {
        try {
            if (!Utils.hasPiP() || activity.isInPictureInPictureMode()) return;
            builder.setAspectRatio(new Rational(four ? 4 : 16, four ? 3 : 9));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setSeamlessResizeEnabled(true);
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
