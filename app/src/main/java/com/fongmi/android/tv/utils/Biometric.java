package com.fongmi.android.tv.utils;

public class Biometric {

    public static boolean enable() {
        return false;
    }

    public interface Callback {

        void onBiometricSuccess();
    }
}
