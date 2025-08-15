package com.fongmi.android.tv.player;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 综合视频播放优化系统（新增类）
 * 功能：设备检测/格式分析/刷新率优化/性能监控
 */
public class VideoOptimizationSystem {

    private static final String TAG = "VideoOptimizer";
    private static VideoOptimizationSystem instance;
    private DeviceProfile deviceProfile;
    private RefreshRateOptimizer refreshRateOptimizer;
    private float currentFrameRate = -1;
    private PerformanceMonitor performanceMonitor;
    private Context context;

    private VideoOptimizationSystem(Context context) {
        this.context = context;
        this.refreshRateOptimizer = new RefreshRateOptimizer();
    }

    public static synchronized VideoOptimizationSystem getInstance(Context context) {
        if (instance == null) {
            instance = new VideoOptimizationSystem(context);
        }
        return instance;
    }

    public void initialize(Activity activity) {
        deviceProfile = detectDeviceCapabilities(activity);
        Log.i(TAG, "设备能力检测完成: " + deviceProfile);
    }

    public MediaSource prepareMediaSource(Uri videoUri) {
        Format videoFormat = probeVideoFormat(context, videoUri);
        Log.i(TAG, "视频格式分析: " + videoFormat);
        
        MediaSource originalSource = buildMediaSource(context, videoUri);
        MediaSource adaptedSource = adaptMediaSource(originalSource, videoFormat);
        
        return adaptedSource;
    }

    public void configurePlayer(ExoPlayer player, Format videoFormat) {
        LoadControl loadControl = createAdaptiveLoadControl(videoFormat);
        player.setLoadControl(loadControl);
        
        performanceMonitor = new PerformanceMonitor(player);
        performanceMonitor.start();
    }

