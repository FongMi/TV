package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.android.tv.databinding.DialogButtonsBinding;
import com.fongmi.android.tv.ui.adapter.ButtonsAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ButtonsDialog {
    private final DialogButtonsBinding binding;
    private final ButtonsAdapter adapter;
    private final AlertDialog dialog;


    public static ButtonsDialog create(Activity activity) {
        return new ButtonsDialog(activity);
    }

    public ButtonsDialog(Activity activity) {
        this.adapter = new ButtonsAdapter();
        this.binding = DialogButtonsBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initView();
        initEvent();
    }

    private void initView() {
        setRecyclerView();
        setDialog();
    }

    private void initEvent() {

    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), 1));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(0));

    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

}
