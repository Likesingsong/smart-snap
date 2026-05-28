package com.example.smartsnap.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.coroutines.resumeWithException

/**
 * CameraX 相机管理器
 *
 * 封装相机生命周期管理、预览和拍照功能
 * 通过 ProcessCameraProvider 绑定 Preview + ImageCapture 用例到 LifecycleOwner
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraStarted = false

    /**
     * 创建 PreviewView 供 Compose 嵌入
     * 使用 COMPATIBLE 模式确保兼容性
     */
    fun getPreviewView(): PreviewView {
        return PreviewView(context).also { previewView ->
            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    /**
     * 启动相机预览
     * 重复调用安全：已有绑定则跳过，避免重复 bind 导致崩溃
     */
    fun startCamera(previewView: PreviewView) {
        if (isCameraStarted) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(1280, 720))
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                isCameraStarted = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to bind camera use cases")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 拍照并返回 Bitmap
     * 挂起函数，图片处理在回调线程完成
     */
    suspend fun takePhoto(): Bitmap = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("ImageCapture not initialized"))
            return@suspendCancellableCoroutine
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) {
                        continuation.resumeWith(Result.success(bitmap))
                    } else {
                        continuation.resumeWithException(
                            RuntimeException("Failed to convert image to bitmap")
                        )
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e(exception, "Photo capture failed")
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    fun release() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        isCameraStarted = false
    }

    /**
     * ImageProxy → Bitmap 转换，自动处理旋转
     * @return null 如果解码失败
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return null

        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it != bitmap) bitmap.recycle() }
        }
        return bitmap
    }
}