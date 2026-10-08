package ryo.myappcompany.fixingfieldsynclogger.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ryo.myappcompany.fixingfieldsynclogger.R
import ryo.myappcompany.fixingfieldsynclogger.databinding.ActivityMainBinding
import ryo.myappcompany.fixingfieldsynclogger.viewmodel.FieldSyncLoggerViewModel
import java.util.Locale.getDefault

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: FieldSyncLoggerViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayUiSetting()

        binding.etContent.doOnTextChanged { _, _, _, _ ->
            updateSaveButtonEnabled(viewModel.uiState.value.isLoading)
        }

        binding.btnSave.setOnClickListener {
            val content = binding.etContent.text.toString()
            if (viewModel.onSaveAndSyncClicked(content)) {
                binding.btnSave.isEnabled = false
                binding.etContent.text.clear()
            }
        }
    }

    /**
     * 未入力、または保存中はボタンを押せないようにする
     */
    private fun updateSaveButtonEnabled(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading && binding.etContent.text.toString().isNotBlank()
    }

    /**
     * 画面表示用設定
     */
    private fun displayUiSetting() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateSaveButtonEnabled(state.isLoading)

                        val sb = StringBuilder()

                        if (state.errorMessage != null) {
                            sb.append(getString(state.errorMessage))
                        } else {
                            for (report in state.reports) {
                                val status =
                                    getString(if (report.isSynced) R.string.msg_synced else R.string.msg_unsynced)
                                sb.append(
                                    "[${report.id}] ${report.content?.uppercase(getDefault())} ${
                                        getString(
                                            R.string.msg_sync
                                        )
                                    } $status\n"
                                )
                            }
                        }
                        binding.tvReports.text = sb.toString()
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is LoggerEvent.SaveFailure -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(event.message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
