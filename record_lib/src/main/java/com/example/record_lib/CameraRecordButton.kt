package com.example.record_lib

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.record_lib.listener.CaptureListener
import com.example.record_lib.util.CheckPermission
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/19/19.
 */
class CameraRecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val STATE_IDLE = 10001        // Idle state
        const val STATE_PRESS = 10002       // Press state
        const val STATE_LONG_PRESS = 10003  // Long press state
        const val STATE_RECORDING = 10004 // Recording state
        const val STATE_BAN = 10005
    }

    private var state = 0
    private var buttonState = 0

    private val progressColor = -0x11e951ea
    private val outsideColor = -0x11232324
    private val insideColor = -0x1

    private var eventY: Float = 0f // Touch event down

    private val paint: Paint = Paint()
    private val path: Path = Path()

    private var strokeWidth: Float = 0f
    private var outsideAddSize: Int = 0
    private var insideReduceSize: Int = 0

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    private var buttonRadius: Float = 0f
    private var buttonOutsideRadius: Float = 0f
    private var buttonInsideRadius: Float = 0f
    private var buttonSize: Int = 0

    private var progress: Float = 0.toFloat()
    private var duration: Int = 0
    private var minduration: Int = 0
    private var recordedTime: Long = 0

    private var rectF: RectF = RectF()
    private var rectReDraw = RectF()

    private var captureLisenter: CaptureListener? = null

    private var disposable: Disposable? = null

    constructor(context: Context, size: Int) : this(context) {
        buttonSize = size
        buttonRadius = size / 2f

        buttonOutsideRadius = buttonRadius
        buttonInsideRadius = buttonRadius * 0.85f

        strokeWidth = size / 15f
        outsideAddSize = size / 5
        insideReduceSize = size / 8

        paint.isAntiAlias = true
        paint.flags = Paint.ANTI_ALIAS_FLAG

        state = STATE_IDLE
        buttonState = ButtonState.BUTTON_STATE_BOTH.type()

        centerX = (buttonSize + outsideAddSize * 2) / 2f
        centerY = (buttonSize + outsideAddSize * 2) / 2f

        rectF.set(
            RectF(
                centerX - outsideAddSize,
                centerY - outsideAddSize,
                centerX + outsideAddSize,
                centerY + outsideAddSize
            )
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(buttonSize + outsideAddSize * 2, buttonSize + outsideAddSize * 2)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //draw arc
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = buttonSize / 15f
        Log.d("zxc", "beginnnn $strokeWidth")

        paint.color = ContextCompat.getColor(context, R.color.cameraRecordButtonOutsideColor)
        canvas?.drawCircle(centerX, centerY, buttonOutsideRadius, paint)

        paint.style = Paint.Style.FILL
        paint.color = ContextCompat.getColor(context, R.color.cameraRecordButtonInsideColor)
        canvas?.drawCircle(centerX, centerY, buttonInsideRadius, paint)
        canvas?.drawRoundRect(rectF, 5f, 5f, paint)

        Log.d("zxc", "sss $buttonRadius  ==== $outsideAddSize")

        rectReDraw.set(
            RectF(
                centerX - (buttonRadius + outsideAddSize - strokeWidth / 2),
                centerY - (buttonRadius + outsideAddSize - strokeWidth / 2),
                centerX + (buttonRadius + outsideAddSize - strokeWidth / 2),
                centerY + (buttonRadius + outsideAddSize - strokeWidth / 2)
            )
        )

        paint.color = ContextCompat.getColor(context, R.color.cameraRecordButtonProgressColor)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        canvas?.drawArc(rectReDraw, -90f, 360f, false, paint)

        // If the status is recording, draw a recording progress bar
        /* if (state == STATE_RECORDING) {
             paint.color = ContextCompat.getColor(context, R.color.cameraRecordButtonProgressColor)
             paint.style = Paint.Style.STROKE
             paint.strokeWidth = strokeWidth
             canvas?.drawArc(rectF, -90f, progress, false, paint)
         }*/
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount > 1)
                    return false
                eventY = event.y
//                if ((buttonState == ButtonState.BUTTON_STATE_ONLY_RECORDER.type() || buttonState == ButtonState.BUTTON_STATE_BOTH.type()))
                startZoomAnimation()
            }
            MotionEvent.ACTION_MOVE -> if (captureLisenter != null
                && state == STATE_RECORDING
                && (buttonState == ButtonState.BUTTON_STATE_ONLY_RECORDER.type()
                        || buttonState == ButtonState.BUTTON_STATE_BOTH.type())
            ) {
                captureLisenter?.recordZoom(eventY - event.y)
            }
            MotionEvent.ACTION_UP ->
