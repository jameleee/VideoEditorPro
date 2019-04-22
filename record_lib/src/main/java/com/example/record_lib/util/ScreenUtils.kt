package com.example.record_lib.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
object ScreenUtils {
    fun getScreenHeight(context: Context): Int {
        val metric = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.getMetrics(metric)
        return metric.heightPixels
    }

    fun getScreenWidth(context: Context): Int {
        val metric = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.getMetrics(metric)
        return metric.widthPixels
    }
}
