package com.fongmi.android.tv.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.db.dao.ConfigDao;
import com.fongmi.android.tv.db.dao.DeviceDao;
import com.fongmi.android.tv.db.dao.HistoryDao;
import com.fongmi.android.tv.db.dao.KeepDao;
import com.fongmi.android.tv.db.dao.LiveDao;
import com.fongmi.android.tv.db.dao.SiteDao;
import com.fongmi.android.tv.db.dao.TrackDao;

@Database(entities = {Keep.class, Site.class, Live.class, Track.class, Config.class, Device.class, History.class}, version = AppDatabase.VERSION)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 35;
    public static final String NAME = "tv";
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(Migrations.MIGRATION_30_31)
                .addMigrations(Migrations.MIGRATION_31_32)
                .addMigrations(Migrations.MIGRATION_32_33)
                .addMigrations(Migrations.MIGRATION_33_34)
                .addMigrations(Migrations.MIGRATION_34_35)
                .fallbackToDestructiveMigration(true)
                .allowMainThreadQueries().build();
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract LiveDao getLiveDao();

    public abstract TrackDao getTrackDao();

    public abstract ConfigDao getConfigDao();

    public abstract DeviceDao getDeviceDao();

    public abstract HistoryDao getHistoryDao();
}
