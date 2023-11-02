package com.fongmi.android.tv.bean;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;

public class Info {

    private final Drawable icon;
    private final String name;
    private final String pack;

    public static Info get(ApplicationInfo info) {
        Drawable icon = info.loadIcon(App.get().getPackageManager());
        String name = info.loadLabel(App.get().getPackageManager()).toString();
        String pack = info.packageName;
        return new Info(name, pack, icon);
    }

    public Info(String name, String pack, Drawable icon) {
        this.name = name;
        this.pack = pack;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getPack() {
        return pack;
    }

    public Drawable getIcon() {
        return icon;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Info)) return false;
        Info it = (Info) obj;
        return getPack().equals(it.getPack());
    }
}
