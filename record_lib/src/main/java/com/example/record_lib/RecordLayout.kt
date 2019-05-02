package com.example.record_lib

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.record_lib.listener.CaptureListener
import com.example.record_lib.listener.ClickListener
import com.example.record_lib.listener.ReturnListener
import com.example.record_lib.listener.TypeListener

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/22/19.
 */
class RecordLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var captureListener: CaptureListener? = null
    private var typeListener: TypeListener? = null
    private var returnListener: ReturnListener? = null
    private var leftClickListener: ClickListener? = null
    private var rightClickListener: ClickListener? = null

    private var btnCapture: CameraRecordButton? = null
    private var btnConfirm: TypeButtonView? = null
    private var btnCancel: TypeButtonView? = null
    private var ivCustomLeft: ImageView? = null
    private var ivCustomRight: ImageView? = null
    private var textView: TextView? = null

    private var layoutWidth: Int = 0
    private var layoutHeight: Int = 0
    private var buttonSize: Int = 0
    private var iconLeft = 0
    private var iconRight = 0

    private var isFirst = true

    init {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)

        layoutWidth = if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            outMetrics.widthPixels
        } else {
            outMetrics.widthPixels / 2
        }
        buttonSize = (layoutWidth / 6f).toInt()
        layoutHeight = buttonSize + buttonSize / 5 * 2 + 100

        initView()
        initEvent()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(layoutWidth, layoutHeight)
    }

    private fun initView() {
        setWillNotDraw(false)
        // Camera button
        btnCapture = CameraRecordButton(context, buttonSize)
        val btnCaptureParam =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btnCaptureParam.gravity = Gravity.CENTER
        btnCapture?.layoutParams = btnCaptureParam
        this.addView(btnCapture)
        btnCapture?.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                if (captureListener != null) {
                    captureListener?.takePictures()
                }
            }

            override fun recordShort(time: Long) {
                if (captureListener != null) {
                    captureListener?.recordShort(time)
                }
                startAlphaAnimation()
            }

            override fun recordStart() {
                if (captureListener != null) {
                    captureListener?.recordStart()
                }
                startAlphaAnimation()
            }

            override fun recordEnd(time: Long) {
                if (captureListener != null) {
                    captureListener?.recordEnd(time)
                }
                startAlphaAnimation()
                startTypeBtnAnimator()
            }

            override fun recordZoom(zoom: Float) {
                if (captureListener != null) {
                    captureListener?.recordZoom(zoom)
                }
            }

            override fun recordError() {
                if (captureListener != null) {
                    captureListener?.recordError()
                }
            }
        })

        // Cancel button
        btnCancel = TypeButtonView(context, TypeButtonView.TYPE_CANCEL, buttonSize)
        val btnCancelParam =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btnCancelParam.gravity = Gravity.CENTER_VERTICAL
        btnCancelParam.setMargins(layoutWidth / 4 - buttonSize / 2, 0, 0, 0)
        btnCancel?.layoutParams = btnCancelParam
        btnCancel?.setOnClickListener {
            if (typeListener != null) {
                typeListener?.cancel()
            }
            startAlphaAnimation()
            //                resetCaptureLayout();
        }

        // Confirm button
        btnConfirm = TypeButtonView(context, TypeButtonView.TYPE_CONFIRM, buttonSize)
        val btnConfirmParam =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btnConfirmParam.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        btnConfirmParam.setMargins(0, 0, layoutWidth / 4 - buttonSize / 2, 0)
        btnConfirm?.layoutParams = btnConfirmParam
        btnConfirm?.setOnClickListener {
            if (typeListener != null) {
                typeListener?.confirm()
            }
            startAlphaAnimation()
            //                resetCaptureLayout();
        }

        /* // Return button
         btnReturn = ReturnButton(context, (buttonSize / 2.5f).toInt())
         val btnReturnParam =
             FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
         btnReturnParam.gravity = Gravity.CENTER_VERTICAL
         btnReturnParam.setMargins(layoutWidth / 6, 0, 0, 0)
         btnReturn?.layoutParams = btnReturnParam
         btnReturn?.setOnClickListener {
             if (leftClickListener != null) {
                 leftClickListener?.onClick()
             }
         }*/
        // Custom button on the left
        ivCustomLeft = ImageView(context)
        val ivCustomParamLeft = FrameLayout.LayoutParams((buttonSize / 2.5f).toInt(), (buttonSize / 2.5f).toInt())
        ivCustomParamLeft.gravity = Gravity.CENTER_VERTICAL
        ivCustomParamLeft.setMargins(layoutWidth / 6, 0, 0, 0)
        ivCustomLeft?.layoutParams = ivCustomParamLeft
        ivCustomLeft?.setOnClickListener {
            if (leftClickListener != null) {
                leftClickListener?.onClick()
            }
        }

        // Custom button on the right
        ivCustomRight = ImageView(context)
        val ivCustomParamRight = FrameLayout.LayoutParams((buttonSize / 2.5f).toInt(), (buttonSize / 2.5f).toInt())
        ivCustomParamRight.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        ivCustomParamRight.setMargins(0, 0, layoutWidth / 6, 0)
        ivCustomRight?.layoutParams = ivCustomParamRight
        ivCustomRight?.setOnClickListener {
            if (rightClickListener != null) {
                rightClickListener?.onClick()
            }
        }

        textView = TextView(context)
        val txtParam =
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        txtParam.gravity = Gravity.CENTER_HORIZONTAL
        txtParam.setMargins(0, 0, 0, 0)
        textView?.text = "kkk"
        textView?.setTextColor(-0x1)
        textView?.gravity = Gravity.CENTER
        textView?.layoutParams = txtParam

        this.addView(btnCancel)
        this.addView(btnConfirm)
