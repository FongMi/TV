package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.History;

import java.util.List;

@Dao
public abstract class HistoryDao extends BaseDao<History> {

    @Query("SELECT * FROM history ORDER BY createTime DESC")
    public abstract List<History> getAll();

    @Query("SELECT * FROM history WHERE `key` = :key")
    public abstract History find(String key);

    @Query("DELETE FROM history WHERE `key` = :key")
    public abstract void delete(String key);

    @Query("DELETE FROM history")
    public abstract void delete();
}
