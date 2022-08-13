package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.adapter.KeyboardAdapter;

public class CustomKeyboard implements KeyboardAdapter.OnClickListener {

    private final ActivitySearchBinding binding;

    public static void init(ActivitySearchBinding binding) {
        new CustomKeyboard(binding).initView();
    }

    public CustomKeyboard(ActivitySearchBinding binding) {
        this.binding = binding;
    }

    private void initView() {
        binding.keyboard.setHasFixedSize(true);
        binding.keyboard.addItemDecoration(new SpaceItemDecoration(10, 8));
        binding.keyboard.setAdapter(new KeyboardAdapter(this));
    }

    @Override
    public void onTextClick(String text) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        if (binding.keyword.length() > 29) return;
        sb.insert(cursor, text);
        binding.keyword.setText(sb.toString());
        binding.keyword.setSelection(cursor + 1);
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onIconClick(int resId) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        switch (resId) {
            case R.drawable.ic_keyboard_space:
                sb.insert(cursor, " ");
                binding.keyword.setText(sb.toString());
                binding.keyword.setSelection(cursor + 1);
                break;
            case R.drawable.ic_keyboard_left:
                binding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
                break;
            case R.drawable.ic_keyboard_right:
                binding.keyword.setSelection(++cursor > binding.keyword.length() ? binding.keyword.length() : cursor);
                break;
            case R.drawable.ic_keyboard_back:
                if (cursor == 0) return;
                sb.deleteCharAt(cursor - 1);
                binding.keyword.setText(sb.toString());
                binding.keyword.setSelection(cursor - 1);
                break;
        }
    }
}
