package com.example.record_lib.camera2state

import android.hardware.camera2.CameraDevice
import android.view.Surface
import android.view.TextureView
import com.example.record_lib.Camera2Interface

/**
 * @author Dat Bui T. on 4/22/19.
 */
interface State {

    fun start(holder: TextureView, screenProp: Float) {}

    fun stop() {}

    fun focus(x: Float, y: Float, callback: Camera2Interface.FocusCallback) {}

    fun switchView(holder: TextureView, screenProp: Float, stateCallback: CameraDevice.StateCallback) {}

    fun restart() {}

    fun capture() {}

    fun record(surface: Surface, screenProp: Float) {}

    fun stopRecord(isShort: Boolean, time: Long) {}

    fun cancel(holder: TextureView, screenProp: Float) {}

    fun confirm() {}

    fun zoom(zoom: Float, type: Int) {}

    fun flash(mode: String) {}
}
