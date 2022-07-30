package com.fongmi.android.tv.ui.custom;

import androidx.collection.ArrayMap;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import java.util.ArrayList;
import java.util.List;

public class CustomSelector extends PresenterSelector {

    private final List<Presenter> mPresenters;
    private final ArrayMap<Class<?>, Presenter> mSingleMap;
    private final ArrayMap<Class<?>, ArrayMap<Class<?>, Presenter>> mClassMap;

    public CustomSelector() {
        super();
        mPresenters = new ArrayList<>();
        mSingleMap = new ArrayMap<>();
        mClassMap = new ArrayMap<>();
    }

    public void addPresenter(Class<?> cls, Presenter presenter) {
        mSingleMap.put(cls, presenter);
        if (!mPresenters.contains(presenter)) {
            mPresenters.add(presenter);
        }
    }

    public void addPresenter(Class<?> cls, Presenter presenter, Class<?> childType) {
        ArrayMap<Class<?>, Presenter> classPresenterArrayMap = mClassMap.get(cls);
        if (classPresenterArrayMap == null) classPresenterArrayMap = new ArrayMap<>();
        classPresenterArrayMap.put(childType, presenter);
        mClassMap.put(cls, classPresenterArrayMap);
        if (!mPresenters.contains(presenter)) {
            mPresenters.add(presenter);
        }
    }

    @Override
    public Presenter getPresenter(Object item) {
        Class<?> cls = item.getClass();
        Presenter presenter;
        presenter = mSingleMap.get(cls);
        if (presenter != null) return presenter;
        ArrayMap<Class<?>, Presenter> presenters = mClassMap.get(cls);
        assert presenters != null;
        if (presenters.size() == 1) return presenters.valueAt(0);
        if (item instanceof ListRow) {
            ListRow listRow = (ListRow) item;
            Presenter childPresenter = listRow.getAdapter().getPresenter(listRow);
            Class<?> childCls = childPresenter.getClass();
            do {
                presenter = presenters.get(childCls);
                childCls = childCls.getSuperclass();
            } while (presenter == null && childCls != null);
        }
        return presenter;
    }

    @Override
    public Presenter[] getPresenters() {
        return mPresenters.toArray(new Presenter[]{});
    }
}