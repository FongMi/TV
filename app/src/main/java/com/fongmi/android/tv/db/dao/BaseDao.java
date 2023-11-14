package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract Long insert(T item);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract List<Long> insert(List<T> items);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(T item);

    @Update
    public abstract void update(List<T> items);

    @Transaction
    public void insertOrUpdate(T item) {
        long id = insert(item);
        if (id == -1) update(item);
    }

    @Transaction
    public void insertOrUpdate(List<T> items) {
        List<Long> result = insert(items);
        List<T> list = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) if (result.get(i) == -1) list.add(items.get(i));
        if (list.size() > 0) update(list);
    }
}
