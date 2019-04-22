package com.example.record_lib.view

import android.graphics.Bitmap

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
interface CameraView {
    fun resetState(type: Int)

    fun confirmState(type: Int)

    fun showPicture(bitmap: Bitmap, isVertical: Boolean)

    fun playVideo(firstFrame: Bitmap, url: String)

    fun stopVideo()

    fun setTip(tip: String)

    fun startPreviewCallback()

    fun handlerFoucs(x: Float, y: Float): Boolean
}
