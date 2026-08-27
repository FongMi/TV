-dontwarn android.content.res.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn java.lang.ClassValue
-dontwarn java.lang.SafeVarargs
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.ietf.jgss.**
-dontwarn org.kxml2.io.**
-dontwarn org.xmlpull.v1.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.conscrypt.**
-dontwarn javax.**
-dontwarn okio.**

-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }

-keepclassmembernames class com.google.common.base.Function { *; }

-keep class com.hierynomus.** { *; }
-keep class net.engio.mbassy.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

# libmedia3ass resolves these classes and constructors by their binary names.
-keep class androidx.media3.exoplayer.libass.LibassNative { *; }
-keep class androidx.media3.exoplayer.libass.LibassFrame { *; }
-keep class androidx.media3.exoplayer.libass.LibassImage { *; }

-keep, includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
  private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int, int);
}

-keep class is.xyz.mpv.MPVLib { *; }

-keep class androidx.media3.decoder.VideoDecoderOutputBuffer { *; }
-keep class androidx.media3.decoder.DecoderInputBuffer { *; }
-keep class androidx.media3.decoder.av1.Dav1dDecoder { *; }
-keep class androidx.media3.decoder.SimpleDecoderOutputBuffer { *; }
-keep class androidx.media3.decoder.mpegh** { *; }

-keepclassmembers class androidx.media3.datasource.RawResourceDataSource {
  public static android.net.Uri buildRawResourceUri(int);
}

-dontnote androidx.media3.datasource.rtmp.RtmpDataSource
-keepclassmembers class androidx.media3.datasource.rtmp.RtmpDataSource {
  <init>();
}

-dontnote androidx.media3.decoder.vp9.LibvpxVideoRenderer
-keepclassmembers class androidx.media3.decoder.vp9.LibvpxVideoRenderer {
  <init>(long, android.os.Handler, androidx.media3.exoplayer.video.VideoRendererEventListener, int);
}

-dontnote androidx.media3.decoder.av1.Libdav1dVideoRenderer
-keepclassmembers class androidx.media3.decoder.av1.Libdav1dVideoRenderer {
  <init>(long, android.os.Handler, androidx.media3.exoplayer.video.VideoRendererEventListener, int);
}

-dontnote androidx.media3.decoder.ffmpeg.FfmpegVideoRenderer
-keepclassmembers class androidx.media3.decoder.ffmpeg.FfmpegVideoRenderer {
  <init>(android.content.Context, long, android.os.Handler, androidx.media3.exoplayer.video.VideoRendererEventListener, int);
}

-dontnote androidx.media3.decoder.ffmpeg.FfmpegDolbyVisionP7Converter$Factory
-keep class androidx.media3.decoder.ffmpeg.FfmpegDolbyVisionP7Converter$Factory {
  <init>();
}

