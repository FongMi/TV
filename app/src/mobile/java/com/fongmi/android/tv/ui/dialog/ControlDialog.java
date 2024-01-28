package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.databinding.DialogControlBinding;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.Timer;
import com.fongmi.android.tv.ui.adapter.ParseAdapter;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import java.util.List;

public class ControlDialog extends BaseDialog implements ParseAdapter.OnClickListener {

    private DialogControlBinding binding;
    private ActivityVideoBinding parent;
    private FragmentActivity activity;
    private List<TextView> scales;
    private final String[] scale;
    private Listener listener;
    private History history;
    private Players player;
    private boolean parse;

    public static ControlDialog create() {
        return new ControlDialog();
    }

    public ControlDialog() {
        this.scale = ResUtil.getStringArray(R.array.select_scale);
    }

    public ControlDialog parent(ActivityVideoBinding parent) {
        this.parent = parent;
        return this;
    }

    public ControlDialog history(History history) {
        this.history = history;
        return this;
    }

    public ControlDialog player(Players player) {
        this.player = player;
        return this;
    }

    public ControlDialog parse(boolean parse) {
        this.parse = parse;
        return this;
    }

    public ControlDialog show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return this;
        show(activity.getSupportFragmentManager(), null);
        this.listener = (Listener) activity;
        this.activity = activity;
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = DialogControlBinding.inflate(inflater, container, false);
        scales = Arrays.asList(binding.scale0, binding.scale1, binding.scale2, binding.scale3, binding.scale4);
        return binding;
    }

    @Override
    protected void initView() {
        if (player == null) dismiss();
        if (player == null) return;
        binding.speed.setValue(Math.max(player.getSpeed(), 0.5f));
        binding.player.setText(parent.control.action.player.getText());
        binding.decode.setText(parent.control.action.decode.getText());
        binding.ending.setText(parent.control.action.ending.getText());
        binding.opening.setText(parent.control.action.opening.getText());
        binding.loop.setActivated(parent.control.action.loop.isActivated());
        binding.timer.setActivated(Timer.get().isRunning());
        binding.dptime.setActivated(Setting.isDisplayTime());
        binding.dpspeed.setActivated(Setting.isDisplaySpeed());
        binding.dpduration.setActivated(Setting.isDisplayDuration());
        setTrackVisible();
        setScaleText();
        setParse();
    }

    @Override
    protected void initEvent() {
        binding.timer.setOnClickListener(this::onTimer);
        binding.speed.addOnChangeListener(this::setSpeed);
        for (TextView view : scales) view.setOnClickListener(this::setScale);
        binding.text.setOnClickListener(v -> dismiss(parent.control.action.text));
        binding.audio.setOnClickListener(v -> dismiss(parent.control.action.audio));
        binding.video.setOnClickListener(v -> dismiss(parent.control.action.video));
        binding.loop.setOnClickListener(v -> active(binding.loop, parent.control.action.loop));
        binding.player.setOnClickListener(v -> click(binding.player, parent.control.action.player));
        binding.decode.setOnClickListener(v -> click(binding.decode, parent.control.action.decode));
        binding.ending.setOnClickListener(v -> click(binding.ending, parent.control.action.ending));
        binding.opening.setOnClickListener(v -> click(binding.opening, parent.control.action.opening));
        binding.player.setOnLongClickListener(v -> longClick(binding.player, parent.control.action.player));
        binding.ending.setOnLongClickListener(v -> longClick(binding.ending, parent.control.action.ending));
        binding.opening.setOnLongClickListener(v -> longClick(binding.opening, parent.control.action.opening));
        binding.dptime.setOnClickListener(v -> displayTime());
        binding.dpspeed.setOnClickListener(v -> displaySpeed());
        binding.dpduration.setOnClickListener(v -> displayDuration());
    }

    private void displayTime() {
        boolean display = Setting.isDisplayTime();
        parent.display.time.setVisibility(!display ? View.VISIBLE : View.GONE);
        Setting.putDisplayTime(!display);
        binding.dptime.setActivated(!display);
    }

    private void displaySpeed() {
        boolean display = Setting.isDisplaySpeed();
        parent.display.netspeed.setVisibility(!display ? View.VISIBLE : View.GONE);
        Setting.putDisplaySpeed(!display);
        binding.dpspeed.setActivated(!display);
    }

    private void displayDuration() {
        boolean display = Setting.isDisplayDuration();
        parent.display.duration.setVisibility(!display ? View.VISIBLE : View.GONE);
        Setting.putDisplayDuration(!display);
        binding.dpduration.setActivated(!display);
    }

    private void onTimer(View view) {
        App.post(() -> TimerDialog.create().show(activity), 200);
        dismiss();
    }

    private void setSpeed(@NonNull Slider slider, float value, boolean fromUser) {
        parent.control.action.speed.setText(player.setSpeed(value));
        if (history != null) history.setSpeed(player.getSpeed());
    }

    private void setScaleText() {
        for (int i = 0; i < scales.size(); i++) {
            scales.get(i).setText(scale[i]);
            scales.get(i).setActivated(scales.get(i).getText().equals(parent.control.action.scale.getText()));
        }
    }

    private void setParse() {
        setParseVisible(parse);
        binding.parse.setHasFixedSize(true);
        binding.parse.setItemAnimator(null);
        binding.parse.addItemDecoration(new SpaceItemDecoration(8));
        binding.parse.setAdapter(new ParseAdapter(this, ViewType.LIGHT));
    }

    private void setScale(View view) {
        for (TextView textView : scales) textView.setActivated(false);
        listener.onScale(Integer.parseInt(view.getTag().toString()));
        view.setActivated(true);
    }

    private void active(View view, TextView target) {
        target.performClick();
        view.setActivated(target.isActivated());
    }

    private void click(TextView view, TextView target) {
        target.performClick();
        view.setText(target.getText());
    }

    private boolean longClick(TextView view, TextView target) {
        target.performLongClick();
        view.setText(target.getText());
        return true;
    }

    private void dismiss(View view) {
        App.post(view::performClick, 200);
        dismiss();
    }

    public void updateParse() {
        binding.parse.getAdapter().notifyItemRangeChanged(0, binding.parse.getAdapter().getItemCount());
    }

    public void updatePlayer() {
        binding.player.setText(parent.control.action.player.getText());
    }

    public void setParseVisible(boolean visible) {
        binding.parse.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.parseText.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setTrackVisible() {
        binding.text.setVisibility(parent.control.action.text.getVisibility());
        binding.audio.setVisibility(parent.control.action.audio.getVisibility());
        binding.video.setVisibility(parent.control.action.video.getVisibility());
        binding.track.setVisibility(binding.text.getVisibility() == View.GONE && binding.audio.getVisibility() == View.GONE && binding.video.getVisibility() == View.GONE ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(Parse item) {
        listener.onParse(item);
        binding.parse.getAdapter().notifyItemRangeChanged(0, binding.parse.getAdapter().getItemCount());
    }

    public interface Listener {

        void onScale(int tag);

        void onParse(Parse item);
    }
}
