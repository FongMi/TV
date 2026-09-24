package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.mpv.MpvScripts;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.ui.dialog.MpvScriptButtonsDialog;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.utils.Prefers;

import org.json.JSONException;

/** Playback shortcuts that resolve the active player at click time, not at view creation. */
public final class MpvScriptButtonsView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final Observer<PlayerManager> playerObserver = this::updateVisibility;
    private LiveData<PlayerManager> players;

    public MpvScriptButtonsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setVisibility(GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Prefers.getPrefers().registerOnSharedPreferenceChangeListener(this);
        refresh();
        if (activity() instanceof PlaybackActivity activity) {
            players = activity.getPlaybackPlayerState();
            players.observe(activity, playerObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (players != null) players.removeObserver(playerObserver);
        players = null;
        setVisibility(GONE);
        updateFocusNeighbors();
        Prefers.getPrefers().unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }

    private FragmentActivity activity() {
        Context context = getContext();
        while (context instanceof ContextWrapper wrapper && !(context instanceof FragmentActivity)) context = wrapper.getBaseContext();
        return context instanceof FragmentActivity activity ? activity : null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (MpvScripts.PREFERENCE_KEY.equals(key)) refresh();
    }

    private void refresh() {
        View focused = getFocusedChild();
        Object focusedId = focused == null ? null : focused.getTag();
        removeAllViews();
        TextView manage = addButton(getContext().getString(R.string.mpv_script_buttons));
        manage.setOnClickListener(view -> manage(null));
        try {
            for (MpvScripts.Item button : MpvScripts.read()) {
                if (button.automatic || button.hidden) continue;
                TextView view = addButton(button.title);
                view.setAlpha(button.enabled ? 1f : 0.5f);
                if (!button.enabled) view.setContentDescription(button.title + " · " + getContext().getString(R.string.mpv_script_disabled));
                view.setTag(button.id);
                view.setOnClickListener(v -> run(button));
                view.setOnLongClickListener(v -> {
                    manage(button.id);
                    return true;
                });
            }
        } catch (JSONException e) {
            manage.setContentDescription(Notify.getError(R.string.mpv_script_error, e));
        }
        updateFocusNeighbors();
        if (focused != null) {
            View target = focusedId == null ? manage : findViewWithTag(focusedId);
            (target == null ? manage : target).requestFocus();
        }
    }

    private TextView addButton(String title) {
        TextView view = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.view_mpv_script_button, this, false);
        view.setText(title);
        view.setId(View.generateViewId());
        view.setNextFocusDownId(view.getId());
        addView(view);
        return view;
    }

    private void updateFocusNeighbors() {
        boolean visible = getVisibility() == VISIBLE;
        View first = getChildAt(0);
        View last = getChildAt(getChildCount() - 1);
        View left = getRootView().findViewById(getNextFocusLeftId());
        View right = getRootView().findViewById(getNextFocusRightId());
        if (left != null) {
            first.setNextFocusLeftId(left.getId());
            left.setNextFocusRightId(visible ? first.getId() : getNextFocusRightId());
        }
        if (right != null) {
            last.setNextFocusRightId(right.getId());
            right.setNextFocusLeftId(visible ? last.getId() : getNextFocusLeftId());
        }
    }

    private void updateVisibility(PlayerManager player) {
        boolean visible = player != null && player.getEngine() == PlayerSetting.ENGINE_MPV;
        boolean restoreFocus = !visible && hasFocus();
        setVisibility(visible ? VISIBLE : GONE);
        updateFocusNeighbors();
        if (restoreFocus) {
            View target = getRootView().findViewById(getNextFocusLeftId());
            if (target != null && target.isShown()) target.requestFocus();
        }
    }

    private void manage(String id) {
        FragmentActivity activity = activity();
        if (activity != null) MpvScriptButtonsDialog.show(activity, id);
    }

    private void run(MpvScripts.Item item) {
        FragmentActivity activity = activity();
        PlayerManager manager = activity instanceof PlaybackActivity playback ? playback.getPlaybackPlayer() : null;
        if (manager == null || !manager.runMpvScript(item)) Notify.show(R.string.mpv_script_unavailable);
    }
}
