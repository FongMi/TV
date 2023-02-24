package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.databinding.DialogFilterBinding;
import com.fongmi.android.tv.impl.FilterCallback;
import com.fongmi.android.tv.ui.adapter.FilterAdapter;

import java.util.List;

public class FilterDialog extends BaseDialog {

    private final FilterCallback callback;
    private DialogFilterBinding binding;
    private List<Filter> filter;

    public static FilterDialog create(FilterCallback callback) {
        return new FilterDialog(callback);
    }

    public FilterDialog(FilterCallback callback) {
        this.callback = callback;
    }

    public FilterDialog filter(List<Filter> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogFilterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setAdapter(new FilterAdapter(callback, filter));
        binding.recycler.setHasFixedSize(true);
    }
}
