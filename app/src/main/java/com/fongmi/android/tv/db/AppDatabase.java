package com.fongmi.android.tv.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
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
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

@Database(entities = {Keep.class, Site.class, Live.class, Track.class, Config.class, Device.class, History.class}, version = AppDatabase.VERSION)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 27;
    public static final String NAME = "tv";
    public static final String SYMBOL = "@@@";

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    public static File getBackup() {
        return new File(Path.tv(), NAME);
    }

    public static String getDate() {
        return Setting.isBackupAuto() ? ResUtil.getString(R.string.setting_backup_auto) : getBackup().exists() ? Util.format(new SimpleDateFormat("MMddHHmmss", Locale.getDefault()), getBackup().lastModified()) : "";
    }

    public static void backup() {
        if (Setting.isBackupAuto()) backup(new com.fongmi.android.tv.impl.Callback());
    }

    public static void backup(com.fongmi.android.tv.impl.Callback callback) {
        App.execute(() -> {
            File db = App.get().getDatabasePath(NAME).getAbsoluteFile();
            File wal = App.get().getDatabasePath(NAME + "-wal").getAbsoluteFile();
            File shm = App.get().getDatabasePath(NAME + "-shm").getAbsoluteFile();
            if (db.exists()) Path.copy(db, new File(Path.tv(), db.getName()));
            if (wal.exists()) Path.copy(wal, new File(Path.tv(), wal.getName()));
            if (shm.exists()) Path.copy(shm, new File(Path.tv(), shm.getName()));
            Prefers.backup(new File(Path.tv(), NAME + "-pref"));
            App.post(callback::success);
        });
    }

    public static void restore(com.fongmi.android.tv.impl.Callback callback) {
        App.execute(() -> {
            File db = new File(Path.tv(), NAME);
            File wal = new File(Path.tv(), NAME + "-wal");
            File shm = new File(Path.tv(), NAME + "-shm");
            File pref = new File(Path.tv(), NAME + "-pref");
            if (db.exists()) Path.move(db, App.get().getDatabasePath(db.getName()).getAbsoluteFile());
            if (wal.exists()) Path.move(wal, App.get().getDatabasePath(wal.getName()).getAbsoluteFile());
            if (shm.exists()) Path.move(shm, App.get().getDatabasePath(shm.getName()).getAbsoluteFile());
            if (pref.exists()) Prefers.restore(pref);
            App.post(callback::success);
        });
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(MIGRATION_11_12)
                .addMigrations(MIGRATION_12_13)
                .addMigrations(MIGRATION_13_14)
                .addMigrations(MIGRATION_14_15)
                .addMigrations(MIGRATION_15_16)
                .addMigrations(MIGRATION_16_17)
                .addMigrations(MIGRATION_17_18)
                .addMigrations(MIGRATION_18_19)
                .addMigrations(MIGRATION_19_20)
                .addMigrations(MIGRATION_20_21)
                .addMigrations(MIGRATION_21_22)
                .addMigrations(MIGRATION_22_23)
                .addMigrations(MIGRATION_23_24)
                .addMigrations(MIGRATION_24_25)
                .addMigrations(MIGRATION_25_26)
                .addMigrations(MIGRATION_26_27)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
    }

    public abstract KeepDao getKeepDao();

    public abstract SiteDao getSiteDao();

    public abstract LiveDao getLiveDao();

    public abstract TrackDao getTrackDao();

    public abstract ConfigDao getConfigDao();

    public abstract DeviceDao getDeviceDao();

    public abstract HistoryDao getHistoryDao();

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Config ADD COLUMN type INTEGER DEFAULT 0 NOT NULL");
            database.execSQL("ALTER TABLE Config ADD COLUMN home TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Keep ADD COLUMN type INTEGER DEFAULT 0 NOT NULL");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP INDEX IF EXISTS index_Config_url");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_Config_url_type ON Config(url, type)");
        }
    };

    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE History ADD COLUMN scale INTEGER DEFAULT -1 NOT NULL");
        }
    };

    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE History ADD COLUMN speed REAL DEFAULT 1 NOT NULL");
            database.execSQL("ALTER TABLE History ADD COLUMN player INTEGER DEFAULT -1 NOT NULL");
        }
    };

    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Track` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `group` INTEGER NOT NULL, `track` INTEGER NOT NULL, `player` INTEGER NOT NULL, `key` TEXT, `name` TEXT, `selected` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Track_key_player_type` ON `Track` (`key`, `player`, `type`)");
        }
    };

    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Config ADD COLUMN parse TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Site ADD COLUMN changeable INTEGER DEFAULT 1");
        }
    };

    static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Config ADD COLUMN name TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Device` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `uuid` TEXT, `name` TEXT, `ip` TEXT)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Device_uuid_name` ON `Device` (`uuid`, `name`)");
        }
    };

    static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Device ADD COLUMN type INTEGER DEFAULT 0 NOT NULL");
        }
    };

    static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("UPDATE History SET player = 2 WHERE player = 0");
            if (Setting.getLivePlayer() == 0) Setting.putLivePlayer(2);
            if (Setting.getPlayer() == 0) Setting.putPlayer(2);
        }
    };

    static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Track ADD COLUMN `adaptive` INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_24_25 = new Migration(24, 25) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Site ADD COLUMN recordable INTEGER DEFAULT 1");
        }
    };

    static final Migration MIGRATION_25_26 = new Migration(25, 26) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE Site_Backup (`key` TEXT NOT NULL, name TEXT, searchable INTEGER, changeable INTEGER, recordable INTEGER, PRIMARY KEY (`key`))");
            database.execSQL("INSERT INTO Site_Backup SELECT `key`, name, searchable, changeable, recordable FROM Site");
            database.execSQL("DROP TABLE Site");
            database.execSQL("ALTER TABLE Site_Backup RENAME to Site");
        }
    };

    static final Migration MIGRATION_26_27 = new Migration(26, 27) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Live` (`name` TEXT NOT NULL, `boot` INTEGER NOT NULL, `pass` INTEGER NOT NULL, PRIMARY KEY(`name`))");
        }
    };
}
