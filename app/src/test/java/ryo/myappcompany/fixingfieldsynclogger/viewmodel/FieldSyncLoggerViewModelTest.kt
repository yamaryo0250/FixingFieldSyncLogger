package ryo.myappcompany.fixingfieldsynclogger.viewmodel

import android.util.Log
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ryo.myappcompany.fixingfieldsynclogger.R
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.ui.LoggerEvent
import ryo.myappcompany.fixingfieldsynclogger.usecase.LoadReportsUseCase
import ryo.myappcompany.fixingfieldsynclogger.usecase.SaveLoggerUseCase
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@Suppress("NonAsciiCharacters")
@OptIn(ExperimentalCoroutinesApi::class)
class FieldSyncLoggerViewModelTest {

    // モックの準備
    private lateinit var loadReportsUseCase: LoadReportsUseCase
    private lateinit var saveLoggerUseCase: SaveLoggerUseCase

    // テスト対象
    private lateinit var viewModel: FieldSyncLoggerViewModel

    // テスト用のディスパッチャ
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // メインスレッドをテスト用にすり替え
        Dispatchers.setMain(dispatcher)

        // UseCaseのモック化
        loadReportsUseCase = mockk()
        saveLoggerUseCase = mockk()

        // android.util.Logの静的モック化
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()

        // 静的モック化の解除
        unmockkStatic(Log::class)
    }

    @Test
    fun 初期化時_uiStateの初期状態が正しいこと() = runTest(dispatcher) {
        // loadReportsUseCaseが呼ばれた時のモックの振る舞いを定義
        every { loadReportsUseCase() } returns flowOf(emptyList())
        // viewModelの初期化
        viewModel = FieldSyncLoggerViewModel(loadReportsUseCase, saveLoggerUseCase)

        Assert.assertEquals(
            MainUiState(
                reports = emptyList(),
                isLoading = false,
                errorMessage = null
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun 作業記録が取得できた場合_正常系_uiStateに正しく反映されること() = runTest(dispatcher) {
        // loadReportsUseCaseが呼ばれた時のモックの振る舞いを定義
        val reports: List<Report> = listOf(
            Report(
                id = 1,
                content = "TEST",
                isSynced = true
            ),
            Report(
                id = 2,
                content = "TEST_2",
                isSynced = false
            )
        )
        every { loadReportsUseCase() } returns flowOf(
            reports
        )

        // viewModelの初期化
        viewModel = FieldSyncLoggerViewModel(loadReportsUseCase, saveLoggerUseCase)

        viewModel.uiState.test {
            // 購読開始
            awaitItem()  // この時点では、購読開始時点のもの(初期値)が返る

            advanceUntilIdle()

            awaitItem() // この時点でUseCaseの実行結果が反映
        }

        Assert.assertEquals(
            MainUiState(
                reports = reports,
                isLoading = false,
                errorMessage = null
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun 作業記録取得に失敗した場合_異常系_uiStateに正しく反映されること() = runTest(dispatcher) {
        // loadReportsUseCaseが呼ばれた時のモックの振る舞いを定義
        every { loadReportsUseCase() } returns flow { throw Exception("TEST") }

        // viewModelの初期化
        viewModel = FieldSyncLoggerViewModel(loadReportsUseCase, saveLoggerUseCase)

        viewModel.uiState.test {

            awaitItem()

            advanceUntilIdle()

            awaitItem()
        }

        Assert.assertEquals(
            MainUiState(
                reports = emptyList(),
                isLoading = false,
                errorMessage = R.string.msg_load_error
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun onSaveAndSyncClicked_正常系_isLoadingが切り替わり_保存処理が呼ばれること() =
        runTest(dispatcher) {
            every { loadReportsUseCase() } returns flowOf(emptyList())

            // 保存処理をスタブ(処理完了まで)
            val saveFinished = CompletableDeferred<Unit>()

            coEvery { saveLoggerUseCase("DO TEST") } coAnswers {
                saveFinished.await()
            }

            viewModel = FieldSyncLoggerViewModel(loadReportsUseCase, saveLoggerUseCase)

            // viewModelの初期化完了まで進める
            viewModel.uiState.test {
                awaitItem()

                advanceUntilIdle()

                viewModel.uiEvent.test {
                    // onSaveAndSyncClicked()を実行
                    val result = viewModel.onSaveAndSyncClicked("DO TEST")

                    // onSaveAndSyncClicked()開始後、次のsuspendポイントまで進める
                    runCurrent()
                    // (「saveFinished.await()」により、suspendポイント(SaveLoggerUseCase.invoke())で止まる)

                    // 保存処理が呼ばれること
                    coVerify(exactly = 1) { saveLoggerUseCase("DO TEST") }

                    // 「saveFinished.await()」による中断を解く
                    saveFinished.complete(Unit)

                    // 保存処理が完了するまで進める
                    advanceUntilIdle()

                    // 完了まで `SaveFailure` が無いこと
                    expectNoEvents()

                    // onSaveAndSyncClicked()の戻り値がTrueであること
                    Assert.assertTrue(result)

                    cancelAndIgnoreRemainingEvents()
                }

                // isLoadingがtrueに切り替わること
                Assert.assertEquals(
                    MainUiState(
                        reports = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    ),
                    awaitItem()
                )

                // isLoadingがfalseに切り替わること
                Assert.assertEquals(
                    MainUiState(
                        reports = emptyList(),
                        isLoading = false,
                        errorMessage = null
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun onSaveAndSyncClicked_異常系_DB保存失敗イベントが送られること() = runTest(dispatcher) {
        every { loadReportsUseCase() } returns flowOf(emptyList())

        // 保存処理をスタブ(処理完了まで)
        coEvery { saveLoggerUseCase("DO TEST") } coAnswers {
            kotlinx.coroutines.delay(1000.milliseconds)
            throw IOException("TEST")
        }

        viewModel = FieldSyncLoggerViewModel(loadReportsUseCase, saveLoggerUseCase)

        // viewModelの初期化完了まで進める
        viewModel.uiState.test {
            awaitItem()

            advanceUntilIdle()

            viewModel.uiEvent.test {
                // onSaveAndSyncClicked()を実行
                val result = viewModel.onSaveAndSyncClicked("DO TEST")

                runCurrent()

                // 保存処理が呼ばれること
                coVerify(exactly = 1) { saveLoggerUseCase("DO TEST") }

                // 保存処理が完了するまで進める
                advanceUntilIdle()

                Assert.assertEquals(
                    LoggerEvent.SaveFailure(R.string.msg_save_error),
                    awaitItem()
                )

                // onSaveAndSyncClicked()の戻り値がTrueであること
                Assert.assertTrue(result)
            }

            // isLoadingがtrueに切り替わること
            Assert.assertEquals(
                MainUiState(
                    reports = emptyList(),
                    isLoading = true,
                    errorMessage = null
                ),
                awaitItem()
            )

            // isLoadingがfalseに切り替わること
            Assert.assertEquals(
                MainUiState(
                    reports = emptyList(),
                    isLoading = false,
                    errorMessage = null
                ),
                awaitItem()
            )
        }
    }
}
