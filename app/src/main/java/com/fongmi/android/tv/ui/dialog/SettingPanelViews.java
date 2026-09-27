package com.fongmi.android.tv.ui.dialog;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

final class SettingPanelViews {

    static void applyEnabled(View view, boolean enabled) {
        view.setAlpha(enabled ? 1.0f : 0.38f);
        setEnabled(view, enabled);
    }

    private static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) setEnabled(group.getChildAt(i), enabled);
    }

    static void checkPreset(ChipGroup[] groups, int chip, ChipGroup.OnCheckedStateChangeListener listener) {
        setPresetListeners(groups, null);
        for (ChipGroup group : groups) group.clearCheck();
        if (chip != View.NO_ID) {
            for (ChipGroup group : groups) {
                if (group.findViewById(chip) == null) continue;
                group.check(chip);
                break;
            }
        }
        setPresetListeners(groups, listener);
    }

    static void clearOtherPresets(ChipGroup[] groups, ChipGroup checked, ChipGroup.OnCheckedStateChangeListener listener) {
        setPresetListeners(groups, null);
        for (ChipGroup group : groups) if (group != checked) group.clearCheck();
        setPresetListeners(groups, listener);
    }

    private static void setPresetListeners(ChipGroup[] groups, ChipGroup.OnCheckedStateChangeListener listener) {
        for (ChipGroup group : groups) group.setOnCheckedStateChangeListener(listener);
    }

    static void bindTabs(MaterialButtonToggleGroup group, MaterialButton[] tabs, IntConsumer onSelected) {
        if (Util.isLeanback()) for (MaterialButton tab : tabs) tab.setOnFocusChangeListener((view, focused) -> {
            if (focused) group.check(tab.getId());
        });
        group.addOnButtonCheckedListener((buttons, checkedId, checked) -> {
            if (!checked) return;
            for (int i = 0; i < tabs.length; i++) if (checkedId == tabs[i].getId()) onSelected.accept(i);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    static void bindCompare(View view, Consumer<Boolean> preview) {
        view.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) preview.accept(true);
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) preview.accept(false);
            return false;
        });
        view.setOnKeyListener((target, keyCode, event) -> {
            boolean supported = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
            if (supported && event.getAction() == KeyEvent.ACTION_DOWN) preview.accept(true);
            else if (supported && event.getAction() == KeyEvent.ACTION_UP) preview.accept(false);
            return false;
        });
    }

    private SettingPanelViews() {
    }
}
