package com.example.record_lib.util

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
object AngleUtil {
    fun getSensorAngle(x: Float, y: Float): Int {
        return if (Math.abs(x) > Math.abs(y)) {
            /**
             * Horizontal screen tilt angle is relatively large
             */
            when {
                x > 4 ->
                    /**
                     * Tilt to the left
                     */
                    270
                x < -4 ->
                    /**
                     * Tilt to the right
                     */
                    90
                else ->
                    /**
                     * Tilt angle is not big enough
                     */
                    0
            }
        } else {
            /**
             * Vertical screen tilt angle is relatively large
             */
            when {
                y > 7 ->
                    /**
                     * Tilt left
                     */
                    0
                y < -7 ->
                    /**
                     * Tilt to the right
                     */
                    180
                else ->
                    /**
                     *
                     * Tilt angle is not big enough
                     */
                    0
            }
        }
    }
}
