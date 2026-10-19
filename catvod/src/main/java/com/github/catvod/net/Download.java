package com.github.catvod.net;

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Task;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public final class Download implements AutoCloseable {

    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");
    private static final Pattern CACHE_FILE = Pattern.compile("((?:[0-9a-f]{64}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})\\.part)(?:\\.properties(?:\\.tmp)?|\\.[0-3])?");
    private static final String[] PART_SUFFIXES = {"", ".0", ".1", ".2", ".3", ".properties"};
    private static final int MAX_PARTS = 4;
    private static final long MIN_PART_BYTES = 1024 * 1024;
    private static final long MIN_SAMPLE_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long MIN_REMAINING_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(7);
    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> RETAINED = new HashMap<>();
    private static final Set<String> CONDITIONAL = Set.of("Range", "If-Range", "If-Match", "If-None-Match", "If-Modified-Since", "If-Unmodified-Since");

    private final File target;
    private final Set<Call> requests = ConcurrentHashMap.newKeySet();
    private Request original;
    private Request transferRequest;
    private Call.Factory factory;
    private volatile Call call;
    private volatile boolean canceled;
    private volatile boolean paused;
    private volatile long speed;
    private long sampleTime;
    private long sampleBytes;
    private long callbackTime;
    private Callback callback;
    private Future<?> future;
    private int lastProgress;
    private File directory;
    private File previousDirectory;
    private File partial;
    private File metadata;
    private String key;
    private Closeable retained;
    private String validator = "";
    private String responseIdentity = "";
    private long offset;
    private long length = -1;
    private long maxBytes = Long.MAX_VALUE;
    private long expectedSize = -1;
    private long initialBytes;
    private boolean resume = true;
    private boolean strictStatus = true;
    private boolean allowEmpty;
    private int parts;
    private long startedAt;
    private volatile boolean invalidPartial;
    private volatile boolean partsCanceled;
    private boolean initialRange;
    private AtomicLongArray positions;
    private boolean separateParts;
    private int threads;
    private boolean rangeUnsupported;

    public static Download create(String url, File target) {
        return new Download(url, target);
    }

    public Download(String url, File target) {
        this(new Request.Builder().url(url).tag(url).build(), target);
    }

    public Download(Request request, File target) {
        this.original = request;
        this.target = target;
        this.directory = new File(target.getAbsoluteFile().getParentFile(), ".downloads");
    }

    public Download tag(String tag) {
        original = original.newBuilder().tag(tag).build();
        return this;
    }

    public Download directory(File directory) {
        this.directory = directory;
        return this;
    }

    public Download previousDirectory(File directory) {
        previousDirectory = directory;
        return this;
    }

    public static synchronized Closeable retain(File... files) throws IOException {
        Set<String> paths = new HashSet<>();
        for (File file : files) paths.add(file.getCanonicalPath());
        for (String path : paths) RETAINED.merge(path, 1, Integer::sum);
        return () -> {
            synchronized (Download.class) {
                for (String path : paths) RETAINED.computeIfPresent(path, (name, count) -> count == 1 ? null : count - 1);
                paths.clear();
            }
        };
    }

    public static synchronized void clearCache(File file) throws IOException {
        String path = file.getCanonicalPath();
        for (String retained : RETAINED.keySet()) if (path.equals(retained) || path.startsWith(retained + File.separator)) return;
        if (Files.isDirectory(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            File[] children = file.listFiles();
            if (children == null) throw new IOException("Unable to list " + file);
            for (File child : children) clearCache(child);
        }
        for (String retained : RETAINED.keySet()) if (retained.startsWith(path + File.separator)) return;
        Files.deleteIfExists(file.toPath());
    }

    public Download maxBytes(long maxBytes) {
        this.maxBytes = maxBytes > 0 ? maxBytes : Long.MAX_VALUE;
        return this;
    }

    public Download size(long size) {
        if (size <= 0) throw new IllegalArgumentException("Invalid download size");
        expectedSize = size;
        return this;
    }

    public Download threads(int threads) {
        if (threads < 0 || threads > MAX_PARTS) throw new IllegalArgumentException("Invalid download threads");
        this.threads = threads;
        return this;
    }

    public Download resume(boolean resume) {
        this.resume = resume;
        return this;
    }

    public Download allowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
        return this;
    }

    public Download strictStatus(boolean strictStatus) {
        this.strictStatus = strictStatus;
        return this;
    }

    public File get() {
        doInBackground();
        return target;
    }

    public File get(CancellationSignal cancellation, ProgressListener progress) throws IOException {
        cancellation.throwIfCanceled();
        cancellation.setOnCancelListener(() -> {
            canceled = true;
            cancelRequests();
        });
        try {
            download(cancellation, progress);
            cancellation.throwIfCanceled();
            return target;
        } finally {
            cancellation.setOnCancelListener(null);
        }
    }

    public void start(Callback callback) {
        this.lastProgress = -1;
        this.canceled = false;
        this.paused = false;
        this.callback = callback;
        future = Task.submit(this::doInBackground);
    }

    public Download cancel() {
        canceled = true;
        if (future != null) future.cancel(true);
        try {
            cancelRequests();
        } finally {
            future = null;
        }
        return this;
    }

    public Download pause() {
        paused = true;
        cancelRequests();
        return this;
    }

    public long getSpeed() {
        return speed;
    }

    private void doInBackground() {
        try {
            download(null, null);
            postCallback(listener -> listener.success(target));
        } catch (Exception error) {
            if (paused && callback != null) postCallback(Callback::paused);
            else handleError(error);
        }
    }

    private synchronized void download(CancellationSignal cancellation, ProgressListener progress) throws IOException {
        if (Thread.currentThread().isInterrupted() || canceled || paused) throw new InterruptedIOException();
        if (key != null) throw new IOException("Download already in progress");
        try (this) {
            Call.Factory client = OkHttp.client();
            call = client.newCall(request(client));
            if (canceled || paused) call.cancel();
            if (cancellation != null) cancellation.throwIfCanceled();
            write(call, call.execute(), (length, bytes) -> {
                if (progress != null) progress.onProgress(length, bytes, length > 0 ? 100f * bytes / length : -1);
                updateProgress(bytes, length);
            });
        } finally {
            call = null;
        }
    }

    private void updateProgress(long totalBytes, long length) {
        int progress = length > 0 ? Math.min(100, (int) (totalBytes * 100.0 / length)) : -1;
        boolean changed = progress >= 0 && progress != lastProgress;
        long now = System.nanoTime(), currentSpeed = speed;
        if (!changed && now - callbackTime < TimeUnit.MILLISECONDS.toNanos(500)) return;
        callbackTime = now;
        if (changed) lastProgress = progress;
        postCallback(listener -> {
            listener.progress(length, totalBytes, currentSpeed);
            if (changed) listener.progress(progress);
        });
    }

    private void handleError(Exception error) {
        if (callback == null) throw new RuntimeException(error.getMessage(), error);
        postCallback(listener -> listener.error(error.getMessage()));
    }

    private void postCallback(Consumer<Callback> action) {
        Callback current = callback;
        if (current == null || canceled) return;
        MainThread.handler.post(() -> {
            if (!canceled && callback == current) action.accept(current);
        });
    }

    public Request request() throws IOException {
        return request(null);
    }

    public Request request(Call.Factory factory) throws IOException {
        if (key != null) throw new IllegalStateException("Download already prepared");
        this.factory = factory;
        requests.clear();
        invalidPartial = false;
        partsCanceled = false;
        rangeUnsupported = false;
        resume = resume && original.method().equals("GET") && original.body() == null && CONDITIONAL.stream().noneMatch(name -> original.header(name) != null);
        String encoding = original.header("Accept-Encoding");
        resume = resume && (encoding == null || encoding.equalsIgnoreCase("identity"));
        String name = resume ? Crypto.sha256(original.url() + "\n" + identity(original)) : UUID.randomUUID().toString();
        partial = new File(directory, name + ".part");
        metadata = new File(directory, name + ".part.properties");
        String path = partial.getCanonicalPath();
        if (!ACTIVE.add(path)) throw new IOException("Download already in progress");
        key = path;
        retained = previousDirectory == null ? retain(target, directory) : retain(target, directory, previousDirectory);
        clearExpired(directory);
        prepare();
        migratePartial();
        initialRange = factory != null && resume && threads > 1 && validator.isEmpty();
        long count = Math.min(threads > 0 ? threads : MAX_PARTS, Math.max(1, expectedSize / MIN_PART_BYTES));
        initialBytes = Math.min(maxBytes, expectedSize > 0 ? expectedSize / count + Math.min(expectedSize % count, 1) : MIN_PART_BYTES);
        Request.Builder builder = original.newBuilder();
        if (resume) builder.header("Accept-Encoding", "identity");
        if (!validator.isEmpty()) builder.header("Range", parts > 0 ? "bytes=0-0" : "bytes=" + offset + "-").header("If-Range", validator);
        else if (initialRange) builder.header("Range", "bytes=0-" + (initialBytes - 1));
        startedAt = System.nanoTime();
        sampleTime = startedAt;
        sampleBytes = offset;
        if (positions != null) for (int i = 0; i < parts; i++) sampleBytes += positions.get(i);
        speed = 0;
        callbackTime = 0;
        return builder.build();
    }

    private void migratePartial() throws IOException {
        if (previousDirectory == null || !validator.isEmpty()) return;
        File previous = new File(previousDirectory, partial.getName());
        String path = previous.getCanonicalPath();
        if (!new File(previous.getPath() + ".properties").isFile() || !ACTIVE.add(path)) return;
        try {
            for (String suffix : PART_SUFFIXES) {
                File source = new File(previous.getPath() + suffix);
                if (source.isFile()) Files.copy(source.toPath(), new File(partial.getPath() + suffix).toPath());
            }
            prepare();
            for (String suffix : PART_SUFFIXES) Files.deleteIfExists(new File(previous.getPath() + suffix).toPath());
        } finally {
            ACTIVE.remove(path);
        }
    }

    public static void clearExpired(File directory) throws IOException {
        File[] files = directory.listFiles((dir, name) -> CACHE_FILE.matcher(name).matches());
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - MAX_AGE;
        for (File file : files) clearExpiredFile(directory, file, cutoff);
    }

    private static void clearExpiredFile(File directory, File file, long cutoff) throws IOException {
        Matcher name = CACHE_FILE.matcher(file.getName());
        if (!name.matches() || !file.isFile()) return;
        File data = new File(directory, name.group(1));
        File info = new File(data.getPath() + ".properties");
        String path = data.getCanonicalPath();
        if (!ACTIVE.add(path)) return;
        try {
            clearExpiredFiles(data, info, cutoff);
        } finally {
            ACTIVE.remove(path);
        }
    }

    private static void clearExpiredFiles(File data, File info, long cutoff) throws IOException {
        long modified = Math.max(data.lastModified(), info.lastModified());
        modified = Math.max(modified, new File(info.getPath() + ".tmp").lastModified());
        for (int i = 0; i < MAX_PARTS; i++) modified = Math.max(modified, new File(data.getPath() + "." + i).lastModified());
        if (modified >= cutoff) return;
        Files.deleteIfExists(info.toPath());
        Files.deleteIfExists(data.toPath());
        Files.deleteIfExists(new File(info.getPath() + ".tmp").toPath());
        for (int i = 0; i < MAX_PARTS; i++) Files.deleteIfExists(new File(data.getPath() + "." + i).toPath());
    }

    private void prepare() throws IOException {
        validator = "";
        responseIdentity = "";
        offset = 0;
        length = -1;
        parts = 0;
        positions = null;
        separateParts = false;
        if (resume && metadata.isFile()) restorePartial();
        if (length < 0 && expectedSize > 0) length = expectedSize;
        if (parts < 0 || parts > MAX_PARTS || parts > 0 && length < parts || expectedSize > 0 && length >= 0 && length != expectedSize) validator = "";
        if (parts > 0 && !validator.isEmpty()) validatePositions();
        if (validator.isEmpty() || responseIdentity.isEmpty() || parts == 0 && offset == 0 || length >= 0 && offset > length) {
            discardPartial();
            offset = 0;
            length = -1;
            validator = "";
            parts = 0;
        }
        if (parts > 0 && separateParts) mergePartial();
        if (parts > 0 && (factory == null || threads == 1)) flattenPartial();
        checkSize(offset);
        if (parts == 0 && offset > 0 && offset == length) offset--;
        Files.createDirectories(directory.toPath());
    }

    private void validatePositions() {
        for (int i = 0; i < parts; i++) {
            long saved = positions.get(i);
            if (saved < 0 || saved > partStart(i + 1) - partStart(i) || !separateParts && saved > 0 && partial.length() < partStart(i) + saved) validator = "";
        }
    }

    private void restorePartial() throws IOException {
        Properties info = new Properties();
        try (InputStream input = new FileInputStream(metadata)) {
            info.load(input);
            if (!identity(original).equals(info.getProperty("request"))) return;
            validator = info.getProperty("validator", "");
            responseIdentity = info.getProperty("response", "");
            length = Long.parseLong(info.getProperty("length", "-1"));
            parts = Integer.parseInt(info.getProperty("parts", "0"));
            offset = parts == 0 ? partial.length() : 0;
            if (parts > 0 && parts <= MAX_PARTS) restorePositions(info);
        } catch (IllegalArgumentException e) {
            validator = "";
        }
    }

    private void restorePositions(Properties info) {
        positions = new AtomicLongArray(parts);
        separateParts = !info.containsKey("offset.0");
        for (int i = 0; i < parts; i++) positions.set(i, separateParts ? part(i).length() : Long.parseLong(info.getProperty("offset." + i)));
    }

    public void write(Call call, Response response, BiConsumer<Long, Long> progress) throws IOException {
        this.call = call;
        try (response) {
            writeResponse(call, response, progress);
        } finally {
            this.call = null;
        }
    }

    private void writeResponse(Call call, Response response, BiConsumer<Long, Long> progress) throws IOException {
        if (canceled || paused) call.cancel();
        checkCanceled(call);
        long responseLength = validateResponse(response);
        requireSpace();
        boolean initialPartial = initialRange && response.code() == 206 && responseLength < length;
        if (initialPartial && validator.isEmpty()) {
            restartSequential(call, response, progress);
            return;
        }
        Response first = prepareParts(response, initialPartial);
        if (parts > 0) writeParts(call, first, responseLength, progress);
        else writeSequential(call, response, responseLength, progress);
    }

    private Response prepareParts(Response response, boolean initialPartial) throws IOException {
        if (initialPartial) {
            parts = (int) Math.min(threads, Math.max(1, length / MIN_PART_BYTES));
            positions = new AtomicLongArray(parts);
            saveMetadata();
            return response;
        }
        if (parts > 0) {
            validatePartial();
            readProbe(response);
            response.close();
        }
        return null;
    }

    private void writeParts(Call call, Response first, long responseLength, BiConsumer<Long, Long> progress) throws IOException {
        downloadParts(call, first, 0, responseLength, progress);
        publish(call);
    }

    private void restartSequential(Call call, Response response, BiConsumer<Long, Long> progress) throws IOException {
        response.close();
        initialRange = false;
        Call full = newCall(call, transferRequest.newBuilder().removeHeader("Range").removeHeader("If-Range").build());
        write(full, full.execute(), progress);
    }

    private void requireSpace() throws IOException {
        if (expectedSize > 0 && length < 0) length = expectedSize;
        checkSize(length);
        if (expectedSize > 0 && length != expectedSize) throw new IOException("Download length mismatch");
        long saved = offset;
        if (parts > 0) for (int i = 0; i < parts; i++) saved += positions.get(i);
        Path.requireSpace(directory, length - saved);
    }

    private void writeSequential(Call call, Response response, long responseLength, BiConsumer<Long, Long> progress) throws IOException {
        try (var output = new RandomAccessFile(partial, "rw")) {
            output.setLength(offset);
        }
        if (resume) saveMetadata();
        if (!copy(call, response.body().byteStream(), responseLength, response.header("Accept-Ranges", "").equalsIgnoreCase("bytes"), progress)) {
            response.close();
            saveMetadata();
            downloadRemainingParts(call, progress);
            return;
        }
        publish(call);
    }

    private long validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) {
            if (response.code() == 416) discardPartial();
            throw new IOException("HTTP " + response.code() + " " + response.message());
        }
        long responseLength = original.method().equals("HEAD") ? -1 : response.body().contentLength();
        if (!resume) {
            length = responseLength;
            return responseLength;
        }
        Response network = response.networkResponse() == null ? response : response.networkResponse();
        Request request = network.request();
        transferRequest = request;
        if (!network.header("Content-Encoding", "identity").equalsIgnoreCase("identity")) throw new IOException("Unexpected download encoding");
        if (response.code() == 206 && initialRange) {
            responseLength = validateInitialRange(response, request, responseLength);
        } else if (response.code() == 206 && !validator.isEmpty()) {
            if (parts > 0) validatePart(response, 0, 0);
            else responseLength = validateRange(response, request, responseLength);
        } else if (response.code() == 200) {
            if (parts > 0) discardPartial();
            parts = 0;
            offset = 0;
            length = responseLength;
            validator = validator(response);
            responseIdentity = identity(request);
            if (!request.method().equals("GET") || request.body() != null) validator = "";
        } else if (!strictStatus) {
            if (parts > 0) discardPartial();
            parts = 0;
            offset = 0;
            length = responseLength;
            validator = "";
        } else {
            throw new IOException("Unexpected download status: " + response.code());
        }
        return responseLength;
    }

    private long validateInitialRange(Response response, Request request, long contentLength) throws IOException {
        Matcher range = CONTENT_RANGE.matcher(response.header("Content-Range", ""));
        try {
            if (!range.matches()) throw new IllegalArgumentException();
            long start = Long.parseLong(range.group(1)), end = Long.parseLong(range.group(2)), total = Long.parseLong(range.group(3));
            if (start != 0 || total <= 0 || end != Math.min(total, initialBytes) - 1 || contentLength >= 0 && contentLength != end + 1 || !request.method().equals("GET") || request.body() != null || !("bytes=0-" + (initialBytes - 1)).equals(request.header("Range")) || request.header("If-Range") != null) throw new IllegalArgumentException();
            length = total;
            validator = validator(response);
            responseIdentity = identity(request);
            return end + 1;
        } catch (IllegalArgumentException e) {
            if (!strictStatus) {
                length = contentLength;
                validator = "";
                return contentLength;
            }
            invalidPartial = true;
            throw new IOException("Invalid download range", e);
        }
    }

    private void validatePartial() throws IOException {
        long saved = partial.length();
        for (int i = 0; i < parts; i++) {
            if (positions.get(i) > 0 && saved < partStart(i) + positions.get(i)) {
                invalidPartial = true;
                throw new IOException("Invalid download partial");
            }
        }
    }

    private void readProbe(Response response) throws IOException {
        InputStream input = response.body().byteStream();
        if (input.read() == -1 || input.read() != -1) throw new IOException("Invalid download probe");
    }

    private Call newCall(Call parent, Request request) throws IOException {
        checkCanceled(parent);
        long remaining = remainingTime(parent);
        Call next;
        try {
            next = factory.newCall(request);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        requests.add(next);
        if (remaining != Long.MAX_VALUE) next.timeout().timeout(remaining, TimeUnit.NANOSECONDS);
        if (canceled || partsCanceled || parent.isCanceled()) next.cancel();
        return next;
    }

    private long remainingTime(Call parent) throws SocketTimeoutException {
        long now = System.nanoTime(), timeout = parent.timeout().timeoutNanos();
        long remaining = timeout > 0 ? timeout - (now - startedAt) : Long.MAX_VALUE;
        if (parent.timeout().hasDeadline()) remaining = Math.min(remaining, parent.timeout().deadlineNanoTime() - now);
        if (remaining <= 0) throw new SocketTimeoutException("timeout");
        return remaining;
    }

    private void validatePart(Response response, long start, long end) throws IOException {
        Response network = response.networkResponse() == null ? response : response.networkResponse();
        Request request = network.request();
        if (!responseIdentity.equals(identity(request))) {
            invalidPartial = true;
            throw new IOException("Download request changed");
        }
        String current = response.header(validator.startsWith("\"") ? "ETag" : "Last-Modified");
        if (response.code() != 206 || !("bytes " + start + "-" + end + "/" + length).equals(response.header("Content-Range")) || !network.header("Content-Encoding", "identity").equalsIgnoreCase("identity") || !request.method().equals("GET") || request.body() != null || !("bytes=" + start + "-" + end).equals(request.header("Range")) || !validator.equals(request.header("If-Range")) || current != null && !current.equals(validator) || response.body().contentLength() >= 0 && response.body().contentLength() != end - start + 1) {
            invalidPartial = true;
            throw new IOException("Invalid download range");
        }
    }

    private File part(int index) {
        return new File(directory, partial.getName() + "." + index);
    }

    private void downloadRemainingParts(Call parent, BiConsumer<Long, Long> progress) throws IOException {
        int index = positions.get(0) < partStart(1) ? 0 : 1;
        long start = partStart(index) + positions.get(index), end = partStart(index + 1) - 1;
        Call call = newCall(parent, transferRequest.newBuilder().header("Range", "bytes=" + start + "-" + end).header("If-Range", validator).build());
        try (Response response = call.execute()) {
            if (response.code() == 200) {
                parts = 0;
                positions = null;
                offset = 0;
                rangeUnsupported = true;
                write(call, response, progress);
                return;
            }
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " " + response.message());
            validatePart(response, start, end);
            downloadParts(parent, response, index, end - start + 1, progress);
        }
        publish(parent);
    }

    private long partStart(int index) {
        return length / parts * index + Math.min(length % parts, index);
    }

    private void downloadParts(Call parent, Response first, int firstIndex, long firstLength, BiConsumer<Long, Long> progress) throws IOException {
        checkSize(length);
        AtomicLong bytes = new AtomicLong();
        for (int i = 0; i < parts; i++) bytes.addAndGet(positions.get(i));
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(parts, threads > 0 ? threads : MAX_PARTS));
        CompletionService<Void> completed = new ExecutorCompletionService<>(workers);
        AtomicReference<Exception> failure = new AtomicReference<>();
        boolean finished = false;
        Throwable error = null;
        long checkpoint = System.nanoTime();
        try {
            int pending = submitParts(parent, first, firstIndex, firstLength, bytes, failure, completed);
            awaitParts(parent, completed, pending, bytes, progress, checkpoint);
            finished = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = new InterruptedIOException();
            throw (InterruptedIOException) error;
        } catch (ExecutionException e) {
            Exception failed = failure.get();
            error = failed instanceof IOException || failed instanceof RuntimeException ? failed : new IOException(e.getCause());
            if (error instanceof IOException io) throw io;
            throw (RuntimeException) error;
        } catch (IOException | RuntimeException | Error e) {
            error = e;
            throw e;
        } finally {
            try {
                finishParts(parent, workers, finished);
            } catch (IOException | RuntimeException | Error closing) {
                if (error == null) throw closing;
                if (error != closing) error.addSuppressed(closing);
            }
        }
        checkCanceled(parent);
        if (partial.length() != length) throw new IOException("Incomplete download");
        for (int i = 0; i < parts; i++) if (positions.get(i) != partStart(i + 1) - partStart(i)) throw new IOException("Incomplete download");
    }

    private int submitParts(Call parent, Response first, int firstIndex, long firstLength, AtomicLong bytes, AtomicReference<Exception> failure, CompletionService<Void> completed) {
        int pending = 0;
        for (int i = 0; i < parts; i++) {
            int index = i;
            if (positions.get(index) == partStart(index + 1) - partStart(index)) continue;
            pending++;
            completed.submit(() -> {
                transferPart(parent, index, index == firstIndex ? first : null, firstLength, bytes, failure);
                return null;
            });
        }
        return pending;
    }

    private void awaitParts(Call parent, CompletionService<Void> completed, int pending, AtomicLong bytes, BiConsumer<Long, Long> progress, long checkpoint) throws IOException, InterruptedException, ExecutionException {
        long reported = -1;
        while (pending > 0) {
            checkCanceled(parent);
            remainingTime(parent);
            Future<Void> done = completed.poll(100, TimeUnit.MILLISECONDS);
            long received = bytes.get();
            if (received != reported) reportProgress(progress, reported = received);
            if (done != null) {
                done.get();
                pending--;
            }
            if (pending > 0 && System.nanoTime() - checkpoint >= TimeUnit.SECONDS.toNanos(1)) {
                saveMetadata();
                checkpoint = System.nanoTime();
            }
        }
        if (reported != bytes.get()) reportProgress(progress, bytes.get());
    }

    private void transferPart(Call parent, int index, Response first, long firstLength, AtomicLong bytes, AtomicReference<Exception> failure) throws Exception {
        try {
            if (failure.get() != null) return;
            if (first != null) writePart(parent, parent, first, index, firstLength, bytes);
            if (positions.get(index) < partStart(index + 1) - partStart(index)) copyPart(parent, index, bytes);
        } catch (Exception e) {
            if (failure.compareAndSet(null, e)) {
                try {
                    cancelParts();
                } catch (RuntimeException | Error closing) {
                    if (closing != e) e.addSuppressed(closing);
                }
            }
            throw e;
        }
    }

    private void finishParts(Call parent, ExecutorService workers, boolean finished) throws IOException {
        Throwable closing = stopParts(parent, workers, finished);
        try {
            if (invalidPartial) discardPartial();
            else saveMetadata();
        } catch (IOException | RuntimeException | Error e) {
            if (closing == null) closing = e;
            else if (closing != e) closing.addSuppressed(e);
        }
        if (closing instanceof IOException io) throw io;
        if (closing instanceof RuntimeException runtime) throw runtime;
        if (closing instanceof Error error) throw error;
    }

    private Throwable stopParts(Call parent, ExecutorService workers, boolean finished) {
        Throwable closing = null;
        try {
            if (!finished) parent.cancel();
        } catch (RuntimeException | Error e) {
            closing = e;
        }
        try {
            stopWorkers(workers);
        } catch (RuntimeException | Error e) {
            if (closing == null) closing = e;
            else if (closing != e) closing.addSuppressed(e);
        }
        return closing;
    }

    private void copyPart(Call parent, int index, AtomicLong bytes) throws IOException {
        long start = partStart(index) + positions.get(index), end = partStart(index + 1) - 1;
        Call next = newCall(parent, transferRequest.newBuilder().header("Range", "bytes=" + start + "-" + end).header("If-Range", validator).build());
        try (Response response = next.execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " " + response.message());
            validatePart(response, start, end);
            writePart(parent, next, response, index, end - start + 1, bytes);
        }
    }

    private void writePart(Call parent, Call call, Response response, int index, long expected, AtomicLong bytes) throws IOException {
        try (InputStream input = response.body().byteStream(); RandomAccessFile output = new RandomAccessFile(partial, "rw")) {
            output.seek(partStart(index) + positions.get(index));
            copyPartData(parent, call, input, output, index, expected, bytes);
        }
    }

    private void copyPartData(Call parent, Call call, InputStream input, RandomAccessFile output, int index, long expected, AtomicLong bytes) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long received = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            checkCanceled(parent);
            checkCanceled(call);
            if (received + count > expected) {
                invalidPartial = true;
                throw new IOException("Download length mismatch");
            }
            output.write(buffer, 0, count);
            received += count;
            positions.addAndGet(index, count);
            bytes.addAndGet(count);
        }
        if (received != expected) throw new IOException("Incomplete download");
    }

    private void mergePartial() throws IOException {
        try (RandomAccessFile output = new RandomAccessFile(partial, "rw")) {
            output.setLength(0);
            for (int i = 0; i < parts; i++) if (positions.get(i) != 0) mergePart(output, i);
            output.getFD().sync();
        }
        separateParts = false;
        saveMetadata();
        for (int i = 0; i < MAX_PARTS; i++) Files.deleteIfExists(part(i).toPath());
    }

    private void mergePart(RandomAccessFile output, int index) throws IOException {
        output.seek(partStart(index));
        try (InputStream input = new FileInputStream(part(index))) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                checkCanceled(null);
                output.write(buffer, 0, count);
            }
        }
    }

    private void flattenPartial() throws IOException {
        try (RandomAccessFile output = new RandomAccessFile(partial, "rw")) {
            offset = 0;
            for (int i = 0; i < parts; i++) {
                offset += positions.get(i);
                if (positions.get(i) != partStart(i + 1) - partStart(i)) break;
            }
            output.setLength(offset);
            output.getFD().sync();
        }
        parts = 0;
        saveMetadata();
        for (int i = 0; i < MAX_PARTS; i++) Files.deleteIfExists(part(i).toPath());
    }

    private void publish(Call call) throws IOException {
        checkCanceled(call);
        if (parts > 0) remainingTime(call);
        Path.chmod(partial);
        checkCanceled(call);
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(metadata.toPath());
        for (int i = 0; i < MAX_PARTS; i++) Files.deleteIfExists(part(i).toPath());
    }

    private void cancelParts() {
        partsCanceled = true;
        Throwable failure = null;
        for (Call request : requests) {
            try {
                request.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private void cancelRequests() {
        Call request = call;
        Throwable failure = null;
        try {
            if (request != null) request.cancel();
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        try {
            cancelParts();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private void stopWorkers(ExecutorService workers) {
        Throwable failure = null;
        try {
            cancelParts();
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        workers.shutdownNow();
        awaitWorkers(workers);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private void awaitWorkers(ExecutorService workers) {
        boolean interrupted = false;
        for (;;) {
            try {
                if (workers.awaitTermination(1, TimeUnit.SECONDS)) break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    void releaseCalls(Consumer<Call> release) {
        for (Call request : requests) release.accept(request);
        requests.clear();
    }

    private long validateRange(Response response, Request request, long contentLength) throws IOException {
        Matcher range = CONTENT_RANGE.matcher(response.header("Content-Range", ""));
        if (!range.matches() || !partial.isFile() || partial.length() < offset) throw new IOException("Invalid download range");
        if (!request.method().equals("GET") || request.body() != null || !("bytes=" + offset + "-").equals(request.header("Range")) || !validator.equals(request.header("If-Range"))) throw new IOException("Invalid download range");
        if (!responseIdentity.equals(identity(request))) {
            discardPartial();
            throw new IOException("Download request changed");
        }
        try {
            long start = Long.parseLong(range.group(1)), end = Long.parseLong(range.group(2)), total = Long.parseLong(range.group(3));
            String current = response.header(validator.startsWith("\"") ? "ETag" : "Last-Modified");
            if (start != offset || end < start || end >= total || length >= 0 && length != total || contentLength >= 0 && contentLength != end - start + 1 || current != null && !current.equals(validator)) throw new IOException("Invalid download range");
            length = total;
            return end - start + 1;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid download range", e);
        }
    }

    private static String identity(Request request) {
        StringBuilder value = new StringBuilder(request.method()).append('\n').append(request.url());
        Headers headers = request.headers();
        for (String name : headers.names().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) appendHeaderIdentity(value, headers, name);
        return Crypto.sha256(value.toString());
    }

    private static void appendHeaderIdentity(StringBuilder value, Headers headers, String name) {
        if (name.equalsIgnoreCase("Range") || name.equalsIgnoreCase("If-Range")) return;
        value.append('\n').append(name.toLowerCase(Locale.ROOT)).append(':');
        for (String header : headers.values(name)) value.append(header.length()).append(':').append(header);
    }

    private static String validator(Response response) {
        String etag = response.header("ETag", "");
        if (etag.length() >= 2 && etag.startsWith("\"") && etag.endsWith("\"")) return etag;
        if (!etag.isEmpty()) return "";
        String modified = response.header("Last-Modified", ""), date = response.header("Date", "");
        try {
            long age = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() - ZonedDateTime.parse(modified, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond();
            return age >= 60 ? modified : "";
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private void saveMetadata() throws IOException {
        long[] saved = new long[parts];
        for (int i = 0; i < parts; i++) saved[i] = positions.get(i);
        if (parts > 0) {
            try (RandomAccessFile output = new RandomAccessFile(partial, "rw")) {
                output.getFD().sync();
            }
        }
        Properties info = new Properties();
        info.setProperty("request", identity(original));
        info.setProperty("response", responseIdentity);
        info.setProperty("validator", validator);
        info.setProperty("length", Long.toString(length));
        if (parts > 0) {
            info.setProperty("parts", Integer.toString(parts));
            for (int i = 0; i < parts; i++) info.setProperty("offset." + i, Long.toString(saved[i]));
        }
        writeMetadata(info);
    }

    private void writeMetadata(Properties info) throws IOException {
        File temp = new File(metadata.getPath() + ".tmp");
        try {
            try (FileOutputStream output = new FileOutputStream(temp)) {
                info.store(output, null);
                output.getFD().sync();
            }
            Files.move(temp.toPath(), metadata.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }

    private void discardPartial() throws IOException {
        Files.deleteIfExists(metadata.toPath());
        Files.deleteIfExists(partial.toPath());
        for (int i = 0; i < MAX_PARTS; i++) Files.deleteIfExists(part(i).toPath());
    }

    private boolean copy(Call call, InputStream stream, long responseLength, boolean ranges, BiConsumer<Long, Long> progress) throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(stream); FileOutputStream output = new FileOutputStream(partial, true)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            long total = offset;
            long sample = System.nanoTime();
            boolean split = threads == 0 && !rangeUnsupported && factory != null && resume && offset == 0 && !validator.isEmpty() && ranges && length >= MAX_PARTS * MIN_PART_BYTES;
            while ((count = input.read(buffer, 0, split ? (int) Math.min(buffer.length, MIN_PART_BYTES - total) : buffer.length)) != -1) {
                total = writeChunk(call, output, buffer, count, total, responseLength, progress);
                if (split && total >= MIN_PART_BYTES) {
                    split = false;
                    if (beginParallel(total, sample)) return false;
                }
            }
            if (!allowEmpty && total == 0) throw new IOException("Empty download");
            if (length >= 0 && total != length || responseLength >= 0 && total - offset != responseLength) throw new IOException("Incomplete download");
            output.getFD().sync();
            return true;
        }
    }

    private boolean beginParallel(long total, long sample) {
        long elapsed = System.nanoTime() - sample;
        double remaining = (double) (length - total) * elapsed / total;
        if (elapsed >= MIN_SAMPLE_NANOS && remaining >= MIN_REMAINING_NANOS) {
            parts = (int) Math.min(MAX_PARTS, length / MIN_PART_BYTES);
            positions = new AtomicLongArray(parts);
            positions.set(0, total);
            return true;
        }
        return false;
    }

    private long writeChunk(Call call, FileOutputStream output, byte[] buffer, int count, long total, long responseLength, BiConsumer<Long, Long> progress) throws IOException {
        checkCanceled(call);
        total += count;
        checkSize(total);
        if (length >= 0 && total > length || responseLength >= 0 && total - offset > responseLength) throw new IOException("Download length mismatch");
        output.write(buffer, 0, count);
        reportProgress(progress, total);
        return total;
    }

    private void checkCanceled(Call call) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || canceled || paused || call != null && call.isCanceled()) throw new InterruptedIOException();
    }

    private void reportProgress(BiConsumer<Long, Long> progress, long bytes) {
        long now = System.nanoTime(), elapsed = now - sampleTime;
        if (elapsed >= TimeUnit.MILLISECONDS.toNanos(500)) {
            speed = (long) ((double) Math.max(0, bytes - sampleBytes) * TimeUnit.SECONDS.toNanos(1) / elapsed);
            sampleBytes = bytes;
            sampleTime = now;
        }
        if (progress != null) progress.accept(length, bytes);
    }

    private void checkSize(long size) throws IOException {
        if (size > maxBytes || expectedSize > 0 && size > expectedSize) throw new IOException("Download size limit exceeded");
    }

    @Override
    public void close() throws IOException {
        if (key == null) return;
        try {
            long saved = partial.length();
            if (parts > 0 && parts <= MAX_PARTS && positions != null) {
                saved = 0;
                for (int i = 0; i < parts; i++) saved += positions.get(i);
            }
            if (invalidPartial || validator.isEmpty() || saved == 0) discardPartial();
        } finally {
            ACTIVE.remove(key);
            key = null;
            if (retained != null) retained.close();
            retained = null;
        }
    }

    @FunctionalInterface
    public interface ProgressListener {

        void onProgress(long total, long bytes, float percent);
    }

    public interface Callback {

        default void progress(long total, long downloaded, long speed) {
        }

        default void paused() {
        }

        default void progress(int progress) {
        }

        void error(String msg);

        void success(File file);
    }

    private static class MainThread {
        private static final Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }
}
