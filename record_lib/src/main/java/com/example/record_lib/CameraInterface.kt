package com.example.record_lib

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.hardware.*
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.ImageView
import com.example.record_lib.cameraview.CameraView
import com.example.record_lib.listener.ErrorListener
import com.example.record_lib.util.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author Dat Bui T. on 4/23/19.
 */
class CameraInterface : Camera.PreviewCallback {

    companion object {

        private const val TAG = "CameraInterface"

        val instance: CameraInterface by lazy {
            synchronized(CameraInterface::class.java) {
                CameraInterface()
            }
        }

        const val TYPE_RECORDER = 11000
        const val TYPE_CAPTURE = 11001

        private fun calculateTapArea(x: Float, y: Float, coefficient: Float, context: Context): Rect {
            val focusAreaSize = 300f
            val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()
            val centerX = (x / ScreenUtils.getScreenWidth(context) * 2000 - 1000).toInt()
            val centerY = (y / ScreenUtils.getScreenHeight(context) * 2000 - 1000).toInt()
            val left = clamp(centerX - areaSize / 2, -1000, 1000)
            val top = clamp(centerY - areaSize / 2, -1000, 1000)
            val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
            return Rect(
                Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom)
            )
        }

        private fun clamp(x: Int, min: Int, max: Int): Int {
            return when {
                x > max -> max
                x < min -> min
                else -> x
            }
        }
    }

    private var mCamera: Camera? = null
    private var mParams: Camera.Parameters? = null
    private var isPreviewing = false

    private var cameraSelected = -1
    private var cameraPostPosition = -1
    private var cameraFrontPosition = -1

    private var mHolder: SurfaceHolder? = null
    private var screenProp = -1.0f

    private var isRecorder = false
    private var mediaRecorder: MediaRecorder? = null
    private var videoFileName: String? = null
    private var saveVideoPath = ""
    private var videoFileAbsPath: String? = null
    private var videoFirstFrame: Bitmap? = null

    private var errorListener: ErrorListener? = null

    private var mSwitchView: ImageView? = null
    private var mFlashLamp: ImageView? = null

    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    private var angle = 0
    private var cameraAngle = 90 //Camera angle defaults to 90 degrees
    private var rotation = 0
    private var firstFrameData: ByteArray? = null
    private var nowScaleRate = 0
    private var recordScaleRate = 0

    // Video quality
    private var mediaQuality = CameraView.MEDIA_QUALITY_MIDDLE
    private var sm: SensorManager? = null

    private var nowAngle: Int = 0
    private var handlerTime = 0

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (Sensor.TYPE_ACCELEROMETER != event.sensor.type) {
                return
            }
            val values = event.values
            angle = AngleUtil.getSensorAngle(values[0], values[1])
            rotationAnimation()
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    init {
        // find camera available in your device
        findAvailableCameras()
        cameraSelected = cameraPostPosition
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        firstFrameData = data
    }

    //Switch camera icon to rotate with the phone angle
    @SuppressLint("ObjectAnimatorBinding")
    private fun rotationAnimation() {
        if (mSwitchView == null) {
            return
        }
        if (rotation != angle) {
            var startRotation = 0
            var endRotation = 0
            when (rotation) {
                0 -> {
                    startRotation = 0
                    when (angle) {
                        90 -> endRotation = -90
                        270 -> endRotation = 90
                    }
                }
                90 -> {
                    startRotation = -90
                    when (angle) {
                        0 -> endRotation = 0
                        180 -> endRotation = -180
                    }
                }
                180 -> {
                    startRotation = 180
                    when (angle) {
                        90 -> endRotation = 270
                        270 -> endRotation = 90
                    }
                }
                270 -> {
                    startRotation = 90
                    when (angle) {
                        0 -> endRotation = 0
                        180 -> endRotation = 180
                    }
                }
            }
            val animCamera =
                ObjectAnimator.ofFloat(mSwitchView, "rotation", startRotation.toFloat(), endRotation.toFloat())
            val animFlash =
                ObjectAnimator.ofFloat(mFlashLamp, "rotation", startRotation.toFloat(), endRotation.toFloat())
            val set = AnimatorSet()
            set.playTogether(animCamera, animFlash)
            set.duration = 500
            set.start()
            rotation = angle
        }
    }

    private fun setFlashModel() {
        mParams = mCamera?.parameters
        mParams?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        mCamera?.parameters = mParams
    }

    private fun findAvailableCameras() {
        val info = Camera.CameraInfo()
        val cameraNum = Camera.getNumberOfCameras()
        for (i in 0 until cameraNum) {
            Camera.getCameraInfo(i, info)
            when (info.facing) {
                Camera.CameraInfo.CAMERA_FACING_FRONT -> cameraFrontPosition = info.facing
                Camera.CameraInfo.CAMERA_FACING_BACK -> cameraPostPosition = info.facing
            }
        }
    }

    @Synchronized
    private fun openCamera(id: Int) {
        try {
            this.mCamera = Camera.open(id)
        } catch (var3: Exception) {
            var3.printStackTrace()
            if (this.errorListener != null) {
                this.errorListener!!.onError()
            }
        }

        try {
            this.mCamera?.enableShutterSound(false)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Enable shutter sound failed ==== ${e.message}")
        }
    }

    /**
     * This func use to switch camera
     */
    @Synchronized
    fun switchCamera(holder: SurfaceHolder, screenProp: Float) {
        cameraSelected = if (cameraSelected == cameraPostPosition) {
            cameraFrontPosition
        } else {
            cameraPostPosition
        }
        doDestroyCamera()
        openCamera(cameraSelected)
        //        mCamera = Camera.open();
        if (Build.VERSION.SDK_INT > 21 && this.mCamera != null) {
            try {
                this.mCamera?.enableShutterSound(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        doStartPreview(holder, screenProp)
    }

    /**
     * This func use to set flash mode
     */
    fun setFlashMode(flashMode: String) {
        mCamera?.parameters?.flashMode = flashMode
    }

    /**
     * This func use to set zoom size
     */
    fun setZoom(zoom: Float, type: Int) {
        if (mCamera == null) {
            return
        }
        if (mParams == null) {
            mParams = mCamera?.parameters
        }

        mParams?.run {
            if (!isZoomSupported || !isSmoothZoomSupported) {
                return
            }
            when (type) {
                TYPE_RECORDER -> {
                    //If it is not recorded the video, the slide will not zoom
                    if (!isRecorder) {
                        return
                    }
                    if (zoom >= 0) {
                        // Scale each level by 40 pixels
                        val scaleRate = (zoom / 40).toInt()
                        if (scaleRate in nowScaleRate..maxZoom && recordScaleRate != scaleRate) {
                            this.zoom = scaleRate
                            mCamera?.parameters = this
                            recordScaleRate = scaleRate
                        }
                    }
                }
                TYPE_CAPTURE -> {
                    if (isRecorder) {
                        return
                    }
                    // Scale each level by 50 pixels
                    val scaleRate = (zoom / 50).toInt()
                    if (scaleRate < maxZoom) {
                        nowScaleRate += scaleRate
                        if (nowScaleRate < 0) {
                            nowScaleRate = 0
                        } else if (nowScaleRate > maxZoom) {
                            nowScaleRate = maxZoom
                        }
                        this.zoom = nowScaleRate
                        mCamera?.parameters = this
                    }
                }
            }
        }
    }

    /**
     * Start preview
     */
    fun doStartPreview(holder: SurfaceHolder?, screenProp: Float) {
        if (isPreviewing) {
            Log.i(TAG, "doStartPreview isPreviewing")
        }
        if (this.screenProp < 0) {
            this.screenProp = screenProp
        }
        if (holder == null) {
            return
        }
        this.mHolder = holder
        if (mCamera != null) {
            try {
                mParams = mCamera?.parameters
                mParams?.run {
                    val previewSize = CameraParamUtil.instance.getPreviewSize(
                        supportedPreviewSizes, 1000, screenProp
                    )
                    val pictureSize = CameraParamUtil.instance.getPictureSize(
                        supportedPictureSizes, 1200, screenProp
                    )

                    setPreviewSize(previewSize.width, previewSize.height)

                    previewWidth = previewSize.width
                    previewHeight = previewSize.height

                    setPictureSize(pictureSize.width, pictureSize.height)

                    if (CameraParamUtil.instance.isSupportedFocusMode(
                            supportedFocusModes,
                            Camera.Parameters.FOCUS_MODE_AUTO
                        )
                    ) {
                        focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                    }
                    if (CameraParamUtil.instance.isSupportedPictureFormats(
                            supportedPictureFormats,
                            ImageFormat.JPEG
                        )
                    ) {
                        pictureFormat = ImageFormat.JPEG
                        jpegQuality = 100
                    }
                    mCamera?.parameters = this
                    mParams = mCamera?.parameters
                    mCamera?.setPreviewDisplay(holder)
                    mCamera?.setDisplayOrientation(cameraAngle)
                    mCamera?.setPreviewCallback(this@CameraInterface)
                    mCamera?.startPreview()
                    isPreviewing = true
                }
                Log.i(TAG, "=== Start Preview ===")
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Stop preview
     */
    fun doStopPreview() {
        try {
            mCamera?.setPreviewCallback(null)
            mCamera?.stopPreview()
            // This sentence should be executed after stopPreview, otherwise it will be stuck or flower screen
            mCamera?.setPreviewDisplay(null)
            isPreviewing = false
            Log.i(TAG, "=== Stop Preview ===")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Take picture
     */
    fun takePicture(callback: TakePictureCallback?) {
        if (mCamera == null) {
            return
        }
        when (cameraAngle) {
            90 -> nowAngle = Math.abs(angle + cameraAngle) % 360
            270 -> nowAngle = Math.abs(cameraAngle - angle)
        }
        //
        Log.i(TAG, "==== $angle = $cameraAngle = $nowAngle ===")
        mCamera?.takePicture(null, null, Camera.PictureCallback { data, camera ->
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            val matrix = Matrix()
            if (cameraSelected == cameraPostPosition) {
                matrix.setRotate(nowAngle.toFloat())
            } else if (cameraSelected == cameraFrontPosition) {
                matrix.setRotate((360 - nowAngle).toFloat())
                matrix.postScale(-1f, 1f)
            }

            bitmap = createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (callback != null) {
                if (nowAngle == 90 || nowAngle == 270) {
                    callback.captureResult(bitmap, true)
                } else {
                    callback.captureResult(bitmap, false)
                }
            }
        })
    }

    /**
     * Change camera
     */
    fun setSwitchView(mSwitchView: ImageView?, mFlashLamp: ImageView) {
        this.mSwitchView = mSwitchView
        this.mFlashLamp = mFlashLamp
        if (mSwitchView != null) {
            cameraAngle = CameraParamUtil.instance.getCameraDisplayOrientation(
                mSwitchView.context,
                cameraSelected
            )
        }
    }

    /**
     *  Start record video
     */
    fun startRecord(surface: Surface, screenProp: Float, callback: ErrorCallback?) {
        mCamera?.setPreviewCallback(null)
        val nowAngle = (angle + 90) % 360
        // Get the first frame of pictures
        mCamera?.parameters?.apply {
            val width = previewSize.width
            val height = previewSize.height
            val yuv = YuvImage(firstFrameData, previewFormat, width, height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, width, height), 50, out)
            val bytes = out.toByteArray()
            videoFirstFrame = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        val matrix = Matrix()
        if (cameraSelected == cameraPostPosition) {
            matrix.setRotate(nowAngle.toFloat())
        } else if (cameraSelected == cameraFrontPosition) {
            matrix.setRotate(270f)
        }
        videoFirstFrame = videoFirstFrame?.let { createBitmap(it, 0, 0, it.width, it.height, matrix, true) }

        if (isRecorder) {
            return
        }
        if (mCamera == null) {
            openCamera(cameraSelected)
        }
        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorder()
        }
        if (mParams == null) {
            mParams = mCamera?.parameters
        }
        val focusModes = mParams?.supportedFocusModes
        if (focusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == true) {
            mParams?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        mCamera?.parameters = mParams
        mCamera?.unlock()
        mediaRecorder?.run {
            reset()
            setCamera(mCamera)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            val videoSize = if (mParams?.supportedVideoSizes == null) {
                mParams?.supportedPreviewSizes?.let { CameraParamUtil.instance.getPreviewSize(it, 600, screenProp) }
            } else {
                mParams?.supportedVideoSizes?.let { CameraParamUtil.instance.getPreviewSize(it, 600, screenProp) }
            }

            Log.i(TAG, "setVideoSize    width = " + videoSize?.width + "height = " + videoSize?.height)

            if (videoSize != null) {
                if (videoSize.width == videoSize.height) {
                    setVideoSize(previewWidth, previewHeight)
                } else {
                    setVideoSize(videoSize.width, videoSize.height)
                }
            }
            /* if (cameraSelected == cameraFrontPosition) {
                 setOrientationHint(270)
             } else {
                 setOrientationHint(nowAngle)
                 //            setOrientationHint(90)
             }*/

            if (cameraSelected == cameraFrontPosition) {
                // Phone preview inverted processing
                if (cameraAngle == 270) {
                    //Horizontal screen
                    when (nowAngle) {
                        0 -> setOrientationHint(180)
                        270 -> setOrientationHint(270)
                        else -> setOrientationHint(90)
                    }
                } else {
                    when (nowAngle) {
                        90 -> setOrientationHint(270)
                        270 -> setOrientationHint(90)
                        else -> setOrientationHint(nowAngle)
                    }
                }
            } else {
                setOrientationHint(nowAngle)
            }

            setVideoEncodingBitRate(mediaQuality)

            setPreviewDisplay(surface)

            videoFileName = "video_" + System.currentTimeMillis() + ".mp4"
            if (saveVideoPath == "") {
                saveVideoPath = Environment.getExternalStorageDirectory().path
            }
            videoFileAbsPath = saveVideoPath + File.separator + videoFileName
            setOutputFile(videoFileAbsPath)
            try {
                prepare()
                start()
                isRecorder = true
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                Log.i(TAG, "startRecord IllegalStateException")
                this@CameraInterface.errorListener?.onError()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i(TAG, "startRecord IOException")
                this@CameraInterface.errorListener?.onError()
            } catch (e: RuntimeException) {
                Log.i(TAG, "startRecord RuntimeException")
            }
        }
    }

    /**
     *  Stop recording
     */
    fun stopRecord(isShort: Boolean, callback: StopRecordCallback) {
        if (!isRecorder) {
            return
        }
        mediaRecorder?.run {
            setOnErrorListener(null)
            setOnInfoListener(null)
            setPreviewDisplay(null)
            try {
                stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                mediaRecorder = null
                mediaRecorder = MediaRecorder()
            } finally {
                release()
                mediaRecorder = null
                isRecorder = false
            }
        }

        if (isShort) {
            // Delete file
            if (FileUtil.deleteFile(videoFileAbsPath ?: "")) {
                callback.recordResult(null, null)
            }
            return
        }
        doStopPreview()
        val fileName = saveVideoPath + File.separator + videoFileName
        callback.recordResult(fileName, videoFirstFrame)
    }

    /**
     * This func use to handle when we focus
     * @param context
     * @param x is X of the touch point
     * @param y is Y of the touch point
     * @param callback is the instance of interface FocusCallback
     */
    fun handleFocus(context: Context, x: Float, y: Float, callback: FocusCallback) {
        if (mCamera == null) {
            return
        }
        val params = mCamera?.parameters
        val focusRect = calculateTapArea(x, y, 1f, context)
        mCamera?.cancelAutoFocus()

        if (params != null) {
            if (params.maxNumFocusAreas > 0) {
                val focusAreas = ArrayList<Camera.Area>()
                focusAreas.add(Camera.Area(focusRect, 800))
                params.focusAreas = focusAreas
            } else {
                Log.i(TAG, "focus areas not supported")
                callback.focusSuccess()
                return
            }
        }

        val currentFocusMode = params?.focusMode
        try {
            params?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            mCamera?.parameters = params
            mCamera?.autoFocus { success, camera ->
                if (success || handlerTime > 10) {
                    val parameters = camera.parameters
                    parameters.focusMode = currentFocusMode
                    camera.parameters = parameters
                    handlerTime = 0
                    callback.focusSuccess()
                } else {
                    handlerTime++
                    handleFocus(context, x, y, callback)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "autoFocus failed")
        }
    }

    /**
     * Destroy camera
     */
    internal fun doDestroyCamera() {
        errorListener = null
        // Clear data
        try {
            mCamera?.setPreviewCallback(null)
            mSwitchView = null
            mFlashLamp = null
            mCamera?.stopPreview()
            // This sentence should be executed after stopPreview, otherwise it will be stuck or flower screen
            mCamera?.setPreviewDisplay(null)
            mHolder = null
            isPreviewing = false
            mCamera?.release()
            mCamera = null
            Log.i(TAG, "=== Destroy Camera ===")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * This func use to set error listener
     * @param errorListener is the instance of interface
     */
    internal fun setErrorListener(errorListener: ErrorListener) {
        this.errorListener = errorListener
    }

    /**
     * This func use to register sensor manager
     * @param context
     */
    internal fun registerSensorManager(context: Context) {
        if (sm == null) {
            sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        sm?.registerListener(
            sensorEventListener,
            sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * This func use to unregister sensor manager
     * @param context
     */
    internal fun unregisterSensorManager(context: Context) {
        if (sm == null) {
            sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        sm?.unregisterListener(sensorEventListener)
    }

    /**
     * This func use to save path of the video
     */
    internal fun setSaveVideoPath(saveVideoPath: String) {
        this.saveVideoPath = saveVideoPath
        val file = File(saveVideoPath)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /**
     * This func use to set quality of the media
     */
    internal fun setMediaQuality(quality: Int) {
        this.mediaQuality = quality
    }

    /**
     * Open Camera
     */
    internal fun doOpenCamera(callback: CameraOpenOverCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!CheckPermission.isCameraUseable(cameraSelected) && this.errorListener != null) {
                this.errorListener!!.onError()
                return
            }
        }
        if (mCamera == null) {
            openCamera(cameraSelected)
        }
        callback.cameraHasOpened()
    }

    /**
     * This func use to check is in preview state
     * @param isPreview is state preview or not
     */
    internal fun isPreview(isPreview: Boolean) {
        this.isPreviewing = isPreview
    }

    /**
     * This interface use to call back stop recording
     */
    interface StopRecordCallback {

        /**
         * @param url is the path of video
         * @param firstFrame is the first frame of the video at bitmap type
         */
        fun recordResult(url: String?, firstFrame: Bitmap?)
    }

    /**
     * This interface use to call back error event
     */
    interface ErrorCallback {
        fun onError()
    }

    /**
     * This interface use to call back take picture event
     */
    interface TakePictureCallback {

        /**
         * @param bitmap is the picture as bitmap type
         * @param isVertical is the orientation of the picture (vertical or horizontal)
         */
        fun captureResult(bitmap: Bitmap, isVertical: Boolean)
    }

    /**
     * This interface use to call back stop recording
     */
    interface FocusCallback {

        /**
         * Focus success
         */
        fun focusSuccess()
    }

    /**
     * This interface use to call back camera open over event
     */
    interface CameraOpenOverCallback {

        /**
         * Check camera has opened yet
         */
        fun cameraHasOpened()
    }
}
