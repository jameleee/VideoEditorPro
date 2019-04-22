package com.example.record_lib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.record_lib.listener.CaptureListener
import kotlinx.android.synthetic.main.camera_view.view.*

/**
 * @author Dat Bui T. on 4/22/19.
 */
class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        setWillNotDraw(false)
        LayoutInflater.from(context).inflate(R.layout.camera_view, this)
        capture_layout.setCaptureLisenter(object : CaptureListener {
            override fun takePictures() {
            }

            override fun recordShort(time: Long) {
            }

            override fun recordStart() {
            }

            override fun recordEnd(time: Long) {
            }

            override fun recordZoom(zoom: Float) {
            }

            override fun recordError() {
            }
        })
    }
}
