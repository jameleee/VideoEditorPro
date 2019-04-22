package com.example.record_lib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * @author Dat Bui T.
 */
class ReturnButton(context: Context) : View(context) {

    private var buttonSize: Int = 0

    private var centerX: Int = 0
    private var centerY: Int = 0
    private var strokeWidth: Float = 0f

    private var paint: Paint = Paint()
    internal var path: Path = Path()

    constructor(context: Context, size: Int) : this(context) {
        buttonSize = size
        centerX = size / 2
        centerY = size / 2

        strokeWidth = size / 15f

        paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth

        path = Path()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(buttonSize, buttonSize / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.moveTo(strokeWidth, strokeWidth / 2)
        path.lineTo(centerX.toFloat(), centerY - strokeWidth / 2)
        path.lineTo(buttonSize - strokeWidth, strokeWidth / 2)
        canvas.drawPath(path, paint)
    }
}
