package com.fongmi.chaquo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.internal.Common;
import com.github.catvod.Init;
import com.github.catvod.utils.Path;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class Platform extends Python.Platform {

    private static final String[] OBSOLETE_FILES = {"app.zip", "requirements.zip", "chaquopy.mp3", "stdlib.mp3", "chaquopy.zip", "lib-dynload", "stdlib.zip", "bootstrap.zip", "stdlib-common.zip", "ticket.txt"};
    private static final String[] OBSOLETE_CACHE = {"AssetFinder"};

    private final SharedPreferences preferences;
    private final JSONObject assetHashes;
    private final JSONObject buildJson;
    private final AssetManager assets;
    private final Context context;
    private final String abi;

    private Platform() {
        context = Init.context();
        preferences = context.getSharedPreferences(Common.ASSET_DIR, Context.MODE_PRIVATE);
        assets = context.getAssets();
        abi = getProcessAbi();
        try {
            try (InputStream input = assets.open(Common.ASSET_DIR + "/" + Common.ASSET_BUILD_JSON)) {
                buildJson = new JSONObject(Path.readOrThrow(input));
            }
            assetHashes = buildJson.getJSONObject("assets");
            loadNativeLibs(buildJson.getString("python_version"));
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to initialize Chaquopy", e);
        }
        setImporterAbi(abi);
    }

    public static Platform create() {
        return new Platform();
    }

    @NonNull
    public Context getApplication() {
        return context;
    }

    @Override
    @NonNull
    public String getPath() {
        String[] pathAssets = {Common.assetZip(Common.ASSET_STDLIB, Common.ABI_COMMON), Common.assetZip(Common.ASSET_BOOTSTRAP), Common.ASSET_BOOTSTRAP_NATIVE + "/" + abi};
        String[] extractionAssets = Arrays.copyOf(pathAssets, pathAssets.length + 1);
        extractionAssets[pathAssets.length] = Common.ASSET_CACERT;
        try {
            deleteObsolete(Path.files(), OBSOLETE_FILES);
            deleteObsolete(Path.cache(), OBSOLETE_CACHE);
            extractAssets(extractionAssets);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract Chaquopy assets for " + abi, e);
        }
        return getPythonPath(pathAssets);
    }

    @Override
    public void onStart(@NonNull Python py) {
        String[] appPath = {Common.ASSET_APP, Common.ASSET_REQUIREMENTS, Common.ASSET_STDLIB + "-" + abi};
        py.getModule("java.android").callAttr("initialize", context, buildJson, appPath);
    }

    private static String getProcessAbi() {
        String[] supportedAbis = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
        if (supportedAbis.length == 0) throw new IllegalStateException("No ABI reported for the current process");
        return supportedAbis[0];
    }

    @SuppressWarnings("deprecation")
    private static void setImporterAbi(String abi) {
        AndroidPlatform.ABI = abi;
    }

    private String getPythonPath(String[] pathAssets) {
        File assetDir = Path.files(Common.ASSET_DIR);
        StringBuilder path = new StringBuilder();
        for (String asset : pathAssets) {
            if (path.length() > 0) path.append(File.pathSeparatorChar);
            path.append(new File(assetDir, asset).getAbsolutePath());
        }
        return path.toString();
    }

    private void deleteObsolete(File baseDir, String[] filenames) {
        for (String filename : filenames) {
            filename = filename.replace("<abi>", abi);
            Path.clear(new File(baseDir, Common.ASSET_DIR + "/" + filename));
        }
    }

    private void extractAssets(String[] roots) throws IOException {
        SharedPreferences.Editor editor = preferences.edit();
        for (String root : roots) {
            boolean directory = !assetHashes.has(root);
            if (!extractAssetTree(editor, root)) throw new IOException("Missing packaged asset: " + root);
            if (directory) cleanExtractedDir(root);
        }
        editor.apply();
    }

    private boolean extractAssetTree(SharedPreferences.Editor editor, String path) throws IOException {
        if (assetHashes.has(path)) {
            extractAsset(editor, path);
            return true;
        }
        String[] children = assets.list(Common.ASSET_DIR + "/" + path);
        if (children == null || children.length == 0) return false;
        boolean extracted = false;
        for (String child : children) extracted |= extractAssetTree(editor, path + "/" + child);
        return extracted;
    }

    private void extractAsset(SharedPreferences.Editor editor, String path) throws IOException {
        String fullPath = Common.ASSET_DIR + "/" + path;
        File outFile = Path.files(fullPath);
        String preferenceKey = "asset." + path;
        String newHash = assetHashes.optString(path, "");
        if (outFile.exists() && preferences.getString(preferenceKey, "").equals(newHash)) return;
        Path.writeAtomically(outFile, assets.open(fullPath));
        editor.putString(preferenceKey, newHash);
    }

    private void cleanExtractedDir(String directory) {
        File outDir = Path.files(Common.ASSET_DIR + "/" + directory);
        File[] files = outDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String path = directory + "/" + file.getName();
            if (file.isDirectory()) cleanExtractedDir(path);
            else if (!assetHashes.has(path)) file.delete();
        }
    }

    private void loadNativeLibs(String pythonVersion) {
        for (String suffix : new String[]{"chaquopy", "python"}) {
            System.loadLibrary("crypto_" + suffix);
            System.loadLibrary("ssl_" + suffix);
            System.loadLibrary("sqlite3_" + suffix);
        }
        System.loadLibrary("python" + pythonVersion);
        System.loadLibrary("chaquopy_java");
    }
}
