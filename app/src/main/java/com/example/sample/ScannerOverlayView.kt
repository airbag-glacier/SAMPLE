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
import android.view.ViewGroup
import android.widget.ImageView
import kotlin.math.min

class ScannerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dimBackgroundPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // 70% Opacity Dark Background
    }

    private val transparentEraserPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimBackgroundPaint)

        val faceFrame = (parent as? ViewGroup)?.findViewById<ImageView>(R.id.faceFrame)

        if (faceFrame != null && faceFrame.width > 0) {


            val drawnSize = min(faceFrame.width, faceFrame.height).toFloat()


            val paddingLeft = (faceFrame.width - drawnSize) / 2f
            val paddingTop = (faceFrame.height - drawnSize) / 2f


            val viewportScale = drawnSize / 24f

            val ovalLeft = faceFrame.x + paddingLeft + (5f * viewportScale)
            val ovalTop = faceFrame.y + paddingTop + (2f * viewportScale)
            val ovalRight = faceFrame.x + paddingLeft + (19f * viewportScale)
            val ovalBottom = faceFrame.y + paddingTop + (19f * viewportScale)

            val faceRect = RectF(ovalLeft, ovalTop, ovalRight, ovalBottom)


            canvas.drawOval(faceRect, transparentEraserPaint)

        } else {
            postInvalidateDelayed(50)
        }
    }
}