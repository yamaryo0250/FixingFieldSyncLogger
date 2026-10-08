package ryo.myappcompany.fixingfieldsynclogger.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ryo.myappcompany.fixingfieldsynclogger.R
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.ui.LoggerEvent
import ryo.myappcompany.fixingfieldsynclogger.usecase.LoadReportsUseCase
import ryo.myappcompany.fixingfieldsynclogger.usecase.SaveLoggerUseCase
import javax.inject.Inject

/**
 *  ViewModel
 *
 *  @param loadReportsUseCase 作業記録取得用UseCase
 *  @param saveLoggerUseCase 作業記録保存用UseCase
 */
@HiltViewModel
class FieldSyncLoggerViewModel @Inject constructor(
    private val loadReportsUseCase: LoadReportsUseCase,
    private val saveLoggerUseCase: SaveLoggerUseCase,
) : ViewModel() {
    // 取得中・保存中フラグ
    private val _isLoading = MutableStateFlow(false)

    // 作業記録取得時エラー
    private val _hasLoadError = MutableStateFlow(false)

    // DBから取得したリスト
    private val reportsState: Flow<List<Report>> = loadReportsUseCase()
        .catch { e ->
            Log.e("FieldSyncLoggerViewModel", "Failed to load reports", e)

            _hasLoadError.value = true

            // Flowが終了しないように、空のリストをフォールバックとして流す
            emit(emptyList())
        }

    // 作業記録時イベント(現時点で、主にDB保存失敗のハンドリング用)
    private val _uiEvent = Channel<LoggerEvent>(Channel.BUFFERED)
    // ※公開用
    val uiEvent: Flow<LoggerEvent> = _uiEvent.receiveAsFlow()

    // DBから取得した「作業記録リスト」と、「フラグ」をセットにして、画面表示用として管理する
    val uiState: StateFlow<MainUiState> = combine(
        reportsState,
        _isLoading,
        _hasLoadError
    ) { state, isLoading, hasLoadError ->
        MainUiState(
            reports = state,
            isLoading = isLoading,
            errorMessage = if (hasLoadError) R.string.msg_load_error else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    /**
     * クリック時の作業記録保存＆同期
     *
     * 未入力、または保存中の再クリックは受け付けない
     *
     * @param content 作業内容
     * @return 保存を開始した場合は true
     */
    fun onSaveAndSyncClicked(content: String): Boolean {
        if (_isLoading.value || content.isBlank()) {
            return false
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                saveLoggerUseCase(content)
            } catch (e: Exception) {
                Log.e("FieldSyncLoggerViewModel", "Save logger failed.", e)
                _uiEvent.send(
                    LoggerEvent.SaveFailure(R.string.msg_save_error)
                )
            } finally {
                _isLoading.value = false
            }
        }
        return true
    }
}
