package com.fongmi.android.tv.player.exo;

import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.audio.AudioSink;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class ExoPlaybackLog implements AnalyticsListener {

    private final Map<String, Integer> audioDecoders = new LinkedHashMap<>();
    private final Map<String, Integer> videoDecoders = new LinkedHashMap<>();
    private final Map<AudioOutput, Integer> audioOutputs = new LinkedHashMap<>();

    @Override
    public void onAudioDecoderInitialized(EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        audioDecoders.merge(decoderName, 1, Integer::sum);
    }

    @Override
    public void onAudioDecoderReleased(EventTime eventTime, String decoderName) {
        remove(audioDecoders, decoderName);
    }

    @Override
    public void onVideoDecoderInitialized(EventTime eventTime, String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        videoDecoders.merge(decoderName, 1, Integer::sum);
    }

    @Override
    public void onVideoDecoderReleased(EventTime eventTime, String decoderName) {
        remove(videoDecoders, decoderName);
    }

    @Override
    public void onAudioTrackInitialized(EventTime eventTime, AudioSink.AudioTrackConfig config) {
        audioOutputs.merge(AudioOutput.from(config), 1, Integer::sum);
    }

    @Override
    public void onAudioTrackReleased(EventTime eventTime, AudioSink.AudioTrackConfig config) {
        remove(audioOutputs, AudioOutput.from(config));
    }

    JsonObject snapshot() {
        JsonObject body = new JsonObject();
        body.add("audioDecoders", decoders(audioDecoders));
        body.add("videoDecoders", decoders(videoDecoders));
        body.add("audioOutputs", audioOutputs());
        return body;
    }

    void clear() {
        audioDecoders.clear();
        videoDecoders.clear();
        audioOutputs.clear();
    }

    private static JsonArray decoders(Map<String, Integer> entries) {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, Integer> entry : entries.entrySet()) result.add(describeDecoder(entry));
        return result;
    }

    private static JsonObject describeDecoder(Map.Entry<String, Integer> entry) {
        JsonObject decoder = new JsonObject();
        decoder.addProperty("name", entry.getKey());
        decoder.addProperty("instanceCount", entry.getValue());
        return decoder;
    }

    private JsonArray audioOutputs() {
        return audioOutputs.entrySet().stream().map(entry -> entry.getKey().toJson(entry.getValue())).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }

    private static <T> void remove(Map<T, Integer> entries, T key) {
        entries.computeIfPresent(key, (value, count) -> count == 1 ? null : count - 1);
    }

    private record AudioOutput(int encoding, int sampleRate, int channelConfig, boolean tunneling, boolean offload, int bufferSizeBytes) {

        private static AudioOutput from(AudioSink.AudioTrackConfig config) {
            return new AudioOutput(config.encoding, config.sampleRate, config.channelConfig, config.tunneling, config.offload, config.bufferSize);
        }

        private JsonObject toJson(int instanceCount) {
            JsonObject body = new JsonObject();
            body.addProperty("encoding", encoding);
            body.addProperty("encodingName", Util.getEncodingString(encoding));
            body.addProperty("sampleRate", sampleRate);
            body.addProperty("channelConfig", channelConfig);
            body.addProperty("tunneling", tunneling);
            body.addProperty("offload", offload);
            body.addProperty("bufferSizeBytes", bufferSizeBytes);
            body.addProperty("instanceCount", instanceCount);
            return body;
        }
    }
}
