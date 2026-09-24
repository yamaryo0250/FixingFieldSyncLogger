package ryo.myappcompany.fixingfieldsynclogger.usecase

import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import javax.inject.Inject

/**
 * 作業記録保存用UseCase
 */
class SaveLoggerUseCase @Inject constructor(
    private val fieldSyncLoggerRepository: FieldSyncLoggerRepository
) {
    suspend operator fun invoke(content: String) {
        fieldSyncLoggerRepository.saveLogger(content)
    }
}