-dontnote androidx.media3.decoder.opus.LibopusAudioRenderer
-keepclassmembers class androidx.media3.decoder.opus.LibopusAudioRenderer {
  <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-dontnote androidx.media3.decoder.flac.LibflacAudioRenderer
-keepclassmembers class androidx.media3.decoder.flac.LibflacAudioRenderer {
  <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-dontnote androidx.media3.decoder.iamf.IamfAudioRenderer$Builder
-keepclasseswithmembers class androidx.media3.decoder.iamf.IamfAudioRenderer$Builder {
  <init>(androidx.media3.exoplayer.audio.AudioSink);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setEventHandlerAndListener(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setOutputSampleType(int);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setChannelOrdering(int);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setRequestedOutputLayout(int);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setRequestedMixPresentationId(long);
  androidx.media3.decoder.iamf.IamfAudioRenderer$Builder setEnableIntegratedBinaural(boolean);
  androidx.media3.decoder.iamf.IamfAudioRenderer build();
}

-dontnote androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
-keepclassmembers class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer {
  <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-dontnote androidx.media3.decoder.midi.MidiRenderer
-keepclassmembers class androidx.media3.decoder.midi.MidiRenderer {
  <init>(android.content.Context, android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-dontnote androidx.media3.decoder.mpegh.MpeghAudioRenderer
-keepclassmembers class androidx.media3.decoder.mpegh.MpeghAudioRenderer {
  <init>(android.os.Handler, androidx.media3.exoplayer.audio.AudioRendererEventListener, androidx.media3.exoplayer.audio.AudioSink);
}

-dontnote androidx.media3.exoplayer.dash.offline.DashDownloader$Factory
-keepclassmembers class androidx.media3.exoplayer.dash.offline.DashDownloader$Factory {
  <init>(androidx.media3.datasource.cache.CacheDataSource$Factory);
}

-dontnote androidx.media3.exoplayer.hls.offline.HlsDownloader$Factory
-keepclassmembers class androidx.media3.exoplayer.hls.offline.HlsDownloader$Factory {
  <init>(androidx.media3.datasource.cache.CacheDataSource$Factory);
}

-dontnote androidx.media3.exoplayer.smoothstreaming.offline.SsDownloader$Factory
-keepclassmembers class androidx.media3.exoplayer.smoothstreaming.offline.SsDownloader$Factory {
  <init>(androidx.media3.datasource.cache.CacheDataSource$Factory);
}

-dontnote androidx.media3.exoplayer.dash.DashMediaSource$Factory
-keepclasseswithmembers class androidx.media3.exoplayer.dash.DashMediaSource$Factory {
  <init>(androidx.media3.datasource.DataSource$Factory);
}

-dontnote androidx.media3.exoplayer.hls.HlsMediaSource$Factory
-keepclasseswithmembers class androidx.media3.exoplayer.hls.HlsMediaSource$Factory {
  <init>(androidx.media3.datasource.DataSource$Factory);
}

-dontnote androidx.media3.exoplayer.smoothstreaming.SsMediaSource$Factory
-keepclasseswithmembers class androidx.media3.exoplayer.smoothstreaming.SsMediaSource$Factory {
  <init>(androidx.media3.datasource.DataSource$Factory);
}

-dontnote androidx.media3.exoplayer.rtsp.RtspMediaSource$Factory
-keepclasseswithmembers class androidx.media3.exoplayer.rtsp.RtspMediaSource$Factory {
  <init>();
}

-if class * implements androidx.media3.exoplayer.ExoPlayer {
    public void setVideoEffects(java.util.List);
}
-keepclasseswithmembers class androidx.media3.effect.SingleInputVideoGraph$Factory {
  <init>(androidx.media3.common.VideoFrameProcessor$Factory);
}

-if class * implements androidx.media3.exoplayer.ExoPlayer {
    public void setVideoEffects(java.util.List);
}
-keepclasseswithmembers class androidx.media3.effect.DefaultVideoFrameProcessor$Factory$Builder {
  <init>();
  androidx.media3.effect.DefaultVideoFrameProcessor$Factory build();
  androidx.media3.effect.DefaultVideoFrameProcessor$Factory$Builder setEnableReplayableCache(boolean);
  androidx.media3.effect.DefaultVideoFrameProcessor$Factory$Builder setOutputSurfaceSizeAdjustmentEnabled(boolean);
}

-dontnote androidx.media3.effect.SingleInputVideoGraph$Factory
-dontnote androidx.media3.effect.DefaultVideoFrameProcessor$Factory$Builder

-dontnote androidx.media3.decoder.flac.FlacExtractor
-keepclassmembers class androidx.media3.decoder.flac.FlacExtractor {
  <init>(int);
}

-dontnote androidx.media3.decoder.flac.FlacLibrary
-keepclassmembers class androidx.media3.decoder.flac.FlacLibrary {
  public static boolean isAvailable();
}

-dontnote androidx.media3.decoder.midi.MidiExtractor
-keepclassmembers class androidx.media3.decoder.midi.MidiExtractor {
  <init>();
}

-dontnote androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView
-keepclassmembers class androidx.media3.exoplayer.video.spherical.SphericalGLSurfaceView {
  <init>(android.content.Context);
}

-dontnote androidx.media3.exoplayer.video.VideoDecoderGLSurfaceView
-keepclassmembers class androidx.media3.exoplayer.video.VideoDecoderGLSurfaceView {
  <init>(android.content.Context);
}

-keepnames class androidx.media3.exoplayer.ExoPlayer {}
-keepclassmembers class androidx.media3.exoplayer.ExoPlayer {
  void setImageOutput(androidx.media3.exoplayer.image.ImageOutput);
  void setScrubbingModeEnabled(boolean);
  boolean isScrubbingModeEnabled();
}

-keepclasseswithmembers class androidx.media3.exoplayer.image.ImageOutput {
  void onImageAvailable(long, android.graphics.Bitmap);
}

-dontnote androidx.media3.exoplayer.util.DebugTextViewHelper
-keep class androidx.media3.exoplayer.util.DebugTextViewHelper {
  <init>(androidx.media3.exoplayer.ExoPlayer, android.widget.TextView);
  void start();
  void stop();
}

-dontnote androidx.media3.mpvplayer.MpvPlayer
-keepclassmembers class androidx.media3.mpvplayer.MpvPlayer {
  boolean toggleGeneralStats();
  void setOsdSurfaceView(android.view.SurfaceView);
  void clearOsdSurfaceView(android.view.SurfaceView);
}

-keepnames class androidx.media3.transformer.CompositionPlayer {}
-keepclassmembers class androidx.media3.transformer.CompositionPlayer {
  void setScrubbingModeEnabled(boolean);
  boolean isScrubbingModeEnabled();
}

-dontnote androidx.appcompat.app.AlertDialog.Builder
-keepclassmembers class androidx.appcompat.app.AlertDialog$Builder {
  <init>(android.content.Context, int);
  public android.content.Context getContext();
  public androidx.appcompat.app.AlertDialog$Builder setTitle(java.lang.CharSequence);
  public androidx.appcompat.app.AlertDialog$Builder setView(android.view.View);
  public androidx.appcompat.app.AlertDialog$Builder setPositiveButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog$Builder setNegativeButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog create();
}
