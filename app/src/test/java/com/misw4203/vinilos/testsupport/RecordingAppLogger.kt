package com.misw4203.vinilos.testsupport

import com.misw4203.vinilos.core.logging.AppLogger

class RecordingAppLogger : AppLogger {
    data class Entry(val tag: String, val message: String, val throwable: Throwable?)
    val entries = mutableListOf<Entry>()
    override fun w(tag: String, message: String, throwable: Throwable?) {
        entries += Entry(tag, message, throwable)
    }
}
