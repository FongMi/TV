package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
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
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ViewWallBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public class CustomWallView extends FrameLayout implements DefaultLifecycleObserver {

    private ViewWallBinding binding;
    private ExoPlayer player;
    private Drawable cache;

    public CustomWallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) init();
    }

    private void init() {
        binding = ViewWallBinding.inflate(LayoutInflater.from(getContext()), this, true);
        ((ComponentActivity) getContext()).getLifecycle().addObserver(this);
        createPlayer();
        refresh();
    }

    private void createPlayer() {
        player = new ExoPlayer.Builder(getContext()).build();
        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        player.setPlayWhenReady(true);
        player.setVolume(0);
    }

    private boolean isVideo(File file) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            return "yes".equalsIgnoreCase(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGif(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return "image/gif".equals(options.outMimeType);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.WALL) refresh();
    }

    private void refresh() {
        cache = Drawable.createFromPath(FileUtil.getWallCache().getAbsolutePath());
        load(FileUtil.getWall(Setting.getWall()));
    }

    private void load(File file) {
        if (!file.getName().endsWith("0")) loadRes(ResUtil.getDrawable(file.getName()));
        else if (isVideo(file)) loadVideo(file);
        else if (isGif(file)) loadGif(file);
        else loadImage();
    }

    private void loadRes(int resId) {
        player.clearMediaItems();
        binding.video.setPlayer(null);
        binding.video.setVisibility(GONE);
        binding.image.setImageResource(resId);
    }

    private void loadVideo(File file) {
        binding.video.setPlayer(player);
        binding.video.setVisibility(VISIBLE);
        binding.image.setImageDrawable(cache);
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
        player.prepare();
    }

    private void loadGif(File file) {
        player.clearMediaItems();
        binding.video.setPlayer(null);
        binding.video.setVisibility(GONE);
        Glide.with(binding.image).load(file).placeholder(cache).error(cache).override(ResUtil.getScreenWidth(), ResUtil.getScreenHeight()).into(binding.image);
    }

    private void loadImage() {
        player.clearMediaItems();
        binding.video.setPlayer(null);
        binding.video.setVisibility(GONE);
        if (cache != null) binding.image.setImageDrawable(cache);
        else binding.image.setImageResource(R.drawable.wallpaper_1);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (binding.video.getVisibility() != VISIBLE || player == null || player.getMediaItemCount() == 0) return;
        binding.video.setPlayer(player);
        player.play();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (binding.video.getVisibility() != VISIBLE || player == null || player.getMediaItemCount() == 0) return;
        binding.video.setPlayer(null);
        player.pause();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().unregister(this);
        binding.video.setPlayer(null);
        player.release();
        binding = null;
        player = null;
        cache = null;
    }
}
