package ryo.myappcompany.fixingfieldsynclogger.worker

import androidx.work.WorkManager
import androidx.work.WorkRequest
import javax.inject.Inject

/**
 * 同期処理WorkManagerのキュー登録用実装クラス
 */
class WorkManagerSyncWorkEnqueuer @Inject constructor(
    private val workManager: WorkManager
) : SyncWorkEnqueuer {
    override fun enqueue(request: WorkRequest) {
        workManager.enqueue(request)
    }
}
