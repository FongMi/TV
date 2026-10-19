package com.fongmi.android.tv.player.mpv;

import android.os.Handler;
import android.os.SystemClock;

import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MpvScriptSession {

    public enum State {
        RUNNING, DONE, ERROR, TIMEOUT, LOADING, LOADED
    }

    public record Status(State state, String error) {
    }

    private static final long RESPONSE_TIMEOUT_MS = 10000;
    private static final long POLL_INTERVAL_MS = 100;
    private static final int LOG_LEVEL_ERROR = 20;
    private static final int MAX_ERROR_LENGTH = 8192;
    private static final Pattern SCRIPT_BINDING = Pattern.compile("^(?:nonscalable\\s+)?script-binding\\s+([^\\s;\"']+/[^\\s;\"']+)$");
    private final Map<String, Runner> runners = new HashMap<>();
    private final Map<String, Status> statuses = new HashMap<>();
    private final Map<String, MpvScripts.Item> startupScripts = new HashMap<>();
    private final MpvPlayer player;
    private final Handler handler;
    private long actionGeneration;
    private long startupGeneration;

    MpvScriptSession(MpvPlayer player, List<MpvScripts.Item> items) {
        this.player = player;
        this.handler = new Handler(player.getApplicationLooper());
        player.setLogListener(LOG_LEVEL_ERROR, this::onLogMessage);
        reload(items, true, null);
    }

    private void onLogMessage(String prefix, int level, String text) {
        boolean startupError = false;
        for (MpvScripts.Item item : startupScripts.values()) {
            JSONObject data = startupData(item);
            if (data == null || !prefix.equals(data.optString("name"))) continue;
            startupError = true;
            logStartupError(item, text);
        }
        if (!startupError && (prefix.startsWith("tv_") || text.contains("/mpv/lua/"))) Notify.show(prefix + ": " + text);
    }

    private void logStartupError(MpvScripts.Item item, String text) {
        Status previous = statuses.get(item.id);
        String error = text.strip();
        boolean continuation = previous != null && previous.state == State.ERROR && !error.startsWith("Lua error:");
        if (continuation) error = previous.error + "\n" + error;
        updateStatus(item, State.ERROR, error);
        if (!continuation) Notify.show(item.title + ": " + ResUtil.getString(R.string.mpv_script_failed));
    }

    void release() {
        handler.removeCallbacksAndMessages(null);
        for (Runner runner : runners.values()) runner.release(false);
        runners.clear();
        statuses.clear();
        startupScripts.clear();
        player.setLogListener(LOG_LEVEL_ERROR, null);
    }

    void reload(List<MpvScripts.Item> items, boolean reloadStartupScripts, String reloadButtonId) {
        updateRunners(items, reloadButtonId);
        if (reloadStartupScripts) reloadStartupScripts(items);
    }

    private void updateRunners(List<MpvScripts.Item> items, String reloadButtonId) {
        Set<String> activeIds = new HashSet<>();
        Map<String, MpvScripts.Item> buttons = new HashMap<>();
        for (MpvScripts.Item item : items) {
            if (!item.enabled) continue;
            activeIds.add(item.id);
            if (!item.automatic) buttons.put(item.id, item);
        }
        for (String id : new ArrayList<>(runners.keySet())) removeChangedRunner(id, buttons, reloadButtonId);
        statuses.keySet().retainAll(activeIds);
    }

    private void removeChangedRunner(String id, Map<String, MpvScripts.Item> buttons, String reloadButtonId) {
        Runner runner = runners.get(id);
        MpvScripts.Item current = buttons.get(id);
        if (current != null && runner != null && !id.equals(reloadButtonId) && (!current.isCommand() || current.command.equals(runner.item.command))) return;
        runners.remove(id);
        statuses.remove(id);
        if (runner != null) runner.release(true);
    }

    private void reloadStartupScripts(List<MpvScripts.Item> items) {
        String generation = Long.toString(++startupGeneration);
        List<File> scripts = new ArrayList<>(items.size());
        startupScripts.clear();
        for (MpvScripts.Item item : items) {
            if (!item.automatic || !item.enabled) continue;
            addStartupScript(item, generation, scripts);
        }
        if (!player.setStartupScripts(scripts)) {
            for (MpvScripts.Item item : startupScripts.values()) updateStatus(item, State.ERROR, ResUtil.getString(R.string.mpv_script_unavailable));
            Notify.show(R.string.mpv_script_unavailable);
        }
    }

    private void addStartupScript(MpvScripts.Item item, String generation, List<File> scripts) {
        try {
            scripts.add(MpvScripts.prepareStartup(item, generation));
            startupScripts.put(item.id, item);
            updateStatus(item, State.LOADING, "");
        } catch (IOException e) {
            updateStatus(item, State.ERROR, e.getMessage());
            Notify.show(Notify.getError(R.string.mpv_script_error, e));
        }
    }

    boolean run(MpvScripts.Item item) {
        if (item.automatic || !item.enabled) return false;
        try {
            item = currentItem(item.id);
        } catch (JSONException e) {
            updateStatus(item, State.ERROR, e.getMessage());
            return false;
        }
        if (item == null || item.automatic || !item.enabled) return false;
        Runner runner = runners.get(item.id);
        if (runner == null) runner = createRunner(item);
        if (runner == null) return false;
        runner.item = item;
        runner.pending++;
        if (!runner.polling) {
            runner.polling = true;
            runner.deadline = SystemClock.uptimeMillis() + RESPONSE_TIMEOUT_MS;
            handler.post(runner);
        }
        return true;
    }

    private Runner createRunner(MpvScripts.Item item) {
        Runner runner = new Runner(item, Long.toString(++actionGeneration));
        runners.put(item.id, runner);
        try {
            File runnerFile = MpvScripts.prepareRunner(item, runner.generation);
            if (!player.loadScript(runnerFile.getAbsolutePath(), runner::onLoaded)) {
                runners.remove(item.id);
                runner.release(false);
                return null;
            }
        } catch (IOException e) {
            runners.remove(item.id);
            runner.release(false);
            updateStatus(item, State.ERROR, e.getMessage());
            return null;
        }
        return runner;
    }

    private MpvScripts.Item currentItem(String id) throws JSONException {
        for (MpvScripts.Item item : MpvScripts.read()) if (item.id.equals(id)) return item;
        return null;
    }

    Status status(String id) {
        MpvScripts.Item item = startupScripts.get(id);
        Status previous = statuses.get(id);
        if (item != null && (previous == null || previous.state == State.LOADING)) {
            JSONObject data = startupData(item);
            if (data != null) {
                State state = switch (data.optString("state")) {
                    case "loaded" -> State.LOADED;
                    case "error" -> State.ERROR;
                    default -> State.LOADING;
                };
                updateStatus(item, state, data.optString("message"));
            }
        }
        return statuses.get(id);
    }

    private JSONObject startupData(MpvScripts.Item item) {
        String value = player.getScriptData(item.statusKey());
        if (value == null) return null;
        try {
            JSONObject data = new JSONObject(value);
            return data.optString("generation").equals(Long.toString(startupGeneration)) ? data : null;
        } catch (JSONException e) {
            updateStatus(item, State.ERROR, e.getMessage());
            return null;
        }
    }

    List<String> bindings() throws JSONException {
        TreeSet<String> names = new TreeSet<>();
        String value = player.getInputBindings();
        if (value == null) return new ArrayList<>();
        JSONArray array = new JSONArray(value);
        for (int i = 0; i < array.length(); i++) addBinding(names, array.getJSONObject(i));
        return new ArrayList<>(names);
    }

    private void addBinding(Set<String> names, JSONObject entry) {
        if (entry.optString("owner").isEmpty()) return;
        Matcher matcher = SCRIPT_BINDING.matcher(entry.optString("cmd").strip());
        if (matcher.matches() && !matcher.group(1).matches("tv_[a-f0-9]{32}/run")) names.add(matcher.group(1));
    }

    private void updateStatus(MpvScripts.Item item, State state, String error) {
        Status previous = statuses.get(item.id);
        if (error != null) {
            if (!item.isCommand()) error = error.replace(item.source().getAbsolutePath(), item.fileName);
            if (error.length() > MAX_ERROR_LENGTH) error = error.substring(0, MAX_ERROR_LENGTH);
        }
        statuses.put(item.id, new Status(state, error != null ? error : previous == null ? "" : previous.error));
    }

    private final class Runner implements Runnable {

        private MpvScripts.Item item;
        private final String generation;
        private int pending;
        private int completed;
        private boolean running;
        private boolean polling;
        private boolean released;
        private long clientId;
        private long deadline;

        private Runner(MpvScripts.Item item, String generation) {
            this.item = item;
            this.generation = generation;
        }

        private void onLoaded(boolean success, long clientId) {
            if (!success) {
                if (runners.get(item.id) == this) {
                    runners.remove(item.id);
                    cancelPending();
                    updateStatus(item, State.ERROR, ResUtil.getString(R.string.mpv_script_unavailable));
                }
                return;
            }
            if (released || runners.get(item.id) != this) player.unloadScript(clientId);
            else this.clientId = clientId;
        }

        private void release(boolean unload) {
            released = true;
            cancelPending();
            handler.removeCallbacks(this);
            if (unload && clientId > 0) player.unloadScript(clientId);
        }

        @Override
        public void run() {
            if (released) return;
            try {
                poll();
            } catch (JSONException e) {
                cancelPending();
                updateStatus(item, State.ERROR, e.getMessage());
                Notify.show(Notify.getError(R.string.mpv_script_error, e));
            }
        }

        private void poll() throws JSONException {
            JSONObject status = readStatus();
            updateCompletion(status);
            if (pending == 0 && !running) {
                polling = false;
                return;
            }
            if (SystemClock.uptimeMillis() >= deadline) {
                onTimeout();
                return;
            }
            if (status != null && clientId > 0 && !running && !runPending(status)) return;
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }

        private void onTimeout() {
            cancelPending();
            updateStatus(item, State.TIMEOUT, null);
            Notify.show(item.title + ": " + ResUtil.getString(R.string.mpv_script_timeout));
        }

        private JSONObject readStatus() throws JSONException {
            String value = player.getScriptData(item.statusKey());
            JSONObject status = value == null ? null : new JSONObject(value);
            return status != null && !generation.equals(status.optString("generation")) ? null : status;
        }

        private void updateCompletion(JSONObject status) throws JSONException {
            if (status != null && running && status.getInt("completed") > completed) {
                completed = status.getInt("completed");
                running = false;
                if (status.getString("state").equals("error")) {
                    updateStatus(item, State.ERROR, status.getString("message"));
                    Notify.show(item.title + ": " + ResUtil.getString(R.string.mpv_script_failed));
                } else {
                    updateStatus(item, State.DONE, null);
                }
                deadline = SystemClock.uptimeMillis() + RESPONSE_TIMEOUT_MS;
            }
        }

        private boolean runPending(JSONObject status) throws JSONException {
            MpvScripts.Item current = currentItem(item.id);
            if (current == null || current.automatic || !current.enabled) {
                cancelPending();
                return false;
            }
            completed = status.getInt("completed");
            if (!player.runScriptBinding(item.binding())) {
                cancelPending();
                updateStatus(item, State.ERROR, ResUtil.getString(R.string.mpv_script_unavailable));
                Notify.show(R.string.mpv_script_unavailable);
                return false;
            }
            running = true;
            updateStatus(item, State.RUNNING, null);
            pending--;
            return true;
        }

        private void cancelPending() {
            pending = 0;
            polling = false;
        }
    }
}
