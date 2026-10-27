@file:Suppress("NonAsciiCharacters")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ryo.myappcompany.fixingfieldsynclogger.repository

import android.util.Log
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.data.ReportDao
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorkEnqueuer
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorker
import kotlin.test.assertFailsWith

class FieldSyncLoggerRepositoryImplTest {
    // テスト対象
    private lateinit var repository: FieldSyncLoggerRepository

    // モック化
    private lateinit var syncWorkEnqueuer: SyncWorkEnqueuer
    private lateinit var reportDao: ReportDao

    @Before
    fun setUp() {
        syncWorkEnqueuer = mockk()
        reportDao = mockk()
        repository = FieldSyncLoggerRepositoryImpl(syncWorkEnqueuer, reportDao)

        // android.util.Logの静的モック化
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        // 静的モック化の解除
        unmockkStatic(Log::class)
    }

    @Test
    fun 作業記録取得処理_正常系_返り値が期待値通りだった場合_そのまま返すこと() = runTest {
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

        val flowReports = flowOf(reports)

        every { reportDao.getAllReports() } returns flowReports

        // ReportDao.getAllReports()が返したFlowをそのまま返すこと
        Assert.assertSame(flowReports, repository.loadReports())

        // ReportDao.getAllReports()を実行していること
        verify (exactly = 1) { reportDao.getAllReports() }
    }

    @Test
    fun 作業記録保存_正常系_受け取った作業記録情報をDB登録し_enqueueしていること() =
        runTest {
            val testNewReport = Report(content = "TEST",
                isSynced = false)

            coEvery { reportDao.insert(any()) } coAnswers { 1.toLong() }
            every { syncWorkEnqueuer.enqueue(any()) } just Runs

            repository.saveLogger("TEST")

            // ReportDao.insert()へ渡すReportが引数のcontent、かつisSynced = falseであること
            // ReportDao.insert()を1回実行していること
            coVerify(exactly = 1) {reportDao.insert(testNewReport)}

            // enqueueへ渡すWorkRequestがSyncWorkerのOneTimeWorkRequest、かつrequiredNetworkType が NetworkType.CONNECTED であること
            verify(exactly = 1) {
                syncWorkEnqueuer.enqueue(withArg { request ->
                    Assert.assertTrue(request is OneTimeWorkRequest)
                    Assert.assertEquals(
                        SyncWorker::class.java.name,
                        request.workSpec.workerClassName
                    )
                    Assert.assertEquals(
                        NetworkType.CONNECTED,
                        request.workSpec.constraints.requiredNetworkType
                    )
                })
            }

            // ReportDao.insert()の後で、SyncWorkEnqueuer.enqueue()を1回実行していること
            coVerifyOrder {
                reportDao.insert(testNewReport)
                syncWorkEnqueuer.enqueue(any())
            }
        }

    @Test
    fun 作業記録保存_異常系_DB保存でExceptionが発生した場合_そのまま再スローすること() = runTest {
        // ReportDao.insert()へ渡すReportが引数のcontent、かつisSynced = falseであること
        val testNewReport = Report(content = "TEST", isSynced = false)

        coEvery { reportDao.insert(any()) } coAnswers {
            throw Exception("TEST")
        }

        // Exceptionを再スローしていること
        assertFailsWith<Exception> {
            repository.saveLogger("TEST")
        }

        // ReportDao.insert()へ渡すReportが引数のcontent、かつisSynced = falseであること
        // ReportDao.insert()を1回実行していること
        coVerify(exactly = 1) {reportDao.insert(testNewReport)}

        // ReportDao.insert()を1回実行していること
        coVerify(exactly = 1) { reportDao.insert(testNewReport) }

        // SyncWorkEnqueuer.enqueue()を実行していないこと
        verify(exactly = 0) { syncWorkEnqueuer.enqueue(any()) }
    }

    @Test
    fun 作業記録保存_異常系_enqueueでExceptionが発生した場合_そのまま再スローすること() = runTest {
        val testNewReport = Report(content = "TEST", isSynced = false)

        coEvery { reportDao.insert(any()) } coAnswers { 1.toLong() }
        every { syncWorkEnqueuer.enqueue(any()) } throws Exception("TEST")

        // Exceptionを再スローしていること
        assertFailsWith<Exception> {
            repository.saveLogger("TEST")
        }

        // ReportDao.insert()へ渡すReportが引数のcontent、かつisSynced = falseであること
        // ReportDao.insert()を1回実行していること
        coVerify(exactly = 1) {reportDao.insert(testNewReport)}

        // SyncWorkEnqueuer.enqueue()を実行していること
        verify(exactly = 1) { syncWorkEnqueuer.enqueue(any()) }
    }

