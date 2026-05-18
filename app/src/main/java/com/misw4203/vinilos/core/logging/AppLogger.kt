package com.misw4203.vinilos.core.logging

/** Logging seam para excepciones tragadas best-effort (no cambia la política de fallback). */
interface AppLogger {
    fun w(tag: String, message: String, throwable: Throwable? = null)
}
