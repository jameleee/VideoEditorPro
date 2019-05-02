package com.example.record_lib

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import com.example.record_lib.cameraview.CameraView
import com.example.record_lib.util.AngleUtil
import com.example.record_lib.util.ScreenUtils
import java.io.IOException
import java.util.*


/**
 * @author Dat Bui T. on 4/24/19.
 */
class Camera2Interface {

    companion object {
        private const val TAG = "CameraInterface"

        val instance: Camera2Interface by lazy {
            synchronized(CameraInterface::class.java) {
                Camera2Interface()
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

    private var isPreviewing = false

    private var cameraSelected = CameraMetadata.LENS_FACING_BACK
    private var cameraPosition: String? = null

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
    private val sparseIntArray = SparseIntArray()

    private var nowAngle: Int = 0
    private var handlerTime = 0

    private var isFlashSupported = false
    private var cameraManager: CameraManager? = null
    private var previewsize: Size? = null
    private var jpegSizes: Array<Size> = arrayOf()
    internal var cameraDevice: CameraDevice? = null
    private var previewRequest: CaptureRequest? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var previewSession: CameraCaptureSession? = null
    private var characteristics: CameraCharacteristics? = null

    init {
        /* val cameraId = cameraManager?.cameraIdList?.get(0)

         val cameraIdList = cameraManager?.cameraIdList // may be empty
         val characteristics = cameraManager?.getCameraCharacteristics(cameraId)
         val cameraLensFacing = characteristics?.get(CameraCharacteristics.LENS_FACING)*/
        sparseIntArray.append(Surface.ROTATION_0, 90)
        sparseIntArray.append(Surface.ROTATION_90, 0)
        sparseIntArray.append(Surface.ROTATION_180, 270)
        sparseIntArray.append(Surface.ROTATION_270, 180)
    }

    fun getFirstCameraIdFacing(
        cameraManager: CameraManager?,
        facing: Int = CameraMetadata.LENS_FACING_BACK
    ): String? {
        val cameraIds = cameraManager?.cameraIdList
        // Iterate over the list of cameras and return the first one matching desired
        // lens-facing configuration
        cameraIds?.forEach {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                return it
            }
        }
        // If no camera matched desired orientation, return the first one from the list
        return cameraIds?.firstOrNull()
    }

    /**
     * Start preview
     */
    fun doStartPreview(textureView: TextureView, screenProp: Float) {
        if (cameraDevice == null || !textureView.isAvailable || previewsize == null) {
            return
        }
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewsize!!.width, previewsize!!.height)
        val surface = Surface(texture)
        previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        previewBuilder?.addTarget(surface)
        try {
            cameraDevice?.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // The camera is already closed
                    if (cameraDevice == null) return
                    // When the session is ready, we start displaying the preview.
                    previewSession = session
                    try {
                        // Auto focus should be continuous for camera preview.
                        previewBuilder?.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        // Flash is automatically enabled when necessary.
                        setAutoFlash()

                        // Finally, we start displaying the camera preview.
                        previewRequest = previewBuilder?.build()
                        getChangedPreview()
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, e.toString())
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: Exception) {
        }
    }

    internal fun getChangedPreview() {
        if (cameraDevice == null) {
            return
        }
        previewBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val thread = HandlerThread("changed Preview")
        thread.start()
        val handler = Handler(thread.looper)
        try {
            previewSession?.setRepeatingRequest(previewRequest, null, handler)
        } catch (e: Exception) {
        }
    }

    private fun filterCameraIdsFacing(
        cameraIds: Array<String>,
        cameraManager: CameraManager,
        facing: Int
    ): List<String> {
        return cameraIds.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            characteristics.get(CameraCharacteristics.LENS_FACING) == facing
        }
    }

