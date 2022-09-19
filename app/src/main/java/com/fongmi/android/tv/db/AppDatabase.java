package com.fongmi.android.tv.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.db.dao.ConfigDao;
import com.fongmi.android.tv.db.dao.HistoryDao;
import com.fongmi.android.tv.db.dao.SiteDao;

@Database(entities = {Config.class, Site.class, History.class}, version = AppDatabase.VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 7;
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "tv").addMigrations(MIGRATION_6_7).allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    public abstract SiteDao getSiteDao();

    public abstract ConfigDao getConfigDao();

    public abstract HistoryDao getHistoryDao();

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE History ADD COLUMN position INTEGER DEFAULT 0 NOT NULL");
            database.execSQL("UPDATE History set position = duration");
            database.execSQL("UPDATE History set duration = 0");
        }
    };
}
