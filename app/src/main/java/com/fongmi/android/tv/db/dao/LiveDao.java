package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.fongmi.android.tv.bean.Live;

import java.util.List;

@Dao
public abstract class LiveDao extends BaseDao<Live> {

    @Query("SELECT * FROM Live")
    public abstract List<Live> findAll();

    @Query("SELECT * FROM Live WHERE name = :name")
    public abstract Live find(String name);

    @Query("UPDATE Live SET boot = :boot, pass = :pass WHERE name = :name")
    protected abstract void updateSettings(String name, boolean boot, boolean pass);

    @Transaction
    public void saveSettings(List<Live> items) {
        if (items.isEmpty()) return;
        insert(items);
        for (Live item : items) updateSettings(item.getName(), item.isBoot(), item.isPass());
    }
}