//                handlerUnPressByState()
            {
            }
        }
        return true
    }

    //The logic that is processed when the finger releases the button
    private fun handlerUnPressByState() {
        disposable?.dispose()
//        startCaptureAnimation(buttonInsideRadius)
        // Process according to current state
        recordEnd()
/* when (state) {
            // Currently click state
            STATE_PRESS -> if (captureLisenter != null && (buttonState == ButtonState.BUTTON_STATE_ONLY_CAPTURE.type() || buttonState == ButtonState.BUTTON_STATE_BOTH.type())) {
                startCaptureAnimation(buttonInsideRadius)
            } else {
                state = STATE_IDLE
            }
            // Currently is a long press state
            STATE_RECORDING -> {
                recordEnd()
            }
        }*/
    }

    private fun animateRoundOutsize(time: Long) {
        var isDrawIn = false
        disposable = Observable.interval(time, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                when {
                    strokeWidth <= buttonSize / 15f -> {
                        strokeWidth += 1
                        isDrawIn = false
                    }
                    strokeWidth >= buttonSize / 15f + 20 -> {
                        strokeWidth -= 1
                        isDrawIn = true
                    }
                    isDrawIn -> strokeWidth -= 1
                    else -> strokeWidth += 1
                }
                invalidate()
            }
    }

    private fun recordEnd() {
        if (captureLisenter != null) {
            if (recordedTime < minduration)
                captureLisenter?.recordShort(recordedTime)
            else
                captureLisenter?.recordEnd(recordedTime)
        }
        resetRecordAnim()
    }

    // Remastered state
    private fun resetRecordAnim() {
        state = STATE_IDLE
        progress = 0f
        invalidate()
        // Restore button initial state animation
        startRecordAnimation(
            buttonOutsideRadius,
            buttonRadius,
            buttonInsideRadius,
            buttonRadius * 0.85f
        )
    }

    private fun startZoomAnimation() {

        // No recording permission
        if (CheckPermission.recordState !== CheckPermission.STATE_SUCCESS) {
            if (captureLisenter != null) {
                captureLisenter?.recordError()
                return
            }
        }
        // Start button animation, the outer circle becomes zoom animation, and the inner circle to the border rectangle
        startRecordAnimation(
            strokeWidth,
            strokeWidth + 20,
            buttonInsideRadius,
            0f
        )
        /*startRecordAnimation(
            buttonOutsideRadius,
            buttonOutsideRadius + outsideAddSize,
            buttonInsideRadius,
            buttonInsideRadius - insideReduceSize
        )*/
    }

    private fun startCaptureAnimation(inside_start: Float) {
        val insideAnim = ValueAnimator.ofFloat(inside_start, inside_start * 0.75f, inside_start)
        insideAnim.addUpdateListener { animation ->
            buttonInsideRadius = animation.animatedValue as Float
            invalidate()
        }
        insideAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Callback camera interface
                captureLisenter?.takePictures()
            }
        })
        insideAnim.duration = 100
        insideAnim.start()
    }

    private fun startRecordAnimation(outside_start: Float, outside_end: Float, inside_start: Float, inside_end: Float) {
        val outsideAnim = ValueAnimator.ofFloat(outside_start, outside_end)
        val insideAnim = ValueAnimator.ofFloat(inside_start, inside_end)
        // outside circle animation
        outsideAnim.addUpdateListener { animation ->
            //            strokeWidth = animation.animatedValue as Float
            buttonOutsideRadius = animation.animatedValue as Float
            invalidate()
        }
        // Inner circle animation
        insideAnim.addUpdateListener { animation ->
            buttonInsideRadius = animation.animatedValue as Float
            invalidate()
        }
        val set = AnimatorSet()
        // Start recording Runnable when the animation is over and call back the recording start interface
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                disposable?.dispose()
                animateRoundOutsize(50)
                // Set to recording status
                if (state == STATE_LONG_PRESS) {
                    if (captureLisenter != null)
                        captureLisenter?.recordStart()
                    state = STATE_RECORDING
                }
            }
        })
        set.playTogether(outsideAnim, insideAnim)
        set.duration = 100
        set.start()
    }

    // set duration
    fun setDuration(duration: Int) {
        this.duration = duration
    }

    // set min duration
    fun setMinDuration(duration: Int) {
        minduration = duration
    }

    // init capture listener
    fun setCaptureLisenter(captureLisenter: CaptureListener) {
        this.captureLisenter = captureLisenter
    }

    // set button state
    fun setButtonFeatures(state: Int) {
        buttonState = state
    }

    // Check is Idle state
    fun isIdle(): Boolean = state == STATE_IDLE

    // Reset state
    fun resetState() {
        state = STATE_IDLE
    }
}
