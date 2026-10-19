package com.fongmi.android.tv.model;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.media3.exoplayer.iso.BdjRuntime;
import androidx.media3.exoplayer.offline.Downloader;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.utils.ApkCache;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.RuntimeInstaller;
import com.github.catvod.net.Download;
import com.github.catvod.runtime.PythonRuntime;
import com.github.catvod.runtime.QuickJsRuntime;
import com.github.catvod.runtime.ThunderRuntime;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ModuleViewModel extends AndroidViewModel {

    public enum Phase {IDLE, CHECKING, TESTING, INSTALLING, REMOVING, CANCELLING, READY, REMOVAL_READY, CANCELLED, FAILED}

    public enum HealthPhase {UNKNOWN, CHECKING, PASSED, FAILED}

    public record Health(HealthPhase phase, String error) {
    }

    public record Status(String abi, String installedVersion, String availableVersion, boolean updateAvailable, boolean restartRequired, boolean removable) {
    }

    public record State(Phase phase, Status runtime, long downloaded, long total, String error) {
        public boolean isBusy() {
            return phase == Phase.CHECKING || phase == Phase.TESTING || phase == Phase.INSTALLING || phase == Phase.REMOVING || phase == Phase.CANCELLING;
        }

        public boolean isPending() {
            return phase == Phase.READY || phase == Phase.REMOVAL_READY;
        }

        public boolean canInstall() {
            return runtime != null && runtime.abi() != null && runtime.availableVersion() != null && !runtime.restartRequired() && (runtime.installedVersion() == null || runtime.updateAvailable()) && !isBusy() && !isPending();
        }

        public boolean canRemove() {
            return runtime != null && runtime.removable() && !isBusy() && !isPending();
        }

        public boolean canTest() {
            return runtime != null && runtime.abi() != null && runtime.installedVersion() != null && !runtime.restartRequired() && !isBusy() && !isPending();
        }

        public boolean canRestart() {
            return runtime != null && runtime.restartRequired() && !isBusy() && !isPending();
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Phase.IDLE, null, -1, -1, null));
    private final MutableLiveData<Health> health = new MutableLiveData<>(new Health(HealthPhase.UNKNOWN, null));
    private BroadcastReceiver receiver;
    private Intent installResult;
    private Runnable pendingCancellation;
    private CancellationSignal cancellation;
    private boolean packageChanged;
    private boolean cleared;
    private final RuntimeModule module;

    public static ModuleViewModel get(ViewModelStoreOwner owner, RuntimeModule module) {
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> type) {
                return (T) new ModuleViewModel(App.get(), module);
            }
        };
        return new ViewModelProvider(owner, factory).get(ModuleViewModel.class.getName() + ":" + module.name(), ModuleViewModel.class);
    }

    public ModuleViewModel(@NonNull Application application, RuntimeModule module) {
        super(application);
        this.module = module;
        registerReceiver(application);
        check();
    }

    private void registerReceiver(Application application) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(RuntimeInstaller.RESULT);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onRuntimeEvent(intent);
            }
        };
        ContextCompat.registerReceiver(application, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void onRuntimeEvent(Intent intent) {
        if (cleared || intent.getData() == null || !module.getPackageName().equals(intent.getData().getSchemeSpecificPart())) return;
        if (RuntimeInstaller.RESULT.equals(intent.getAction())) {
            installResult = intent;
            if (pendingCancellation != null) pendingCancellation.run();
            else if (!state.getValue().isBusy()) finishInstallation();
            return;
        }
        if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction()) && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
        packageChanged = true;
        health.setValue(new Health(HealthPhase.UNKNOWN, null));
        check();
    }

    public String getTitle() {
        if (module == RuntimeModule.THUNDER) return getApplication().getString(R.string.module_thunder);
        return module.title;
    }

    public int getIcon() {
        return switch (module) {
            case BDJ -> R.drawable.ic_module_bdj;
            case PYTHON -> R.drawable.ic_module_python;
            case NODEJS -> R.drawable.ic_module_nodejs;
            case QUICKJS -> R.drawable.ic_module_quickjs;
            case THUNDER -> R.drawable.ic_module_thunder;
            case MPV -> R.drawable.ic_module_mpv;
        };
    }

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<Health> getHealth() {
        return health;
    }

    public int getHealthText(Health health) {
        return switch (health.phase()) {
            case UNKNOWN -> R.string.module_untested;
            case CHECKING -> R.string.module_testing;
            case PASSED -> module == RuntimeModule.BDJ || module == RuntimeModule.MPV ? R.string.module_environment_ready : R.string.module_test_passed;
            case FAILED -> R.string.module_test_failed;
        };
    }

    public void checkRuntime() {
        State current = state.getValue();
        if (cleared || !current.canTest()) return;
        CancellationSignal signal = cancellation = new CancellationSignal();
        state.setValue(new State(Phase.TESTING, current.runtime(), -1, -1, null));
        health.setValue(new Health(HealthPhase.CHECKING, null));
        Task.execute(() -> testRuntime(current.runtime(), signal));
    }

    private void testRuntime(Status runtime, CancellationSignal signal) {
        Health result;
        Status installed = runtime;
        try {
            long version = runRuntimeCheck(signal);
            installed = getInstalledStatus();
            installed = getTestedStatus(runtime, installed, version);
            result = new Health(HealthPhase.PASSED, null);
        } catch (Exception | LinkageError e) {
            result = signal.isCanceled() ? new Health(HealthPhase.UNKNOWN, null) : new Health(HealthPhase.FAILED, e.getMessage() == null ? e.toString() : e.getMessage());
            if (module == RuntimeModule.MPV) {
                try {
                    installed = mergeInstalledStatus(getInstalledStatus(), runtime);
                } catch (IOException | RuntimeException statusError) {
                    if (!signal.isCanceled()) result = new Health(HealthPhase.FAILED, result.error() + "\n" + (statusError.getMessage() == null ? statusError.toString() : statusError.getMessage()));
                }
            }
        }
        Health checked = result;
        Status status = installed;
        App.post(() -> {
            Health completed = packageChanged || signal.isCanceled() || state.getValue().phase() == Phase.CANCELLING ? new Health(HealthPhase.UNKNOWN, null) : checked;
            finishWork(new State(Phase.IDLE, status, -1, -1, null), completed, false);
        });
    }

    private Status getTestedStatus(Status runtime, Status installed, long version) throws IOException {
        if (installed.restartRequired()) throw new IOException(getApplication().getString(R.string.module_restart));
        if (version != module.getVersionCode() || !Objects.equals(runtime.installedVersion(), installed.installedVersion())) throw new IOException(getApplication().getString(R.string.module_test_changed));
        return new Status(installed.abi(), installed.installedVersion(), runtime.availableVersion(), runtime.updateAvailable(), installed.restartRequired(), installed.removable());
    }

    private long runRuntimeCheck(CancellationSignal signal) throws Exception {
        signal.throwIfCanceled();
        long version = module.getVersionCode();
        switch (module) {
            case BDJ -> BdjRuntime.prepare(getApplication(), signal);
            case PYTHON -> module.entry(PythonRuntime.class).check();
            case NODEJS -> checkNodeRuntime(signal);
            case QUICKJS -> module.entry(QuickJsRuntime.class).check();
            case THUNDER -> module.entry(ThunderRuntime.class).check();
            case MPV -> MpvUtil.loadRuntime();
        }
        signal.throwIfCanceled();
        return version;
    }

    private void checkNodeRuntime(CancellationSignal signal) throws Exception {
        Thread worker = Thread.currentThread();
        signal.setOnCancelListener(worker::interrupt);
        try {
            signal.throwIfCanceled();
            NodeRuntime.get().client().check();
        } finally {
            signal.setOnCancelListener(null);
        }
    }

    public void check() {
        State current = state.getValue();
        if (current.isBusy() || cleared) return;
        boolean changed = packageChanged;
        packageChanged = false;
        state.setValue(new State(Phase.CHECKING, current.runtime(), -1, -1, null));
        CancellationSignal signal = cancellation = new CancellationSignal();
        Task.execute(() -> checkStatus(current, changed, signal));
    }

    private void checkStatus(State current, boolean changed, CancellationSignal signal) {
        State result;
        Status runtime = current.runtime();
        try {
            signal.throwIfCanceled();
            boolean pending = !changed && RuntimeInstaller.hasPendingSession(module);
            if (!pending) {
                runtime = getInstalledStatus();
                runtime = getStatus(runtime, signal);
            }
            Phase phase = pending ? current.phase() == Phase.REMOVAL_READY ? Phase.REMOVAL_READY : Phase.READY : Phase.IDLE;
            signal.throwIfCanceled();
            result = new State(phase, runtime, -1, -1, null);
        } catch (IOException | RuntimeException e) {
            Phase phase = signal.isCanceled() ? Phase.CANCELLED : Phase.FAILED;
            String error = phase == Phase.FAILED ? e.getMessage() == null ? e.toString() : e.getMessage() : null;
            result = new State(phase, runtime, -1, -1, error);
        }
        postResult(result, false);
    }

    public void refresh() {
        State current = state.getValue();
        if (cleared || current.isBusy()) return;
        if (current.runtime() == null || current.isPending() || packageChanged) {
            check();
            return;
        }
        try {
            Status runtime = current.runtime();
            Status installed = getInstalledStatus();
            if (Objects.equals(runtime.abi(), installed.abi()) && Objects.equals(runtime.installedVersion(), installed.installedVersion()) && runtime.restartRequired() == installed.restartRequired() && runtime.removable() == installed.removable()) return;
            health.setValue(new Health(HealthPhase.UNKNOWN, null));
            check();
        } catch (IOException | RuntimeException e) {
            health.setValue(new Health(HealthPhase.UNKNOWN, null));
            state.setValue(new State(Phase.FAILED, current.runtime(), -1, -1, e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    private Status getInstalledStatus() throws IOException {
        RuntimeModule.refresh();
        String installed = module.getVersionName();
        boolean restart = module == RuntimeModule.BDJ ? BdjRuntime.getStatus(getApplication()).restartRequired : module.isRestartRequired();
        boolean removable = installed != null;
        return new Status(RuntimeInstaller.getAbi(), installed, installed, false, restart, removable);
    }

    private Status mergeInstalledStatus(Status installed, Status cached) {
        return new Status(installed.abi(), installed.installedVersion(), cached.availableVersion(), cached.updateAvailable() && Objects.equals(cached.installedVersion(), installed.installedVersion()), installed.restartRequired(), installed.removable());
    }

    private Status getStatus(Status runtime, CancellationSignal signal) throws IOException {
        String installed = runtime.installedVersion();
        long code = module.getVersionCode();
        JSONObject asset = RuntimeInstaller.getAsset(module, signal);
        signal.throwIfCanceled();
        boolean newer = asset != null && asset.optLong("versionCode") > code;
        String available = newer ? asset.optString("versionName") : installed;
        return new Status(runtime.abi(), installed, available, installed != null && newer, runtime.restartRequired(), runtime.removable());
    }

    public int getStatusText(State state) {
        Status runtime = state.runtime();
        return switch (state.phase()) {
            case CHECKING -> R.string.module_checking;
            case TESTING -> R.string.module_testing;
            case INSTALLING -> state.downloaded() < 0 ? R.string.module_waiting : state.downloaded() < state.total() ? R.string.module_downloading : R.string.module_installing;
            case REMOVING -> R.string.module_removing;
            case CANCELLING -> R.string.module_cancelling;
            case READY -> R.string.module_confirm_install;
            case REMOVAL_READY -> R.string.module_confirm_remove;
            case CANCELLED -> R.string.module_cancelled;
            case FAILED -> runtime != null && (runtime.installedVersion() != null || runtime.restartRequired()) ? getRuntimeText(runtime) : R.string.module_failed_title;
            case IDLE -> getRuntimeText(runtime);
        };
    }

    public int getRuntimeText(Status runtime) {
        if (runtime == null) return R.string.module_checking;
        if (runtime.abi() == null) return R.string.module_unsupported;
        if (runtime.restartRequired()) return R.string.module_restart;
        if (runtime.installedVersion() == null) return runtime.availableVersion() == null ? R.string.module_unavailable : R.string.module_not_installed;
        return runtime.updateAvailable() ? R.string.module_available : R.string.module_current;
    }

    public void install() {
        changeRuntime(false);
    }

    public void remove() {
        changeRuntime(true);
    }

    private void changeRuntime(boolean remove) {
        State current = state.getValue();
        if (cleared || (remove ? !current.canRemove() : !current.canInstall())) return;
        health.setValue(new Health(HealthPhase.UNKNOWN, null));
        CancellationSignal signal = cancellation = new CancellationSignal();
        state.setValue(new State(remove ? Phase.REMOVING : Phase.INSTALLING, current.runtime(), -1, -1, null));
        Task.submitLarge(() -> changeRuntime(signal, current.runtime(), remove));
    }

    private void changeRuntime(CancellationSignal signal, Status runtime, boolean remove) {
        Phase phase = remove ? Phase.REMOVAL_READY : Phase.READY;
        String error = null;
        try {
            if (remove) RuntimeInstaller.remove(module, signal);
            else installRuntime(signal);
        } catch (IOException | OperationCanceledException | IllegalArgumentException | SecurityException e) {
            phase = signal.isCanceled() ? Phase.CANCELLED : Phase.FAILED;
            if (phase == Phase.FAILED) error = e.getMessage() == null ? e.toString() : e.getMessage();
        }
        postResult(new State(phase, runtime, -1, -1, error), true);
    }

    private void installRuntime(CancellationSignal signal) throws IOException {
        File directory = ApkCache.modules();
        try (Closeable retained = Download.retain(directory)) {
            File file = RuntimeInstaller.download(module, directory, signal, getProgressListener(signal));
            RuntimeInstaller.install(module, file, signal);
        }
    }

    private void finishInstallation() {
        Intent result = installResult;
        installResult = null;
        int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        Phase phase = status == PackageInstaller.STATUS_SUCCESS ? Phase.IDLE : status == PackageInstaller.STATUS_FAILURE_ABORTED ? Phase.CANCELLED : Phase.FAILED;
        finishInstallation(phase, phase == Phase.FAILED ? result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) : null);
    }

    private void finishInstallation(Phase phase, String error) {
        State current = state.getValue();
        boolean success = phase == Phase.IDLE;
        boolean changed = success || packageChanged;
        packageChanged = false;
        try {
            Status installed = getInstalledStatus();
            Status cached = current.runtime();
            Status runtime = success || cached == null ? installed : mergeInstalledStatus(installed, cached);
            if (changed || !Objects.equals(cached, runtime)) health.setValue(new Health(HealthPhase.UNKNOWN, null));
            state.setValue(new State(phase, runtime, -1, -1, error));
            if (success) Notify.show(runtime.restartRequired() ? R.string.module_restart : R.string.module_current);
        } catch (IOException | RuntimeException e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
            health.setValue(new Health(HealthPhase.UNKNOWN, null));
            state.setValue(new State(Phase.FAILED, current.runtime(), -1, -1, error));
            Notify.show(error);
        }
    }

    private void postResult(State result, boolean notify) {
        App.post(() -> finishWork(result, null, notify));
    }

    private void finishWork(State result, Health checked, boolean notify) {
        cancellation = null;
        if (cleared) return;
        if (checked != null) health.setValue(checked);
        state.setValue(result);
        if (notify && result.error() != null) Notify.show(result.error());
        if (installResult != null) finishInstallation();
        else if (packageChanged) check();
    }

    private Downloader.ProgressListener getProgressListener(CancellationSignal signal) {
        return new Downloader.ProgressListener() {
            private int percent = -1;
            private long updated;

            @Override
            public void onProgress(long total, long downloaded, float percentDownloaded) {
                int progress = (int) (100L * downloaded / total);
                long now = System.nanoTime();
                if (percent == progress && now - updated < TimeUnit.MILLISECONDS.toNanos(500)) return;
                percent = progress;
                updated = now;
                App.post(() -> updateProgress(signal, downloaded, total));
            }
        };
    }

    private void updateProgress(CancellationSignal signal, long downloaded, long total) {
        if (cleared || cancellation != signal || state.getValue().phase() != Phase.INSTALLING) return;
        state.setValue(new State(Phase.INSTALLING, state.getValue().runtime(), downloaded, total, null));
    }

    public void cancel() {
        State current = state.getValue();
        if (cleared || current.phase() == Phase.CANCELLING || current.phase() == Phase.REMOVING) return;
        if (current.phase() == Phase.READY) {
            state.setValue(new State(Phase.CANCELLING, current.runtime(), -1, -1, null));
            pendingCancellation = RuntimeInstaller.cancelInstallation(module, this::onInstallationCancelled);
            return;
        }
        if (cancellation == null) return;
        state.setValue(new State(Phase.CANCELLING, current.runtime(), current.downloaded(), current.total(), null));
        Task.execute(cancellation::cancel);
    }

    private void onInstallationCancelled(boolean success, String error) {
        pendingCancellation = null;
        if (cleared) return;
        if (installResult != null) finishInstallation();
        else finishInstallation(success ? Phase.IDLE : error == null ? Phase.CANCELLED : Phase.FAILED, error);
        if (error != null) Notify.show(error);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        if (pendingCancellation != null) pendingCancellation.run();
        if (receiver != null) getApplication().unregisterReceiver(receiver);
        if (cancellation != null) Task.execute(cancellation::cancel);
    }
}
