package com.fongmi.android.tv.ui.dialog;

import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

final class PlaybackDialogFocus {

    private PlaybackDialogFocus() {
    }

    static void selectFirstTab(View root, MaterialButtonToggleGroup group, MaterialButton tab) {
        group.check(tab.getId());
        request(root, tab);
    }

    static void request(View root, View target) {
        if (Util.isLeanback()) root.post(target::requestFocus);
    }

    static void preferCheckedChips(View root) {
        if (Util.isLeanback()) new CheckedChipFocus(root).start();
    }

    private static final class CheckedChipFocus implements ViewTreeObserver.OnGlobalFocusChangeListener, View.OnAttachStateChangeListener {

        private final View root;
        private ViewTreeObserver observer;

        private CheckedChipFocus(View root) {
            this.root = root;
        }

        private void start() {
            root.addOnAttachStateChangeListener(this);
            if (root.isAttachedToWindow()) register();
        }

        private void register() {
            observer = root.getViewTreeObserver();
            observer.addOnGlobalFocusChangeListener(this);
        }

        private void unregister() {
            if (observer != null && observer.isAlive()) observer.removeOnGlobalFocusChangeListener(this);
            observer = null;
        }

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            ChipGroup group = findChipGroup(newFocus);
            if (group == null || group == findChipGroup(oldFocus)) return;
            View checked = group.findViewById(group.getCheckedChipId());
            if (checked == null || checked == newFocus || !checked.isFocusable()) return;
            if (checked.isShown() && checked.isEnabled()) checked.requestFocus();
        }

        private ChipGroup findChipGroup(View view) {
            View current = view;
            while (current != null && current != root) {
                if (current instanceof ChipGroup group) return group;
                ViewParent parent = current.getParent();
                current = parent instanceof View ? (View) parent : null;
            }
            return null;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            register();
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            unregister();
            root.removeOnAttachStateChangeListener(this);
        }
    }
}
