package ryo.myappcompany.fixingfieldsynclogger.repository

import android.content.Context
import android.util.Log
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
        val newReport = Report(
            content = content,
            isSynced = false
        )

        // ローカルDBへのinsert
        reportDao.insert(newReport)

        // Workの登録。同期対象は実行時点の未同期レコード全て
        val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java).build()

        WorkManager.getInstance(context).enqueue(syncWork)
    }

    /**
     * 作業記録同期
     *
     * 処理開始時に未同期の作業記録を確定し、そのすべてを同期する
     */
    override suspend fun syncUnsyncedReports() {
        try {
            val unsyncedReports = reportDao.getUnSyncedReports()
            if (unsyncedReports.isEmpty()) {
                return
            }

            for (report in unsyncedReports) {
                Log.d("SyncWorker", "Uploading: ${report.content}")
            }
            // 擬似的なネットワーク遅延
            delay(3000.milliseconds)

            // アップロード成功とみなし、実行開始時の未同期分をすべて更新
            for (report in unsyncedReports) {
                reportDao.update(report.copy(isSynced = true))
            }

            Log.d(
                "SyncWorker",
                "Upload Success for IDs: ${unsyncedReports.joinToString { it.id.toString() }}"
            )
        } catch (e: Exception) {
            Log.e("SyncWorker", "Upload failed", e)
        }
    }
}
