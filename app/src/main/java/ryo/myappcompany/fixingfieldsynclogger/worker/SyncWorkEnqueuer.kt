package ryo.myappcompany.fixingfieldsynclogger.worker

import androidx.work.WorkRequest

/**
 * 同期処理WorkManagerのキュー登録用interface
 */
fun interface SyncWorkEnqueuer {
    fun enqueue(request: WorkRequest)
}
