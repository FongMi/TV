package com.fongmi.android.tv;

import android.os.CancellationSignal;
import android.os.Process;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.fongmi.android.tv.service.UpdateService;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.dialog.UpdateDialog;
import com.fongmi.android.tv.utils.ApkCache;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Github;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.RuntimeInstaller;
import com.fongmi.android.tv.utils.UpdatePackage;
import com.github.catvod.net.Download;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Updater {

    private WeakReference<FragmentActivity> activity = new WeakReference<>(null);
    private WeakReference<UpdateDialog> dialog = new WeakReference<>(null);
    private UpdatePackage update;
    private String desc;
    private File ready;
    private boolean checking;
    private boolean downloading;
    private boolean paused;
    private boolean modulesChecked;
    private Map<RuntimeModule, Long> moduleUpdates;
    private int progress;

    private static class Loader {
        static final Updater INSTANCE = new Updater();
    }

    private Updater() {
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    public void start(FragmentActivity activity, boolean force) {
        if (!force && !modulesChecked) {
            modulesChecked = true;
            Task.execute(this::checkModules);
        }
        if (force) {
            Notify.show(R.string.update_check);
            Setting.putUpdate(true);
        }
        if (!Setting.getUpdate()) return;
        if (downloading || paused || ready != null) {
            if (force) show(activity);
            return;
        }
        this.activity = new WeakReference<>(activity);
        if (checking || dialog.get() != null || activity.getSupportFragmentManager().findFragmentByTag(UpdateDialog.TAG) != null) return;
        checking = true;
        Task.execute(this::checkRelease);
    }

    private void checkModules() {
        try {
            List<RuntimeModule> installed = getInstalledModules();
            if (installed.isEmpty()) return;
            JSONObject catalog = RuntimeInstaller.getCatalog(new CancellationSignal());
            if (catalog == null) return;
            Map<RuntimeModule, Long> updates = getModuleUpdates(installed, catalog);
            if (updates.isEmpty()) return;
            App.post(() -> onModulesChecked(updates));
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private List<RuntimeModule> getInstalledModules() {
        RuntimeModule.refresh();
        return Arrays.stream(RuntimeModule.values()).filter(RuntimeModule::isInstalled).toList();
    }

    private Map<RuntimeModule, Long> getModuleUpdates(List<RuntimeModule> installed, JSONObject catalog) throws IOException {
        Map<RuntimeModule, Long> updates = new EnumMap<>(RuntimeModule.class);
        for (RuntimeModule module : installed) addModuleUpdate(module, catalog, updates);
        return updates;
    }

    private void addModuleUpdate(RuntimeModule module, JSONObject catalog, Map<RuntimeModule, Long> updates) throws IOException {
        JSONObject asset = RuntimeInstaller.getExternalAsset(module, catalog);
        if (asset == null) return;
        long code = module.getVersionCode();
        long version = asset.optLong("versionCode");
        if (code > 0 && version > code && version > Prefers.getLong("module_update_" + module.id)) updates.put(module, version);
    }

    private void onModulesChecked(Map<RuntimeModule, Long> updates) {
        moduleUpdates = updates;
        notifyModules();
    }

    public void notifyModules() {
        if (moduleUpdates == null || App.activity() == null) return;
        List<String> titles = moduleUpdates.keySet().stream().map(this::getModuleTitle).toList();
        Notify.show(ResUtil.getString(R.string.module_update_notice, String.join(", ", titles)));
        moduleUpdates.forEach((module, version) -> Prefers.put("module_update_" + module.id, version));
        moduleUpdates = null;
    }

    private String getModuleTitle(RuntimeModule module) {
        return module == RuntimeModule.THUNDER ? ResUtil.getString(R.string.module_thunder) : module.title;
    }

    private void checkRelease() {
        try {
            String abi = Process.is64Bit() ? "arm64_v8a" : "armeabi_v7a";
            String filename = BuildConfig.FLAVOR + "-" + abi + ".apk";
            JSONObject release = Github.getUpdate(new JSONArray(OkHttp.string(Github.getReleases())), BuildConfig.VERSION_NAME, filename);
            if (release == null) {
                App.post(() -> checking = false);
                return;
            }
            prepareRelease(release, filename);
        } catch (Exception e) {
            e.printStackTrace();
            App.post(() -> checking = false);
        }
    }

    private void prepareRelease(JSONObject release, String filename) throws IOException {
        String tag = release.optString("tag_name");
        String name = tag.startsWith("v") ? tag.substring(1) : tag;
        String desc = Github.getNotes(release.optString("body"), BuildConfig.FLAVOR);
        JSONObject asset = Github.getAsset(release, filename);
        UpdatePackage update = new UpdatePackage(name, asset.optString("browser_download_url"), asset.optLong("size"), asset.isNull("digest") ? "" : asset.optString("digest"));
        File cached = verifyCachedApk(update);
        App.post(() -> onReleaseChecked(update, desc, cached));
    }

    private void onReleaseChecked(UpdatePackage update, String desc, File cached) {
        checking = false;
        this.update = update;
        this.desc = desc;
        ready = cached;
        show(activity.get());
    }

    private File verifyCachedApk(UpdatePackage update) throws IOException {
        File file = update.file(ApkCache.app());
        try (Closeable retained = Download.retain(file)) {
            if (!file.isFile()) return null;
            try {
                update.verifyApk(App.get(), file);
                return file;
            } catch (IOException e) {
                Files.deleteIfExists(file.toPath());
                return null;
            }
        }
    }

    private void show(FragmentActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || activity.getSupportFragmentManager().isStateSaved() || dialog.get() != null) return;
        UpdateDialog restored = (UpdateDialog) activity.getSupportFragmentManager().findFragmentByTag(UpdateDialog.TAG);
        dialog = new WeakReference<>(restored != null ? restored : UpdateDialog.create().title(ResUtil.getString(R.string.update_version, update.version())).desc(desc).show(activity));
    }

    public void attach(UpdateDialog dialog) {
        if (update == null) {
            dialog.dismissAllowingStateLoss();
            return;
        }
        this.dialog = new WeakReference<>(dialog);
        dialog.setState(downloading, progress, ready != null);
    }

    public void detach(UpdateDialog dialog) {
        if (this.dialog.get() == dialog) this.dialog.clear();
    }

    public void onConfirm(View view) {
        if (downloading) {
            onPause(view);
            return;
        }
        if (update == null) return;
        if (ready != null && !ready.isFile()) ready = null;
        if (ready != null) {
            install(ready);
            return;
        }
        downloading = true;
        paused = false;
        progress(0);
        try {
            UpdateService.start(update);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public void onCancel(View view) {
        if (!downloading && !paused) Setting.putUpdate(false);
        dismiss();
    }

    public void onPause(View view) {
        if (downloading && progress >= 0 && progress < 100) UpdateService.pause();
    }

    public boolean isPaused() {
        return paused;
    }

    public static String transferText(long total, long downloaded, long speed) {
        if (speed <= 0 || total <= downloaded) return "";
        long remaining = total - downloaded;
        return ResUtil.getString(R.string.transfer_detail, Formatter.formatFileSize(App.get(), speed), DateUtils.formatElapsedTime(remaining / speed + (remaining % speed == 0 ? 0 : 1)));
    }

    public void paused() {
        downloading = false;
        paused = true;
        progress(progress);
    }

    private void dismiss() {
        UpdateDialog current = dialog.get();
        dialog.clear();
        if (current != null) current.dismissAllowingStateLoss();
    }

    public void progress(int progress) {
        this.progress = progress;
        UpdateDialog current = dialog.get();
        if (current != null) current.setState(downloading, progress, ready != null);
    }

    public void error(String msg) {
        downloading = false;
        paused = false;
        Notify.show(msg);
        dismiss();
    }

    public void success(File file) {
        downloading = false;
        paused = false;
        ready = file;
        UpdateDialog current = dialog.get();
        if (current != null && current.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) install(file);
    }

    private void install(File file) {
        try {
            Path.requireSpace(file, file.length());
            FileUtil.openFile(file);
            dismiss();
        } catch (Exception e) {
            error(e.getMessage());
        }
    }
}
