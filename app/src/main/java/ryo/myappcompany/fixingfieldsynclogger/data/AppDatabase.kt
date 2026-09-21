package ryo.myappcompany.fixingfieldsynclogger.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Report::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
