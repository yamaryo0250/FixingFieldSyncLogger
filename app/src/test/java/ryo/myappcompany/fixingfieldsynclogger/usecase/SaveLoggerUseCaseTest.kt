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

class SaveLoggerUseCaseTest {
    // テスト対象
    private lateinit var saveLoggerUseCase: SaveLoggerUseCase
    // モック化
    private lateinit var fieldSyncLoggerRepository: FieldSyncLoggerRepository

    @Before
    fun setUp() {
        fieldSyncLoggerRepository = mockk()
        saveLoggerUseCase = SaveLoggerUseCase(fieldSyncLoggerRepository)
    }

    @Test
    fun UseCaseを呼んだ時_内部で正しくRepositoryのメソッドが呼ばれること() = runTest {

        coEvery { fieldSyncLoggerRepository.saveLogger(" Test do ") } just Runs

        saveLoggerUseCase.invoke(" Test do ")

        coVerify(exactly = 1) { fieldSyncLoggerRepository.saveLogger(" Test do ") }
    }

    @Test
    fun Repositoryが投げた例外を_そのまま再スローすること() = runTest {

        val expectedException = Exception("Test Exception")
        coEvery { fieldSyncLoggerRepository.saveLogger("Test") } throws expectedException

        val result = assertFailsWith<Exception> { saveLoggerUseCase.invoke("Test") }

        coVerify(exactly = 1) { fieldSyncLoggerRepository.saveLogger("Test") }

        Assert.assertSame(expectedException, result)
    }
}