    @Test
    fun 作業記録同期_正常系_未同期の作業記録を取得し_同期完了後_同期済みに更新されること() = runTest {
        // ※2件以上の未同期データで確認
        val unsyncedReports: List<Report> = listOf(
            Report(
                id = 1,
                content = "TEST",
                isSynced = false
            ),
            Report(
                id = 2,
                content = "TEST_2",
                isSynced = false
            )
        )

        coEvery { reportDao.getUnSyncedReports() } returns unsyncedReports
        coEvery { reportDao.update(any()) } just Runs

        repository.syncUnsyncedReports()

        // ReportDao.getUnSyncedReports()を1回実行していること
        coVerify(exactly = 1) { reportDao.getUnSyncedReports() }

        advanceUntilIdle()

        val syncedReports: List<Report> = unsyncedReports.map { report ->
             report.copy(isSynced = true)
         }

        // 取得した未同期のidとcontentはそのままに、isSynced = trueにしたリストで、ReportDao.update()を1回実行していること
        coVerify(exactly = 1) { reportDao.update(syncedReports) }
    }

    @Test
    fun 作業記録同期_正常系_未同期の作業記録が無い場合_そのまま処理終了すること() = runTest {
        coEvery { reportDao.getUnSyncedReports() } returns emptyList()

        repository.syncUnsyncedReports()

        // ReportDao.getUnSyncedReports()を1回実行していること
        coVerify(exactly = 1) { reportDao.getUnSyncedReports() }

        // ReportDao.update()を実行していないこと
        coVerify(exactly = 0) { reportDao.update(any()) }
    }

    @Test
    fun 作業記録同期_異常系_ネットワーク遅延中にキャンセルされた場合_CancellationExceptionが再スローされること() =
        runTest {
            // ※2件以上の未同期データで確認
            val unsyncedReports: List<Report> = listOf(
                Report(
                    id = 1,
                    content = "TEST",
                    isSynced = false
                ),
                Report(
                    id = 2,
                    content = "TEST_2",
                    isSynced = false
                )
            )

            // ReportDao.getUnSyncedReports()が1件以上のリストを返すこと
            coEvery { reportDao.getUnSyncedReports() } returns unsyncedReports

            val deferred = async {
                repository.syncUnsyncedReports()
            }

            // delayによる遅延シミュレートまで進める
            runCurrent()

            // 遅延中にキャンセル
            deferred.cancel()

            // CancellationExceptionが再スローされること
            assertFailsWith<CancellationException> {
                deferred.await()
            }

            // ReportDao.getUnSyncedReports()を1回実行していること
            coVerify(exactly = 1) { reportDao.getUnSyncedReports() }

            // ReportDao.update()を実行していないこと
            coVerify(exactly = 0) { reportDao.update(any()) }
        }

    @Test
    fun 作業記録同期_異常系_未同期の作業記録取得でExceptionが発生した場合_そのまま再スローすること() = runTest {
        coEvery { reportDao.getUnSyncedReports() } throws Exception("TEST")

        // Exceptionを再スローしていること
        assertFailsWith<Exception> { repository.syncUnsyncedReports() }

        // ReportDao.getUnSyncedReports()を実行していること
        coVerify(exactly = 1) { reportDao.getUnSyncedReports() }

        // ReportDao.update()を実行していないこと
        coVerify(exactly = 0) { reportDao.update(any()) }
    }

    @Test
    fun 作業記録同期_異常系_同期済みの更新でExceptionが発生した場合_そのまま再スローすること() = runTest {
        val unsyncedReports: List<Report> = listOf(
            Report(
                id = 1,
                content = "TEST",
                isSynced = false
            ),
            Report(
                id = 2,
                content = "TEST_2",
                isSynced = false
            )
        )

        // ReportDao.getUnSyncedReports()が1件以上のリストを返すこと
        coEvery { reportDao.getUnSyncedReports() } returns unsyncedReports
        coEvery { reportDao.update(any()) } throws Exception("TEST")

        // Exceptionを再スローしていること
        assertFailsWith<Exception> { repository.syncUnsyncedReports() }

        // ReportDao.getUnSyncedReports()を実行していること
        coVerify(exactly = 1) { reportDao.getUnSyncedReports() }

        // ReportDao.update()を実行していること
        coVerify(exactly = 1) { reportDao.update(any()) }
    }
}
