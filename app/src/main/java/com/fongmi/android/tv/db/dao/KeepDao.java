package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Keep;

import java.util.List;

@Dao
public abstract class KeepDao extends BaseDao<Keep> {

    @Query("SELECT * FROM Keep WHERE type = 0 ORDER BY createTime DESC")
    public abstract List<Keep> getVod();

    @Query("SELECT * FROM Keep WHERE type = 1 ORDER BY createTime DESC")
    public abstract List<Keep> getLive();

    @Query("SELECT * FROM Keep WHERE type = 0 AND cid = :cid AND `key` = :key")
    public abstract Keep find(int cid, String key);

    @Query("SELECT * FROM Keep WHERE type = 1 AND `key` = :key")
    public abstract Keep find(String key);

    @Query("DELETE FROM Keep WHERE type = 1 AND `key` = :key")
    public abstract void delete(String key);

    @Query("DELETE FROM Keep WHERE type = 0 AND cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM Keep WHERE type = 0 AND cid = :cid")
    public abstract void delete(int cid);

    @Query("DELETE FROM Keep")
    public abstract void delete();
}
