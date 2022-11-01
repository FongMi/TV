package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Config;

import java.util.List;

@Dao
public abstract class ConfigDao extends BaseDao<Config> {

    @Query("SELECT * FROM Config WHERE type = :type ORDER BY time DESC")
    public abstract List<Config> findByType(int type);

    @Query("SELECT * FROM Config WHERE id = :id")
    public abstract Config findById(int id);

    @Query("SELECT * FROM Config WHERE url = :url")
    public abstract Config findByUrl(String url);

    @Query("SELECT * FROM Config WHERE type = :type ORDER BY time DESC LIMIT 1")
    public abstract Config find(int type);

    @Query("DELETE FROM Config WHERE url = :url")
    public abstract void delete(String url);

    @Query("DELETE FROM Config WHERE type = :type")
    public abstract void delete(int type);
}
