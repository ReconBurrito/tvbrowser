package net.reconburrito.tvbrowser.util

object UserAgents {
    private val androidPlatform = Regex("\\(Linux; Android [^)]*\\)")

    /**
     * Derive a desktop UA from the WebView's default UA so the Chrome version stays current
     * with whatever WebView build the Shield has. Drops the "wv" and "Mobile" markers.
     */
    fun desktopFrom(defaultUa: String): String {
        var ua = defaultUa.replace(androidPlatform, "(X11; Linux x86_64)")
        ua = ua.replace(" Mobile", "")
        ua = ua.replace("; wv", "")
        if (!ua.contains("X11; Linux x86_64")) {
            ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        }
        return ua
    }

    fun isDesktop(ua: String): Boolean = !ua.contains("Mobile") && !ua.contains("Android")
}
