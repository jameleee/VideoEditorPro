package com.example.record_lib.camera2state

import android.hardware.camera2.CameraDevice
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.example.record_lib.Camera2Interface

/**
 * @author Dat Bui T. on 4/22/19.
 */
internal class PreviewState(private val machine: CameraMachine) : State {
    companion object {
        private const val TAG = "PreviewState"
    }

    override fun start(holder: TextureView, screenProp: Float) {
        Camera2Interface.instance.doStartPreview(holder, screenProp)
    }

    override fun stop() {
        Camera2Interface.instance.doStopPreview()
    }


    override fun focus(x: Float, y: Float, callback: Camera2Interface.FocusCallback) {
        Log.d(TAG, "preview state focus")
        if (machine.viewListener.handlerFoucs(x, y)) {
            Camera2Interface.instance.handleFocus(machine.context, x, y, callback)
        }
    }

    override fun switchView(holder: TextureView, screenProp: Float, stateCallback: CameraDevice.StateCallback) {
        Camera2Interface.instance.switchCamera(holder, screenProp, stateCallback)
    }

    override fun restart() {

    }

    override fun capture() {
        /* Camera2Interface.instance.takePicture(object : Camera2Interface.TakePictureCallback {
             override fun captureResult(bitmap: Bitmap, isVertical: Boolean) {
                 machine.viewListener.showPicture(bitmap, isVertical)
                 machine.state = machine.borrowPictureState
                 Log.d(TAG, "capture")
             }
         })*/
    }

    override fun record(surface: Surface, screenProp: Float) {
//        Camera2Interface.instance.startRecord(surface, screenProp, null)
    }

    override fun stopRecord(isShort: Boolean, time: Long) {
        /* Camera2Interface.instance.stopRecord(isShort, object : Camera2Interface.StopRecordCallback {
             override fun recordResult(url: String?, firstFrame: Bitmap?) {
                 if (isShort) {
                     machine.viewListener.resetState(CameraViewListener.TYPE_SHORT)
                 } else {
                     if (firstFrame != null && url != null) {
                         machine.viewListener.playVideo(firstFrame, url)
                     }
                     machine.state = machine.borrowVideoState
                 }
             }
         })*/
    }

    override fun cancel(holder: TextureView, screenProp: Float) {
        Log.d(TAG, "cancel")
    }

    override fun confirm() {
        Log.d(TAG, "confirm")
    }

    override fun zoom(zoom: Float, type: Int) {
        Log.d(TAG, "zoom")
//        Camera2Interface.instance.setZoom(zoom, type)
    }

    override fun flash(mode: String) {
        Camera2Interface.instance.setFlashMode(mode)
    }
}
