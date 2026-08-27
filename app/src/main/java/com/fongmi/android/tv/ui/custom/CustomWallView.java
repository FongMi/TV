package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.palette.graphics.Palette;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewWallBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.utils.Path;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class CustomWallView extends FrameLayout implements DefaultLifecycleObserver {

    private static final int[] WALL_PAPERS = {0, R.drawable.wallpaper_1, R.drawable.wallpaper_2, R.drawable.wallpaper_3, R.drawable.wallpaper_4};
    private static final int[] WALL_COLORS = {0, 0xFF40C090, 0xFF4870E0, 0xFF48B0C0, 0xFF404040};
    private static final int TYPE_RES = 0;
    private static final int TYPE_GIF = 1;
    private static final int TYPE_VIDEO = 2;
    private ViewWallBinding binding;
    private GifDrawable drawable;
    private PlayerView video;
    private ExoPlayer player;

    public CustomWallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) return;
        binding = ViewWallBinding.inflate(LayoutInflater.from(getContext()), this, true);
        ((ComponentActivity) getContext()).getLifecycle().addObserver(this);
        refresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (event.type() == ConfigEvent.Type.WALL) refresh();
    }

    private void refresh() {
        stop();
        load();
        theme();
    }

    private void stop() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.clearMediaItems();
        }
        if (video != null) {
            video.setPlayer(null);
            video.setVisibility(GONE);
        }
        if (drawable != null) {
            drawable.stop();
            drawable.recycle();
            drawable = null;
        }
    }

    private void load() {
        int wall = Setting.getWall();
        int type = Setting.getWallType();
        if (isBuiltIn(wall, type)) loadRes(WALL_PAPERS[wall]);
        else if (type == TYPE_VIDEO) loadVideo(Path.wall(wall));
        else if (type == TYPE_GIF) loadGif(Path.wall(wall));
        else loadImage();
    }

    private void theme() {
        int newColor = getWallColor();
        int oldColor = Setting.getWallColor();
        if (newColor == oldColor) return;
        Setting.putWallColor(newColor);
        if (Setting.getThemeColor() == 0) RefreshEvent.theme();
    }

    private void loadRes(int resId) {
        binding.image.setImageResource(resId);
    }

    private void loadImage() {
        Drawable cache = cache();
        if (cache != null) binding.image.setImageDrawable(cache);
        else binding.image.setImageResource(R.drawable.wallpaper_1);
    }

    private void loadVideo(File file) {
        ensurePlayer();
        ensureVideoView();
        video.setPlayer(player);
        video.setVisibility(VISIBLE);
        binding.image.setImageDrawable(cache());
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
        player.prepare();
    }

    private void loadGif(File file) {
        drawable = gif(file);
        if (drawable != null) binding.image.setImageDrawable(drawable);
        else loadImage();
    }

    private Drawable cache() {
        File file = Path.wallCache();
        return file.exists() ? Drawable.createFromPath(file.getAbsolutePath()) : null;
    }

    private GifDrawable gif(File file) {
        try {
            return new GifDrawable(file);
        } catch (IOException e) {
            return null;
        }
    }

    private void ensurePlayer() {
        if (player != null) return;
        player = new ExoPlayer.Builder(getContext()).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        player.setPlayWhenReady(true);
        player.mute();
    }

    private void ensureVideoView() {
        if (video != null) return;
        video = (PlayerView) LayoutInflater.from(getContext()).inflate(R.layout.view_wall_video, this, false);
        addView(video, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private boolean hasVideo() {
        return player != null && video != null && video.getVisibility() == VISIBLE && player.getMediaItemCount() > 0;
    }

    private int getWallColor() {
        int wall = Setting.getWall();
        int type = Setting.getWallType();
        if (isBuiltIn(wall, type)) return WALL_COLORS[wall];
        File file = Path.wallCache();
        return file.exists() ? paletteColor(file) : WALL_COLORS[1];
    }

    private int paletteColor(File file) {
        Bitmap bitmap = decodeBitmap(file);
        if (bitmap == null) return WALL_COLORS[1];
        Palette palette = Palette.from(bitmap).maximumColorCount(8).generate();
        bitmap.recycle();
        return swatchColor(palette);
    }

    private Bitmap decodeBitmap(File file) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 8;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    private int swatchColor(Palette palette) {
        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getDominantSwatch();
        return swatch != null ? swatch.getRgb() : WALL_COLORS[1];
    }

    private boolean isBuiltIn(int wall, int type) {
        return type == TYPE_RES && wall > 0 && wall < WALL_PAPERS.length;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (drawable != null) drawable.start();
        if (!hasVideo()) return;
        video.setPlayer(player);
        player.play();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (drawable != null) drawable.pause();
        if (!hasVideo()) return;
        video.setPlayer(null);
        player.pause();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().unregister(this);
        if (drawable != null) drawable.recycle();
        if (video != null) removeView(video);
        if (player != null) player.release();
        drawable = null;
        binding = null;
        player = null;
        video = null;
    }
}
