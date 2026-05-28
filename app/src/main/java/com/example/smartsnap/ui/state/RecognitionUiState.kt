package com.example.smartsnap.ui.state

import android.graphics.Bitmap
import com.example.smartsnap.domain.model.RecognizedItem

/**
 * UI 状态密封类，表示识别流程的四种状态
 *
 * Idle    — 空闲，等待用户拍照
 * Loading — 识别进行中，显示加载动画
 * Success — 识别成功，携带 Top 2-3 个结果
 * Failure — 识别失败，显示错误提示和重试按钮
 */
sealed class RecognitionUiState {
    data object Idle : RecognitionUiState()
    data object Loading : RecognitionUiState()
    data class Success(
        val items: List<RecognizedItem>,
        val annotatedBitmap: Bitmap? = null  // v1.1 预留：带标注框的图片
    ) : RecognitionUiState()
    data object Failure : RecognitionUiState()
}