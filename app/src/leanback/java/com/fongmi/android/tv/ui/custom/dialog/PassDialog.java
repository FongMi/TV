package com.fongmi.android.tv.ui.custom.dialog;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogPassBinding;
import com.fongmi.android.tv.impl.PassCallback;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PassDialog extends BaseDialog {

    private final PassCallback callback;
    private DialogPassBinding binding;

    public static void show(FragmentActivity activity) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof BottomSheetDialogFragment) return;
        new PassDialog(activity).show(activity.getSupportFragmentManager(), null);
    }

    private PassDialog(FragmentActivity activity) {
        this.callback = (PassCallback) activity;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogPassBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initEvent() {
        binding.positive.setOnClickListener(this::onPass);
        binding.pass.setOnEditorActionListener(this::onDone);
    }

    private void onPass(View view) {
        String pass = binding.pass.getText().toString().trim();
        if (pass.length() > 0) callback.setPass(pass);
        dismiss();
    }

    private boolean onDone(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ResUtil.dp2px(250), -1);
    }
}
