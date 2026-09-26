package com.fongmi.android.tv.api.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.Process;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.fongmi.chaquo.Loader;
import com.github.catvod.Init;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class PythonPlatformTest {

    @SuppressWarnings("deprecation")
    @Test
    public void startsWithCurrentProcessAbiAndNativeModules() {
        new Loader();

        String[] supportedAbis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
        String abi = supportedAbis[0];
        assertEquals(abi, AndroidPlatform.ABI);

        File bridge = new File(Init.context().getFilesDir(), "chaquopy/bootstrap-native/" + abi + "/java/chaquopy.so");
        assertTrue(bridge.getAbsolutePath(), bridge.isFile());

        Python python = Python.getInstance();
        assertNotNull(python.getModule("ujson"));
        assertNotNull(python.getModule("Crypto.Cipher.AES"));
    }
}
