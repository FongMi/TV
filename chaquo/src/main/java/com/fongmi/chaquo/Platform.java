package com.fongmi.chaquo;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;

import androidx.annotation.NonNull;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.internal.Common;
import com.github.catvod.Runtime;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Platform extends Python.Platform {

    private static final String[] OBSOLETE_FILES = {"app.zip", "requirements.zip", "chaquopy.mp3", "stdlib.mp3", "chaquopy.zip", "lib-dynload", "stdlib.zip", "bootstrap.zip", "stdlib-common.zip", "ticket.txt"};
    private static final String[] OBSOLETE_CACHE = {"AssetFinder"};

    private final SharedPreferences preferences;
    private final JSONObject assetHashes;
    private final JSONObject buildJson;
    private final AssetManager assets;
    private final Context context;
    private final Context importerContext;
    private final String abi;

    private Platform() {
        context = Runtime.context();
        preferences = context.getSharedPreferences(Common.ASSET_DIR, Context.MODE_PRIVATE);
        try {
            RuntimeModule.PYTHON.load(path -> System.load(path));
            File directory = RuntimeModule.PYTHON.getDirectory();
            abi = Process.is64Bit() ? "arm64-v8a" : "armeabi-v7a";
            buildJson = readBuild(new FileInputStream(new File(directory, "chaquopy/build.json")));
            if (!buildJson.getString("python_version").equals("3.10")) throw new IOException("Unsupported Python version");
            assetHashes = buildJson.getJSONObject("assets");
            assets = getRuntimeAssets(directory);
            importerContext = createImporterContext();
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to initialize Chaquopy", e);
        }
        setImporterAbi(abi);
    }

    private Context createImporterContext() {
        return new ContextWrapper(context) {
            @Override
            public ClassLoader getClassLoader() {
                return RuntimeModule.getSpiderClassLoader();
            }

            @Override
            public AssetManager getAssets() {
                return assets;
            }
        };
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
        py.getModule("java.android").callAttr("initialize", importerContext, buildJson, appPath);
    }

    private JSONObject readBuild(InputStream input) throws IOException, JSONException {
        try (input) {
            return new JSONObject(Path.readOrThrow(input));
        }
    }

    private AssetManager getRuntimeAssets(File directory) throws IOException, JSONException {
        File snapshot = new File(directory, "assets-" + assetHashes.getString("app.imy") + ".apk");
        if (!snapshot.isFile()) writeSnapshot(snapshot, directory);
        pruneSnapshots(directory, snapshot);
        return openSnapshot(snapshot);
    }

    private void pruneSnapshots(File directory, File snapshot) throws IOException {
        File[] snapshots = directory.listFiles();
        if (snapshots != null) for (File file : snapshots) if (file.isFile() && file.getName().matches("assets-[0-9a-f]{40}\\.apk") && !file.equals(snapshot) && !file.delete() && file.exists()) throw new IOException("Unable to remove old Python asset snapshot");
    }

    private AssetManager openSnapshot(File snapshot) throws IOException {
        try {
            if ((Os.stat(snapshot.getPath()).st_mode & 0222) != 0) throw new IOException("Python asset snapshot is writable");
            ApplicationInfo info = new ApplicationInfo(context.getApplicationInfo());
            info.sourceDir = info.publicSourceDir = snapshot.getPath();
            info.splitSourceDirs = info.splitPublicSourceDirs = null;
            return context.getPackageManager().getResourcesForApplication(info).getAssets();
        } catch (ErrnoException | PackageManager.NameNotFoundException e) {
            throw new IOException("Unable to open Python asset snapshot", e);
        }
    }

    private void writeSnapshot(File snapshot, File directory) throws IOException {
        File temporary = File.createTempFile("assets-", ".tmp", directory);
        try (FileOutputStream output = new FileOutputStream(temporary); ZipOutputStream zip = new ZipOutputStream(output)) {
            Os.chmod(temporary.getPath(), 0400);
            addDirectory(zip, new File(directory, "chaquopy"), "assets/chaquopy");
            addEntry(zip, "assets/chaquopy/build.json", buildJson.toString().getBytes(StandardCharsets.UTF_8));
            zip.finish();
            output.getFD().sync();
            Os.rename(temporary.getPath(), snapshot.getPath());
        } catch (ErrnoException e) {
            throw new IOException("Unable to prepare Python asset snapshot", e);
        } finally {
            if (!temporary.delete() && temporary.exists()) throw new IOException("Unable to remove Python asset staging file");
        }
    }

    private void addDirectory(ZipOutputStream zip, File directory, String path) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Missing Python assets: " + directory);
        Arrays.sort(files);
        for (File file : files) addAssetFile(zip, file, path + "/" + file.getName());
    }

    private void addAssetFile(ZipOutputStream zip, File file, String path) throws IOException {
        if (file.isDirectory()) {
            addDirectory(zip, file, path);
        } else if (!path.equals("assets/chaquopy/build.json")) {
            try (InputStream input = new FileInputStream(file)) {
                addEntry(zip, path, Path.readToByte(input));
            }
        }
    }

    private void addEntry(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        ZipEntry entry = new ZipEntry(path);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCrc(crc.getValue());
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
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
        for (String filename : filenames) Path.clear(new File(baseDir, Common.ASSET_DIR + "/" + filename));
    }

    private void extractAssets(String[] roots) throws IOException {
        SharedPreferences.Editor editor = preferences.edit();
        for (String root : roots) extractAssetRoot(editor, root);
        editor.apply();
    }

    private void extractAssetRoot(SharedPreferences.Editor editor, String root) throws IOException {
        boolean directory = !assetHashes.has(root);
        if (!extractAssetTree(editor, root)) throw new IOException("Missing packaged asset: " + root);
        if (directory) cleanExtractedDir(root);
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
        if (!outFile.exists() || !preferences.getString(preferenceKey, "").equals(newHash)) {
            Path.writeAtomically(outFile, assets.open(fullPath));
            editor.putString(preferenceKey, newHash);
        }
        if (path.endsWith(".so") && !outFile.setReadOnly()) throw new IOException("Unable to protect native library: " + path);
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

}
