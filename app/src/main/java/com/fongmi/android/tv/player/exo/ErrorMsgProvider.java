package com.fongmi.android.tv.player.exo;

import android.content.res.Resources;

import androidx.media3.common.PlaybackException;
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class ErrorMsgProvider {

    private final Resources resources;

    public ErrorMsgProvider() {
        this.resources = App.get().getResources();
    }

    public String get(PlaybackException e) {
        if (e.getCause() instanceof MediaCodecRenderer.DecoderInitializationException e2) return custom(e2);
        return resources.getString(getId(e.errorCode));
    }

    private String custom(MediaCodecRenderer.DecoderInitializationException e) {
        if (e.codecInfo != null) return resources.getString(R.string.error_instantiating_decoder, e.codecInfo.name);
        if (e.getCause() instanceof MediaCodecUtil.DecoderQueryException) return resources.getString(R.string.error_querying_decoders);
        else if (e.secureDecoderRequired) return resources.getString(R.string.error_no_secure_decoder, e.mimeType);
        else return resources.getString(R.string.error_no_decoder, e.mimeType);
    }

    private int getId(int errorCode) {
        return switch (errorCode) {
            case PlaybackException.ERROR_CODE_REMOTE_ERROR -> R.string.error_remote_error;
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> R.string.error_behind_live_window;
            case PlaybackException.ERROR_CODE_TIMEOUT -> R.string.error_timeout;
            case PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> R.string.error_failed_runtime_check;
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> R.string.error_io_unspecified;
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> R.string.error_io_network_connection_failed;
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> R.string.error_io_network_connection_timeout;
            case PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> R.string.error_io_invalid_http_content_type;
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> R.string.error_io_bad_http_status;
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> R.string.error_io_file_not_found;
            case PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> R.string.error_io_no_permission;
            case PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> R.string.error_io_cleartext_not_permitted;
            case PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> R.string.error_io_read_position_out_of_range;
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> R.string.error_parsing_container_malformed;
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> R.string.error_parsing_manifest_malformed;
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> R.string.error_parsing_container_unsupported;
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> R.string.error_parsing_manifest_unsupported;
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> R.string.error_decoder_init_failed;
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> R.string.error_decoder_query_failed;
            case PlaybackException.ERROR_CODE_DECODING_FAILED -> R.string.error_decoding_failed;
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> R.string.error_decoding_format_exceeds_capabilities;
            case PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> R.string.error_decoding_format_unsupported;
            case PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED -> R.string.error_decoding_resources_reclaimed;
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> R.string.error_audio_track_init_failed;
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> R.string.error_audio_track_write_failed;
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED -> R.string.error_audio_track_offload_write_failed;
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED -> R.string.error_audio_track_offload_init_failed;
            case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> R.string.error_drm_unspecified;
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> R.string.error_drm_provisioning_failed;
            case PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> R.string.error_drm_content_error;
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> R.string.error_drm_license_acquisition_failed;
            case PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> R.string.error_drm_disallowed_operation;
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> R.string.error_drm_system_error;
            case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> R.string.error_drm_device_revoked;
            case PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> R.string.error_drm_license_expired;
            case PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED -> R.string.error_video_frame_processor_init_failed;
            case PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED -> R.string.error_video_frame_processing_failed;
            default -> R.string.error_unspecified;
        };
    }
}
