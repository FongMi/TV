package com.fongmi.nodejs;

import android.content.Context;
import android.net.Uri;

import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class NodeBundle {

    private static final Pattern MD5 = Pattern.compile("(?i)(?:^|\\s)([a-f0-9]{32})(?:\\s|$)");
    private static final long MAX_INDEX_BYTES = 32L * 1024 * 1024;
    private static final long MAX_CONFIG_BYTES = 2L * 1024 * 1024;
    private static final int MAX_MD5_BYTES = 1024;
    private static final int MAX_MANIFEST_BYTES = 32 * 1024;
    private static final long REJECT_TTL_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private static final String PENDING = ".pending";
    private static final String REJECTED = "rejected";
    private static final String TRUSTED_KEY = "trusted-key.sha256";
    private static final OkHttpClient CLIENT = OkHttp.client().newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).followSslRedirects(false).build();
    private static final Map<String, Object> SOURCE_LOCKS = new ConcurrentHashMap<>();
    private static final AtomicReference<LoadSession> ACTIVE_LOAD = new AtomicReference<>();

    private final File root;
    private final File index;
    private final File config;
    private final String indexMd5;
    private final String configMd5;
    private final String signingKey;
    private boolean pending;

    NodeBundle(File root, String indexMd5, String configMd5) {
        this(root, indexMd5, configMd5, false, "");
    }

    private NodeBundle(File root, String indexMd5, String configMd5, boolean pending, String signingKey) {
        this.root = root;
        this.index = new File(root, "index.js");
        this.config = new File(root, "index.config.js");
        this.indexMd5 = indexMd5;
        this.configMd5 = configMd5;
        this.signingKey = signingKey;
        this.pending = pending;
    }

    public static boolean isConfig(String url) {
        if (url == null) return false;
        if (url.regionMatches(true, 0, "content://", 0, 10)) return false;
        String value = withoutSuffix(url);
        try {
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
        }
        return value.toLowerCase(Locale.ROOT).endsWith("/index.js.md5") || value.equalsIgnoreCase("index.js.md5");
    }

    static NodeBundle prepare(Context context, String source) throws Exception {
        LoadSession session = new LoadSession();
        LoadSession previous = ACTIVE_LOAD.getAndSet(session);
        if (previous != null) previous.cancel();
        try {
            if (!isConfig(source)) throw new IOException("Node config must end with index.js.md5");
            validateSource(source);
            String key = sha256(source);
            synchronized (SOURCE_LOCKS.computeIfAbsent(key, ignored -> new Object())) {
                session.check();
                return prepare(context, source, key, session);
            }
        } finally {
            ACTIVE_LOAD.compareAndSet(session, null);
        }
    }

    private static NodeBundle prepare(Context context, String source, String key, LoadSession session) throws Exception {
        File sourceRoot = new File(context.getFilesDir(), "nodejs/bundles/" + key);
        File active = new File(sourceRoot, "active");
        restorePrevious(active);
        pruneSourceRoot(sourceRoot);
        String indexLocation = companion(source, "index.js");
        String configLocation = companion(source, "index.config.js");
        String configMd5Location = companion(source, "index.config.js.md5");
        String manifestLocation = companion(source, NodeManifest.FILE_NAME);
        try {
            String indexMd5 = fetchMd5(source, session);
            String configMd5 = fetchMd5(configMd5Location, session);
            String manifestText = fetchOptional(manifestLocation, MAX_MANIFEST_BYTES, session);
            String trustedKey = readTrustedKey(sourceRoot);
            if (manifestText == null && !trustedKey.isEmpty()) throw new IOException("Signed Node bundle cannot downgrade to unsigned");
            NodeManifest manifest = manifestText == null ? null : NodeManifest.parse(manifestText);
            NodeBundle stored = readStored(active);
            if (stored != null && stored.matches(indexMd5, configMd5) && verifyStoredManifest(stored, manifest, trustedKey)) {
                session.check();
                return stored;
            }
            if (stored != null && isRejected(sourceRoot, indexMd5, configMd5, System.currentTimeMillis())) {
                session.check();
                SpiderDebug.log("NodeBundle", "Skipped previously rejected Node bundle version");
                return stored;
            }
            File staging = new File(sourceRoot, "staging-" + UUID.randomUUID());
            try {
                if (!staging.mkdirs() && !staging.isDirectory()) throw new IOException("Unable to create Node staging directory");
                File index = copy(indexLocation, new File(staging, "index.js"), MAX_INDEX_BYTES, session);
                File config = copy(configLocation, new File(staging, "index.config.js"), MAX_CONFIG_BYTES, session);
                if (!Crypto.equals(index, indexMd5)) throw new IOException("index.js MD5 mismatch");
                if (!Crypto.equals(config, configMd5)) throw new IOException("index.config.js MD5 mismatch");
                String signingKey = "";
                if (manifest != null) {
                    manifest.verify(index, config, trustedKey);
                    signingKey = manifest.fingerprint();
                    writeAtomically(manifest.raw().getBytes(StandardCharsets.UTF_8), new File(staging, NodeManifest.FILE_NAME));
                }
                writeAtomically((indexMd5 + "\n").getBytes(StandardCharsets.US_ASCII), new File(staging, "index.js.md5"));
                writeAtomically((configMd5 + "\n").getBytes(StandardCharsets.US_ASCII), new File(staging, "index.config.js.md5"));
                writeAtomically(version(indexMd5, configMd5).getBytes(StandardCharsets.US_ASCII), new File(staging, PENDING));
                session.check();
                commit(active, staging);
                session.check();
                return new NodeBundle(active, indexMd5, configMd5, true, signingKey);
            } finally {
                Path.clear(staging);
            }
        } catch (Exception e) {
            session.check(e);
            NodeBundle stored = readStored(active);
            if (stored == null) throw e;
            SpiderDebug.log("NodeBundle", "Update failed, using last known good bundle: %s", e.getMessage());
            return stored;
        }
    }

    static void cancelLoad() {
        LoadSession session = ACTIVE_LOAD.getAndSet(null);
        if (session != null) session.cancel();
    }

    static String companion(String source, String name) throws IOException {
        int end = source.length();
        int query = source.indexOf('?');
        int fragment = source.indexOf('#');
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        String path = source.substring(0, end);
        int slash = path.lastIndexOf('/');
        if (slash < 0) throw new IOException("Node config URL has no parent path");
        return path.substring(0, slash + 1) + name + source.substring(end);
    }

    private static String fetchMd5(String location, LoadSession session) throws IOException {
        try (Resource resource = open(location, session)) {
            return parseMd5(readLimited(resource.input, MAX_MD5_BYTES, session));
        }
    }

    private static String fetchOptional(String location, int limit, LoadSession session) throws IOException {
        Resource resource = openOptional(location, session);
        if (resource == null) return null;
        try (resource) {
            return readLimited(resource.input, limit, session);
        }
    }

    private static String readMd5(File file) {
        try {
            return parseMd5(Path.read(file));
        } catch (Exception e) {
            return "";
        }
    }

    static String parseMd5(String text) throws IOException {
        Matcher matcher = MD5.matcher(text == null ? "" : text.trim());
        if (!matcher.find()) throw new IOException("Invalid MD5 sidecar");
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }

    private static String readLimited(InputStream input, int limit, LoadSession session) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            int total = 0;
            int count;
            while ((count = stream.read(buffer)) != -1) {
                session.check();
                total += count;
                if (total > limit) throw new IOException("MD5 sidecar is too large");
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static File copy(String location, File target, long maxBytes, LoadSession session) throws IOException {
        try (Resource resource = open(location, session)) {
            if (resource.length > maxBytes) throw new IOException("Node bundle file is too large");
            File parent = target.getParentFile();
            if (parent == null || (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())) throw new IOException("Unable to create Node staging directory");
            try (FileOutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                long total = 0;
                int count;
                while ((count = resource.input.read(buffer)) != -1) {
                    session.check();
                    total += count;
                    if (total > maxBytes) throw new IOException("Node bundle file is too large");
                    output.write(buffer, 0, count);
                }
                if (total == 0) throw new IOException("Empty Node bundle file");
                output.flush();
                output.getFD().sync();
            } catch (IOException e) {
                Path.clear(target);
                throw e;
            }
            return target;
        }
    }

    private static Resource open(String location, LoadSession session) throws IOException {
        session.check();
        if (isRemote(location)) {
            Call call = CLIENT.newCall(new Request.Builder().url(location).build());
            session.attach(call);
            Response response = null;
            try {
                response = call.execute();
                session.check();
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    int code = response.code();
                    String url = response.request().url().redact();
                    throw new IOException("HTTP " + code + " for " + url);
                }
                return new Resource(body.byteStream(), body.contentLength(), response, session, call);
            } catch (IOException e) {
                if (response != null) response.close();
                session.detach(call);
                session.check(e);
                throw e;
            }
        }
        File file = localFile(location);
        if (!file.isFile() || !file.canRead()) throw new IOException("Unable to read Node file: " + file);
        return new Resource(new FileInputStream(file), file.length(), null, null, null);
    }

    private static Resource openOptional(String location, LoadSession session) throws IOException {
        session.check();
        if (isRemote(location)) {
            Call call = CLIENT.newCall(new Request.Builder().url(location).build());
            session.attach(call);
            Response response = null;
            try {
                response = call.execute();
                session.check();
                if (response.code() == 404 || response.code() == 410) {
                    response.close();
                    session.detach(call);
                    return null;
                }
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    int code = response.code();
                    String url = response.request().url().redact();
                    throw new IOException("HTTP " + code + " for " + url);
                }
                return new Resource(body.byteStream(), body.contentLength(), response, session, call);
            } catch (IOException e) {
                if (response != null) response.close();
                session.detach(call);
                session.check(e);
                throw e;
            }
        }
        File file = localFile(location);
        return file.isFile() && file.canRead() ? new Resource(new FileInputStream(file), file.length(), null, null, null) : null;
    }

    static File localFile(String location) throws IOException {
        if (!location.regionMatches(true, 0, "file://", 0, 7)) return new File(withoutSuffix(location)).getCanonicalFile();
        String value = Uri.decode(withoutSuffix(location).substring(7));
        if (value.startsWith("/")) return new File(value).getCanonicalFile();
        File root = Path.root().getCanonicalFile();
        File file = new File(root, value).getCanonicalFile();
        if (!file.getPath().startsWith(root.getPath() + File.separator)) throw new IOException("Node file escapes shared storage");
        return file;
    }

    static void validateSource(String source) throws IOException {
        String lower = source.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://") || source.startsWith("/")) return;
        throw new IOException("Node config only supports HTTP, HTTPS, file, or absolute paths");
    }

    private static boolean isRemote(String location) {
        return location.regionMatches(true, 0, "http://", 0, 7) || location.regionMatches(true, 0, "https://", 0, 8);
    }

    private static boolean isValid(File directory, String indexMd5, String configMd5) {
        return directory.isDirectory() && Crypto.equals(new File(directory, "index.js"), indexMd5) && Crypto.equals(new File(directory, "index.config.js"), configMd5);
    }

    static NodeBundle readStored(File directory) {
        String indexMd5 = readMd5(new File(directory, "index.js.md5"));
        String configMd5 = readMd5(new File(directory, "index.config.js.md5"));
        if (!isValid(directory, indexMd5, configMd5)) return null;
        File sourceRoot = directory.getParentFile();
        String trustedKey = readTrustedKey(sourceRoot);
        File manifestFile = new File(directory, NodeManifest.FILE_NAME);
        if (!manifestFile.isFile()) return trustedKey.isEmpty() ? new NodeBundle(directory, indexMd5, configMd5, new File(directory, PENDING).isFile(), "") : null;
        try {
            NodeManifest manifest = NodeManifest.parse(Path.read(manifestFile));
            manifest.verify(new File(directory, "index.js"), new File(directory, "index.config.js"), trustedKey);
            return new NodeBundle(directory, indexMd5, configMd5, new File(directory, PENDING).isFile(), manifest.fingerprint());
        } catch (Exception e) {
            SpiderDebug.log("NodeBundle", "Stored Node manifest is invalid: %s", e.getMessage());
            return null;
        }
    }

    private static boolean verifyStoredManifest(NodeBundle stored, NodeManifest manifest, String trustedKey) throws IOException {
        if (manifest == null) return stored.signingKey.isEmpty() && trustedKey.isEmpty();
        manifest.verify(stored.index, stored.config, trustedKey);
        return manifest.fingerprint().equals(stored.signingKey) && (!trustedKey.isEmpty() || stored.pending);
    }

    private static String readTrustedKey(File sourceRoot) {
        if (sourceRoot == null) return "";
        String value = Path.read(new File(sourceRoot, TRUSTED_KEY)).trim().toLowerCase(Locale.ROOT);
        return value.matches("[a-f0-9]{64}") ? value : "";
    }

    private static boolean isRejected(File sourceRoot, String indexMd5, String configMd5, long now) {
        return isRejectedText(Path.read(new File(sourceRoot, REJECTED)), indexMd5, configMd5, now);
    }

    static boolean isRejectedText(String text, String indexMd5, String configMd5, long now) {
        if (text == null || !text.startsWith(version(indexMd5, configMd5))) return false;
        String marker = "rejectedAt=";
        int start = text.indexOf(marker, version(indexMd5, configMd5).length());
        if (start < 0) return false;
        int end = text.indexOf('\n', start);
        try {
            long timestamp = Long.parseLong(text.substring(start + marker.length(), end < 0 ? text.length() : end).trim());
            return timestamp <= now && now - timestamp < REJECT_TTL_MILLIS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static String version(String indexMd5, String configMd5) {
        return indexMd5 + "\n" + configMd5 + "\n";
    }

    static String rejection(String indexMd5, String configMd5, long timestamp) {
        return version(indexMd5, configMd5) + "rejectedAt=" + timestamp + "\n";
    }

    private static void restorePrevious(File active) throws IOException {
        if (readStored(active) != null) return;
        File parent = active.getParentFile();
        if (parent == null) return;
        File previous = new File(parent, "previous");
        if (readStored(previous) == null) return;
        Path.clear(active);
        Path.move(previous, active);
        SpiderDebug.log("NodeBundle", "Restored previous bundle after interrupted update");
    }

    private static void commit(File active, File staging) throws IOException {
        File parent = active.getParentFile();
        if (parent == null) throw new IOException("Node bundle has no parent directory");
        File previous = new File(parent, "previous");
        Path.clear(previous);
        boolean hadActive = active.exists();
        if (hadActive) Path.move(active, previous);
        try {
            Path.move(staging, active);
        } catch (IOException e) {
            if (hadActive && previous.exists()) Path.move(previous, active);
            throw e;
        }
    }

    synchronized NodeBundle rollback() throws IOException {
        if (!pending) return null;
        File parent = root.getParentFile();
        if (parent == null) return null;
        File previous = new File(parent, "previous");
        if (readStored(previous) == null) return null;
        File failed = new File(parent, "failed-" + UUID.randomUUID());
        Path.move(root, failed);
        try {
            Path.move(previous, root);
            NodeBundle fallback = readStored(root);
            if (fallback == null) throw new IOException("Rolled back Node bundle is invalid");
            try {
                writeAtomically(rejection(indexMd5, configMd5, System.currentTimeMillis()).getBytes(StandardCharsets.US_ASCII), new File(parent, REJECTED));
            } catch (IOException e) {
                SpiderDebug.log("NodeBundle", "Unable to quarantine rejected Node bundle: %s", e.getMessage());
            }
            pruneSourceRoot(parent);
            return fallback;
        } catch (IOException e) {
            Path.clear(root);
            if (failed.exists()) Path.move(failed, root);
            throw e;
        } finally {
            Path.clear(failed);
        }
    }

    synchronized void accept() throws IOException {
        if (!pending) return;
        File parent = root.getParentFile();
        if (parent != null && !signingKey.isEmpty()) {
            String trustedKey = readTrustedKey(parent);
            if (!trustedKey.isEmpty() && !trustedKey.equals(signingKey)) throw new IOException("Node bundle signing key changed");
            if (trustedKey.isEmpty()) writeAtomically((signingKey + "\n").getBytes(StandardCharsets.US_ASCII), new File(parent, TRUSTED_KEY));
        }
        File marker = new File(root, PENDING);
        Path.clear(marker);
        pending = marker.exists();
        if (pending) return;
        if (parent != null) {
            Path.clear(new File(parent, REJECTED));
            pruneSourceRoot(parent);
        }
    }

    public static synchronized void prune(Context context, Iterable<String> sources, String currentSource) {
        File bundles = new File(context.getFilesDir(), "nodejs/bundles");
        Set<String> keep = new HashSet<>();
        addSource(keep, currentSource);
        if (sources != null) for (String source : sources) addSource(keep, source);
        pruneRoots(bundles, keep);
    }

    private static void addSource(Set<String> keep, String source) {
        if (isConfig(source)) keep.add(sha256(source));
    }

    static void pruneRoots(File bundles, Set<String> keep) {
        File[] roots = bundles.listFiles(File::isDirectory);
        if (roots == null) return;
        Set<File> keepData = new HashSet<>();
        for (File sourceRoot : roots) {
            if (keep.contains(sourceRoot.getName())) keepData.addAll(pruneSourceRoot(sourceRoot));
            else Path.clear(sourceRoot);
        }
        pruneDataRoots(bundles, keepData);
    }

    private static void pruneDataRoots(File bundles, Set<File> keep) {
        File[] data = new File(bundles.getParentFile(), "data").listFiles();
        if (data != null) for (File file : data) {
            String name = file.getName();
            if (name.startsWith("staging-") || name.matches("[A-Za-z0-9_-]{43}") && !keep.contains(file)) Path.clear(file);
        }
    }

    static Set<File> pruneSourceRoot(File sourceRoot) {
        Set<File> keepData = new HashSet<>();
        addStoredData(keepData, new File(sourceRoot, "active"));
        addStoredData(keepData, new File(sourceRoot, "previous"));
        File[] files = sourceRoot.listFiles();
        if (files != null) for (File file : files) {
            String name = file.getName();
            if (name.startsWith("staging-") || name.startsWith("failed-")) Path.clear(file);
            else if (!keepData.isEmpty() && name.startsWith("data-") && !keepData.contains(file)) Path.clear(file);
        }
        return keepData;
    }

    private static void addStoredData(Set<File> keep, File directory) {
        if (!directory.isDirectory()) return;
        String indexMd5 = readMd5(new File(directory, "index.js.md5"));
        String configMd5 = readMd5(new File(directory, "index.config.js.md5"));
        if (indexMd5.isEmpty() || configMd5.isEmpty()) return;
        File sourceRoot = directory.getParentFile();
        keep.add(new File(sourceRoot, dataDirectoryName(indexMd5, configMd5)));
        keep.add(dataDirectory(sourceRoot, indexMd5, configMd5));
    }

    private static String sha256(String value) {
        MessageDigest digest = Crypto.newDigest("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) builder.append(String.format("%02x", item & 0xff));
        return builder.toString();
    }

    private static void writeAtomically(byte[] data, File target) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null) throw new IOException("Node file has no parent directory");
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Unable to create Node file directory");
        File staging = new File(parent, target.getName() + ".tmp-" + UUID.randomUUID());
        try {
            try (FileOutputStream output = new FileOutputStream(staging)) {
                output.write(data);
                output.flush();
                output.getFD().sync();
            }
            Path.move(staging, target);
        } finally {
            Path.clear(staging);
        }
    }

    public File getRoot() {
        return root;
    }

    public File getIndex() {
        return index;
    }

    public File getConfig() {
        return config;
    }

    String getIndexMd5() {
        return indexMd5;
    }

    String getConfigMd5() {
        return configMd5;
    }

    String getSigningKey() {
        return signingKey;
    }

    synchronized File getData() throws IOException {
        File parent = root.getParentFile();
        if (parent == null) throw new IOException("Node bundle has no data parent");
        File target = dataDirectory(parent, indexMd5, configMd5);
        if (target.isDirectory()) return target;
        if (target.exists()) throw new IOException("Node data path is not a directory");
        File pool = target.getParentFile();
        if (!pool.isDirectory() && !pool.mkdirs() && !pool.isDirectory()) throw new IOException("Unable to create Node data directory");
        File legacy = new File(parent, dataDirectoryName(indexMd5, configMd5));
        if (!legacy.isDirectory() && !pending) legacy = new File(parent, "data");
        if (legacy.isDirectory()) {
            if (!legacy.renameTo(target)) throw new IOException("Unable to migrate Node data directory");
        } else if (pending) {
            seedPreviousData(parent, target);
        }
        if (!target.isDirectory() && !target.mkdirs() && !target.isDirectory()) throw new IOException("Unable to create versioned Node data directory");
        return target;
    }

    static File dataDirectory(File sourceRoot, String indexMd5, String configMd5) {
        File pool = new File(sourceRoot.getParentFile().getParentFile(), "data");
        byte[] identity = (sourceRoot.getName() + "\n" + version(indexMd5, configMd5)).getBytes(StandardCharsets.UTF_8);
        byte[] digest = Crypto.newDigest("SHA-256").digest(identity);
        return new File(pool, Base64.getUrlEncoder().withoutPadding().encodeToString(digest));
    }

    private static void seedPreviousData(File parent, File target) throws IOException {
        NodeBundle stored = readStored(new File(parent, "previous"));
        File source = stored == null ? null : dataDirectory(parent, stored.indexMd5, stored.configMd5);
        if (stored != null && !source.isDirectory()) source = new File(parent, dataDirectoryName(stored.indexMd5, stored.configMd5));
        if (source == null || !source.isDirectory()) source = new File(parent, "data");
        if (!source.isDirectory()) return;
        File staging = new File(target.getParentFile(), "staging-" + UUID.randomUUID());
        try {
            copyDirectory(source, staging, source.getCanonicalFile(), new HashSet<>());
            if (!staging.renameTo(target)) throw new IOException("Unable to promote Node data directory");
        } finally {
            Path.clear(staging);
        }
    }

    private static void copyDirectory(File source, File target, File root, Set<String> visited) throws IOException {
        File canonical = source.getCanonicalFile();
        String rootPath = root.getPath();
        if (!canonical.equals(root) && !canonical.getPath().startsWith(rootPath + File.separator)) throw new IOException("Node data link escapes its directory");
        if (!visited.add(canonical.getPath())) throw new IOException("Node data contains a directory cycle");
        if (!target.isDirectory() && !target.mkdirs() && !target.isDirectory()) throw new IOException("Unable to create Node data staging directory");
        File[] children = source.listFiles();
        if (children == null) throw new IOException("Unable to list Node data directory");
        for (File child : children) {
            File output = new File(target, child.getName());
            if (child.isDirectory()) copyDirectory(child, output, root, visited);
            else if (child.isFile()) copyDataFile(child, output, root);
        }
    }

    private static void copyDataFile(File source, File target, File root) throws IOException {
        File canonical = source.getCanonicalFile();
        String rootPath = root.getPath();
        if (!canonical.getPath().startsWith(rootPath + File.separator)) throw new IOException("Node data link escapes its directory");
        try (InputStream input = new FileInputStream(canonical); FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Node data migration canceled");
                output.write(buffer, 0, count);
            }
            output.flush();
            output.getFD().sync();
        }
    }

    static String dataDirectoryName(String indexMd5, String configMd5) {
        return "data-" + sha256(version(indexMd5, configMd5));
    }

    boolean isSameVersion(NodeBundle other) {
        return other != null && root.equals(other.root) && indexMd5.equals(other.indexMd5) && configMd5.equals(other.configMd5);
    }

    private boolean matches(String otherIndexMd5, String otherConfigMd5) {
        return indexMd5.equals(otherIndexMd5) && configMd5.equals(otherConfigMd5);
    }

    boolean isPending() {
        return pending;
    }

    private static String withoutSuffix(String source) {
        int end = source.length();
        int query = source.indexOf('?');
        int fragment = source.indexOf('#');
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        return source.substring(0, end);
    }

    private static final class Resource implements AutoCloseable {

        private final InputStream input;
        private final long length;
        private final Response response;
        private final LoadSession session;
        private final Call call;

        private Resource(InputStream input, long length, Response response, LoadSession session, Call call) {
            this.input = input;
            this.length = length;
            this.response = response;
            this.session = session;
            this.call = call;
        }

        @Override
        public void close() throws IOException {
            try {
                if (response != null) response.close();
                else input.close();
            } finally {
                if (session != null) session.detach(call);
            }
        }
    }

    private static final class LoadSession {

        private boolean canceled;
        private Call call;

        private synchronized void attach(Call call) throws InterruptedIOException {
            if (canceled || Thread.currentThread().isInterrupted()) {
                call.cancel();
                throw canceled(null);
            }
            this.call = call;
        }

        private synchronized void detach(Call call) {
            if (this.call == call) this.call = null;
        }

        private synchronized void cancel() {
            canceled = true;
            if (call != null) call.cancel();
        }

        private synchronized void check() throws InterruptedIOException {
            if (canceled || Thread.currentThread().isInterrupted()) throw canceled(null);
        }

        private synchronized void check(Throwable cause) throws InterruptedIOException {
            if (canceled || Thread.currentThread().isInterrupted()) throw canceled(cause);
        }

        private static InterruptedIOException canceled(Throwable cause) {
            InterruptedIOException error = new InterruptedIOException("Node bundle load canceled");
            if (cause != null) error.initCause(cause);
            return error;
        }
    }

}
