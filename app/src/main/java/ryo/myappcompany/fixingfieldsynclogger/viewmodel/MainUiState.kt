package ryo.myappcompany.fixingfieldsynclogger.viewmodel

import ryo.myappcompany.fixingfieldsynclogger.data.Report

data class MainUiState(
    val reports: List<Report> = emptyList(), // DBから取得したリスト
    val isLoading: Boolean = false, // 取得中・保存中フラグ
    val errorMessage: Int? = null // 取得失敗等のエラー状態
)
