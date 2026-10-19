package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.github.catvod.net.Download;
import com.github.catvod.utils.Archive;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Upload implements Process {

    private static final long CHUNK_SIZE = 4 * 1024 * 1024;
    private static final long MAX_AGE = 7 * 24 * 60 * 60 * 1000L;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private final File root;
    private final File storage;

    public Upload() {
        this(Path.root(), Path.cache("uploads"));
    }

    Upload(File root, File storage) {
        this.root = root;
        this.storage = storage;
    }

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.equals("/upload/start") || url.equals("/upload/chunk") || url.equals("/upload/finish");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        if (session.getMethod() != Method.POST) return Nano.error(Status.METHOD_NOT_ALLOWED, "POST required");
        LOCK.lock();
        try (Closeable retained = Download.retain(storage)) {
            return handleUpload(url, session.getParms(), files);
        } catch (IllegalArgumentException | SecurityException e) {
            return Nano.error(Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        } finally {
            LOCK.unlock();
        }
    }

    private Response handleUpload(String url, Map<String, String> params, Map<String, String> files) throws IOException {
        String id = params.get("id");
        if (id == null || !id.matches("[0-9a-f]{32}")) throw new IllegalArgumentException("Invalid upload ID");
        File meta = new File(storage, id + ".properties");
        File data = new File(storage, id + ".part");
        Properties info = new Properties();
        if (meta.isFile()) loadInfo(info, meta);
        return handleUpload(url, params, files, info, data, meta);
    }

    private Response handleUpload(String url, Map<String, String> params, Map<String, String> files, Properties info, File data, File meta) throws IOException {
        if (url.equals("/upload/start")) {
            start(params, info, data, meta);
        } else if (info.isEmpty()) {
            return Nano.error(Status.NOT_FOUND, "Upload expired; select the file again");
        } else {
            if (info.containsKey("revision") && !info.getProperty("revision").equals(params.get("revision"))) throw new IllegalArgumentException("Upload revision changed; resume the file");
            if (url.equals("/upload/chunk") && info.containsKey("revision")) storeChunk(params, files, info, data, meta);
            else if (url.equals("/upload/chunk")) append(params, files, info, data);
            else if (url.equals("/upload/finish")) finish(info, data, meta);
        }
        Response response = Nano.ok(state(info, data).toString());
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    private void start(Map<String, String> params, Properties info, File data, File meta) throws IOException {
        String path = params.get("path"), name = params.get("name"), hash = params.get("sha256");
        long size = Long.parseLong(params.get("size"));
        File target = target(path, name);
        if (size < 0 || size > 9007199254740991L || hash == null || !hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid upload file");
        if (!info.isEmpty()) {
            if (!path.equals(info.getProperty("path")) || !name.equals(info.getProperty("name")) || !hash.equals(info.getProperty("sha256")) || size != Long.parseLong(info.getProperty("size"))) throw new IllegalArgumentException("Upload file changed");
            if (!info.containsKey("revision") && !Boolean.parseBoolean(info.getProperty("complete"))) {
                if (data.length() > size) throw new IOException("Invalid saved upload size");
                if (!data.exists()) Files.createFile(data.toPath());
            }
        } else {
            Files.createDirectories(storage.toPath());
            cleanup();
            Path.requireSpace(storage, size);
            Path.requireSpace(target, size);
            info.setProperty("path", path);
            info.setProperty("name", name);
            info.setProperty("size", Long.toString(size));
            info.setProperty("sha256", hash);
            info.setProperty("complete", "false");
            if (Boolean.parseBoolean(params.get("parallel"))) {
                info.setProperty("revision", UUID.randomUUID().toString());
            } else {
                try (var output = new FileOutputStream(data)) {
                    output.getFD().sync();
                }
            }
        }
        save(info, meta);
    }

    private JsonObject state(Properties info, File data) throws IOException {
        boolean complete = Boolean.parseBoolean(info.getProperty("complete"));
        long total = Long.parseLong(info.getProperty("size")), offset = complete ? total : data.length();
        JsonObject result = new JsonObject();
        if (info.containsKey("revision")) {
            TreeSet<Long> saved = readSavedChunks(info, data, total, complete);
            offset = 0;
            while (saved.contains(offset / CHUNK_SIZE) && offset < total) offset += Math.min(CHUNK_SIZE, total - offset);
            JsonArray chunks = new JsonArray();
            for (long index : saved) chunks.add(index);
            result.add("chunks", chunks);
            result.addProperty("threads", 4);
            result.addProperty("revision", info.getProperty("revision"));
        }
        result.addProperty("offset", complete ? total : offset);
        result.addProperty("complete", complete);
        result.addProperty("chunkSize", CHUNK_SIZE);
        return result;
    }

    private TreeSet<Long> readSavedChunks(Properties info, File data, long total, boolean complete) throws IOException {
        TreeSet<Long> saved = new TreeSet<>();
        String prefix = data.getName() + "." + info.getProperty("revision") + ".";
        File[] files = storage.listFiles();
        if (complete || files == null) return saved;
        for (File file : files) addSavedChunk(saved, file, prefix, total);
        return saved;
    }

    private void addSavedChunk(TreeSet<Long> saved, File file, String prefix, long total) throws IOException {
        if (!file.isFile() || !file.getName().startsWith(prefix)) return;
        String index = file.getName().substring(prefix.length());
        if (!index.matches("0|[1-9][0-9]{0,9}")) return;
        long value = Long.parseLong(index), start = value * CHUNK_SIZE;
        if (start >= total || file.length() != Math.min(CHUNK_SIZE, total - start)) throw new IOException("Invalid saved upload chunk");
        saved.add(value);
    }

    private void storeChunk(Map<String, String> params, Map<String, String> files, Properties info, File data, File meta) throws IOException {
        if (Boolean.parseBoolean(info.getProperty("complete"))) return;
        String source = files.get("chunk");
        if (source == null) throw new IllegalArgumentException("Missing upload chunk");
        File chunk = new File(source);
        long offset = Long.parseLong(params.get("offset")), size = Long.parseLong(params.get("size")), total = Long.parseLong(info.getProperty("size"));
        if (offset < 0 || offset >= total || offset % CHUNK_SIZE != 0 || size != Math.min(CHUNK_SIZE, total - offset) || size != chunk.length()) throw new IllegalArgumentException("Invalid upload chunk range");
        String revision = info.getProperty("revision");
        File staged = File.createTempFile(data.getName() + ".", ".tmp", storage);
        try {
            stageChunk(chunk, staged, size);
            readRevision(info, meta, revision);
            if (Boolean.parseBoolean(info.getProperty("complete"))) return;
            File target = new File(data.getPath() + "." + revision + "." + offset / CHUNK_SIZE);
            saveChunk(target, staged, size);
            save(info, meta);
        } finally {
            Files.deleteIfExists(staged.toPath());
        }
    }

    private void readRevision(Properties info, File meta, String revision) throws IOException {
        info.clear();
        loadInfo(info, meta);
        if (!revision.equals(info.getProperty("revision"))) throw new IllegalArgumentException("Upload revision changed; resume the file");
    }

    private void saveChunk(File target, File staged, long size) throws IOException {
        if (target.isFile()) {
            if (target.length() != size || !Arrays.equals(Crypto.sha256(target), Crypto.sha256(staged))) throw new IllegalArgumentException("Upload chunk changed");
        } else {
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
    }

    private void stageChunk(File chunk, File staged, long size) throws IOException {
        LOCK.unlock();
        try {
            stageFile(chunk, staged);
            if (staged.length() != size) throw new IOException("Upload chunk changed");
        } finally {
            LOCK.lock();
        }
    }

    private void append(Map<String, String> params, Map<String, String> files, Properties info, File data) throws IOException {
        if (Boolean.parseBoolean(info.getProperty("complete"))) throw new IllegalArgumentException("Upload already completed");
        String source = files.get("chunk");
        if (source == null) throw new IllegalArgumentException("Missing upload chunk");
        File chunk = new File(source);
        long offset = Long.parseLong(params.get("offset")), size = Long.parseLong(params.get("size"));
        long total = Long.parseLong(info.getProperty("size"));
        if (offset < 0 || offset != data.length() || size <= 0 || size > CHUNK_SIZE || size != chunk.length() || offset > total - size) throw new IllegalArgumentException("Invalid upload chunk range");
        try (var input = new FileInputStream(chunk); var output = new FileOutputStream(data, true)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        } catch (IOException e) {
            truncate(data, offset);
            throw e;
        }
    }

    private void finish(Properties info, File data, File meta) throws IOException {
        if (Boolean.parseBoolean(info.getProperty("complete"))) return;
        File target = target(info.getProperty("path"), info.getProperty("name"));
        long size = Long.parseLong(info.getProperty("size"));
        boolean parallel = info.containsKey("revision");
        if (!parallel && (!data.isFile() || data.length() != size)) throw new IOException("Upload is incomplete");
        Path.requireSpace(target, size);
        File staged = File.createTempFile("upload-", ".tmp", target.getParentFile());
        try {
            byte[] hash = parallel ? assembleChunks(info, data, staged) : Crypto.sha256(data);
            if (!String.format(Locale.ROOT, "%064x", new BigInteger(1, hash)).equals(info.getProperty("sha256"))) {
                resetFailedUpload(info, data, meta, parallel);
                throw new IOException("Upload checksum mismatch; resume to resend the file");
            }
            publishUpload(target, data, staged, parallel);
        } finally {
            Files.deleteIfExists(staged.toPath());
        }
        info.setProperty("complete", "true");
        save(info, meta);
        Files.deleteIfExists(data.toPath());
        if (info.containsKey("revision")) deleteChunks(data, info.getProperty("revision"));
    }

    private void resetFailedUpload(Properties info, File data, File meta, boolean parallel) throws IOException {
        if (parallel) {
            String revision = info.getProperty("revision");
            info.setProperty("revision", UUID.randomUUID().toString());
            save(info, meta);
            deleteChunks(data, revision);
        } else {
            truncate(data, 0);
        }
    }

    private void publishUpload(File target, File data, File staged, boolean parallel) throws IOException {
        if (target.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            if (!Archive.zipDecompress(parallel ? staged : data, target.getParentFile())) throw new IOException("Unable to extract archive");
        } else {
            if (!parallel) stageFile(data, staged);
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void stageFile(File source, File staged) throws IOException {
        Files.copy(source.toPath(), staged.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (var output = new FileOutputStream(staged, true)) {
            output.getFD().sync();
        }
    }

    private void truncate(File data, long length) throws IOException {
        try (var output = new RandomAccessFile(data, "rw")) {
            output.setLength(length);
        }
    }

    private void loadInfo(Properties info, File meta) throws IOException {
        try (var input = new FileInputStream(meta)) {
            info.load(input);
        }
    }

    private byte[] assembleChunks(Properties info, File data, File staged) throws IOException {
        long total = Long.parseLong(info.getProperty("size"));
        MessageDigest hash = Crypto.newDigest("SHA-256");
        try (var output = new FileOutputStream(staged)) {
            copyChunks(info, data, output, total, hash);
            output.getFD().sync();
        }
        return hash.digest();
    }

    private void copyChunks(Properties info, File data, FileOutputStream output, long total, MessageDigest hash) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        for (long offset = 0; offset < total; offset += CHUNK_SIZE) {
            File chunk = new File(data.getPath() + "." + info.getProperty("revision") + "." + offset / CHUNK_SIZE);
            if (!chunk.isFile() || chunk.length() != Math.min(CHUNK_SIZE, total - offset)) throw new IOException("Upload is incomplete");
            copyChunk(chunk, output, buffer, hash);
        }
    }

    private void copyChunk(File chunk, FileOutputStream output, byte[] buffer, MessageDigest hash) throws IOException {
        try (var input = new FileInputStream(chunk)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                hash.update(buffer, 0, count);
            }
        }
    }

    private void deleteChunks(File data, String revision) throws IOException {
        File[] files = storage.listFiles();
        if (files == null) return;
        String prefix = data.getName() + "." + revision + ".";
        for (File file : files) if (file.isFile() && file.getName().startsWith(prefix)) Files.deleteIfExists(file.toPath());
    }

    private File target(String path, String name) throws IOException {
        if (path == null || name == null || name.isEmpty() || name.equals(".") || name.equals("..") || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid upload path");
        while (path.startsWith("/")) path = path.substring(1);
        File directory = new File(root, path).getCanonicalFile();
        File target = new File(directory, name).getCanonicalFile();
        if (!directory.toPath().startsWith(root.getCanonicalFile().toPath()) || !target.toPath().startsWith(directory.toPath())) throw new SecurityException("Path outside upload directory");
        if (!directory.isDirectory()) throw new IOException("Upload directory not found");
        return target;
    }

    private void save(Properties info, File meta) throws IOException {
        File staged = new File(meta.getPath() + ".tmp");
        try {
            writeInfo(info, staged);
            Files.move(staged.toPath(), meta.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged.toPath());
        }
    }

    private void writeInfo(Properties info, File staged) throws IOException {
        try (var output = new FileOutputStream(staged)) {
            info.store(output, null);
            output.getFD().sync();
        }
    }

    private void cleanup() throws IOException {
        File[] files = storage.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - MAX_AGE;
        for (File file : files) {
            if (!file.isFile() || file.lastModified() >= cutoff) continue;
            File meta = new File(storage, file.getName().split("\\.", 2)[0] + ".properties");
            if (!meta.isFile() || meta.lastModified() < cutoff) Files.deleteIfExists(file.toPath());
        }
    }
}
