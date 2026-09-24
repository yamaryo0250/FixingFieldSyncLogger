package ryo.myappcompany.fixingfieldsynclogger.ui

/**
 * 作業記録イベントinterface
 *
 * 現時点では、作業記録保存時におけるDB保存失敗のハンドリング用
 */
interface LoggerEvent {
    val message: Int

    /**
     * DB保存失敗
     */
    data class SaveFailure(
        override val message: Int
    ) : LoggerEvent
}
