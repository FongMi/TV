package com.fongmi.chaquo;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;

import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.internal.Common;
import com.github.catvod.Init;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class Platform extends AndroidPlatform {

    @SuppressWarnings("deprecation")
    private Platform() {
        super(Init.context());
        AndroidPlatform.ABI = findProcessAbi(getApplication().getAssets());
    }

    public static Platform create() {
        return new Platform();
    }

    @NonNull
    @Override
    public Application getApplication() {
        return super.getApplication();
    }

    private static String findProcessAbi(AssetManager assets) {
        String[] supportedAbis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
        for (String abi : supportedAbis) {
            try (InputStream ignored = assets.open(Common.ASSET_DIR + "/" + Common.assetZip(Common.ASSET_STDLIB, abi))) {
                return abi;
            } catch (IOException ignored) {
            }
        }
        throw new RuntimeException("No supported ABI found in: " + Arrays.toString(supportedAbis));
    }
}
