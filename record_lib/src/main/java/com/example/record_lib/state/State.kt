package com.example.record_lib.state

import android.view.Surface
import android.view.SurfaceHolder
import com.example.record_lib.CameraInterface

/**
 * @author Dat Bui T. on 4/22/19.
 */
interface State {

    fun start(holder: SurfaceHolder, screenProp: Float) {}

    fun stop() {}

    fun focus(x: Float, y: Float, callback: CameraInterface.FocusCallback) {}

    fun switchView(holder: SurfaceHolder, screenProp: Float) {}

    fun restart() {}

    fun capture() {}

    fun record(surface: Surface, screenProp: Float) {}

    fun stopRecord(isShort: Boolean, time: Long) {}

    fun cancel(holder: SurfaceHolder, screenProp: Float) {}

    fun confirm() {}

    fun zoom(zoom: Float, type: Int) {}

    fun flash(mode: String) {}
}
