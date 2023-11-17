package tv.danmaku.ijk.media.player;

import android.util.Log;

import java.util.Arrays;

public abstract class IjkLibLoader {

    private static final String TAG = "IjkLibLoader";

    private final String[] nativeLibraries;
    private boolean loadAttempted;
    private boolean isAvailable;

    public IjkLibLoader(String... libraries) {
        nativeLibraries = libraries;
    }

    public synchronized boolean isAvailable() {
        if (loadAttempted) return isAvailable;
        loadAttempted = true;
        try {
            for (String lib : nativeLibraries) {
                loadLibrary(lib);
            }
            isAvailable = true;
        } catch (Throwable exception) {
            // Log a warning as an attempt to check for the library indicates that the app depends on an
            // extension and generally would expect its native libraries to be available.
            Log.w(TAG, "Failed to load " + Arrays.toString(nativeLibraries));
        }
        return isAvailable;
    }

    protected abstract void loadLibrary(String name);
}
