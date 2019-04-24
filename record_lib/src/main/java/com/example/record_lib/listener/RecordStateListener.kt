package com.example.record_lib.listener

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/18/19.
 */
interface RecordStateListener {
    fun recordStart()
    fun recordEnd(time: Long)
    fun recordCancel()
}
