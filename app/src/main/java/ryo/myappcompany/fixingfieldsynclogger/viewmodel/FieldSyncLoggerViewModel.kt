package ryo.myappcompany.fixingfieldsynclogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    // ※公開用
    val isLoading: StateFlow<Boolean> = _isLoading

    // DBから取得したリスト
    private val reportsState: Flow<List<Report>> = loadReportsUseCase()

    // 作業記録時イベント(現時点で、主にDB保存失敗のハンドリング用)
    private val _uiEvent = Channel<LoggerEvent>(Channel.BUFFERED)
    // ※公開用
    val uiEvent: Flow<LoggerEvent> = _uiEvent.receiveAsFlow()

    // DBから取得した「作業記録リスト」と、「フラグ」をセットにして、画面表示用として管理する
    val uiState: StateFlow<MainUiState> = combine(
        reportsState,
        isLoading
    ) { state, isLoading ->
        MainUiState(
            reports = state,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    /**
     * クリック時の作業記録保存＆同期
     *
     * @param content 作業内容
     */
    fun onSaveAndSyncClicked(content: String) {
        viewModelScope.launch {
            saveLoggerUseCase(content)
        }
    }
}
