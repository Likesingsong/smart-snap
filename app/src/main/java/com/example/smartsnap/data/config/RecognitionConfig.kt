package com.example.smartsnap.data.config

import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * 识别参数配置
 *
 * @param minConfidence 最低置信度阈值，低于此值的结果将被过滤
 * @param maxResults 最大返回结果数（最终显示 2-3 个）
 * @param objectDetectionMode 检测模式（SINGLE_IMAGE / STREAM）
 * @param enableMultipleObjects 是否检测多个物体
 * @param enableClassification 是否启用 ObjectDetector 自带分类（关闭，由 ImageLabeler 负责）
 */
data class RecognitionConfig(
    val minConfidence: Float = 0.5f,
    val maxResults: Int = 3,
    val objectDetectionMode: Int = ObjectDetectorOptions.SINGLE_IMAGE_MODE,
    val enableMultipleObjects: Boolean = true,
    val enableClassification: Boolean = false
)