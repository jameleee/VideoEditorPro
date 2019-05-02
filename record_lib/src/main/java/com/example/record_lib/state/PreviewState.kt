package com.example.record_lib.state

import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.example.record_lib.CameraInterface
import com.example.record_lib.cameraview.CameraView

/**
 * @author Dat Bui T. on 4/22/19.
 */
internal class PreviewState(private val machine: CameraMachine) : State {
    companion object {
        private const val TAG = "PreviewState"
    }

    override fun start(holder: SurfaceHolder, screenProp: Float) {
        CameraInterface.instance.doStartPreview(holder, screenProp)
    }

    override fun stop() {
        CameraInterface.instance.doStopPreview()
    }


    override fun focus(x: Float, y: Float, callback: CameraInterface.FocusCallback) {
        Log.d(TAG, "preview state focus")
        if (machine.viewListener.handlerFoucs(x, y)) {
            CameraInterface.instance.handleFocus(machine.context, x, y, callback)
        }
    }

    override fun switchView(holder: SurfaceHolder, screenProp: Float) {
        CameraInterface.instance.switchCamera(holder, screenProp)
    }

    override fun restart() {

    }

    override fun capture() {
        CameraInterface.instance.takePicture(object : CameraInterface.TakePictureCallback {
            override fun captureResult(bitmap: Bitmap, isVertical: Boolean) {
                machine.viewListener.showPicture(bitmap, isVertical)
                machine.state = machine.borrowPictureState
                Log.d(TAG, "capture")
            }
        })
    }

    override fun record(surface: Surface, screenProp: Float) {
        CameraInterface.instance.startRecord(surface, screenProp, null)
    }

    override fun stopRecord(isShort: Boolean, time: Long) {
        CameraInterface.instance.stopRecord(isShort, object : CameraInterface.StopRecordCallback {
            override fun recordResult(url: String?, firstFrame: Bitmap?) {
                if (isShort) {
                    machine.viewListener.resetState(CameraView.TYPE_SHORT)
                } else {
                    if (firstFrame != null && url != null) {
                        machine.viewListener.playVideo(firstFrame, url)
                    }
                    machine.state = machine.borrowVideoState
                }
            }
        })
    }

    override fun cancel(holder: SurfaceHolder, screenProp: Float) {
        Log.d(TAG, "cancel")
    }

    override fun confirm() {
        Log.d(TAG, "confirm")
    }

    override fun zoom(zoom: Float, type: Int) {
        Log.d(TAG, "zoom")
        CameraInterface.instance.setZoom(zoom, type)
    }

    override fun flash(mode: String) {
        CameraInterface.instance.setFlashMode(mode)
    }
}
