package com.example.record_lib.state

import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import com.example.record_lib.CameraInterface
import com.example.record_lib.view.CameraView

/**
 * @author Dat Bui T. on 4/22/19.
 */
class CameraMachine(
    val context: Context,
    val view: CameraView,
    cameraOpenOverCallback: CameraInterface.CameraOpenOverCallback
) : State {

    var state: State? = null
    //    private CameraInterface.CameraOpenOverCallback cameraOpenOverCallback;

    // Get idle status
    internal val previewState: State       // Browse status (idle)
    // Get browsing status
    internal val borrowPictureState: State // View image
    // Get browsing video status
    internal val borrowVideoState: State   // Browse the video

    init {
        previewState = PreviewState(this)
        borrowPictureState = BorrowPictureState(this)
        borrowVideoState = BorrowVideoState(this)
        // The default setting is idle
        this.state = previewState
        // this.cameraOpenOverCallback = cameraOpenOverCallback;
    }

    override fun focus(x: Float, y: Float, callback: CameraInterface.FocusCallback) {
        state?.focus(x, y, callback)
    }

    override fun start(holder: SurfaceHolder, screenProp: Float) {
        state?.start(holder, screenProp)
    }

    override fun stop() {
        state?.stop()
    }

    override fun switchView(holder: SurfaceHolder, screenProp: Float) {
        state?.switchView(holder, screenProp)
    }

    override fun restart() {
        state?.restart()
    }

    override fun capture() {
        state?.capture()
    }

    override fun record(surface: Surface, screenProp: Float) {
        state?.record(surface, screenProp)
    }

    override fun stopRecord(isShort: Boolean, time: Long) {
        state?.stopRecord(isShort, time)
    }

    override fun cancel(holder: SurfaceHolder, screenProp: Float) {
        state?.cancel(holder, screenProp)
    }

    override fun confirm() {
        state?.confirm()
    }

    override fun zoom(zoom: Float, type: Int) {
        state?.zoom(zoom, type)
    }

    override fun flash(mode: String) {
        state?.flash(mode)
    }
}
