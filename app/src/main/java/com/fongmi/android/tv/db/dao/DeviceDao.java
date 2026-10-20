package com.fongmi.android.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.fongmi.android.tv.bean.Device;

import java.util.List;

@Dao
public abstract class DeviceDao extends BaseDao<Device> {

    @Query("SELECT * FROM Device")
    public abstract List<Device> findAll();

    @Query("DELETE FROM Device")
    public abstract void delete();

    @Query("DELETE FROM Device WHERE uuid = :uuid")
    public abstract void delete(String uuid);

    @Query("DELETE FROM Device WHERE type IN (0, 1)")
    public abstract void deleteAppDevices();

    @Override
    @Transaction
    public void insertOrUpdate(Device device) {
        delete(device.getUuid());
        device.setId(null);
        insert(device);
    }

    @Transaction
    public void replaceAppDevices(List<Device> devices) {
        deleteAppDevices();
        for (Device device : devices) insertOrUpdate(device);
    }
}
