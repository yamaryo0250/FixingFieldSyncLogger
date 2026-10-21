package ryo.myappcompany.fixingfieldsynclogger.repository

import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.data.ReportDao
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorkEnqueuer
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorker
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class FieldSyncLoggerRepositoryImpl @Inject constructor(
    private val syncWorkEnqueuer: SyncWorkEnqueuer,
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

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Workの登録。同期対象は実行時点の未同期レコード全て
        val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setConstraints(constraints)
            .build()

        syncWorkEnqueuer.enqueue(syncWork)
    }

    /**
     * 作業記録同期
     *
     * 処理開始時に未同期の作業記録を確定し、そのすべてを同期する
     */
    override suspend fun syncUnsyncedReports() {
        val unsyncedReports = reportDao.getUnSyncedReports()
        if (unsyncedReports.isEmpty()) {
            return
        }

        for ((_, content) in unsyncedReports) {
            Log.d("SyncWorker", "Uploading: $content")
        }
        // 擬似的なネットワーク遅延
        delay(3000.milliseconds)

        // アップロード成功とみなし、実行開始時の未同期分をすべて更新
        val updatedReports = unsyncedReports.map { it.copy(isSynced = true) }
        reportDao.update(updatedReports)

        Log.d(
            "SyncWorker",
            "Upload Success for IDs: ${unsyncedReports.joinToString { it.id.toString() }}"
        )
    }
}
