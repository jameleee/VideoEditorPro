package com.example.videoeditorpro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.record_lib.ButtonState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraRecord.run {
            setMinDuration(3000)
            setDuration(10000)
            setFeatures(ButtonState.BUTTON_STATE_ONLY_RECORDER.type())
        }
    }

    override fun onResume() {
        super.onResume()
        cameraRecord.onResume()
    }

    override fun onPause() {
        super.onPause()
        cameraRecord.onPause()
    }
}
