package net.reconburrito.tvbrowser

import android.app.Application
import net.reconburrito.tvbrowser.util.CrashLogger

class TvBrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
