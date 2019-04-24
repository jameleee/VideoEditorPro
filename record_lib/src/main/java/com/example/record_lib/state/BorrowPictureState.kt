package com.example.record_lib.state

import android.util.Log
import android.view.SurfaceHolder
import com.example.record_lib.CameraInterface
import com.example.record_lib.CameraView

/**
 * @author Dat Bui T. on 4/22/19.
 */
class BorrowPictureState(private val machine: CameraMachine) : State {
    companion object {
        private const val TAG = "BorrowPictureState"
    }

    override fun start(holder: SurfaceHolder, screenProp: Float) {
        CameraInterface.instance.doStartPreview(holder, screenProp)
        machine.state = machine.previewState
    }

    override fun cancel(holder: SurfaceHolder, screenProp: Float) {
        CameraInterface.instance.doStartPreview(holder, screenProp)
        machine.view.resetState(CameraView.TYPE_PICTURE)
        machine.state = machine.previewState
    }

    override fun confirm() {
        machine.view.confirmState(CameraView.TYPE_PICTURE)
        machine.state = machine.previewState
    }

    override fun zoom(zoom: Float, type: Int) {
        Log.d(TAG, "zoom")
    }
}
