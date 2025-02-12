package com.geek.autofacecapture.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CameraOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var currentStateText = ""

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#272727")
        alpha = 200
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val leftPadding = 30f
        val rightPadding = 30f
        val radius = (width - leftPadding - rightPadding) / 2f

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        canvas.drawCircle(centerX, centerY, radius, clearPaint)

        canvas.drawText("CUBOX", centerX, centerY - radius - 20, textPaint)

        canvas.drawText(currentStateText, centerX, centerY + radius + 100, textPaint)
    }

    fun setCurrentState(time: Int) {
        currentStateText = time.toString()
        postInvalidate()
    }

    fun resetCurrentState() {
        currentStateText = "Detecting..."
        postInvalidate()
    }
}