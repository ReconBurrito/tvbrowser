package net.reconburrito.tvbrowser.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences("tvbrowser", Context.MODE_PRIVATE)

    var desktopUa: Boolean
        get() = sp.getBoolean(KEY_DESKTOP_UA, true)
        set(v) = sp.edit().putBoolean(KEY_DESKTOP_UA, v).apply()

    var sessionJson: String?
        get() = sp.getString(KEY_SESSION, null)
        set(v) = sp.edit().putString(KEY_SESSION, v).apply()

    companion object {
        const val KEY_DESKTOP_UA = "desktop_ua"
        const val KEY_SESSION = "session"
    }
}
