package com.fongmi.android.tv.server;

import android.os.SystemClock;

import com.github.catvod.utils.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class LogSession {

    static final long DURATION = TimeUnit.MINUTES.toMillis(60);
    static final int MAX_LINES = 5000;
    static final int MAX_BYTES = 2 * 1024 * 1024;
    static final int MAX_LINE_BYTES = 16 * 1024;
    static final int MAX_BATCH_LINES = 200;
    static final int MAX_BATCH_BYTES = 64 * 1024;
    static final int INITIAL_TAIL_LINES = 500;

    private static final List<String> BUFFERS = List.of("main", "system", "crash");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String TRUNCATED = " [truncated]";

    private final SecureRandom random = new SecureRandom();
    private final LongSupplier clock;
    private final Callable<Process> starter;
    private final Executor executor;
    private final ScheduledExecutorService scheduler;
    private final List<Runnable> listeners = new ArrayList<>();

    private Session session;
    private String error = "";

    private static class Loader {
        static final LogSession INSTANCE = new LogSession(SystemClock::elapsedRealtime, LogSession::startLogcat, command -> new Thread(command, "LogcatReader").start(), Task.scheduler());
    }

    public static LogSession get() {
        return Loader.INSTANCE;
    }

    private static Process startLogcat() throws IOException {
        List<String> command = new ArrayList<>(List.of("/system/bin/logcat"));
        for (String buffer : BUFFERS) {
            command.add("-b");
            command.add(buffer);
        }
        command.addAll(List.of("-v", "threadtime", "-T", String.valueOf(INITIAL_TAIL_LINES), "*:V"));
        return new ProcessBuilder(command).redirectErrorStream(true).start();
    }

    LogSession(LongSupplier clock, Callable<Process> starter, Executor executor, ScheduledExecutorService scheduler) {
        this.clock = clock;
        this.starter = starter;
        this.executor = executor;
        this.scheduler = scheduler;
    }

    public synchronized void start() {
        expire();
        if (session != null) return;
        error = "";
        Session current = session = new Session(createCode(), clock.getAsLong() + DURATION);
        try {
            current.timeout = scheduler.schedule(() -> stop(current), DURATION, TimeUnit.MILLISECONDS);
            executor.execute(() -> read(current));
        } catch (RuntimeException e) {
            fail(current, e);
        }
        if (session == current) notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    public synchronized void stop() {
        stop(session);
        error = "";
    }

    public synchronized void stop(String code) {
        expire();
        if (session == null) throw new IllegalStateException("Log session ended");
        if (!session.code.equals(code)) throw new SecurityException("Invalid log code");
        stop();
    }

    private synchronized void stop(Session current) {
        if (current == null || session != current) return;
        session = null;
        if (current.timeout != null) current.timeout.cancel(false);
        if (current.process != null) current.process.destroy();
        current.lines.clear();
        current.bytes = 0;
        notifyListeners();
    }

    public synchronized boolean isRunning() {
        expire();
        return session != null;
    }

    public synchronized String getCode() {
        expire();
        return session == null ? "" : session.code;
    }

    public synchronized long getRemainingMillis() {
        expire();
        return session == null ? 0 : Math.max(0, session.deadline - clock.getAsLong());
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized JsonObject info() {
        JsonObject body = new JsonObject();
        JsonArray buffers = new JsonArray();
        for (String buffer : BUFFERS) buffers.add(buffer);
        body.add("requestedBuffers", buffers);
        body.addProperty("initialTailLines", INITIAL_TAIL_LINES);
        body.addProperty("maxRetainedLines", MAX_LINES);
        body.addProperty("maxRetainedBytes", MAX_BYTES);
        body.addProperty("running", isRunning());
        if (!error.isEmpty()) body.addProperty("error", error);
        if (session != null) body.addProperty("startedElapsedRealtimeMs", session.deadline - DURATION);
        return body;
    }

    private void expire() {
        if (session != null && clock.getAsLong() >= session.deadline) stop(session);
    }

    private String createCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return code.toString();
    }

    private void read(Session current) {
        Process process = null;
        try {
            if (!isCurrentSession(current)) return;
            process = starter.call();
            if (!attachProcess(current, process)) return;
            readLogcat(current, process);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            fail(current, e);
        } finally {
            if (process != null) {
                process.destroy();
                closeStreams(current, process);
            }
        }
    }

    private synchronized boolean isCurrentSession(Session current) {
        expire();
        return session == current;
    }

    private synchronized boolean attachProcess(Session current, Process process) {
        expire();
        if (session != current) return false;
        current.process = process;
        return true;
    }

    private void readLogcat(Session current, Process process) throws IOException, InterruptedException {
        try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
            readLines(current, reader);
        }
        fail(current, new IOException("Logcat exited with status " + process.waitFor()));
    }

    private void readLines(Session current, Reader reader) throws IOException {
        char[] chunk = new char[4096];
        StringBuilder line = new StringBuilder();
        boolean truncated = false;
        for (int count; (count = reader.read(chunk)) != -1; ) {
            for (int i = 0; i < count; i++) {
                if (chunk[i] == '\n') {
                    if (!appendLine(current, line, truncated)) return;
                    truncated = false;
                } else if (line.length() < MAX_LINE_BYTES) {
                    line.append(chunk[i]);
                } else {
                    truncated = true;
                }
            }
        }
        if (line.length() > 0 || truncated) append(current, line.toString(), truncated);
    }

    private boolean appendLine(Session current, StringBuilder line, boolean truncated) {
        if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') line.setLength(line.length() - 1);
        if (!append(current, line.toString(), truncated)) return false;
        line.setLength(0);
        return true;
    }

    private synchronized boolean append(Session current, String text, boolean truncated) {
        expire();
        if (session != current) return false;
        text = truncate(text, truncated);
        Line line = new Line(++current.sequence, text);
        current.lines.addLast(line);
        current.bytes += size(text) + 1;
        while (current.lines.size() > MAX_LINES || current.bytes > MAX_BYTES) current.bytes -= size(current.lines.removeFirst().text()) + 1;
        return true;
    }

    private static String truncate(String text, boolean truncated) {
        if (!truncated && size(text) + 1 <= MAX_LINE_BYTES && size(new JsonPrimitive(text).toString()) <= MAX_LINE_BYTES) return text;
        int end = 0, high = text.length();
        while (end < high) {
            int middle = (end + high + 1) / 2;
            String value = text.substring(0, middle) + TRUNCATED;
            if (size(value) + 1 <= MAX_LINE_BYTES && size(new JsonPrimitive(value).toString()) <= MAX_LINE_BYTES) end = middle;
            else high = middle - 1;
        }
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + TRUNCATED;
    }

    private static int size(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    private synchronized void fail(Session current, Exception e) {
        if (session != current) return;
        error = e.getMessage() == null ? e.toString() : e.getMessage();
        stop(current);
    }

    private void closeStreams(Session current, Process process) {
        for (Closeable stream : new Closeable[]{process.getInputStream(), process.getErrorStream(), process.getOutputStream()}) closeStream(current, stream);
    }

    private void closeStream(Session current, Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            fail(current, e);
        }
    }

    public Snapshot read(String code, String cursor) {
        List<Line> lines = new ArrayList<>();
        long after, latest, remaining;
        boolean gap;
        synchronized (this) {
            expire();
            if (session == null) throw new IllegalStateException("Log session ended");
            if (!session.code.equals(code)) throw new SecurityException("Invalid log code");
            long oldest = session.lines.isEmpty() ? session.sequence + 1 : session.lines.getFirst().seq();
            after = cursor == null ? oldest - 1 : parseCursor(cursor);
            latest = session.sequence;
            if (after > latest) throw new IllegalArgumentException("Invalid log cursor");
            gap = cursor == null ? oldest > 1 : after < oldest - 1;
            remaining = session.deadline - clock.getAsLong();
            for (Line line : session.lines) {
                if (line.seq() <= after) continue;
                lines.add(line);
                if (lines.size() == MAX_BATCH_LINES) break;
            }
        }
        return createSnapshot(lines, after, latest, gap, remaining);
    }

    private static Snapshot createSnapshot(List<Line> lines, long after, long latest, boolean gap, long remaining) {
        int bytes = 512;
        long next = after;
        List<Line> batch = new ArrayList<>();
        for (Line line : lines) {
            int length = size(new JsonPrimitive(line.text()).toString()) + 64;
            if (bytes + length > MAX_BATCH_BYTES) break;
            batch.add(line);
            bytes += length;
            next = line.seq();
        }
        return new Snapshot(batch, next, next < latest, gap, Math.max(0, remaining));
    }

    private static long parseCursor(String cursor) {
        if (!cursor.matches("[0-9]+")) throw new IllegalArgumentException("Invalid log cursor");
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid log cursor");
        }
    }

    public record Line(long seq, String text) {
    }

    public record Snapshot(List<Line> lines, long next, boolean backlog, boolean gap, long remainingMillis) {
    }

    private static final class Session {
        final String code;
        final long deadline;
        final ArrayDeque<Line> lines = new ArrayDeque<>();
        ScheduledFuture<?> timeout;
        Process process;
        long sequence;
        int bytes;

        Session(String code, long deadline) {
            this.code = code;
            this.deadline = deadline;
        }
    }
}
