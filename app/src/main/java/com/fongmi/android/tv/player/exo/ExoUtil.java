package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.exoplayer.text.TextRenderer;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.util.EventLogger;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SpeedSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ExoUtil {

    public static ExoPlayer buildPlayer(Player.Listener listener, DefaultPreloadManager.Builder preloadManagerBuilder) {
        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(App.get()).setSkipSilenceEnabled(SpeedSetting.isSkipSilence());
        ExoPlayer player = preloadManagerBuilder.buildExoPlayer(playerBuilder);
        if (BuildConfig.DEBUG) player.addAnalyticsListener(new EventLogger());
        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        player.setHandleAudioBecomingNoisy(true);
        player.setPlayWhenReady(true);
        player.addListener(listener);
        return player;
    }

    @Nullable
    public static String getMimeType(int errorCode) {
        return switch (errorCode) {
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> MimeTypes.APPLICATION_M3U8;
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> MimeTypes.APPLICATION_OCTET_STREAM;
            default -> null;
        };
    }

    static LoadControl buildLoadControl(int maxPreloadBufferBytes) {
        int buffer = PlayerSetting.getBuffer();
        return new DefaultLoadControl.Builder().setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * buffer, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * buffer, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS).setPlayerTargetBufferBytes(PlayerId.PRELOAD.name, maxPreloadBufferBytes).build();
    }

    public static Map<String, String> extractHeaders(MediaItem item) {
        Bundle extras = item.requestMetadata.extras;
        if (extras == null) return new HashMap<>();
        return extras.keySet().stream().filter(key -> extras.getString(key) != null).collect(Collectors.toMap(key -> key, extras::getString));
    }

    static DecodeTrackSelector buildTrackSelector(int decode) {
        DecodeTrackSelector trackSelector = new DecodeTrackSelector(App.get());
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
        if (DecodeSetting.isPreferAAC()) builder.setPreferredAudioMimeType(MimeTypes.AUDIO_AAC);
        builder.setPreferredTextLanguages(LangUtil.getPreferredTextLanguages());
        builder.setTunnelingEnabled(DecodeSetting.isTunnelingEnabled());
        builder.setForceHighestSupportedBitrate(true);
        trackSelector.setParameters(builder.build());
        setDecodePreferences(trackSelector, decode);
        return trackSelector;
    }

    static void setDecodePreferences(DecodeTrackSelector trackSelector, int decode) {
        int audioDecode = isAudioSoftwareDecode(decode) ? PlayerEngine.SOFT : PlayerEngine.HARD;
        int videoDecode = isVideoSoftwareDecode(decode) ? PlayerEngine.SOFT : PlayerEngine.HARD;
        trackSelector.setRendererDecodePreferences(audioDecode, videoDecode);
    }

    private static boolean isAudioSoftwareDecode(int decode) {
        return decode == PlayerEngine.SOFT && DecodeSetting.isAudioPrefer();
    }

    private static boolean isVideoSoftwareDecode(int decode) {
        return decode == PlayerEngine.SOFT && DecodeSetting.isVideoPrefer();
    }

    static RenderersFactory buildRenderersFactory() {
        return new ExoRenderersFactory(null, null, null);
    }

    static RenderersFactory buildRenderersFactory(AudioProcessor audioProcessor, TextOutput secondaryTextOutput, LibassPlaybackSession libassPlaybackSession) {
        return new ExoRenderersFactory(audioProcessor, secondaryTextOutput, libassPlaybackSession);
    }

    private static AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioOutputPlaybackParams, @Nullable AudioProcessor audioProcessor) {
        DefaultAudioSink.Builder builder = new DefaultAudioSink.Builder(context).setEnableFloatOutput(enableFloatOutput).setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams);
        if (!DecodeSetting.isAudioPassThrough()) builder.setAudioOutputProvider(new AudioTrackAudioOutputProvider.Builder(null).build());
        if (audioProcessor != null) builder.setAudioProcessors(new AudioProcessor[]{audioProcessor});
        return builder.build();
    }

    private static final class ExoRenderersFactory extends DefaultRenderersFactory {

        @Nullable private final AudioProcessor audioProcessor;
        @Nullable private final TextOutput secondaryTextOutput;
        @Nullable private final LibassPlaybackSession libassPlaybackSession;

        private ExoRenderersFactory(@Nullable AudioProcessor audioProcessor, @Nullable TextOutput secondaryTextOutput, @Nullable LibassPlaybackSession libassPlaybackSession) {
            super(App.get());
            this.audioProcessor = audioProcessor;
            this.secondaryTextOutput = secondaryTextOutput;
            this.libassPlaybackSession = libassPlaybackSession;
            setEnableDecoderFallback(true);
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);
            setDolbyVisionOutputPolicy(DecodeSetting.getDolbyVisionOutputPolicy());
        }

        @Override
        protected AudioSink buildAudioSink(@NonNull Context context, boolean enableFloatOutput, boolean enableAudioOutputPlaybackParams) {
            return ExoUtil.buildAudioSink(context, enableFloatOutput, enableAudioOutputPlaybackParams, audioProcessor);
        }

        @Override
        protected void buildMiscellaneousRenderers(@NonNull Context context, @NonNull Handler eventHandler, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
            super.buildMiscellaneousRenderers(context, eventHandler, extensionRendererMode, out);
            if (libassPlaybackSession != null && libassPlaybackSession.isAvailable()) out.add(libassPlaybackSession.createClockRenderer());
        }

        @Override
        protected void buildTextRenderers(@NonNull Context context, @NonNull TextOutput output, @NonNull Looper outputLooper, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
            super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out);
            if (secondaryTextOutput != null) out.add(new TextRenderer(secondaryTextOutput, outputLooper));
        }
    }
}
