@file:Suppress("NonAsciiCharacters")

package ryo.myappcompany.fixingfieldsynclogger.worker

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ryo.myappcompany.fixingfieldsynclogger.usecase.SyncUnsyncedReportsUseCase
import java.io.IOException
import kotlin.test.assertFailsWith

class SyncWorkerTest {
    // テスト対象
    private lateinit var syncWorker: SyncWorker
    // モック化
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var syncUnsyncedReportsUseCase: SyncUnsyncedReportsUseCase

    @Before
    fun setUp() {
        context = mockk()
        workerParams = mockk()
        syncUnsyncedReportsUseCase = mockk()
        syncWorker = SyncWorker(context, workerParams, syncUnsyncedReportsUseCase)

        // android.util.Logの静的モック化
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        // 静的モック化の解除
        unmockkStatic(Log::class)
    }

    @Test
    fun 正常系_SyncUnsyncedReportsUseCaseが実行され_successが返ること() = runTest {
        coEvery { syncUnsyncedReportsUseCase() } just Runs

        val result = syncWorker.doWork()
        // syncUnsyncedReportsUseCase()が実行されること
        coVerify(exactly = 1) { syncUnsyncedReportsUseCase() }
        // Result.successが返ること
        Assert.assertEquals(Result.success(), result)
    }

    @Test
    fun 異常系_IOEXCEPTIONが発生した場合_retryが返ること() = runTest {
        coEvery { syncUnsyncedReportsUseCase() } throws IOException("TEST")

        val result = syncWorker.doWork()
        coVerify(exactly = 1) { syncUnsyncedReportsUseCase() }
        // Result.retryが返ること
        Assert.assertEquals(Result.retry(), result)
    }

    @Test
    fun 異常系_CancellationExceptionが発生した場合_再スローされること() = runTest {
        coEvery { syncUnsyncedReportsUseCase() } throws CancellationException("TEST")

        // 再スローされること
        assertFailsWith<CancellationException> {
            syncWorker.doWork()
        }

        coVerify(exactly = 1) { syncUnsyncedReportsUseCase() }
    }

    @Test
    fun 異常系_その他のExceptionが発生した場合_failureが返ること() = runTest {
        coEvery { syncUnsyncedReportsUseCase() } throws Exception("TEST")

        val result = syncWorker.doWork()
        coVerify(exactly = 1) { syncUnsyncedReportsUseCase() }
        // Result.failureが返ること
        Assert.assertEquals(Result.failure(), result)
    }
}
