package com.misw4203.vinilos.core.logging

import android.util.Log
import javax.inject.Inject

class AndroidAppLogger @Inject constructor() : AppLogger {
    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }
}