    private fun setAutoFlash() {
        if (isFlashSupported) {
            previewBuilder?.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun getNextCameraId(cameraManager: CameraManager, currCameraId: String? = null): String? {
        // Get all front, back and external cameras in 3 separate lists
        val cameraIds = cameraManager.cameraIdList
        val backCameras = filterCameraIdsFacing(
            cameraIds, cameraManager, CameraMetadata.LENS_FACING_BACK
        )
        val frontCameras = filterCameraIdsFacing(
            cameraIds, cameraManager, CameraMetadata.LENS_FACING_FRONT
        )
        val externalCameras = filterCameraIdsFacing(
            cameraIds, cameraManager, CameraMetadata.LENS_FACING_EXTERNAL
        )

        // The recommended order of iteration is: all external, first back, first front
        val allCameras = (externalCameras + listOf(
            backCameras.firstOrNull(), frontCameras.firstOrNull()
        )).filterNotNull()

        // Get the index of the currently selected camera in the list
        val cameraIndex = allCameras.indexOf(currCameraId)

        // The selected camera may not be on the list, for example it could be an
        // external camera that has been removed by the user
        return if (cameraIndex == -1) {
            // Return the first camera from the list
            allCameras.getOrNull(0)
        } else {
            // Return the next camera from the list, wrap around if necessary
            allCameras.getOrNull((cameraIndex + 1) % allCameras.size)
        }
    }

    internal fun setCameraManager(context: Context, stateCallback: CameraDevice.StateCallback) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraPosition =
            getFirstCameraIdFacing(cameraManager, cameraSelected)?.apply { openCamera(this, stateCallback) }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback) {
        characteristics = cameraManager?.getCameraCharacteristics(cameraId)
        val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        previewsize = map?.getOutputSizes(SurfaceTexture::class.java)?.get(0)
        // Check if the flash is supported.
        isFlashSupported = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        cameraManager?.openCamera(cameraId, stateCallback, null)
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

    /**
     * This func use to switch camera
     */
    @Synchronized
    fun switchCamera(holder: TextureView, screenProp: Float, stateCallback: CameraDevice.StateCallback) {
        cameraPosition = cameraManager?.let { getNextCameraId(it, cameraPosition) }
        cameraSelected = cameraPosition?.toInt() ?: CameraMetadata.LENS_FACING_BACK
        doDestroyCamera()
        openCamera(cameraSelected.toString(), stateCallback)

        doStartPreview(holder, screenProp)
    }

    /**
     * This func use to set flash mode
     */
    fun setFlashMode(flashMode: String) {
        previewBuilder?.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        /*  previewBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
          previewBuilder?.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)*/
//        mCamera?.parameters?.flashMode = flashMode
    }

    /**
     * Stop preview
     */
    fun doStopPreview() {
        try {
            // This sentence should be executed after stopPreview, otherwise it will be stuck or flower screen
            cameraDevice?.close()
            cameraDevice = null
//            isPreviewing = false
            Log.i(TAG, "=== Stop Preview ===")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Change camera
     */
    fun setSwitchView(mSwitchView: ImageView?, mFlashLamp: ImageView) {
        this.mSwitchView = mSwitchView
        this.mFlashLamp = mFlashLamp
    }

    /**
     * This func use to handle when we focus
     * @param context
     * @param x is X of the touch point
     * @param y is Y of the touch point
     * @param callback is the instance of interface FocusCallback
     */
    fun handleFocus(context: Context, x: Float, y: Float, callback: FocusCallback) {
        if (cameraDevice == null) {
            return
        }
        previewBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
        val minimumLens = characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val num = 30 * minimumLens!! / 100
        previewBuilder?.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)

        // open flash
        previewBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        previewSession?.capture(previewBuilder?.build(), null, null)
        /*    val params = mCamera?.parameters
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
            }*/
    }

    /**
     * Destroy camera
     */
    internal fun doDestroyCamera() {
//        errorListener = null
        // Clear data
        try {
            cameraDevice?.close()
            cameraDevice = null
            mSwitchView = null
            mFlashLamp = null
            // This sentence should be executed after stopPreview, otherwise it will be stuck or flower screen
            Log.i(TAG, "=== Destroy Camera ===")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * This func use to check is in preview state
     * @param isPreview is state preview or not
     */
    internal fun isPreview(isPreview: Boolean) {
//        this.isPreviewing = isPreview
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