    public void onVideoFormatChanged(int width, int height, int rotation, float frameRate) {
        if (frameRate <= 0) return;
        
        if (Math.abs(frameRate - currentFrameRate) > 0.1f) {
            currentFrameRate = frameRate;
            
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                refreshRateOptimizer.optimize(activity, currentFrameRate);
            }
        }
    }

    public void onPlayerStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                refreshRateOptimizer.restore(activity);
            }
        }
        
        if (state == Player.STATE_BUFFERING) {
            handleBufferingEvent();
        }
    }

    public void onPause() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            refreshRateOptimizer.restore(activity);
        }
    }

    public void onDestroy() {
        if (performanceMonitor != null) {
            performanceMonitor.stop();
        }
        
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            refreshRateOptimizer.restore(activity);
        }
    }

    // 设备能力配置类
    public static class DeviceProfile {
        public int maxWidth;
        public int maxHeight;
        public float maxFrameRate;
        public int maxBitrate; // Mbps
        public List<String> supportedCodecs = new ArrayList<>();
        public boolean hdrSupport;
        public int ramSizeGB;
        
        @Override
        public String toString() {
            return String.format("DeviceProfile: %dx%d@%.1fHz, Bitrate: %dMbps, Codecs: %s, HDR: %b",
                    maxWidth, maxHeight, maxFrameRate, maxBitrate, supportedCodecs, hdrSupport);
        }
    }

    // 刷新率优化器（核心功能）
    private class RefreshRateOptimizer {
        private static final float TOLERANCE = 0.01f;
        private static final int MAX_MULTIPLE = 5;
        private static final long DEBOUNCE_DELAY_MS = 500;
        
        private int originalModeId = -1;
        private float lastOptimizedRate = -1;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private DisplayManager displayManager;
        private DisplayManager.DisplayListener displayListener;

        public void optimize(Activity activity, float videoFrameRate) {
            if (activity == null || activity.isFinishing()) return;
            
            handler.removeCallbacksAndMessages(null);
            if (Math.abs(videoFrameRate - lastOptimizedRate) < TOLERANCE) return;
            
            handler.postDelayed(() -> {
                try {
                    if (displayManager == null) {
                        displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
                    }
                    
                    Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
                    if (display == null) return;
                    
                    Display.Mode currentMode = display.getMode();
                    if (originalModeId == -1) originalModeId = currentMode.getModeId();
                    
                    Display.Mode[] modes = display.getSupportedModes();
                    if (modes == null || modes.length == 0) return;
                    
                    Display.Mode bestMode = findBestMode(modes, videoFrameRate);
                    if (bestMode == null || bestMode.getModeId() == currentMode.getModeId()) return;
                    
                    Log.i(TAG, "切换刷新率: " + currentMode.getRefreshRate() + "Hz → " + 
                          bestMode.getRefreshRate() + "Hz for " + videoFrameRate + "fps");
                    
                    setPreferredMode(activity.getWindow(), bestMode.getModeId());
                    lastOptimizedRate = videoFrameRate;
                    
                    registerDisplayListener(activity, display);
                } catch (Exception e) {
                    Log.e(TAG, "刷新率优化错误", e);
                }
            }, DEBOUNCE_DELAY_MS);
        }

        public void restore(Activity activity) {
            if (originalModeId == -1 || activity == null || activity.isFinishing()) return;
            
            try {
                Display display = activity.getDisplay();
                if (display == null) return;
                
                Display.Mode currentMode = display.getMode();
                if (currentMode.getModeId() == originalModeId) return;
                
                Log.i(TAG, "恢复原始刷新率: " + currentMode.getRefreshRate() + "Hz → " + 
                      getRefreshRateById(display, originalModeId) + "Hz");
                
                setPreferredMode(activity.getWindow(), originalModeId);
                originalModeId = -1;
                lastOptimizedRate = -1;
            } catch (Exception e) {
                Log.e(TAG, "恢复刷新率错误", e);
            } finally {
                unregisterDisplayListener();
            }
        }

        // 核心算法：匹配最佳刷新率
        private Display.Mode findBestMode(Display.Mode[] modes, float fps) {
            Set<Float> uniqueRates = new HashSet<>();
            for (Display.Mode mode : modes) uniqueRates.add(mode.getRefreshRate());
            List<Float> sortedRates = new ArrayList<>(uniqueRates);
            Collections.sort(sortedRates);
            
            Float bestRate = null;
            
            // 1. 精确匹配（±0.01Hz）
            for (float rate : sortedRates) {
                if (Math.abs(rate - fps) < TOLERANCE) {
                    bestRate = rate;
                    break;
                }
            }
            
            // 2. 整数倍匹配（2×/3×帧率）
            if (bestRate == null) {
                for (int k = 2; k <= MAX_MULTIPLE; k++) {
                    float target = fps * k;
                    for (float rate : sortedRates) {
                        if (Math.abs(rate - target) < TOLERANCE) {
                            bestRate = rate;
                            break;
                        }
                    }
                    if (bestRate != null) break;
                }
            }
            
            // 3. 最高刷新率兜底
            if (bestRate == null && !sortedRates.isEmpty()) {
                bestRate = sortedRates.get(sortedRates.size() - 1);
            }
            
            // 4. 选择最佳模式
            if (bestRate == null) return null;
            Display.Mode bestMode = null;
            for (Display.Mode mode : modes) {
                if (Math.abs(mode.getRefreshRate() - bestRate) < TOLERANCE) {
                    if (bestMode == null || 
                        mode.getPhysicalWidth() * mode.getPhysicalHeight() > 
                        bestMode.getPhysicalWidth() * bestMode.getPhysicalHeight()) {
                        bestMode = mode;
                    }
                }
            }
            return bestMode;
        }

        private void setPreferredMode(Window window, int modeId) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.setPreferredDisplayModeId(modeId);
                } else {
                    Method method = Window.class.getMethod("setPreferredDisplayModeId", int.class);
                    method.invoke(window, modeId);
                }
            } catch (Exception e) {
                Log.e(TAG, "设置显示模式失败", e);
            }
        }

        private void registerDisplayListener(Context context, Display display) {
            if (displayListener != null || displayManager == null) return;
            
            displayListener = new DisplayManager.DisplayListener() {
                @Override public void onDisplayAdded(int displayId) {}
                @Override public void onDisplayRemoved(int displayId) {}
                
                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId != Display.DEFAULT_DISPLAY) return;
                    Display currentDisplay = displayManager.getDisplay(displayId);
                    if (currentDisplay != null) {
                        Log.i(TAG, "显示设备变化. 当前刷新率: " + currentDisplay.getMode().getRefreshRate() + "Hz");
                    }
                }
            };
            displayManager.registerDisplayListener(displayListener, new Handler(Looper.getMainLooper()));
        }
        
        private void unregisterDisplayListener() {
            if (displayListener != null && displayManager != null) {
                displayManager.unregisterDisplayListener(displayListener);
                displayListener = null;
            }
        }
        
        private float getRefreshRateById(Display display, int modeId) {
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getModeId() == modeId) return mode.getRefreshRate();
            }
            return -1;
        }
    }

    // 性能监控器
    private class PerformanceMonitor {
        private static final long MONITOR_INTERVAL = 5000; // 5秒监控间隔
        
        private final ExoPlayer player;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable monitorRunnable = this::monitorPerformance;
        
        private int frameDropCount;
        private int lastDroppedFrames;
        private int bufferUnderrunCount;
        
        public PerformanceMonitor(ExoPlayer player) {
            this.player = player;
        }
        
        public void start() {
            handler.postDelayed(monitorRunnable, MONITOR_INTERVAL);
        }
        
        public void stop() {
            handler.removeCallbacks(monitorRunnable);
        }
        
        private void monitorPerformance() {
            int droppedFrames = player.getVideoFormat() != null ? 
                player.getVideoFormat().getInteger(MediaFormat.KEY_DROP_FRAMES, 0) : 0;
            int currentDrop = droppedFrames - lastDroppedFrames;
            lastDroppedFrames = droppedFrames;
            
            // 检测帧丢弃
            if (currentDrop > 10) {
                frameDropCount++;
                Log.w(TAG, "检测到帧丢弃: " + currentDrop + " 帧");
            }
            
            // 检测缓冲区下溢
            if (player.getPlaybackState() == Player.STATE_BUFFERING) {
                bufferUnderrunCount++;
            }
            
            // 性能熔断机制
            if (frameDropCount > 3 || bufferUnderrunCount > 2) {
                adjustPlaybackStrategy();
                frameDropCount = 0;
                bufferUnderrunCount = 0;
            }
            
            handler.postDelayed(monitorRunnable, MONITOR_INTERVAL);
        }
        
        private void adjustPlaybackStrategy() {
            Log.w(TAG, "性能下降，调整播放策略");
            
            // 1. 降低播放速度
            player.setPlaybackParameters(new PlaybackParameters(0.95f));
            
            // 2. 显示优化提示
            showToast("正在优化播放以解决卡顿问题");
        }
    }

    // 设备能力检测
    private DeviceProfile detectDeviceCapabilities(Activity activity) {
        DeviceProfile profile = new DeviceProfile();
        
        // 1. 显示能力检测
        detectDisplayCapabilities(activity, profile);
        
        // 2. 编解码能力检测
        detectCodecCapabilities(profile);
        
        // 3. 性能能力检测
        detectPerformanceCapabilities(profile);
        
        return profile;
    }
    
    private void detectDisplayCapabilities(Activity activity, DeviceProfile profile) {
        Display display = activity.getDisplay();
        if (display == null) return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        profile.maxWidth = metrics.widthPixels;
        profile.maxHeight = metrics.heightPixels;
        
        float maxRefreshRate = 60f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Display.Mode[] modes = display.getSupportedModes();
            for (Display.Mode mode : modes) {
                if (mode.getRefreshRate() > maxRefreshRate) {
                    maxRefreshRate = mode.getRefreshRate();
                }
            }
        }
        profile.maxFrameRate = maxRefreshRate;
    }
    
    private void detectCodecCapabilities(DeviceProfile profile) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        
        for (MediaCodecInfo info : codecInfos) {
            if (info.isEncoder()) continue;
            
            for (String type : info.getSupportedTypes()) {
                if (!profile.supportedCodecs.contains(type)) {
                    profile.supportedCodecs.add(type);
                }
            }
        }
        
        // 检测HDR支持
        profile.hdrSupport = profile.supportedCodecs.contains("video/hevc");
    }
    
    private void detectPerformanceCapabilities(DeviceProfile profile) {
        // 设备分级策略
        if (profile.maxWidth >= 3840) {
            profile.maxBitrate = 120; // 4K设备
        } else if (profile.maxWidth >= 2560) {
            profile.maxBitrate = 60; // 2K设备
        } else {
            profile.maxBitrate = 40; // 1080p设备
        }
        
        // 估算RAM大小
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            long totalMem = Runtime.getRuntime().totalMemory();
            profile.ramSizeGB = (int) (totalMem / (1024 * 1024 * 1024));
        } else {
            profile.ramSizeGB = 2; // 默认2GB
        }
    }
    
    // 视频格式探测
    private Format probeVideoFormat(Context context, Uri uri) {
        MediaExtractor extractor = new MediaExtractor();
        Format format = new Format();
        
        try {
            extractor.setDataSource(context, uri, null);
            
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                
                if (mime != null && mime.startsWith("video/")) {
                    if (trackFormat.containsKey(MediaFormat.KEY_WIDTH)) {
                        format.width = trackFormat.getInteger(MediaFormat.KEY_WIDTH);
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
                        format.height = trackFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        format.frameRate = trackFormat.getFloat(MediaFormat.KEY_FRAME_RATE);
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        format.bitrate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                    }
                    format.sampleMimeType = mime;
                    
                    if (trackFormat.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        format.colorTransfer = trackFormat.getInteger(MediaFormat.KEY_COLOR_TRANSFER);
                    }
                    
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "视频格式探测错误", e);
        } finally {
            extractor.release();
        }
        
        Log.i(TAG, "探测到视频格式: " + 
              format.width + "x" + format.height + "@" + format.frameRate + "fps, " +
              format.sampleMimeType + ", " + (format.bitrate/1000000) + "Mbps");
        
        return format;
    }
    
    // 创建媒体源
    private MediaSource buildMediaSource(Context context, Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
            context, Util.getUserAgent(context, "FongmiTV"));
        
        switch (Util.inferContentType(uri)) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default:
                throw new IllegalStateException("不支持的类型");
        }
    }
    
    // 适配媒体源
    private MediaSource adaptMediaSource(MediaSource source, Format format) {
        // 在实际项目中，这里可以添加转码、调整分辨率等逻辑
        if (needsResolutionAdjustment(format)) {
            Log.w(TAG, "视频分辨率超出设备能力，建议转码: " + 
                  format.width + "x" + format.height + " > " + 
                  deviceProfile.maxWidth + "x" + deviceProfile.maxHeight);
        }
        
        if (needsFrameRateAdjustment(format)) {
            Log.w(TAG, "视频帧率超出设备能力，建议转码: " + 
                  format.frameRate + "fps > " + deviceProfile.maxFrameRate + "fps");
        }
        
        if (needsTranscoding(format)) {
            Log.w(TAG, "视频编码超出设备支持，建议转码: " + 
                  format.sampleMimeType + " not in " + deviceProfile.supportedCodecs);
        }
        
        return source;
    }
    
    private boolean needsResolutionAdjustment(Format format) {
        return format.width > deviceProfile.maxWidth || format.height > deviceProfile.maxHeight;
    }
    
    private boolean needsFrameRateAdjustment(Format format) {
        return format.frameRate > deviceProfile.maxFrameRate;
    }
    
    private boolean needsTranscoding(Format format) {
        if (format.sampleMimeType == null) return false;
        
        if (!deviceProfile.supportedCodecs.contains(format.sampleMimeType.toLowerCase())) {
            return true;
        }
        
        if (isHdrFormat(format) && !deviceProfile.hdrSupport) {
            return true;
        }
        
        if (format.bitrate > deviceProfile.maxBitrate * 1000000) {
            return true;
        }
        
        return false;
    }
    
    private boolean isHdrFormat(Format format) {
        return format.colorTransfer == Format.COLOR_TRANSFER_HLG || 
               format.colorTransfer == Format.COLOR_TRANSFER_ST2084;
    }
    
    // 创建自适应缓冲区控制（智能缓冲管理）
    private LoadControl createAdaptiveLoadControl(Format format) {
        int baseBuffer = 15; // 默认15秒
        
        // 高分辨率视频需要更大缓冲区
        if (format.width > 1920 || format.height > 1080) {
            baseBuffer += 10;
        }
        
        // 高帧率视频需要更大缓冲区
        if (format.frameRate > 60) {
            baseBuffer += 5;
        }
        
        // 低内存设备减少缓冲区
        if (deviceProfile.ramSizeGB < 2) {
            baseBuffer = Math.min(baseBuffer, 10);
        }
        
        Log.i(TAG, "设置自适应缓冲区: " + baseBuffer + "秒");
        
        return new DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                baseBuffer * 1000, // minBuffer
                baseBuffer * 1000, // maxBuffer
                5000, // minStartBuffer
                5000) // minResumeBuffer
            .build();
    }
    
    // 处理缓冲事件
    private void handleBufferingEvent() {
        Log.w(TAG, "检测到缓冲，正在优化播放...");
        showToast("正在优化播放以解决卡顿问题");
    }
    
    // 显示Toast消息
    private void showToast(String message) {
        if (context instanceof VideoActivity) {
            ((VideoActivity) context).runOnUiThread(() -> 
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }
    }
}
