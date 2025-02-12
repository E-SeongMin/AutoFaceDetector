package com.geek.autofacecapture.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object DataUtil {

    fun yuv420ToBitmap(imageProxy: ImageProxy): Bitmap? {
        val width = imageProxy.width
        val height = imageProxy.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + 2 * uvSize)

        val yBuffer: ByteBuffer = imageProxy.planes[0].buffer
        val uBuffer: ByteBuffer = imageProxy.planes[1].buffer
        val vBuffer: ByteBuffer = imageProxy.planes[2].buffer

        var rowStride = imageProxy.planes[0].rowStride
        var pos = 0

        for (row in 0 until height) {
            val yRowLength = if (rowStride > width) width else rowStride
            yBuffer.get(nv21, pos, yRowLength)
            pos += width
            yBuffer.position(yBuffer.position() + (rowStride - yRowLength)) // Adjust for stride
        }

        rowStride = imageProxy.planes[2].rowStride
        val pixelStride = imageProxy.planes[2].pixelStride

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vIndex = row * rowStride + col * pixelStride
                val uIndex = row * rowStride + col * pixelStride

                nv21[pos++] = vBuffer[vIndex] // V
                nv21[pos++] = uBuffer[uIndex] // U
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)

        val byteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun cropToCircle(bitmap: Bitmap, centerX: Float, centerY: Float, radius: Int): Bitmap {
        val output = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val path = Path().apply {
            addCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), Path.Direction.CCW)
        }

        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(bitmap, -(centerX - radius), -(centerY - radius), paint)

        return output
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }
}