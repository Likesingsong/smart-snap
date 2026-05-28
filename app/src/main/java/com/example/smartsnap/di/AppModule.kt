package com.example.smartsnap.di

import com.example.smartsnap.data.config.RecognitionConfig
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 * 提供识别引擎所需的 ML Kit 客户端单例及配置
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRecognitionConfig(): RecognitionConfig {
        return RecognitionConfig()
    }

    /**
     * ObjectDetector 用于检测画面中物体的位置（bounding box）
     * 基础模型：精度与速度均衡，不启用自带分类（由 ImageLabeler 负责）
     */
    @Provides
    @Singleton
    fun provideObjectDetector(config: RecognitionConfig): ObjectDetector {
        val builder = ObjectDetectorOptions.Builder()
            .setDetectorMode(config.objectDetectionMode)
        val withMulti = if (config.enableMultipleObjects) builder.enableMultipleObjects() else builder
        val withClassify = if (config.enableClassification) withMulti.enableClassification() else withMulti
        return ObjectDetection.getClient(withClassify.build())
    }

    /**
     * ImageLabeler 用于对裁剪后的子图进行分类，返回物品名称和置信度
     * 置信度阈值与 RecognitionConfig.minConfidence 保持一致
     */
    @Provides
    @Singleton
    fun provideImageLabeler(config: RecognitionConfig): ImageLabeler {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(config.minConfidence)
            .build()
        return ImageLabeling.getClient(options)
    }
}