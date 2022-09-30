package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Keep;

import java.util.List;

@Dao
public abstract class KeepDao extends BaseDao<Keep> {

    @Query("SELECT * FROM Keep ORDER BY createTime DESC")
    public abstract List<Keep> getAll();

    @Query("SELECT * FROM Keep WHERE cid = :cid AND `key` = :key")
    public abstract Keep find(int cid, String key);

    @Query("DELETE FROM Keep WHERE cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM Keep WHERE cid = :cid")
    public abstract void delete(int cid);
}
