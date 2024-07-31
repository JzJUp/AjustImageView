package com.example.myadjustcontrast.CustomView.CustomTextView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.example.myadjustcontrast.CustomView.MatrixImageView.MatrixImageUtils
import kotlin.math.sqrt


class CustomTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialDistance = 1f
    private var scale = 1f
    private var rotation = 0f

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val start = PointF()
    private val mid = PointF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Set the paint properties as needed
        paint.textSize = textSize.toFloat()
        paint.color = currentTextColor
    }

    override fun onDraw(canvas: Canvas) {
        // Apply the scaling and rotation transformation
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation, width / 2f, height / 2f)
        canvas.concat(matrix)

        // Draw the text using the updated matrix
        canvas.drawText(text.toString(), 0f, baseline.toFloat(), paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Adjust size after transformations
        val measuredWidth = measuredWidth * scale
        val measuredHeight = measuredHeight * scale
        setMeasuredDimension(measuredWidth.toInt(), measuredHeight.toInt())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                savedMatrix.set(matrix)
                start.set(lastTouchX, lastTouchY)
                midPoint(mid, lastTouchX, lastTouchY)
                initialDistance = getFingerSpacing(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix)
                lastTouchX = event.x
                lastTouchY = event.y
                start.set(lastTouchX, lastTouchY)
                midPoint(mid, lastTouchX, lastTouchY)
                initialDistance = getFingerSpacing(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
                    val newDist = getFingerSpacing(event)
                    if (newDist > 0) {
                        scale *= newDist / initialDistance
                        initialDistance = newDist
                    }
                    midPoint(mid, event.getX(0), event.getY(0))
                    matrix.set(savedMatrix)
                    matrix.postTranslate(-mid.x, -mid.y)
                    matrix.postRotate(rotation + (event.getX(0) - event.getX(1)) / 10f, mid.x, mid.y)
                    matrix.postTranslate(mid.x, mid.y)
                    invalidate()
                } else {
                    matrix.set(savedMatrix)
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                matrix.set(savedMatrix)
                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }

    private fun midPoint(point: PointF, x: Float, y: Float) {
        point.x = x
        point.y = y
    }

    private fun getFingerSpacing(motionEvent: MotionEvent): Float {
        val x = motionEvent.getX(0) - motionEvent.getX(1)
        val y = motionEvent.getY(0) - motionEvent.getY(1)
        return sqrt(x * x + y * y)
    }
}

