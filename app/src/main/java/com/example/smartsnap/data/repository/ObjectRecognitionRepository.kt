package com.example.smartsnap.data.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.smartsnap.data.config.RecognitionConfig
import com.example.smartsnap.domain.model.RecognizedItem
import com.example.smartsnap.ui.state.RecognitionUiState
import com.example.smartsnap.util.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

/**
 * 物品识别仓库
 *
 * 识别流水线：
 * 1. 压缩输入图片至 1024px 宽
 * 2. ML Kit ObjectDetector 检测画面中的物体位置（bounding box）
 * 3. 取前 N 个检测框，裁剪子图，并行调用 ImageLabeler 进行分类
 * 4. 合并检测框与分类标签，按置信度降序排序，取 Top K 结果
 * 5. 过滤置信度低于阈值的项，有效结果数 = 0 时返回 Failure
 */
@Singleton
class ObjectRecognitionRepository @Inject constructor(
    private val objectDetector: ObjectDetector,
    private val imageLabeler: ImageLabeler,
    private val config: RecognitionConfig
) {

    /**
     * 执行完整的物体识别流程
     * @param bitmap 原始拍照图片，调用后由本方法负责 recycle
     */
    suspend fun recognizeObjects(bitmap: Bitmap): RecognitionUiState =
        withContext(Dispatchers.Default) {
            var compressedBitmap: Bitmap? = null
            try {
                compressedBitmap = BitmapUtils.compressBitmap(bitmap)

                if (bitmap !== compressedBitmap) {
                    bitmap.recycle()
                }

                val inputImage = InputImage.fromBitmap(compressedBitmap, 0)

                // Step 1: 物体检测 — 定位画面中所有显著物体
                val detectedObjects = objectDetector.process(inputImage).await()

                if (detectedObjects.isNullOrEmpty()) {
                    return@withContext RecognitionUiState.Failure
                }

                // Step 2: 限制检测框数量，避免对所有框做分类浪费资源
                val topObjects = detectedObjects.take(config.maxResults * 2)

                // Step 3: 对每个检测框裁剪子图，并行执行图像分类
                val detectionResults = detectAndLabel(topObjects, compressedBitmap)

                // Step 4: 合并、排序、过滤结果
                val resultItems = processResults(detectionResults)

                if (resultItems.isEmpty()) {
                    RecognitionUiState.Failure
                } else {
                    RecognitionUiState.Success(items = resultItems)
                }
            } catch (e: Exception) {
                Timber.e(e, "Object recognition failed")
                RecognitionUiState.Failure
            } finally {
                compressedBitmap?.recycle()
            }
        }

    /**
     * 对每个检测框并行执行：裁剪 → 图像分类
     * 每个协程内部裁剪后立即回收子图 Bitmap
     */
    private suspend fun detectAndLabel(
        objects: List<DetectedObject>,
        bitmap: Bitmap
    ): List<Pair<DetectedObject, List<ImageLabel>>> = coroutineScope {
        objects.map { detectedObject ->
            async {
                val boundingBox = detectedObject.boundingBox
                val croppedBitmap = cropBitmap(bitmap, boundingBox)
                val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
                val labels = imageLabeler.process(inputImage).await()
                croppedBitmap.recycle()
                detectedObject to labels
            }
        }.awaitAll()
    }

    private fun cropBitmap(source: Bitmap, boundingBox: Rect): Bitmap {
        return BitmapUtils.cropBitmap(
            source,
            boundingBox.left,
            boundingBox.top,
            boundingBox.width(),
            boundingBox.height()
        )
    }

    /**
     * 合并检测框与分类结果 → 过滤低置信度 → 降序排序 → 取 Top K
     */
    private fun processResults(
        detectionResults: List<Pair<DetectedObject, List<ImageLabel>>>
    ): List<RecognizedItem> {
        return detectionResults
            .flatMap { (detectedObject, labels) ->
                labels.map { label ->
                    RecognizedItem(
                        label = label.text,
                        confidence = label.confidence,
                        boundingBox = detectedObject.boundingBox,
                        labelIndex = label.index
                    )
                }
            }
            .filter { it.confidence >= config.minConfidence }
            .sortedByDescending { it.confidence }
            .take(config.maxResults)
    }

    /**
     * Google Play Services Task → Kotlin 协程适配器
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result ->
                cont.resumeWith(Result.success(result))
            }
            addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
            addOnCanceledListener {
                cont.cancel()
            }
            cont.invokeOnCancellation { }
        }

    fun release() {
        objectDetector.close()
        imageLabeler.close()
    }
}