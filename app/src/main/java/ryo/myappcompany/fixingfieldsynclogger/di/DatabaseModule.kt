package ryo.myappcompany.fixingfieldsynclogger.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ryo.myappcompany.fixingfieldsynclogger.data.AppDatabase
import ryo.myappcompany.fixingfieldsynclogger.data.ReportDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * AppDatabaseの提供
     *
     * @param context: ApplicationContext
     *
     * @return AppDatabase
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "report_database"
        ).build()
    }

    /**
     * ReportDaoの提供
     *
     * @param database: AppDatabase
     *
     * @return ReportDao
     */
    @Provides
    fun provideReportDao(
        database: AppDatabase
    ): ReportDao {
        return database.reportDao()
    }
}
