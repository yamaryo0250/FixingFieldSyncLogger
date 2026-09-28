package ryo.myappcompany.fixingfieldsynclogger.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ryo.myappcompany.fixingfieldsynclogger.R
import ryo.myappcompany.fixingfieldsynclogger.viewmodel.FieldSyncLoggerViewModel
import java.util.Locale.getDefault

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: FieldSyncLoggerViewModel by viewModels()

    private lateinit var etContent: EditText
    private lateinit var btnSave: Button
    private lateinit var tvReports: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etContent = findViewById(R.id.etContent)
        btnSave = findViewById(R.id.btnSave)
        tvReports = findViewById(R.id.tvReports)

        displayUiSetting()

        btnSave.setOnClickListener {
            val content = etContent.text.toString()
            if (content.isNotEmpty()) {
                viewModel.onSaveAndSyncClicked(content)

                etContent.text.clear()
                displayUiSetting()
            }
        }
    }

    /**
     * 画面表示用設定
     */
    private fun displayUiSetting() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val sb = StringBuilder()

                    if (state.errorMessage != null) {
                        sb.append(getString(state.errorMessage))
                    } else {
                        for (report in state.reports) {
                            val status =
                                getString(if (report.isSynced) R.string.msg_synced else R.string.msg_unsynced)
                            sb.append(
                                "[${report.id}] ${report.content.uppercase(getDefault())} ${
                                    getString(
                                        R.string.msg_sync
                                    )
                                } $status\n"
                            )
                        }
                    }
                    tvReports.text = sb.toString()
                }
            }
        }
    }
}
