package com.fongmi.nodejs;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.github.catvod.net.OkHttp;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Keep
public class NodeService extends Service {

    public static final String ACTION_START = "com.fongmi.nodejs.START";
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_CONFIG = "config";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_BRIDGE_PORT = "bridgePort";
    public static final String EXTRA_TOKEN = "token";
    private static final String HEADER_TOKEN = "X-CatVod-Token";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = OkHttp.client().newBuilder().proxy(Proxy.NO_PROXY).connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).writeTimeout(2, TimeUnit.SECONDS).build();

    private final AtomicBoolean started = new AtomicBoolean();
    private final IBinder binder = new Binder();

    private static native int startNodeWithArguments(String[] arguments);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (!ACTION_START.equals(intent.getAction()) || !started.compareAndSet(false, true)) return START_NOT_STICKY;
        String index = intent.getStringExtra(EXTRA_INDEX);
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String data = intent.getStringExtra(EXTRA_DATA);
        int bridgePort = intent.getIntExtra(EXTRA_BRIDGE_PORT, 0);
        String token = intent.getStringExtra(EXTRA_TOKEN);
        new Thread(() -> start(index, config, data, bridgePort, token), "nodejs-runtime").start();
        return START_NOT_STICKY;
    }

    private void start(String index, String config, String data, int bridgePort, String token) {
        String message;
        try {
            File indexFile = requirePrivateFile(index);
            File configFile = requirePrivateFile(config);
            File dataDir = requirePrivateDirectory(data);
            File bootstrap = extractBootstrap();
            System.loadLibrary("node");
            System.loadLibrary("nodebridge");
            int code = startNodeWithArguments(new String[]{"node", bootstrap.getAbsolutePath(), indexFile.getAbsolutePath(), configFile.getAbsolutePath(), dataDir.getAbsolutePath(), String.valueOf(bridgePort), token});
            message = "Node exited with code " + code;
        } catch (Throwable e) {
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        reportError(bridgePort, token, message);
        Process.killProcess(Process.myPid());
    }

    private File requirePrivateFile(String path) throws IOException {
        File file = requirePrivatePath(path);
        if (!file.isFile()) throw new IOException("Node bundle file is missing");
        return file;
    }

    private File requirePrivateDirectory(String path) throws IOException {
        File file = requirePrivatePath(path);
        if (!file.isDirectory() && !file.mkdirs() && !file.isDirectory()) throw new IOException("Unable to create Node data directory");
        return file;
    }

    private File requirePrivatePath(String path) throws IOException {
        if (path == null || path.isEmpty()) throw new IOException("Node path is empty");
        File root = new File(getFilesDir(), "nodejs").getCanonicalFile();
        File file = new File(path).getCanonicalFile();
        if (!file.getPath().startsWith(root.getPath() + File.separator)) throw new IOException("Node path is outside app storage");
        return file;
    }

    private File extractBootstrap() throws IOException {
        File directory = new File(getFilesDir(), "nodejs/runtime");
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) throw new IOException("Unable to create Node runtime directory");
        File target = new File(directory, "bootstrap-v1.js");
        File temporary = File.createTempFile("bootstrap-", ".tmp", directory);
        try {
            try (InputStream input = getAssets().open("nodejs/bootstrap.js"); FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[16 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.flush();
                output.getFD().sync();
            }
            try {
                Os.rename(temporary.getAbsolutePath(), target.getAbsolutePath());
            } catch (ErrnoException e) {
                throw new IOException("Unable to install Node bootstrap", e);
            }
            return target;
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    private void reportError(int port, String token, String message) {
        if (port <= 0) return;
        try {
            JSONObject body = new JSONObject().put("action", "nodeError").put("opt", new JSONObject().put("token", token).put("message", message));
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            Request request = new Request.Builder().url("http://127.0.0.1:" + port + "/msg").header(HEADER_TOKEN, token).post(RequestBody.create(bytes, JSON)).build();
            try (Response ignored = CLIENT.newCall(request).execute()) {
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }
}
