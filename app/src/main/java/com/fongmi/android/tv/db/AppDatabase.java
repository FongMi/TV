package com.fongmi.android.tv.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.dao.HistoryDao;

@Database(entities = {History.class}, version = AppDatabase.VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final int VERSION = 3;

    private static volatile AppDatabase instance;

    public static synchronized AppDatabase get() {
        if (instance == null) instance = create(App.get());
        return instance;
    }

    private static AppDatabase create(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "tv").allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    public abstract HistoryDao getHistoryDao();

    public static void clear() {
        get().getHistoryDao().delete();
    }
}
