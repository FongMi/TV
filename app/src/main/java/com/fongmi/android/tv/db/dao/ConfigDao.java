package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Config;

import java.util.List;

@Dao
public abstract class ConfigDao extends BaseDao<Config> {

    @Query("SELECT * FROM Config ORDER BY time DESC")
    public abstract List<Config> getAll();

    @Query("SELECT * FROM Config WHERE url = :url")
    public abstract Config find(String url);

    @Query("DELETE FROM Config WHERE url = :url")
    public abstract void delete(String url);
}
