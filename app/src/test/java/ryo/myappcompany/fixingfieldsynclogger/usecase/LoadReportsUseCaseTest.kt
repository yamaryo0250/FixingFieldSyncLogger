@file:Suppress("NonAsciiCharacters")

package ryo.myappcompany.fixingfieldsynclogger.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import org.junit.Assert.assertSame
import org.junit.Test

class LoadReportsUseCaseTest {

    private val repository: FieldSyncLoggerRepository = mockk()
    private val useCase = LoadReportsUseCase(repository)

    /**
     * 正常系: loadReports() が1回呼ばれ、useCase() が返す Flow が
     * リポジトリの返した Flow と同一であること
     */
    @Test
    fun 作業記録取得処理_正常系_repositoryのloadReportsが1回呼ばれ返されたFlowがそのまま返されること() {
        val expected = flowOf(emptyList<Nothing>())
        every { repository.loadReports() } returns expected

        val actual = useCase()

        assertSame(expected, actual)
        verify(exactly = 1) { repository.loadReports() }
    }
}
