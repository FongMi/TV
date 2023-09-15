package com.fongmi.android.tv.utils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class Biometric {

    private static BiometricManager getManager() {
        return BiometricManager.from(App.get());
    }

    private static int canAuthenticate() {
        return getManager().canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
    }

    public static boolean enable() {
        return canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void show(FragmentActivity activity) {
        prompt(activity).authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(ResUtil.getString(R.string.app_name)).setNegativeButtonText(ResUtil.getString(R.string.dialog_negative)).build());
    }

    private static BiometricPrompt prompt(FragmentActivity activity) {
        return new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Notify.show(errString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                ((Callback) activity).onBiometricSuccess();
            }
        });
    }

    public interface Callback {

        void onBiometricSuccess();
    }
}
