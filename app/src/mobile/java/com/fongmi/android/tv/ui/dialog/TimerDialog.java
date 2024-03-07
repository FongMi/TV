package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogTimerBinding;
import com.fongmi.android.tv.utils.Timer;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimerDialog extends BaseDialog implements Timer.Callback {

    private DialogTimerBinding binding;
    private StringBuilder builder;
    private Formatter formatter;

    public static TimerDialog create() {
        return new TimerDialog();
    }

    public TimerDialog() {
        builder = new StringBuilder();
        formatter = new Formatter(builder, Locale.getDefault());
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogTimerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        onTick(Timer.get().getTick());
        binding.list.setVisibility(Timer.get().isRunning() ? View.GONE : View.VISIBLE);
        binding.timer.setVisibility(Timer.get().isRunning() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initEvent() {
        Timer.get().setCallback(this);
        binding.delay.setOnClickListener(this::onDelay);
        binding.reset.setOnClickListener(this::onReset);
        binding.time1.setOnClickListener(this::setTimer);
        binding.time2.setOnClickListener(this::setTimer);
        binding.time3.setOnClickListener(this::setTimer);
        binding.time4.setOnClickListener(this::setTimer);
    }

    private void setTimer(View view) {
        int minutes = Integer.parseInt(view.getTag().toString());
        Timer.get().set(TimeUnit.MINUTES.toMillis(minutes));
        dismiss();
    }

    private void onDelay(View view) {
        Timer.get().delay();
    }

    private void onReset(View view) {
        Timer.get().reset();
        dismiss();
    }

    @Override
    public void onTick(long tick) {
        binding.tick.setText(Util.format(builder, formatter, tick));
    }

    @Override
    public void onFinish() {
        dismiss();
    }

    @Override
    public void dismiss() {
        Timer.get().setCallback(null);
        super.dismiss();
    }
}
