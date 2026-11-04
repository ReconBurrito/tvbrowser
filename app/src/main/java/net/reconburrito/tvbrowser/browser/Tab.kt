package net.reconburrito.tvbrowser.browser

import android.webkit.WebView
import net.reconburrito.tvbrowser.util.UrlInput

/**
 * One browser tab. The WebView is created lazily on first activation so restored sessions
 * with many tabs do not spin up ten renderers at once.
 */
class Tab(var url: String = UrlInput.HOME, var title: String = "") {
    var webView: WebView? = null
    /** Set when a URL should be loaded as soon as the WebView exists. */
    var pendingUrl: String? = if (UrlInput.isHome(url)) null else url
    /** uptime of the last renderer crash, used to break crash loops. */
    var lastCrashAt: Long = 0L

    val isHome: Boolean get() = UrlInput.isHome(url)
    val displayTitle: String get() = if (isHome) "Home" else title.ifBlank { url }
}
