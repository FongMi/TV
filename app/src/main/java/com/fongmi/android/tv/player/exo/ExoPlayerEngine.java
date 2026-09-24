package com.fongmi.android.tv.player.exo;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.iso.IsoNavigationMediaSource;
import androidx.media3.exoplayer.iso.IsoNavigationPlayerController;
import androidx.media3.exoplayer.iso.IsoNavigationSession;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.DiscMenuController;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.player.track.TrackUtil;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ExoPlayerEngine implements PlayerEngine, DiscMenuController, AnalyticsListener {

    private final ExoErrorMessageProvider provider;
    private final ExoSubtitleController subtitles;
    private final ExoPlayerSession session;
    private final ExoPlayerEffect effect;
    private final ExoDiskPreload preload;
    private final ExoPlayer player;
    private final IsoNavigationPlayerController navigation;
    private PlaySpec spec;

    public ExoPlayerEngine(Player.Listener listener) {
        this.effect = new ExoPlayerEffect();
        this.preload = new ExoDiskPreload();
        this.provider = new ExoErrorMessageProvider();
        this.session = new ExoPlayerSession(listener, effect.getAudioProcessor());
        this.subtitles = new ExoSubtitleController(session);
        this.player = session.player();
        this.navigation = new IsoNavigationPlayerController(player);
        this.player.addAnalyticsListener(this);
        this.effect.setPlayer(player);
    }

    @Override
    public void onAudioTrackInitialized(@NonNull EventTime eventTime, @NonNull AudioSink.AudioTrackConfig audioTrackConfig) {
        effect.applyAudioEffect();
    }

    @Override
    public void onAudioTrackReleased(@NonNull EventTime eventTime, @NonNull AudioSink.AudioTrackConfig audioTrackConfig) {
        effect.applyAudioEffect();
    }

    @Override
    public void onTracksChanged(@NonNull EventTime eventTime, @NonNull Tracks tracks) {
        effect.applyVideoEffect();
    }

    @Override
    public Type getType() {
        return Type.EXO;
    }

    @Override
    public boolean needsRebuild() {
        return session.hasLibassSettingChanged();
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean hasMenu() {
        return IsoNavigationSession.isAvailable() && isIso(spec) && !navigation.isMenuUnavailable();
    }

    @Override
    public boolean isActive() {
        return navigation.isMenuActive();
    }

    @Override
    public int getMenuDomain() {
        return navigation.getMenuDomain();
    }

    @Override
    public boolean isNavigationPlayback() {
        return navigation.isStarted();
    }

    public int getNavigationRepeatMode() {
        return navigation.getRepeatMode();
    }

    public void setNavigationRepeatOne(boolean repeat) {
        navigation.setRepeatMode(repeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    @Override
    public boolean hasExternalGraphics() {
        return navigation.isStarted();
    }

    @Override
    public boolean sendAction(String action) {
        if (!navigation.isStarted()) return ("menu".equals(action) || "title-menu".equals(action) || "popup".equals(action)) && openDiscMenu(action);
        int mapped = mapDiscAction(action);
        return mapped > 0 && navigation.sendAction(mapped);
    }

    @Override
    public void observeOpen(String action, Consumer<OpenResult> callback) {
        navigation.observeMenuOpen(mapDiscAction(action), result -> callback.accept(switch (result) {
            case OPENED -> OpenResult.OPENED;
            case UNAVAILABLE -> OpenResult.UNAVAILABLE;
            case TIMED_OUT -> OpenResult.TIMED_OUT;
            case CANCELLED -> OpenResult.CANCELLED;
        }));
    }

    private static int mapDiscAction(String action) {
        return switch (action) {
            case "up" -> IsoNavigationSession.ACTION_UP;
            case "down" -> IsoNavigationSession.ACTION_DOWN;
            case "left" -> IsoNavigationSession.ACTION_LEFT;
            case "right" -> IsoNavigationSession.ACTION_RIGHT;
            case "select" -> IsoNavigationSession.ACTION_SELECT;
            case "menu" -> IsoNavigationSession.ACTION_TOP_MENU;
            case "title-menu" -> IsoNavigationSession.ACTION_TITLE_MENU;
            case "popup" -> IsoNavigationSession.ACTION_POPUP;
            case "prev" -> IsoNavigationSession.ACTION_BACK;
            default -> -1;
        };
    }

    @Override
    public boolean supportsPointer() {
        return navigation.isStarted();
    }

    @Override
    public boolean sendPointer(float x, float y, boolean activate) {
        return navigation.sendPointer(x, y, activate);
    }

    public boolean seekDiscChapter(int chapterIndex) {
        return navigation.seekToChapter(chapterIndex);
    }

    public void syncDiscTrack(Track track) {
        if (!navigation.isStarted() || (track.getType() != C.TRACK_TYPE_AUDIO && track.getType() != C.TRACK_TYPE_TEXT)) return;
        if (track.getType() == C.TRACK_TYPE_TEXT && !track.isSelected()) {
            navigation.disableSubtitles();
            return;
        }
        if (!track.isSelected() || track.getFormat() == null) return;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != track.getType()) continue;
            for (int i = 0; i < group.length; i++) {
                Format format = group.getTrackFormat(i);
                if (track.getFormat().equals(TrackUtil.describeFormat(format))) {
                    navigation.selectTrack(track.getType(), format);
                    return;
                }
            }
        }
    }

    @Nullable
    @Override
    public IsoNavigationSession.MenuOverlay getHdmvOverlay(int previousVersion) {
        return navigation.getHdmvMenuOverlay(previousVersion);
    }

    @Nullable
    @Override
    public IsoNavigationSession.MenuHighlight getDvdHighlight() {
        return navigation.getDvdMenuHighlight();
    }

    @Override
    public int getAudioChannelCount() {
        Format format = player.getAudioFormat();
        return format == null ? Format.NO_VALUE : format.channelCount;
    }

    @Override
    public PlayerEffect getEffect() {
        return effect;
    }

    @Override
    public void release() {
        subtitles.release();
        player.removeAnalyticsListener(this);
        preload.release();
        effect.release();
        closeDiscMenu();
        session.release();
    }

    @Override
    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return getDecoderMode(trackType) == null ? List.of() : List.of(DecoderMode.values());
    }

    @Override
    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        androidx.media3.exoplayer.DecoderMode mode = session.decoderMode(trackType);
        return mode == null ? null : mode.toCommonDecoderMode();
    }

    @Override
    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        session.setDecoderMode(trackType, androidx.media3.exoplayer.DecoderMode.fromCommonDecoderMode(mode));
    }

    @Override
    public boolean switchDecoderForRetry(PlaybackException exception) {
        return session.switchDecoderForRetry(exception);
    }

    @Override
    public void resetDecoderFallback(@C.TrackType int trackType) {
        session.resetDecoderFallback(trackType);
    }

    @Override
    public void prepareForNewMedia() {
        session.prepareForNewMedia();
        subtitles.prepareForNewMedia();
    }

    @Override
    public void start(PlaySpec spec, long startPositionMs) {
        this.spec = spec;
        startInternal(startPositionMs);
    }

    @Override
    public void preload(PlaySpec spec, long startPositionMs) {
        session.preload(MediaItemFactory.from(spec), startPositionMs);
    }

    @Override
    public void clearPreload() {
        session.clearPreload();
    }

    @Override
    public void bindPlayerView(PlayerView playerView) {
        subtitles.bindPlayerView(playerView);
    }

    @Override
    public void applySubtitleStyle() {
        subtitles.applySubtitleStyle();
    }

    @Override
    public SubtitleSelectionState getSubtitleSelectionState() {
        return subtitles.getSubtitleSelectionState();
    }

    @Override
    public SubtitleContent getSubtitleContent(TrackSelectionOverride selection) {
        SubtitleSelectionState state = subtitles.getSubtitleSelectionState();
        boolean isPrimary = state.isPrimary(selection);
        if (!isPrimary && !state.isActiveSecondary(selection)) return SubtitleContent.UNSUPPORTED;
        Format format = selection.mediaTrackGroup.getFormat(selection.trackIndices.get(0));
        SubtitleOffsets offsets = getSubtitleOffsets();
        return session.subtitleTranscriptSession().getContent(format, isPrimary ? offsets.primaryMs : offsets.secondaryMs);
    }

    @Override
    public SubtitleOffsets getSubtitleOffsets() {
        return player.getSubtitleOffsets();
    }

    @Override
    public void setSubtitleOffsets(SubtitleOffsets offsets) {
        player.setSubtitleOffsets(offsets);
    }

    @Override
    public boolean supportsSubtitleOffsets() {
        return true;
    }

    @Override
    public boolean supportsSubtitleTranscript() {
        return true;
    }

    @Override
    public void setSubtitleContentEnabled(boolean enabled) {
        session.subtitleTranscriptSession().setContentEnabled(enabled);
    }

    @Override
    public boolean canRetrySubtitleContent(Format format) {
        return session.subtitleTranscriptSession().canRetryContent(format);
    }

    @Override
    public void retrySubtitleContent(Format format) {
        session.subtitleTranscriptSession().retryContent(format);
    }

    @Override
    public void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        subtitles.setSecondarySubtitleSelection(selection);
    }

    @Override
    public void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        subtitles.restoreSecondarySubtitleSelection(selection);
    }

    @Override
    public void stop() {
        preload.stop();
        player.stop();
        closeDiscMenu();
    }

    @Override
    public String getErrorMessage(PlaybackException e) {
        return provider.get(e);
    }

    @Override
    public ErrorAction handleError(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> seekToDefaultPosition();
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> ErrorAction.DECODE;
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> ErrorAction.REFRESH;
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> retryFormat(e.errorCode);
            default -> ErrorAction.FATAL;
        };
    }

    private void startInternal(long position) {
        MediaItem item = MediaItemFactory.from(spec);
        MediaSource source = session.usePreloadedMediaSource(item);
        effect.clearAudioEffect();
        if (source == null) player.setMediaItem(item, position);
        else player.setMediaSource(source, position);
        closeDiscMenu();
        preload.start(player, item);
        prepareAndPlay();
    }

    private boolean openDiscMenu(String action) {
        if (!hasMenu()) return false;
        session.clearPreload();
        int initialMenuAction = switch (action) {
            case "title-menu" -> IsoNavigationSession.ACTION_TITLE_MENU;
            case "popup" -> IsoNavigationSession.ACTION_POPUP;
            default -> IsoNavigationSession.ACTION_TOP_MENU;
        };
        IsoNavigationMediaSource source = new IsoNavigationMediaSource(MediaItemFactory.from(spec), ExoMediaSourceFactory.createDiscIsoDataSourceFactory(spec.getHeaders()), initialMenuAction);
        navigation.start(source);
        return true;
    }

    private void closeDiscMenu() {
        navigation.close();
    }

    private static boolean isIso(@Nullable PlaySpec spec) {
        if (spec == null) return false;
        if (MimeTypes.VIDEO_ISO.equals(spec.getFormat())) return true;
        Uri uri = spec.getUri();
        String path = uri != null ? uri.getPath() : null;
        return path != null && path.toLowerCase(Locale.US).endsWith(".iso");
    }

    private void prepareAndPlay() {
        player.prepare();
        player.play();
    }

    private ErrorAction seekToDefaultPosition() {
        player.seekToDefaultPosition();
        player.prepare();
        return ErrorAction.RECOVERED;
    }

    private ErrorAction retryFormat(int errorCode) {
        spec.setFormat(ExoUtil.getMimeType(errorCode));
        startInternal(player.getCurrentPosition());
        return ErrorAction.RECOVERED;
    }
}
