package com.example.record_lib

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
class TypeButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {
    val TYPE_CANCEL = 1111
    val TYPE_CONFIRM = 1112

    constructor(context: Context, type: Int, size: Int) : this(context) {
        this.type = type
        this.size = size
        radius = size / 2.0f
        centerX = size / 2.0f
        centerY = size / 2.0f

        paint = Paint()
        paint.isAntiAlias = true
        path = Path()
        strokeWidth = size / 50f
        index = this.size / 12f
        rectF = RectF(centerX, centerY - index, centerX + index * 2, centerY + index)
    }

    private var type: Int = 0
    private var size: Int = 0

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 0f

    private var paint: Paint = Paint()
    private var path: Path = Path()
    private var strokeWidth: Float = 0f

    private var index: Float = 0f
    private var rectF: RectF = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (type == TYPE_CANCEL) drawButtonCancel(canvas) else drawButtonOK(canvas)
    }

    private fun drawButtonCancel(canvas: Canvas?) {
        paint.color = -0x11232324
        paint.style = Paint.Style.FILL
        canvas?.drawCircle(centerX, centerY, radius, paint)

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth

        path.moveTo(centerX - index / 7, centerY + index)
        path.lineTo(centerX + index, centerY + index)

        path.arcTo(rectF, 90f, -180f)
        path.lineTo(centerX - index, centerY - index)
        canvas?.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(centerX - index, (centerY - index * 1.5).toFloat())
        path.lineTo(centerX - index, (centerY - index / 2.3).toFloat())
        path.lineTo((centerX - index * 1.6).toFloat(), centerY - index)
        path.close()
        canvas?.drawPath(path, paint)
    }

    private fun drawButtonOK(canvas: Canvas?) {
        paint.color = -0x1
        paint.style = Paint.Style.FILL
        canvas?.drawCircle(centerX, centerY, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.color = -0xff3400
        paint.strokeWidth = strokeWidth

        path.moveTo(centerX - size / 6f, centerY)
        path.lineTo(centerX - size / 21.2f, centerY + size / 7.7f)
        path.lineTo(centerX + size / 4.0f, centerY - size / 8.5f)
        path.lineTo(centerX - size / 21.2f, centerY + size / 9.4f)
        path.close()
        canvas?.drawPath(path, paint)
    }
}
