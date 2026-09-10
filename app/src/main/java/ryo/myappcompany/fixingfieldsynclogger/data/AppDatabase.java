package ryo.myappcompany.fixingfieldsynclogger.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Report.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    private static Context mContext;

    public abstract ReportDao reportDao();

    public static AppDatabase getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = Room.databaseBuilder(mContext,
                                            AppDatabase.class, "report_database")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
