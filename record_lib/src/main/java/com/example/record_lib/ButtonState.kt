package com.example.record_lib

/**
 * Copyright © 2018 AsianTech inc.
 * Create by Dat Bui T. on 4/19/19.
 */
enum class ButtonState(private val state: Int) {
    BUTTON_STATE_BOTH(0),
    BUTTON_STATE_ONLY_CAPTURE(1),
    BUTTON_STATE_ONLY_RECORDER(2),
    BUTTON_STATE_ZOOM(3);

    fun type() = state
}
