@file:Suppress("NonAsciiCharacters")

package ryo.myappcompany.fixingfieldsynclogger.usecase

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import kotlin.test.assertFailsWith

class SyncUnsyncedReportsUseCaseTest {
    // テスト対象
    private lateinit var syncUnsyncedReportsUseCase: SyncUnsyncedReportsUseCase
    // モック化
    private lateinit var fieldSyncLoggerRepository: FieldSyncLoggerRepository

    @Before
    fun setUp() {
        fieldSyncLoggerRepository = mockk()
        syncUnsyncedReportsUseCase = SyncUnsyncedReportsUseCase(fieldSyncLoggerRepository)
    }

    @Test
    fun UseCaseを呼んだ時_内部で正しくRepositoryのメソッドが呼ばれること() = runTest {

        coEvery { fieldSyncLoggerRepository.syncUnsyncedReports() } just Runs

        syncUnsyncedReportsUseCase.invoke()

        coVerify(exactly = 1) { fieldSyncLoggerRepository.syncUnsyncedReports() }
    }

    @Test
    fun Repositoryが投げた例外を_そのまま再スローすること() = runTest {

        val expectedException = Exception("TEST")
        coEvery { fieldSyncLoggerRepository.syncUnsyncedReports() } throws expectedException

        val result = assertFailsWith<Exception> { syncUnsyncedReportsUseCase.invoke() }

        Assert.assertSame(expectedException, result)
    }
}
