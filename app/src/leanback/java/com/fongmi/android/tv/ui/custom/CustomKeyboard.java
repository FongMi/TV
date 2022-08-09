package com.fongmi.android.tv.ui.custom;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;

import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.presenter.KeyboardPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomKeyboard implements KeyboardPresenter.OnClickListener {

    private static final String LEFT = "◁";
    private static final String RIGHT = "▷";
    private static final String BACK = "⌫";
    private static final String ENTER = "⏎";

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
        List<String> keys = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", LEFT, RIGHT, BACK, ENTER);
        List<ListRow> rows = new ArrayList<>();
        for (List<String> items : Lists.partition(keys, 10)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new KeyboardPresenter(this));
            adapter.addAll(0, items);
            rows.add(new ListRow(adapter));
        }
        return rows;
    }

    @Override
    public void onItemClick(String text) {
        StringBuilder sb = new StringBuilder(binding.keyword.getText().toString());
        int cursor = binding.keyword.getSelectionStart();
        switch (text) {
            case ENTER:
                binding.search.performClick();
                break;
            case LEFT:
                binding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
                break;
            case RIGHT:
                binding.keyword.setSelection(++cursor > binding.keyword.length() ? binding.keyword.length() : cursor);
                break;
            case BACK:
                if (cursor == 0) return;
                sb.deleteCharAt(cursor - 1);
                binding.keyword.setText(sb.toString());
                binding.keyword.setSelection(cursor - 1);
                break;
            default:
                sb.insert(cursor, text);
                binding.keyword.setText(sb.toString());
                binding.keyword.setSelection(cursor + 1);
                break;
        }
    }
}
