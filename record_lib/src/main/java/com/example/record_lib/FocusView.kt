package com.example.record_lib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.record_lib.util.ScreenUtils

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
class FocusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var viewSize: Int = 0
    private var centerX: Int = 0
    private var centerY: Int = 0
    private var length: Int = 0
    private val paint = Paint()

    init {
        viewSize = ScreenUtils.getScreenWidth(context) / 3
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = -0x11e951ea
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        centerX = (viewSize / 2.0).toInt()
        centerY = (viewSize / 2.0).toInt()
        length = (viewSize / 2.0).toInt() - 2
        setMeasuredDimension(viewSize, viewSize)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(
            (centerX - length).toFloat(),
            (centerY - length).toFloat(),
            (centerX + length).toFloat(),
            (centerY + length).toFloat(),
            paint
        )
        canvas?.drawLine(2f, (height / 2).toFloat(), (viewSize / 10).toFloat(), (height / 2).toFloat(), paint)
        canvas?.drawLine(
            (width - 2).toFloat(),
            (height / 2).toFloat(),
            (width - viewSize / 10).toFloat(),
            (height / 2).toFloat(),
            paint
        )
        canvas?.drawLine((width / 2).toFloat(), 2f, (width / 2).toFloat(), (viewSize / 10).toFloat(), paint)
        canvas?.drawLine(
            (width / 2).toFloat(),
            (height - 2).toFloat(),
            (width / 2).toFloat(),
            (height - viewSize / 10).toFloat(),
            paint
        )
    }
}
