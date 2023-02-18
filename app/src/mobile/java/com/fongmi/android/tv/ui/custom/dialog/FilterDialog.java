package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.databinding.DialogFilterBinding;
import com.fongmi.android.tv.impl.FilterCallback;
import com.fongmi.android.tv.ui.adapter.FilterAdapter;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class FilterDialog extends BottomSheetDialogFragment {

    private final FilterCallback callback;
    private DialogFilterBinding binding;
    private List<Filter> filter;

    public static FilterDialog create(FilterCallback callback) {
        return new FilterDialog(callback);
    }

    public FilterDialog filter(List<Filter> filter) {
        this.filter = filter;
        return this;
    }

    public FilterDialog(FilterCallback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFilterBinding.inflate(inflater, container, false);
        binding.recycler.setAdapter(new FilterAdapter(callback, filter));
        binding.recycler.setHasFixedSize(true);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setOnShowListener((DialogInterface f) -> setBehavior(dialog));
        return dialog;
    }

    private void setBehavior(BottomSheetDialog dialog) {
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.hideSystemUI(getDialog().getWindow());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
