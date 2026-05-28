package com.example.smartsnap.domain.model

import android.graphics.Rect

/**
 * 单个识别结果
 *
 * @param label 物品名称，如 "手机"
 * @param confidence 置信度 0.0 ~ 1.0
 * @param boundingBox 物品在图片中的位置（像素坐标），可为 null
 * @param labelIndex ML Kit 标签索引
 */
data class RecognizedItem(
    val label: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val labelIndex: Int
)