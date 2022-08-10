package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.presenter.KeyboardPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomKeyboard implements KeyboardPresenter.OnClickListener {

    private final ActivitySearchBinding binding;

    public static void init(ActivitySearchBinding binding) {
        new CustomKeyboard(binding).initView();
    }

    public CustomKeyboard(ActivitySearchBinding binding) {
        this.binding = binding;
    }

    private void initView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(12), KeyboardPresenter.class);
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(selector);
        binding.keyboard.setVerticalSpacing(ResUtil.dp2px(12));
        binding.keyboard.setAdapter(new ItemBridgeAdapter(adapter));
        adapter.addAll(adapter.size(), getRows());
    }

    private List<ListRow> getRows() {
        List<Object> keys = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", R.drawable.ic_keyboard_left, R.drawable.ic_keyboard_right, R.drawable.ic_keyboard_back, R.drawable.ic_keyboard_enter);
        List<ListRow> rows = new ArrayList<>();
        for (List<Object> items : Lists.partition(keys, 10)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new KeyboardPresenter(this));
            adapter.addAll(0, items);
            rows.add(new ListRow(adapter));
        }
        return rows;
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
            case R.drawable.ic_keyboard_enter:
                binding.search.performClick();
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
