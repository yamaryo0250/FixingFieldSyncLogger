package ryo.myappcompany.fixingfieldsynclogger.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import ryo.myappcompany.fixingfieldsynclogger.data.AppDatabase
import java.io.IOException

class SyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val reportContent = inputData.getString("REPORT_CONTENT")
        val reportId = inputData.getInt("REPORT_ID", -1)

        if (reportContent == null || reportId == -1) {
            return Result.failure()
        }

        // サーバーへのアップロード処理（非同期通信をモックで表現）
        Thread {
            try {
                Log.d("SyncWorker", "Uploading: $reportContent")
                // 擬似的なネットワーク遅延
                Thread.sleep(3000)

                // アップロード成功とみなし、DBを更新
                val db = AppDatabase.getInstance(applicationContext)
                val reports = db.reportDao().unsyncedReports

                for (report in reports) {
                    if (report.id == reportId) {
                        report.isSynced = true
                        db.reportDao().update(report)
                    }
                }
                Log.d("SyncWorker", "Upload Success for ID: $reportId")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Upload failed", e)
            }
        }.start()

        return Result.success()
    }
}
