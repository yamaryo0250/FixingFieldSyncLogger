package ryo.myappcompany.fixingfieldsynclogger.repository

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.data.ReportDao
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorker
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class FieldSyncLoggerRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reportDao: ReportDao
) : FieldSyncLoggerRepository {
    /**
     * 作業記録取得
     *
     * @return 作業記録
     */
    override fun loadReports(): Flow<List<Report>> {
        return reportDao.getAllReports()
    }

    /**
     * 作業記録保存
     */
    override suspend fun saveLogger(content: String) {
        val newReport = Report()

        newReport.content = content
        newReport.isSynced = false
        // ローカルDBへのinsert
        val reportId = reportDao.insert(newReport)

        // Workの登録
        val inputData = Data.Builder()
            .putString("REPORT_CONTENT", content)
            .putInt("REPORT_ID", reportId)
            .build()

        val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(syncWork)
    }

    /**
     * 作業記録同期
     */
    override suspend fun syncUnsyncedReports(reportContent: String, reportId: Int) {
        try {
            Log.d("SyncWorker", "Uploading: $reportContent")
            // 擬似的なネットワーク遅延
            delay(3000.milliseconds)

            // アップロード成功とみなし、DBを更新
            val unsyncedReports = reportDao.getUnSyncedReports()

            for (report in unsyncedReports) {
                if (report.id == reportId) {
                    report.isSynced = true
                    reportDao.update(report)
                }
            }

            Log.d("SyncWorker", "Upload Success for ID: $reportId")
        } catch (e: Exception) {
            Log.e("SyncWorker", "Upload failed", e)
        }
    }
}
