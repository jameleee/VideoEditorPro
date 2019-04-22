package com.example.record_lib.util

import android.graphics.Bitmap
import android.os.Environment

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
object FileUtil {
    private val TAG = "CJT"
    private val parentPath = Environment.getExternalStorageDirectory()
    private var storagePath = ""
    private var DST_FOLDER_NAME = "JCamera"

    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    private fun initPath(): String {
        if (storagePath == "") {
            storagePath = parentPath.absolutePath + File.separator + "haodiaoyu" + File.separator + DST_FOLDER_NAME
            val f = File(storagePath)
            if (!f.exists()) {
                f.mkdir()
            }
        }
        return storagePath
    }

    fun saveBitmap(dir: String, b: Bitmap): String {
        DST_FOLDER_NAME = dir
        val path = initPath()
        val dataTake = System.currentTimeMillis()
        val jpegName = path + File.separator + "picture_" + dataTake + ".jpg"
        return try {
            val fout = FileOutputStream(jpegName)
            val bos = BufferedOutputStream(fout)
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
            jpegName
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }

    }

    fun deleteFile(url: String): Boolean {
        var result = false
        val file = File(url)
        if (file.exists()) {
            result = file.delete()
        }
        return result
    }
}
