package com.fongmi.android.tv.ui.custom;

import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.util.Log;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.EpgProgramme;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Keep the original bottom information bar above the channel/programme list. */
public final class LiveProgrammeInfo implements ViewTreeObserver.OnPreDrawListener {

    private final ActivityLiveBinding binding;
    private final Supplier<EpgData> programme;
    private final Supplier<Channel> channel;
    private final BooleanSupplier catchup;
    private final Runnable refresh;
    private final Runnable changed;
    private final int bottomMargin;
    private final int panelMargin;
    private final float centerTranslation;
    private final float rightTranslation;
    private boolean ownsInfo;
    private Channel previousChannel;
    private long start;
    private long end;
    private String title;
    private boolean wasCatchup;

    public LiveProgrammeInfo(ActivityLiveBinding binding, Supplier<Channel> channel, Supplier<EpgData> programme,
                             BooleanSupplier catchup, Runnable refresh, Runnable changed) {
        this.binding = binding;
        this.channel = channel;
        this.programme = programme;
        this.catchup = catchup;
        this.refresh = refresh;
        this.changed = changed;
        bottomMargin = margins(binding.control.bottom).bottomMargin;
        panelMargin = margins(binding.recycler).bottomMargin;
        centerTranslation = binding.control.center.getTranslationY();
        rightTranslation = binding.control.right.getRoot().getTranslationY();
        ViewGroup parent = (ViewGroup) binding.widget.info.getParent();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(binding.widget.info.getLayoutParams());
        params.gravity = Gravity.BOTTOM;
        parent.removeView(binding.widget.info);
        binding.getRoot().addView(binding.widget.info, params);
        binding.getRoot().getViewTreeObserver().addOnPreDrawListener(this);
    }

    /** Preserve the native timeout when changing channel or source line. */
    public void onNativeInfoShown() {
        ownsInfo = false;
        restoreControls();
    }

    private static ViewGroup.MarginLayoutParams margins(View view) {
        return (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    }

    private static void margin(View view, int value) {
        ViewGroup.MarginLayoutParams params = margins(view);
        if (params.bottomMargin == value) return;
        params.bottomMargin = value;
        view.setLayoutParams(params);
    }

    private void restoreControls() {
        margin(binding.control.bottom, bottomMargin);
        binding.control.center.setTranslationY(centerTranslation);
        binding.control.right.getRoot().setTranslationY(rightTranslation);
    }

    private void avoidBottom(View view, float original, int limit) {
        if (view.getHeight() == 0) return;
        float translation = Math.min(original, limit - view.getBottom());
        int first = binding.control.top.getBottom() + ResUtil.dp2px(8);
        view.setTranslationY(Math.max(translation, first - view.getTop()));
    }

    private void layoutInfo() {
        boolean panel = binding.recycler.isShown();
        boolean controls = binding.control.getRoot().isShown() && binding.control.bottom.isShown();
        int height = binding.widget.info.getHeight();
        if (height <= 0) height = binding.widget.info.getLayoutParams().height;
        if (height <= 0) height = ResUtil.dp2px(56);
        if (panel || controls) {
            ownsInfo = true;
            binding.widget.info.setVisibility(View.VISIBLE);
            margin(binding.recycler, panelMargin + (panel ? height : 0));
            margin(binding.control.bottom, bottomMargin + (controls ? height : 0));
            if (controls && binding.control.getRoot().getHeight() > 0) {
                int limit = binding.control.getRoot().getHeight() - binding.control.getRoot().getPaddingBottom()
                        - margins(binding.control.bottom).bottomMargin - binding.control.bottom.getHeight() - ResUtil.dp2px(8);
                avoidBottom(binding.control.center, centerTranslation, limit);
                avoidBottom(binding.control.right.getRoot(), rightTranslation, limit);
            } else restoreControls();
        } else {
            if (ownsInfo) binding.widget.info.setVisibility(View.GONE);
            ownsInfo = false;
            restoreControls();
            margin(binding.recycler, panelMargin);
        }
    }

    @Override
    public boolean onPreDraw() {
        binding.control.subtitle.setVisibility(View.GONE);
        layoutInfo();
        boolean visible = binding.widget.info.isShown() || binding.widget.infoPip.isShown();
        if (!visible) return true;
        try {
            Channel owner = channel.get();
            boolean channelChanged = owner != previousChannel;
            if (channelChanged) {
                binding.widget.play.setText(ResUtil.getString(R.string.epg_history_empty));
                binding.widget.programmeProgress.setVisibility(View.GONE);
            }
            long now = System.currentTimeMillis();
            EpgData data = programme.get();
            long nextStart = data == null ? 0 : data.getStartTime();
            long nextEnd = data == null ? 0 : EpgProgramme.playbackEnd(data);
            String nextTitle = data == null ? "" : data.getTitle();
            boolean replay = catchup.getAsBoolean();
            boolean different = channelChanged || start != nextStart || end != nextEnd
                    || !Objects.equals(title, nextTitle) || replay != wasCatchup;
            // Write the new channel's data before any callback can fail.
            binding.widget.play.setText(data == null || nextTitle.isEmpty() ? ResUtil.getString(R.string.epg_history_empty)
                    : replay && data instanceof EpgProgramme ? ((EpgProgramme) data).playbackFormat() : data.format());
            binding.widget.programmeProgress.setVisibility(!replay && nextEnd > nextStart ? View.VISIBLE : View.GONE);
            binding.widget.programmeProgress.setProgress(nextEnd > nextStart
                    ? (int) Math.max(0, Math.min(1000, (now - nextStart) * 1000 / (nextEnd - nextStart))) : 0);
            previousChannel = owner;
            start = nextStart; end = nextEnd; title = nextTitle; wasCatchup = replay;
            if (different) {
                try { changed.run(); }
                catch (RuntimeException error) { Log.w("LiveProgrammeInfo", "Programme selection update failed", error); }
            }
            try { refresh.run(); }
            catch (RuntimeException error) { Log.w("LiveProgrammeInfo", "Programme refresh failed", error); }
        } catch (RuntimeException error) {
            binding.widget.play.setText(ResUtil.getString(R.string.epg_history_empty));
            binding.widget.programmeProgress.setVisibility(View.GONE);
            Log.w("LiveProgrammeInfo", "Programme information unavailable", error);
        } finally {
            binding.getRoot().postInvalidateDelayed(1000);
        }
        return true;
    }

    public void release() {
        binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
    }
}
