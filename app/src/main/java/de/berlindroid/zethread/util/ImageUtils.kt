package de.berlindroid.zethread.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ImageUtils {

    const val MAX_IMAGE_DIMENSION = 2160

    /**
     * Crops a bitmap to a square centered at the middle.
     * Takes rotation degrees into account if needed.
     */
    fun cropToCenterSquare(source: Bitmap, rotationDegrees: Int = 0): Bitmap {
        val oriented = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val minSide = min(oriented.width, oriented.height)
        val startX = (oriented.width - minSide) / 2
        val startY = (oriented.height - minSide) / 2

        val square = Bitmap.createBitmap(oriented, startX, startY, minSide, minSide)

        return if (square.width > MAX_IMAGE_DIMENSION) {
            Bitmap.createScaledBitmap(square, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION, true)
        } else {
            square
        }
    }

    /**
     * Compresses the bitmap to JPEG (at 90% quality) and converts it to a Base64 string for GitHub API.
     */
    fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Compresses the bitmap to PNG and converts it to a Base64 string for GitHub API.
     */
    fun bitmapToBase64Png(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
