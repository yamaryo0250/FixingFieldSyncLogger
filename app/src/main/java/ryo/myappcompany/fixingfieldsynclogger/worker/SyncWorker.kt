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
        // サーバーへのアップロード処理（非同期通信をモックで表現）
        // 対象は、この実行開始時点で未同期の作業記録すべて
        return try {
            syncUnsyncedReportsUseCase()

            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Upload failed", e)

            Result.failure()
        }
    }
}
