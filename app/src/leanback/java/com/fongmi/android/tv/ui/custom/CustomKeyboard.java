package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.adapter.KeyboardAdapter;

public class CustomKeyboard implements KeyboardAdapter.OnClickListener {

    private final ActivitySearchBinding binding;
    private final Callback callback;

    public static void init(Callback callback, ActivitySearchBinding binding) {
        new CustomKeyboard(callback, binding).initView();
    }

    public CustomKeyboard(Callback callback, ActivitySearchBinding binding) {
        this.callback = callback;
        this.binding = binding;
    }

    private void initView() {
        binding.keyboard.setHasFixedSize(true);
        binding.keyboard.addItemDecoration(new SpaceItemDecoration(6, 8));
        binding.keyboard.setAdapter(new KeyboardAdapter(this));
    }

    @Override
    public void onTextClick(String text) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        if (binding.keyword.length() > 19) return;
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
            case R.drawable.ic_site_search_on:
                callback.showDialog();
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
            case R.drawable.ic_keyboard_remote:
                callback.onRemote();
                break;
            case R.drawable.ic_keyboard_search:
                callback.onSearch();
                break;
        }
    }

    @Override
    public boolean onLongClick(int resId) {
        if (resId != R.drawable.ic_keyboard_back) return false;
        binding.keyword.setText("");
        return true;
    }

    public interface Callback {

        void showDialog();

        void onRemote();

        void onSearch();
    }
}
