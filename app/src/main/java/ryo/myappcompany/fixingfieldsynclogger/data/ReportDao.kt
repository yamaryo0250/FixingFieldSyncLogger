package ryo.myappcompany.fixingfieldsynclogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: Report): Int

    @Query("SELECT * FROM reports")
    fun getAllReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnSyncedReports(): List<Report>

    @Update
    suspend fun update(report: Report?)
}
