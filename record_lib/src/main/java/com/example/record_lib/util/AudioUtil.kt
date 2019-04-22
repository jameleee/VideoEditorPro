package com.example.record_lib.util

import android.content.Context
import android.media.AudioManager

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
object AudioUtil {
    fun setAudioManage(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
    }
}
