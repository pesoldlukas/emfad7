package com.emfad.app.utils

import android.util.Log

object Logger {
    private const val TAG = "EMFAD_APP"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun v(message: String) {
        Log.v(TAG, message)
    }
}