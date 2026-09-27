package com.fongmi.android.tv.playback;

import android.view.View;
import android.widget.TextView;

import androidx.media3.common.C;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.utils.ResUtil;

public final class PlaybackAction {

    public static void setPlaybackMode(PlayerManager player, TextView engine) {
        if (engine != null) engine.setText(getEngineText(player));
    }

    public static float toggleSpeed(PlayerManager player, TextView view) {
        float speed = player.toggleSpeed();
        showSpeedHint(view, speed);
        return speed;
    }

    public static boolean startSpeedPress(PlayerManager player, TextView view) {
        if (!player.startSpeedPress(SpeedSetting.getLongPress())) return false;
        showSpeedPress(view, player.getSpeed());
        return true;
    }

    public static void endSpeedPress(PlayerManager player, TextView view) {
        player.endSpeedPress();
        hideSpeedHint(view);
    }

    public static void showSpeedHint(TextView view, float speed) {
        if (view == null) return;
        setSpeedHint(view, speed);
        view.animate().alpha(0.0f).setStartDelay(900).setDuration(180).withEndAction(() -> clearSpeedHint(view)).start();
    }

    public static void showSpeedPress(TextView view, float speed) {
        if (view == null) return;
        setSpeedHint(view, speed);
    }

    public static void hideSpeedHint(TextView view) {
        if (view == null) return;
        view.animate().cancel();
        clearSpeedHint(view);
    }

    private static void clearSpeedHint(TextView view) {
        view.setVisibility(View.GONE);
        view.setText("");
    }

    private static void setSpeedHint(TextView view, float speed) {
        view.setAlpha(1.0f);
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setText(ResUtil.getString(R.string.play_speed_hint, SpeedSetting.format(speed)));
    }

    public static void setTracks(PlayerManager player, View text, View audio, View video) {
        setVisible(text, player != null && (player.haveTrack(C.TRACK_TYPE_TEXT) || player.isVod()));
        setVisible(audio, player != null && (player.haveTrack(C.TRACK_TYPE_AUDIO) || player.getDecoderMode(C.TRACK_TYPE_AUDIO) != null));
        setVisible(video, player != null && (player.haveTrack(C.TRACK_TYPE_VIDEO) || player.getDecoderMode(C.TRACK_TYPE_VIDEO) != null));
    }

    public static void setTracks(PlayerManager player, View text, View audio, View video, View speed) {
        setTracks(player, text, audio, video);
        setVisible(speed, player != null && player.isVod());
    }

    public static void setMediaOptions(PlayerManager player, View edition, View chapter) {
        setVisible(edition, player != null && player.haveEdition());
        setVisible(chapter, player != null && player.haveChapter());
    }

    public static void setWebPlaybackMode(boolean active, View... nativeOnlyViews) {
        for (View view : nativeOnlyViews) setVisible(view, !active);
    }

    public static String getEngineText(PlayerManager player) {
        return ResUtil.getStringArray(R.array.select_engine)[getEngine(player)];
    }

    public static int getEngine(PlayerManager player) {
        if (player == null || player.isReleased()) return PlayerSetting.getEngine();
        return player.getEngine();
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
