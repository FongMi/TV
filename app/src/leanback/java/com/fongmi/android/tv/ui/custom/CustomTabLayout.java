package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

public final class CustomTabLayout extends LinearLayoutCompat {

    public CustomTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        View next = super.focusSearch(focused, direction);
        if (direction != FOCUS_UP && direction != FOCUS_DOWN) return next;
        ViewGroup group = findCheckedGroup(next);
        if (group == null) return next;
        int checkedId = group instanceof MaterialButtonToggleGroup tabs ? tabs.getCheckedButtonId() : ((ChipGroup) group).getCheckedChipId();
        View target = findRedirectTarget(group, checkedId, focused, next);
        return target == null ? next : target;
    }

    private static ViewGroup findCheckedGroup(View view) {
        ViewParent current = view == null ? null : view.getParent();
        while (current instanceof View) {
            if (current instanceof MaterialButtonToggleGroup || current instanceof ChipGroup) return (ViewGroup) current;
            current = current.getParent();
        }
        return null;
    }

    static View findRedirectTarget(ViewGroup tabs, int checkedId, View focused, View next) {
        if (next == null || isDescendant(tabs, focused) || !isDescendant(tabs, next)) return null;
        View checked = tabs.findViewById(checkedId);
        if (checked == next || checked == null || checked.getVisibility() != VISIBLE
                || !checked.isEnabled() || !checked.isFocusable()) return null;
        return checked;
    }

    private static boolean isDescendant(ViewGroup parent, View child) {
        ViewParent current = child.getParent();
        while (current != null) {
            if (current == parent) return true;
            current = current.getParent();
        }
        return false;
    }
}
