package com.fongmi.android.tv.player.exo;

import androidx.media3.common.C;
import androidx.media3.common.MediaChapter;
import androidx.media3.common.Player;
import androidx.media3.common.VideoTransform;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.SpeedSetting;

import java.util.List;

public final class ExoCommand {

    private static final long CHAPTER_RESTART_THRESHOLD_MS = 5_000;

    public enum Result {
        APPLIED, UNAVAILABLE, UNSUPPORTED
    }

    private ExoCommand() {
    }

    public static boolean isSupported(String commandText) {
        return parse(commandText) != null;
    }

    public static Result execute(PlayerManager manager, String commandText) {
        return execute(manager, null, commandText);
    }

    public static Result execute(PlayerManager manager, PlayerView playerView, String commandText) {
        Parsed parsed = parse(commandText);
        if (parsed == null) return Result.UNSUPPORTED;
        Player player = manager.getPlayer();
        if (player == null || manager.isEmpty()) return Result.UNAVAILABLE;
        switch (parsed.property) {
            case "video-scale-x", "video-scale-y" -> {
                if (playerView == null || playerView.getPlayer() != player || playerView.getVideoSurfaceView() == null) return Result.UNAVAILABLE;
                VideoTransform current = playerView.getVideoTransform();
                boolean horizontal = parsed.property.equals("video-scale-x");
                float scale = (float) propertyValue(parsed, horizontal ? current.scaleX : current.scaleY);
                playerView.setVideoTransform(new VideoTransform(horizontal ? scale : current.scaleX,
                        horizontal ? current.scaleY : scale, current.panX, current.panY));
            }
            case "video-aspect-override" -> {
                if (playerView == null || playerView.getPlayer() != player || playerView.getVideoSurfaceView() == null) return Result.UNAVAILABLE;
                float aspectRatio = (float) propertyValue(parsed, playerView.getVideoAspectRatioOverride());
                playerView.setVideoAspectRatioOverride(aspectRatio);
            }
            case "seek" -> {
                if (!player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || !player.isCurrentMediaItemSeekable()) return Result.UNAVAILABLE;
                long targetMs = seekPosition(parsed, player.getCurrentPosition(), player.getDuration());
                if (targetMs == C.TIME_UNSET) return Result.UNAVAILABLE;
                manager.seekTo(targetMs);
            }
            case "pause" -> {
                if (!player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) return Result.UNAVAILABLE;
                boolean pause = parsed.mode.equals("cycle") ? player.getPlayWhenReady()
                        : propertyValue(parsed, player.getPlayWhenReady() ? 0 : 1) != 0;
                if (pause) manager.pause();
                else manager.play();
            }
            case "loop-file" -> {
                if (!player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) return Result.UNAVAILABLE;
                boolean repeat = propertyValue(parsed, manager.isRepeatOne() ? 1 : 0) != 0;
                manager.setRepeatOne(repeat);
            }
            case "chapter" -> {
                return executeChapter(manager, player, parsed);
            }
            case "speed" -> {
                if (!player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return Result.UNAVAILABLE;
                float speed = (float) propertyValue(parsed, manager.getSpeed());
                manager.setSpeed(speed);
            }
            case "volume" -> {
                if (!player.isCommandAvailable(Player.COMMAND_SET_VOLUME)) return Result.UNAVAILABLE;
                boolean readCurrent = parsed.mode.equals("add") || parsed.values != null;
                if (readCurrent && !player.isCommandAvailable(Player.COMMAND_GET_VOLUME)) return Result.UNAVAILABLE;
                double volume = propertyValue(parsed, readCurrent ? player.getVolume() * 100.0 : 0);
                player.setVolume((float) (Math.clamp(volume, 0, 100) / 100));
            }
            case "sub-delay", "audio-delay" -> {
                return executeOffset(manager, player, parsed);
            }
            default -> {
                return Result.UNSUPPORTED;
            }
        }
        return Result.APPLIED;
    }

    private static Result executeChapter(PlayerManager manager, Player player, Parsed parsed) {
        List<MediaChapter> chapters = manager.getCurrentMediaChapters();
        if (chapters.isEmpty()) return Result.UNAVAILABLE;
        int targetIndex;
        MediaChapter current = null;
        for (MediaChapter chapter : chapters) if (chapter.selected) current = chapter;
        if (!parsed.mode.equals("add")) targetIndex = (int) propertyValue(parsed, current == null ? -1 : current.index);
        else {
            if (current == null) {
                if (parsed.number < 0) return Result.UNAVAILABLE;
                targetIndex = chapters.get(0).index;
            } else {
                targetIndex = current.index + (int) parsed.number;
                if (parsed.number < 0 && current.timeUs != C.TIME_UNSET && player.getCurrentPosition() - current.timeUs / 1000 > CHAPTER_RESTART_THRESHOLD_MS) targetIndex = current.index;
            }
        }
        MediaChapter target = null;
        for (MediaChapter chapter : chapters) if (chapter.index == targetIndex) target = chapter;
        if (target == null) return Result.UNAVAILABLE;
        if (!manager.selectChapter(target)) return Result.UNAVAILABLE;
        return Result.APPLIED;
    }

    private static Result executeOffset(PlayerManager manager, Player player, Parsed parsed) {
        boolean subtitle = parsed.property.equals("sub-delay");
        if (subtitle && !manager.supportsSubtitleOffsets()) return Result.UNAVAILABLE;
        if (!subtitle && (!player.isCommandAvailable(Player.COMMAND_SET_AUDIO_OFFSET) || (parsed.mode.equals("add") || parsed.values != null) && !player.isCommandAvailable(Player.COMMAND_GET_AUDIO_OFFSET))) return Result.UNAVAILABLE;
        SubtitleOffsets subtitles = subtitle ? manager.getSubtitleOffsets() : null;
        long currentOffsetMs = 0;
        if (parsed.mode.equals("add") || parsed.values != null) currentOffsetMs = subtitle ? subtitles.primaryMs : manager.getAudioOffsetMs();
        double offset = parsed.values == null ? currentOffsetMs + parsed.number * 1000
                : propertyValue(parsed, currentOffsetMs / 1000.0) * 1000;
        if (!isMillis(offset)) return Result.UNAVAILABLE;
        if (subtitle) manager.setSubtitleOffsets(new SubtitleOffsets(Math.round(offset), subtitles.secondaryMs));
        else manager.setAudioOffsetMs(Math.round(offset));
        return Result.APPLIED;
    }

    static long seekPosition(String commandText, long currentPositionMs, long durationMs) {
        Parsed parsed = parse(commandText);
        return parsed == null || !parsed.property.equals("seek") ? C.TIME_UNSET : seekPosition(parsed, currentPositionMs, durationMs);
    }

    private static long seekPosition(Parsed parsed, long currentPositionMs, long durationMs) {
        if (currentPositionMs == C.TIME_UNSET) return C.TIME_UNSET;
        if (durationMs == C.TIME_UNSET && (parsed.mode.contains("percent") || parsed.mode.equals("absolute") && parsed.number < 0)) return C.TIME_UNSET;
        double target = switch (parsed.mode) {
            case "absolute" -> parsed.number < 0 ? durationMs + parsed.number * 1000 : parsed.number * 1000;
            case "absolute-percent" -> durationMs * parsed.number / 100;
            case "relative-percent" -> currentPositionMs + durationMs * parsed.number / 100;
            default -> currentPositionMs + parsed.number * 1000;
        };
        if (!isMillis(target)) return C.TIME_UNSET;
        long result = Math.max(0, Math.round(target));
        return durationMs == C.TIME_UNSET ? result : Math.min(durationMs, result);
    }

    private static boolean isMillis(double milliseconds) {
        return Double.isFinite(milliseconds) && milliseconds > Long.MIN_VALUE && milliseconds < Long.MAX_VALUE;
    }

    static float videoScale(String commandText, float current) {
        Parsed parsed = parse(commandText);
        return parsed == null || !isVideoScale(parsed.property) ? Float.NaN : (float) propertyValue(parsed, current);
    }

    static double propertyValue(String commandText, double current) {
        Parsed parsed = parse(commandText);
        return parsed == null ? Double.NaN : propertyValue(parsed, current);
    }

    private static double propertyValue(Parsed parsed, double current) {
        if (parsed.values == null) return parsed.mode.equals("add") ? current + parsed.number : parsed.number;
        for (int i = 0; i < parsed.values.length; i++) if (sameValue(parsed.property, current, parsed.values[i])) return parsed.values[(i + (parsed.reverse ? parsed.values.length - 1 : 1)) % parsed.values.length];
        return parsed.values[parsed.reverse ? parsed.values.length - 1 : 0];
    }

    private static boolean sameValue(String property, double current, double value) {
        return switch (property) {
            case "speed", "video-scale-x", "video-scale-y", "video-aspect-override" -> (float) current == (float) value;
            case "volume" -> (float) (current / 100) == (float) (value / 100);
            case "sub-delay", "audio-delay" -> Math.round(current * 1000) == Math.round(value * 1000);
            default -> current == value;
        };
    }

    private static boolean isVideoScale(String property) {
        return property.equals("video-scale-x") || property.equals("video-scale-y");
    }

    private static Parsed parse(String commandText) {
        if (commandText == null) return null;
        String[] parts = commandText.strip().split("\\s+");
        if (parts.length >= 3 && parts[0].equals("cycle-values")) return parseCycleValues(parts);
        if (parts.length == 3 && parts[0].equals("set")) return parsePropertyValue(parts[1], parts[2]);
        if (parts.length == 2 && parts[0].equals("seek")) return parseNumeric("seek", "relative", parts[1]);
        if (parts.length == 3 && parts[0].equals("seek") && (parts[2].equals("relative") || parts[2].equals("absolute") || parts[2].equals("absolute-percent") || parts[2].equals("relative-percent"))) return parseNumeric("seek", parts[2], parts[1]);
        if (parts.length == 2 && parts[0].equals("cycle") && parts[1].equals("pause")) return new Parsed("pause", "cycle", 0);
        if (parts.length == 3 && parts[0].equals("add") && parts[1].equals("chapter")) return parseChapterStep(parts[2]);
        if (parts.length == 3 && parts[0].equals("add") && (parts[1].equals("speed") || parts[1].equals("volume") || parts[1].equals("sub-delay") || parts[1].equals("audio-delay"))) return parseNumeric(parts[1], parts[0], parts[2]);
        return null;
    }

    private static Parsed parseCycleValues(String[] parts) {
        boolean reverse = parts[1].equals("!reverse");
        int propertyIndex = reverse ? 2 : 1;
        if (parts.length <= propertyIndex + 1) return null;
        double[] values = new double[parts.length - propertyIndex - 1];
        for (int i = propertyIndex + 1; i < parts.length; i++) {
            Parsed value = parsePropertyValue(parts[propertyIndex], parts[i]);
            if (value == null) return null;
            values[i - propertyIndex - 1] = value.number;
        }
        return new Parsed(parts[propertyIndex], "cycle-values", values[0], values, reverse);
    }

    private static Parsed parseChapterStep(String text) {
        try {
            int step = Integer.parseInt(text);
            return step == 1 || step == -1 ? new Parsed("chapter", "add", step) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Parsed parseChapterIndex(String text) {
        try {
            int index = Integer.parseInt(text);
            return index < 0 ? null : new Parsed("chapter", "set", index);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Parsed parsePropertyValue(String property, String text) {
        if (property.equals("pause") || property.equals("loop-file")) {
            if (text.equals("no")) return new Parsed(property, "set", 0);
            if (text.equals("yes") || property.equals("loop-file") && text.equals("inf")) return new Parsed(property, "set", 1);
            return null;
        }
        if (property.equals("chapter")) return parseChapterIndex(text);
        if (property.equals("video-aspect-override")) return parseVideoAspectRatio(text);
        if (!isVideoScale(property) && !property.equals("speed") && !property.equals("volume") && !property.equals("sub-delay") && !property.equals("audio-delay")) return null;
        Parsed parsed = parseNumeric(property, "set", text);
        if (parsed == null) return null;
        double value = parsed.number;
        if (isVideoScale(property) && ((float) value <= 0 || value > VideoTransform.MAX_SCALE)) return null;
        if (property.equals("speed") && ((float) value < SpeedSetting.MIN || (float) value > SpeedSetting.MAX)) return null;
        if (property.equals("volume") && (value < 0 || value > 100)) return null;
        if ((property.equals("sub-delay") || property.equals("audio-delay")) && !isMillis(value * 1000)) return null;
        return parsed;
    }

    private static Parsed parseVideoAspectRatio(String text) {
        String property = "video-aspect-override";
        if (text.equals("no")) return new Parsed(property, "set", 0);
        String[] parts = text.split(":", -1);
        if (parts.length > 2) return null;
        Parsed numerator = parseNumeric(property, "set", parts[0]);
        if (numerator == null || numerator.number <= 0) return null;
        double value = numerator.number;
        if (parts.length == 2) {
            Parsed denominator = parseNumeric(property, "set", parts[1]);
            if (denominator == null || denominator.number <= 0) return null;
            value /= denominator.number;
        }
        return Float.isFinite((float) value) && (float) value > 0 ? new Parsed(property, "set", value) : null;
    }

    private static Parsed parseNumeric(String property, String mode, String numericText) {
        if (!numericText.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?")) return null;
        try {
            double number = Double.parseDouble(numericText);
            return Double.isFinite(number) ? new Parsed(property, mode, number) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Parsed(String property, String mode, double number, double[] values, boolean reverse) {
        Parsed(String property, String mode, double number) {
            this(property, mode, number, null, false);
        }
    }
}
