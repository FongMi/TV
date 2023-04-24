package com.fongmi.android.tv.ui.custom.dialog;

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
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.databinding.DialogControlBinding;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.ParseAdapter;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

public class ControlDialog extends BaseDialog implements ParseAdapter.OnClickListener {

    private DialogControlBinding binding;
    private ActivityDetailBinding detail;
    private List<TextView> scales;
    private final String[] scale;
    private Listener listener;
    private Players players;
    private boolean parse;

    public static ControlDialog create() {
        return new ControlDialog();
    }

    public ControlDialog() {
        this.scale = ResUtil.getStringArray(R.array.select_scale);
    }

    public ControlDialog detail(ActivityDetailBinding detail) {
        this.detail = detail;
        return this;
    }

    public ControlDialog players(Players players) {
        this.players = players;
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
        binding.speed.setValue(players.getSpeed());
        binding.player.setText(detail.control.action.player.getText());
        binding.decode.setText(detail.control.action.decode.getText());
        binding.ending.setText(detail.control.action.ending.getText());
        binding.opening.setText(detail.control.action.opening.getText());
        binding.loop.setActivated(detail.control.action.loop.isActivated());
        setTrackVisible();
        setScaleText();
        setParse();
    }

    @Override
    protected void initEvent() {
        for (TextView view : scales) view.setOnClickListener(this::setScale);
        binding.text.setOnClickListener(v -> dismiss(detail.control.action.text));
        binding.audio.setOnClickListener(v -> dismiss(detail.control.action.audio));
        binding.video.setOnClickListener(v -> dismiss(detail.control.action.video));
        binding.loop.setOnClickListener(v -> active(binding.loop, detail.control.action.loop));
        binding.player.setOnClickListener(v -> click(binding.player, detail.control.action.player));
        binding.decode.setOnClickListener(v -> click(binding.decode, detail.control.action.decode));
        binding.ending.setOnClickListener(v -> click(binding.ending, detail.control.action.ending));
        binding.opening.setOnClickListener(v -> click(binding.opening, detail.control.action.opening));
        binding.ending.setOnLongClickListener(v -> longClick(binding.ending, detail.control.action.ending));
        binding.opening.setOnLongClickListener(v -> longClick(binding.opening, detail.control.action.opening));
        binding.speed.addOnChangeListener((slider, value, fromUser) -> detail.control.action.speed.setText(players.setSpeed(value)));
    }

    private void setScaleText() {
        for (int i = 0; i < scales.size(); i++) {
            scales.get(i).setText(scale[i]);
            scales.get(i).setActivated(scales.get(i).getText().equals(detail.control.action.scale.getText()));
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

    public void setParseVisible(boolean visible) {
        binding.parse.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.parseText.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setTrackVisible() {
        binding.text.setVisibility(detail.control.action.text.getVisibility());
        binding.audio.setVisibility(detail.control.action.audio.getVisibility());
        binding.video.setVisibility(detail.control.action.video.getVisibility());
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