//        this.addView(btnReturn)
        this.addView(ivCustomLeft)
        this.addView(ivCustomRight)
        this.addView(textView)

    }

    private fun initEvent() {
        // The default Type button is hidden
        ivCustomRight?.visibility = View.GONE
        btnCancel?.visibility = View.GONE
        btnConfirm?.visibility = View.GONE
    }

    internal fun startTypeBtnAnimator() {
        // Animation after taking a photo
        if (this.iconLeft != 0)
            ivCustomLeft?.visibility = View.GONE
        if (this.iconRight != 0)
            ivCustomRight?.visibility = View.GONE
//        btnCapture?.visibility = View.GONE
        btnCancel?.visibility = View.VISIBLE
        btnConfirm?.visibility = View.VISIBLE
        btnCancel?.isClickable = false
        btnConfirm?.isClickable = false
        val animatorCancel = ObjectAnimator.ofFloat(btnCancel, "translationX", layoutWidth / 4f, 0f)
        val animatorConfirm = ObjectAnimator.ofFloat(btnConfirm, "translationX", -layoutWidth / 4f, 0f)

        val set = AnimatorSet()
        set.playTogether(animatorCancel, animatorConfirm)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                btnCancel?.isClickable = true
                btnConfirm?.isClickable = true
            }
        })
        set.duration = 200
        set.start()
    }

    fun resetCaptureLayout() {
        btnCapture?.resetState()
        btnCancel?.visibility = View.GONE
        btnConfirm?.visibility = View.GONE
        btnCapture?.visibility = View.VISIBLE
        if (this.iconLeft != 0)
            ivCustomLeft?.visibility = View.VISIBLE
        if (this.iconRight != 0)
            ivCustomRight?.visibility = View.VISIBLE
    }


    fun startAlphaAnimation() {
        if (isFirst) {
            val animatorTextTip = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
            animatorTextTip.duration = 500
            animatorTextTip.start()
            isFirst = false
        }
    }

    fun setTextWithAnimation(tip: String) {
        textView?.text = tip
        val animatorTextTip = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f, 1f, 0f)
        animatorTextTip.duration = 2500
        animatorTextTip.start()
    }

    fun setDuration(duration: Int) {
        btnCapture?.setDuration(duration)
    }

    fun setMinDuration(minDuration: Int) {
        btnCapture?.setMinDuration(minDuration)
    }

    fun setButtonFeatures(state: Int) {
        btnCapture?.setButtonState(state)
    }

    fun setTip(tip: String) {
        textView?.text = tip
    }

    fun showTip() {
        textView?.visibility = View.VISIBLE
    }

    fun setIconSrc(iconLeft: Int, iconRight: Int) {
        this.iconLeft = iconLeft
        this.iconRight = iconRight
        if (this.iconLeft != 0) {
            ivCustomLeft?.setImageResource(iconLeft)
            ivCustomLeft?.visibility = View.VISIBLE
        } else {
            ivCustomLeft?.visibility = View.GONE
        }
        if (this.iconRight != 0) {
            ivCustomRight?.setImageResource(iconRight)
            ivCustomRight?.visibility = View.VISIBLE
        } else {
            ivCustomRight?.visibility = View.GONE
        }
    }

    fun iconLeftVisible(visible: Boolean) {
        ivCustomLeft?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun iconRightVisible(visible: Boolean) {
        ivCustomRight?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun btnReturnVisible(visible: Boolean) {
//        btnReturn?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setLeftClickListener(leftClickListener: ClickListener) {
        this.leftClickListener = leftClickListener
    }

    fun setRightClickListener(rightClickListener: ClickListener) {
        this.rightClickListener = rightClickListener
    }

    fun setTypeLisenter(typeListener: TypeListener) {
        this.typeListener = typeListener
    }

    fun setCaptureListener(captureListener: CaptureListener) {
        this.captureListener = captureListener
    }

    fun setReturnLisenter(returnListener: ReturnListener) {
        this.returnListener = returnListener
    }
}
