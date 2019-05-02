package com.example.record_lib.camera2state

import android.util.Log
import android.view.TextureView
import com.example.record_lib.Camera2Interface
import com.example.record_lib.cameraview.CameraView

/**
 * @author Dat Bui T. on 4/22/19.
 */
class BorrowVideoState(private val machine: CameraMachine) : State {
    companion object {
        private const val TAG = "BorrowVideoState"
    }

    override fun start(holder: TextureView, screenProp: Float) {
        Camera2Interface.instance.doStartPreview(holder, screenProp)
        machine.state = machine.previewState
    }

    override fun confirm() {
        machine.viewListener.confirmState(CameraView.TYPE_VIDEO)
        machine.state = machine.previewState
    }

    override fun cancel(holder: TextureView, screenProp: Float) {
        machine.viewListener.resetState(CameraView.TYPE_VIDEO)
        machine.state = machine.previewState
    }

    override fun zoom(zoom: Float, type: Int) {
        Log.i(TAG, "zoom")
    }
}
