package com.example.record_lib.util

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import java.util.*

/**
 * @author Dat Bui T. on 4/22/19.
 */
class CameraParamUtil private constructor() {

    companion object {

        private const val TAG = "CameraParamUtil"
        val instance: CameraParamUtil by lazy {
            CameraParamUtil()
        }

    }

    private val sizeComparator = CameraSizeComparator()

    private fun getBestSize(list: List<Camera.Size>, rate: Float): Camera.Size {
        var previewDisparity = 100f
        var index = 0
        for (i in list.indices) {
            val cur = list[i]
            val prop = cur.width.toFloat() / cur.height.toFloat()
            if (Math.abs(rate - prop) < previewDisparity) {
                previewDisparity = Math.abs(rate - prop)
                index = i
            }
        }
        return list[index]
    }


    private fun equalRate(s: Camera.Size, rate: Float): Boolean {
        val r = s.width.toFloat() / s.height.toFloat()
        return Math.abs(r - rate) <= 0.2
    }

    private inner class CameraSizeComparator : Comparator<Camera.Size> {
        override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
            return when {
                lhs.width == rhs.width -> 0
                lhs.width > rhs.width -> 1
                else -> -1
            }
        }
    }

    fun getPreviewSize(list: List<Camera.Size>, th: Int, rate: Float): Camera.Size {
        Collections.sort<Camera.Size>(list, sizeComparator)
        var i = 0
        for (s in list) {
            if (s.width > th && equalRate(s, rate)) {
                Log.i(TAG, "MakeSure Preview :w = " + s.width + " h = " + s.height)
                break
            }
            i++
        }
        return if (i == list.size) {
            getBestSize(list, rate)
        } else {
            list[i]
        }
    }

    fun getPictureSize(list: List<Camera.Size>, th: Int, rate: Float): Camera.Size {
        Collections.sort<Camera.Size>(list, sizeComparator)
        var i = 0
        for (s in list) {
            if (s.width > th && equalRate(s, rate)) {
                Log.i(TAG, "MakeSure Picture :w = " + s.width + " h = " + s.height)
                break
            }
            i++
        }
        return if (i == list.size) {
            getBestSize(list, rate)
        } else {
            list[i]
        }
    }

    fun isSupportedFocusMode(focusList: List<String>, focusMode: String): Boolean {
        for (i in focusList.indices) {
            if (focusMode == focusList[i]) {
                Log.i(TAG, "FocusMode supported $focusMode")
                return true
            }
        }
        Log.i(TAG, "FocusMode not supported $focusMode")
        return false
    }

    fun isSupportedPictureFormats(supportedPictureFormats: List<Int>, jpeg: Int): Boolean {
        for (i in supportedPictureFormats.indices) {
            if (jpeg == supportedPictureFormats[i]) {
                Log.i(TAG, "Formats supported $jpeg")
                return true
            }
        }
        Log.i(TAG, "Formats not supported $jpeg")
        return false
    }

    fun getCameraDisplayOrientation(context: Context, cameraId: Int): Int {
        val info = android.hardware.Camera.CameraInfo()
        android.hardware.Camera.getCameraInfo(cameraId, info)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = wm.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }
}
