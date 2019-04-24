package com.example.record_lib.state

import android.util.Log
import android.view.SurfaceHolder
import com.example.record_lib.CameraInterface
import com.example.record_lib.CameraView

/**
 * @author Dat Bui T. on 4/22/19.
 */
class BorrowVideoState(private val machine: CameraMachine) : State {
    companion object {
        private const val TAG = "BorrowVideoState"
    }

    override fun start(holder: SurfaceHolder, screenProp: Float) {
        CameraInterface.instance.doStartPreview(holder, screenProp)
        machine.state = machine.previewState
    }

    override fun confirm() {
        machine.view.confirmState(CameraView.TYPE_VIDEO)
        machine.state = machine.previewState
    }

    override fun cancel(holder: SurfaceHolder, screenProp: Float) {
        machine.view.resetState(CameraView.TYPE_VIDEO)
        machine.state = machine.previewState
    }

    override fun zoom(zoom: Float, type: Int) {
        Log.i(TAG, "zoom")
    }
}
