package com.example.smartsnap.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

/**
 * Bitmap 工具类：压缩、裁剪、旋转、字节数组互转
 */
object BitmapUtils {

    /** 最大宽度限制，超过此值将等比缩放 */
    private const val MAX_WIDTH = 1024

    /** JPEG 压缩质量（0-100） */
    private const val COMPRESS_QUALITY = 90

    /**
     * 等比压缩图片至指定最大宽度
     * 若宽度已 ≤ maxWidth 则直接返回原图（不创建新对象）
     */
    fun compressBitmap(source: Bitmap, maxWidth: Int = MAX_WIDTH): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= maxWidth) {
            return source
        }

        val ratio = maxWidth.toFloat() / width
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(source, maxWidth, newHeight, true)
    }

    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = COMPRESS_QUALITY): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    fun byteArrayToBitmap(data: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

    /**
     * 按指定角度旋转图片
     * @param rotation 旋转角度（0/90/180/270）
     */
    fun rotateBitmapIfNeeded(bitmap: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * 裁剪图片指定区域，自动 clamp 越界坐标
     */
    fun cropBitmap(source: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        val clampedLeft = left.coerceIn(0, source.width)
        val clampedTop = top.coerceIn(0, source.height)
        val clampedWidth = width.coerceIn(0, source.width - clampedLeft)
        val clampedHeight = height.coerceIn(0, source.height - clampedTop)

        return Bitmap.createBitmap(source, clampedLeft, clampedTop, clampedWidth, clampedHeight)
    }
}