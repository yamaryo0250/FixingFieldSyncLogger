package ryo.myappcompany.fixingfieldsynclogger.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import ryo.myappcompany.fixingfieldsynclogger.R
import ryo.myappcompany.fixingfieldsynclogger.data.AppDatabase
import ryo.myappcompany.fixingfieldsynclogger.data.Report
import ryo.myappcompany.fixingfieldsynclogger.worker.SyncWorker
import java.util.Locale.getDefault

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var etContent: EditText
    private lateinit var btnSave: Button
    private lateinit var tvReports: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etContent = findViewById(R.id.etContent)
        btnSave = findViewById(R.id.btnSave)
        tvReports = findViewById(R.id.tvReports)

        db = AppDatabase.getInstance(this)

        updateUi()

        btnSave.setOnClickListener {
            val content = etContent.text.toString()
            if (content.isNotEmpty()) {
                val newReport = Report()
                newReport.content = content
                newReport.isSynced = false

                // DB保存
                db.reportDao().insert(newReport)

                // 最新のIDを取得するために全件取得して最後のものを取る
                val allReports = db.reportDao().allReports
                val savedReport = allReports.last()

                // Workerの実行
                val inputData = Data.Builder()
                    .putString("REPORT_CONTENT", savedReport.content)
                    .putInt("REPORT_ID", savedReport.id)
                    .build()

                val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(this).enqueue(syncWork)

                etContent.text.clear()
                updateUi()
            }
        }
    }

    private fun updateUi() {
        val reports = db.reportDao().allReports
        val sb = StringBuilder()
        for (report in reports) {
            val status = if (report.isSynced) "済" else "未"
            sb.append("[${report.id}] ${report.content.uppercase(getDefault())} - 同期: $status\n")
        }
        tvReports.text = sb.toString()
    }
}
