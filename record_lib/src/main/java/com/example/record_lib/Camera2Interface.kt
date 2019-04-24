package com.example.record_lib

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager

/**
 * @author Dat Bui T. on 4/24/19.
 */
class Camera2Interface(private val context: Context) {

    companion object {
        val instance: Camera2Interface by lazy {
            synchronized(CameraInterface::class.java) {
                Camera2Interface()
            }
        }
    }

    init {

       /* val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                // Do something with `device`
            }

            override fun onDisconnected(device: CameraDevice) {
                device.close()
            }

            override fun onError(device: CameraDevice, error: Int) {
                onDisconnected(device)
            }
        }, null)

        val cameraIdList = cameraManager.cameraIdList // may be empty
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    }*/
}
