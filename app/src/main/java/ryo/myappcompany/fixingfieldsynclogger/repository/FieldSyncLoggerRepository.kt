package ryo.myappcompany.fixingfieldsynclogger.repository

import kotlinx.coroutines.flow.Flow
import ryo.myappcompany.fixingfieldsynclogger.data.Report

interface FieldSyncLoggerRepository {

    /**
     * 作業記録取得
     *
     * @return 作業記録
     */
    fun loadReports(): Flow<List<Report>>

    /**
     * 作業記録保存
     */
    suspend fun saveLogger(content: String)

    /**
     * 作業記録同期
     */
    suspend fun syncUnsyncedReports(reportContent: String, reportId: Int)
}
