package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuBinding;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;

public class DanmuDialog extends BaseDialog {

    private DialogDanmuBinding binding;
    private String[] danmuSpeed;
    private int speed;
    private VideoActivity activity;

    public static DanmuDialog create() {
        return new DanmuDialog();
    }

    public DanmuDialog() {
    }

    public DanmuDialog show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return this;
        show(activity.getSupportFragmentManager(), null);
        this.activity = (VideoActivity) activity;
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDanmuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.list.setVisibility(View.VISIBLE);
        binding.speed.setValue(speed = Setting.getDanmuSpeed());
        binding.size.setValue(Setting.getDanmuSize());
        binding.line.setValue(Setting.getDanmuLine(2));
        binding.alpha.setValue(Setting.getDanmuAlpha());
        binding.speedText.setText((danmuSpeed = ResUtil.getStringArray(R.array.select_danmu_speed))[speed]);
    }

    @Override
    protected void initEvent() {
        binding.speed.addOnChangeListener((@NonNull Slider slider, float value, boolean fromUser) -> {
            int val = (int) slider.getValue();
            binding.speedText.setText(danmuSpeed[val]);
            Setting.putDanmuSpeed(val);
            this.activity.setDanmuViewSettings();
        });
        binding.size.addOnChangeListener((@NonNull Slider slider, float value, boolean fromUser) -> {
            Setting.putDanmuSize(slider.getValue());
            this.activity.setDanmuViewSettings();
        });
        binding.line.addOnChangeListener((@NonNull Slider slider, float value, boolean fromUser) -> {
            Setting.putDanmuLine((int) slider.getValue());
            this.activity.setDanmuViewSettings();
        });
        binding.alpha.addOnChangeListener((@NonNull Slider slider, float value, boolean fromUser) -> {
            Setting.putDanmuAlpha((int) slider.getValue());
            this.activity.setDanmuViewSettings();
        });
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
