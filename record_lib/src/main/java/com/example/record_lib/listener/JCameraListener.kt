package com.example.record_lib.listener

import android.graphics.Bitmap

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
interface JCameraListener {

    fun captureSuccess(bitmap: Bitmap)

    fun recordSuccess(url: String, firstFrame: Bitmap)

}
