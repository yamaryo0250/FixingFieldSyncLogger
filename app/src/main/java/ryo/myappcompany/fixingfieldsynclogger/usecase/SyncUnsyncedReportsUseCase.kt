package ryo.myappcompany.fixingfieldsynclogger.usecase

import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import javax.inject.Inject

/**
 * 作業記録同期用UseCase
 */
class SyncUnsyncedReportsUseCase @Inject constructor(
    private val fieldSyncLoggerRepository: FieldSyncLoggerRepository
) {
    suspend operator fun invoke(reportContent: String, reportId: Int) {
        fieldSyncLoggerRepository.syncUnsyncedReports(reportContent, reportId)
    }
}
