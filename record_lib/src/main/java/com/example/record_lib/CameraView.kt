package com.example.record_lib

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.record_lib.listener.*
import com.example.record_lib.state.CameraMachine
import com.example.record_lib.util.FileUtil
import com.example.record_lib.util.ScreenUtils
import com.example.record_lib.view.CameraView
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.IOException

/**
 * @author Dat Bui T. on 4/22/19.
 */
class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), CameraInterface.CameraOpenOverCallback, SurfaceHolder.Callback,
    CameraView {

    companion object {
        private const val TAG = "CameraView"
        // Status of flash
        private const val TYPE_FLASH_AUTO = 10020
        private const val TYPE_FLASH_ON = 10021
        private const val TYPE_FLASH_OFF = 10022
        private var type_flash = TYPE_FLASH_OFF

        // Type of photo browsing
        const val TYPE_PICTURE = 10030
        const val TYPE_VIDEO = 10031
        const val TYPE_SHORT = 10032
        const val TYPE_DEFAULT = 10034

        // Recording video bit rate
        val MEDIA_QUALITY_HIGH = 20 * 100000
        val MEDIA_QUALITY_MIDDLE = 16 * 100000
        val MEDIA_QUALITY_LOW = 12 * 100000
        val MEDIA_QUALITY_POOR = 8 * 100000
        val MEDIA_QUALITY_FUNNY = 4 * 100000
        val MEDIA_QUALITY_DESPAIR = 2 * 100000
        val MEDIA_QUALITY_SORRY = 1 * 80000
    }

    // Camera state machine
    private var machine: CameraMachine? = null

    // Callback listener
    private var cameraListener: CameraListener? = null
    private var leftClickListener: ClickListener? = null
    private var rightClickListener: ClickListener? = null
    private var mRecordStateListener: RecordStateListener? = null
    private var errorListener: ErrorListener? = null

    private var mMediaPlayer: MediaPlayer? = null

    private var layoutWidth: Int = 0
    private var screenProp = 0f

    private var captureBitmap: Bitmap? = null   // captured image
    private var firstFrame: Bitmap? = null  // first frame picture
    private var videoUrl: String? = null

    // Switch the parameters of the camera button
    private var iconSize = 0
    private var iconMargin = 0
    private var iconSrc = 0
    private var iconLeft = 0
    private var iconRight = 0
    private var duration = 0
    private var minDuration = 0

    // Zoom gradient
    private var zoomGradient = 0

    private var firstTouch = true
    private var firstTouchLength = 0f
    private var mRecordShortTip = "Recording time is too short"

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, 0)
        iconSize = a.getDimensionPixelSize(
            R.styleable.CameraView_iconSize, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 35f, resources.displayMetrics
            ).toInt()
        )
        iconMargin = a.getDimensionPixelSize(
            R.styleable.CameraView_iconMargin, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 15f, resources.displayMetrics
            ).toInt()
        )
        iconSrc = a.getResourceId(R.styleable.CameraView_iconSrc, R.drawable.ic_camera)
        iconLeft = a.getResourceId(R.styleable.CameraView_iconLeft, 0)
        iconRight = a.getResourceId(R.styleable.CameraView_iconRight, 0)
        duration = a.getInteger(R.styleable.CameraView_duration_max, 10000)
        minDuration = a.getInteger(R.styleable.CameraView_duration_min, 1500)
        a.recycle()
        initData()
        initView()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = videoPlayer.measuredWidth.toFloat()
        val heightSize = videoPlayer.measuredHeight.toFloat()
        if (screenProp == 0f) {
            screenProp = heightSize / widthSize
        }
    }

    override fun cameraHasOpened() {
        CameraInterface.instance.doStartPreview(videoPlayer.holder, screenProp)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "CameraView surfaceCreated")
        object : Thread() {
            override fun run() {
                CameraInterface.instance.doOpenCamera(this@CameraView)
            }
        }.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "CameraView surfaceDestroyed")
        CameraInterface.instance.doDestroyCamera()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    // Display focus indicator
                    setFocusViewWidthAnimation(event.x, event.y)
                }
                if (event.pointerCount == 2) {
                    Log.i(TAG, "ACTION_DOWN = " + 2)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    firstTouch = true
                }
                if (event.pointerCount == 2) {
                    val firstPointX = event.getX(0)
                    val firstPointY = event.getY(0)

                    val secondPointX = event.getX(1)
                    val secondPointY = event.getY(1)

                    val result = Math.sqrt(
                        Math.pow(
                            (firstPointX - secondPointX).toDouble(),
                            2.0
                        ) + Math.pow((firstPointY - secondPointY).toDouble(), 2.0)
                    ).toFloat()

                    if (firstTouch) {
                        firstTouchLength = result
                        firstTouch = false
                    }
                    if ((result - firstTouchLength).toInt() / zoomGradient != 0) {
                        firstTouch = true
                        machine?.zoom(result - firstTouchLength, CameraInterface.TYPE_CAPTURE)
                    }
                }
            }
            MotionEvent.ACTION_UP -> firstTouch = true
        }
        return true
    }

    // Focus frame indicator animation
    private fun setFocusViewWidthAnimation(x: Float, y: Float) {
        machine?.focus(x, y, object : CameraInterface.FocusCallback {
            override fun focusSuccess() {
                focusView.visibility = View.INVISIBLE
            }
        })
    }

    private fun updateVideoViewSize(videoWidth: Float, videoHeight: Float) {
        if (videoWidth > videoHeight) {
            val videoViewParam: FrameLayout.LayoutParams
            val height = (videoHeight / videoWidth * width).toInt()
            videoViewParam = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height)
            videoViewParam.gravity = Gravity.CENTER
            videoPlayer.layoutParams = videoViewParam
        }
    }

    override fun resetState(type: Int) {
        when (type) {
            TYPE_VIDEO -> {
                stopVideo()
                // Initialize VideoView
                videoUrl?.let { FileUtil.deleteFile(it) }
                videoPlayer.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                machine?.start(videoPlayer.holder, screenProp)
            }
            TYPE_PICTURE -> imgPhoto.visibility = View.INVISIBLE
            TYPE_SHORT -> {
            }
            TYPE_DEFAULT -> videoPlayer.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        imgSwitch.visibility = View.VISIBLE
        imgFlash.visibility = VISIBLE
        recordLayout.resetCaptureLayout()
    }

    override fun confirmState(type: Int) {
        when (type) {
            TYPE_VIDEO -> {
                stopVideo()
                videoPlayer.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                machine?.start(videoPlayer.holder, screenProp)
                videoUrl?.let { videoUrl ->
                    firstFrame?.let { firstFrame ->
                        cameraListener?.recordSuccess(
                            videoUrl,
                            firstFrame
                        )
                    }
                }
            }
            TYPE_PICTURE -> {
                imgPhoto.visibility = View.INVISIBLE
                captureBitmap?.let { cameraListener?.captureSuccess(it) }
            }
            TYPE_SHORT -> {
            }
            TYPE_DEFAULT -> {
            }
        }
        recordLayout.resetCaptureLayout()
    }

    override fun showPicture(bitmap: Bitmap, isVertical: Boolean) {
        if (isVertical) {
            imgPhoto.scaleType = ImageView.ScaleType.FIT_XY
        } else {
            imgPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        captureBitmap = bitmap
        imgPhoto.setImageBitmap(bitmap)
        imgPhoto.visibility = View.VISIBLE
        recordLayout.startAlphaAnimation()
        recordLayout.startTypeBtnAnimator()
    }

    override fun playVideo(firstFrame: Bitmap, url: String) {
        videoUrl = url
        this.firstFrame = firstFrame
        Thread(Runnable {
            try {
                if (mMediaPlayer == null) {
                    mMediaPlayer = MediaPlayer()
                } else {
                    mMediaPlayer?.reset()
                }
                mMediaPlayer?.run {
                    setDataSource(url)
                    setSurface(videoPlayer.holder.surface)
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setOnVideoSizeChangedListener { mp, width, height ->
                        updateVideoViewSize(
                            videoWidth.toFloat(), videoHeight.toFloat()
                        )
                    }
                    setOnPreparedListener { start() }
                    isLooping = true
                    prepare()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }).start()
    }

    override fun stopVideo() {
        mMediaPlayer?.run {
            if (isPlaying) {
                stop()
                release()
                mMediaPlayer = null
            }
        }
    }

    override fun setTip(tip: String) {
        recordLayout.setTip(tip)
    }

    override fun startPreviewCallback() {
        Log.d(TAG, "startPreviewCallback")
        handlerFoucs(focusView.width / 2f, focusView.height / 2f)
    }

    override fun handlerFoucs(x: Float, y: Float): Boolean {
        var focusX = x
        var focusY = y
        if (focusY > recordLayout.top) {
            return false
        }
        focusView.visibility = View.VISIBLE
        if (focusX < focusView.width / 2) {
            focusX = focusView.width / 2f
        }
        if (focusX > layoutWidth - focusView.width / 2) {
            focusX = layoutWidth - focusView.width / 2f
        }
        if (focusY < focusView.width / 2) {
            focusY = focusView.width / 2f
        }
        if (focusY > recordLayout.top - focusView.width / 2) {
            focusY = recordLayout.top - focusView.width / 2f
        }
        focusView.x = focusX - focusView.width / 2
        focusView.y = focusY - focusView.height / 2
        val scaleX = ObjectAnimator.ofFloat(focusView, "scaleX", 1f, 0.6f)
        val scaleY = ObjectAnimator.ofFloat(focusView, "scaleY", 1f, 0.6f)
        val alpha = ObjectAnimator.ofFloat(focusView, "alpha", 1f, 0.4f, 1f, 0.4f, 1f, 0.4f, 1f)
        val animSet = AnimatorSet()
        animSet.play(scaleX).with(scaleY).before(alpha)
        animSet.duration = 400
        animSet.start()
        return true
    }

    /**
     * This func use on the parent in life cycle onResume
     */
    fun onResume() {
        Log.d(TAG, "CameraView onResume")
        resetState(TYPE_DEFAULT) //重置状态
        CameraInterface.instance.registerSensorManager(context)
        CameraInterface.instance.setSwitchView(imgSwitch, imgFlash)
        machine?.start(videoPlayer.holder, screenProp)
    }

    /**
     * This func use on the parent in life cycle onPause
     */
    fun onPause() {
        Log.d(TAG, "CameraView onPause")
        stopVideo()
        resetState(TYPE_PICTURE)
        CameraInterface.instance.isPreview(false)
        CameraInterface.instance.unregisterSensorManager(context)
    }

    /**
     * This func use save video path
     */
    fun setSaveVideoPath(path: String) {
        CameraInterface.instance.setSaveVideoPath(path)
    }

    /**
     * This func use to set record short tip
     */
    fun setRecordShortTip(tip: String) {
        mRecordShortTip = tip
    }

    /**
     * This func use to set record short tip
     */
    fun setCameraListener(CameraListener: CameraListener) {
        this.cameraListener = CameraListener
    }

    /**
     * This func use to set record short tip
     */
    fun setRecordStateListener(listener: RecordStateListener) {
        mRecordStateListener = listener
    }

    /**
     * This func use to start Camera error callback
     */
    fun setErrorListener(errorListener: ErrorListener) {
        this.errorListener = errorListener
        CameraInterface.instance.setErrorListener(errorListener)
    }

    /**
     * This func use to set the CaptureButton function (photo and video)
     */
    fun setFeatures(state: Int) {
        recordLayout.setButtonFeatures(state)
    }

    /**
     * This func use to set the recording quality
     */
    fun setMediaQuality(quality: Int) {
        CameraInterface.instance.setMediaQuality(quality)
    }

    /**
     * This func use to set the maximum recording duration
     * @param duration is the maximum duration of the video recorded
     */
    fun setDuration(duration: Int) {
        recordLayout.setDuration(duration)
    }

    /**
     * This func use to set the maximum recording duration
     * @param minDuration is the minimum duration of the video recorded
     */
    fun setMinDuration(minDuration: Int) {
        recordLayout.setMinDuration(minDuration)
    }

    /**
     * This func use to set the recording quality
     */
    fun setLeftClickListener(clickListener: ClickListener) {
        this.leftClickListener = clickListener
    }

    /**
     * This func use to set the recording quality
     */
    fun setRightClickListener(clickListener: ClickListener) {
        this.rightClickListener = clickListener
    }

    private fun setFlashRes() {
        when (type_flash) {
            TYPE_FLASH_AUTO -> {
                imgFlash.setImageResource(R.drawable.ic_flash_auto)
                machine?.flash(Camera.Parameters.FLASH_MODE_AUTO)
            }
            TYPE_FLASH_ON -> {
                imgFlash.setImageResource(R.drawable.ic_flash_on)
                machine?.flash(Camera.Parameters.FLASH_MODE_ON)
            }
            TYPE_FLASH_OFF -> {
                imgFlash.setImageResource(R.drawable.ic_flash_off)
                machine?.flash(Camera.Parameters.FLASH_MODE_OFF)
            }
        }
    }

    private fun initData() {
        layoutWidth = ScreenUtils.getScreenWidth(context)
        // Zoom gradient
        zoomGradient = (layoutWidth / 16f).toInt()
        Log.d(TAG, "zoom = $zoomGradient")
        machine = CameraMachine(context, this, this)
    }

    private fun initView() {
        setWillNotDraw(false)
        LayoutInflater.from(context).inflate(R.layout.camera_view, this)

        setFlashRes()

        imgSwitch.setImageResource(iconSrc)
        imgFlash.setOnClickListener {
            type_flash++
            if (type_flash > 0x023)
                type_flash = TYPE_FLASH_AUTO
            setFlashRes()
        }
        btnBack.setOnClickListener {
            leftClickListener?.onClick()
        }
        recordLayout.setDuration(duration)
        recordLayout.setMinDuration(minDuration)
        //隐藏CaptureLayout的左按钮与右按钮
        //        mCaptureLayout.setIconSrc(-1, 0);
        recordLayout.setIconSrc(iconLeft, iconRight)
        videoPlayer.holder.addCallback(this)
        //切换摄像头
        imgSwitch.setOnClickListener { machine?.switchView(videoPlayer.holder, screenProp) }
        //拍照 录像
        recordLayout.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                imgSwitch.visibility = View.INVISIBLE
                imgFlash.visibility = View.INVISIBLE
                machine?.capture()
            }

            override fun recordStart() {
                imgSwitch.visibility = View.INVISIBLE
                imgFlash.visibility = View.INVISIBLE
                machine?.record(videoPlayer.holder.surface, screenProp)
                mRecordStateListener?.recordStart()
            }

            override fun recordShort(time: Long) {
                recordLayout.setTextWithAnimation(mRecordShortTip)
                imgSwitch.visibility = View.VISIBLE
                //                mFlashLamp.setVisibility(VISIBLE);
                postDelayed({ machine?.stopRecord(true, time) }, 1500 - time)
            }

            override fun recordEnd(time: Long) {
                machine?.stopRecord(false, time)
                mRecordStateListener?.recordEnd(time)
            }

            override fun recordZoom(zoom: Float) {
                Log.d(TAG, "recordZoom")
                machine?.zoom(zoom, CameraInterface.TYPE_RECORDER)
            }

            override fun recordError() {
                errorListener?.audioPermissionError()
            }
        })
        //确认 取消
        recordLayout?.setTypeLisenter(object : TypeListener {
            override fun cancel() {
                videoPlayer.holder.let { machine?.cancel(it, screenProp) }
                mRecordStateListener?.recordCancel()
            }

            override fun confirm() {
                machine?.confirm()
            }
        })
        //        mCaptureLayout.setReturnLisenter(new ReturnListener() {
        //            @Override
        //            public void onReturn() {
        //                if (cameraListener != null) {
        //                    cameraListener.quit();
        //                }
        //            }
        //        });
        recordLayout?.setLeftClickListener(object : ClickListener {
            override fun onClick() {
                leftClickListener?.onClick()
            }
        })
        recordLayout?.setRightClickListener(object : ClickListener {
            override fun onClick() {
                rightClickListener?.onClick()
            }
        })
    }
}
