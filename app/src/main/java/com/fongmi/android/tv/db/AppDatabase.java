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
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.db.dao.ConfigDao;
import com.fongmi.android.tv.db.dao.HistoryDao;
import com.fongmi.android.tv.db.dao.KeepDao;
import com.fongmi.android.tv.db.dao.SiteDao;

@Database(entities = {Config.class, Site.class, History.class, Keep.class}, version = AppDatabase.VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 9;
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "tv").addMigrations(MIGRATION_8_9).allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract ConfigDao getConfigDao();

    public abstract HistoryDao getHistoryDao();

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Keep` (`key` TEXT NOT NULL, `siteName` TEXT, `vodName` TEXT, `vodPic` TEXT, `createTime` INTEGER NOT NULL, `cid` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        }
    };
}
