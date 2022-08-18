package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.fongmi.android.tv.bean.Site;

@Dao
public abstract class SiteDao extends BaseDao<Site> {

    @Query("SELECT * FROM site WHERE `key` = :key")
    public abstract Site find(String key);
}
