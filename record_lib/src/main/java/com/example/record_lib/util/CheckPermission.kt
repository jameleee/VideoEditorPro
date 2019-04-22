package com.example.record_lib.util

import android.hardware.Camera
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

object CheckPermission {
    const val STATE_RECORDING = -1
    const val STATE_NO_PERMISSION = -2
    const val STATE_SUCCESS = 1

    /**
     * 用于检测是否具有录音权限
     *
     * @return
     */
    //检测是否可以进入初始化状态
    //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
    //检测是否在录音中
    //检测是否可以获取录音结果
    val recordState: Int
        get() {
            val minBuffer = AudioRecord.getMinBufferSize(
                44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat
                    .ENCODING_PCM_16BIT
            )
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat
                    .CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer * 100
            )
            val point = ShortArray(minBuffer)
            val readSize: Int
            try {
                audioRecord.startRecording()
            } catch (e: Exception) {
                audioRecord.release()
                return STATE_NO_PERMISSION
            }

            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
                audioRecord.release()
                Log.d("CheckAudioPermission", "record state recording")
                return STATE_RECORDING
            } else {
                readSize = audioRecord.read(point, 0, point.size)

                return if (readSize <= 0) {
                    audioRecord.stop()
                    audioRecord.release()
                    Log.d("CheckAudioPermission", "录音的结果为空")
                    STATE_NO_PERMISSION
                } else {
                    audioRecord.stop()
                    audioRecord.release()
                    STATE_SUCCESS
                }
            }
        }

    @Synchronized
    fun isCameraUseable(cameraID: Int): Boolean {
        var canUse = true
        var mCamera: Camera? = null
        try {
            mCamera = Camera.open(cameraID)
            val mParameters = mCamera.parameters
            mCamera.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
            canUse = false
        } finally {
            if (mCamera != null) {
                mCamera.release()
            } else {
                canUse = false
            }
        }
        return canUse
    }
}