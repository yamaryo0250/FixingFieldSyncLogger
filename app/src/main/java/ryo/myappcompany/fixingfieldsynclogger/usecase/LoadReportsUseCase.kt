package ryo.myappcompany.fixingfieldsynclogger.usecase

import kotlinx.coroutines.flow.Flow
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import javax.inject.Inject

/**
 * 作業記録取得用UseCase
 */
class LoadReportsUseCase @Inject constructor(
    private val fieldSyncLoggerRepository: FieldSyncLoggerRepository
) {
    operator fun invoke(): Flow<List<Report>> {
        return fieldSyncLoggerRepository.loadReports()
    }
}
