package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.History;

import java.util.List;

@Dao
public abstract class HistoryDao extends BaseDao<History> {

    @Query("SELECT * FROM history WHERE cid = :cid ORDER BY createTime DESC")
    public abstract List<History> find(int cid);

    @Query("SELECT * FROM history WHERE cid = :cid AND `key` = :key")
    public abstract History find(int cid, String key);

    @Query("SELECT * FROM history WHERE cid = :cid AND vodName = :vodName")
    public abstract History findByName(int cid, String vodName);

    @Query("DELETE FROM history WHERE cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM history WHERE cid = :cid")
    public abstract void delete(int cid);

    @Query("DELETE FROM history")
    public abstract void delete();
}
