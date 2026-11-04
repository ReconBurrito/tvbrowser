package net.reconburrito.tvbrowser.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes uncaught exceptions to crash.log in the app's external files dir
 * (/sdcard/Android/data/net.reconburrito.tvbrowser/files/crash.log) with a fallback to
 * internal filesDir. The log is capped so it can never grow without bound.
 */
object CrashLogger {
    private const val TAG = "TvBrowser"
    private const val FILE_NAME = "crash.log"
    private const val MAX_BYTES = 200L * 1024

    fun logFile(ctx: Context): File {
        val ext = ctx.getExternalFilesDir(null)
        val dir = if (ext != null && (ext.exists() || ext.mkdirs())) ext else ctx.filesDir
        return File(dir, FILE_NAME)
    }

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                append(app, "UNCAUGHT on ${thread.name}", throwable)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun append(ctx: Context, label: String, t: Throwable? = null) {
        val f = logFile(ctx)
        val sw = StringWriter()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sw.append(stamp).append(' ').append(label).append('\n')
        if (t != null) {
            t.printStackTrace(PrintWriter(sw))
        }
        sw.append('\n')
        Log.e(TAG, sw.toString())
        try {
            if (f.exists() && f.length() > MAX_BYTES) {
                val tail = f.readText().takeLast((MAX_BYTES / 2).toInt())
                f.writeText(tail)
            }
            f.appendText(sw.toString())
        } catch (e: Exception) {
            Log.e(TAG, "crash log write failed", e)
        }
    }

    fun read(ctx: Context): String? {
        val f = logFile(ctx)
        return if (f.exists()) f.readText() else null
    }
}
