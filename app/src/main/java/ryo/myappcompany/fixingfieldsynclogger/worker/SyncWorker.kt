package ryo.myappcompany.fixingfieldsynclogger.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ryo.myappcompany.fixingfieldsynclogger.usecase.SyncUnsyncedReportsUseCase
import java.io.IOException

/**
 * 同期処理用のWorker
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUnsyncedReportsUseCase: SyncUnsyncedReportsUseCase
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val reportContent = inputData.getString("REPORT_CONTENT")
        val reportId = inputData.getInt("REPORT_ID", -1)

        if (reportContent == null || reportId == -1) {
            return Result.failure()
        }

        // サーバーへのアップロード処理（非同期通信をモックで表現）
        Log.d("SyncWorker", "Uploading: $reportContent")

        return try {
            syncUnsyncedReportsUseCase(reportContent, reportId)

            Log.d("SyncWorker", "Upload Success for ID: $reportId")

            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Upload failed", e)

            Result.failure()
        }

    }
}
