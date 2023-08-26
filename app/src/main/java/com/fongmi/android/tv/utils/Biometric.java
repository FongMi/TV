package com.fongmi.android.tv.utils;

import androidx.fragment.app.FragmentActivity;

public class Biometric {

    public static boolean enable() {
        return false;
    }

    public static void show(FragmentActivity activity) {
    }

    public interface Callback {

        void onBiometricSuccess();
    }
}
