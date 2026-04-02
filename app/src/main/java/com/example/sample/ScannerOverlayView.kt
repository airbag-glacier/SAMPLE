package com.example.sample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // (70% Opacity Black)
    private val dimBackgroundPaint = Paint().apply {
        color = Color.parseColor("#B3000000")
    }


    private val transparentEraserPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // This makes it act like an eraser!
        isAntiAlias = true
    }

    init {

        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Paint the entire screen dim
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimBackgroundPaint)


        val cutoutWidth = width * 0.80f
        val cutoutHeight = cutoutWidth * (4f / 3f)

        val left = (width - cutoutWidth) / 2f
        val right = left + cutoutWidth


        val emptyVerticalSpace = height - cutoutHeight
        val top = emptyVerticalSpace * 0.4f
        val bottom = top + cutoutHeight

        val faceRect = RectF(left, top, right, bottom)


        canvas.drawOval(faceRect, transparentEraserPaint)
    }
}