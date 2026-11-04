package net.reconburrito.tvbrowser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import net.reconburrito.tvbrowser.data.SavedSession
import net.reconburrito.tvbrowser.data.SavedTab
import net.reconburrito.tvbrowser.util.UrlInput
import net.reconburrito.tvbrowser.util.UserAgents

/**
 * Owns the WebViews. Enforces the tab cap, lazy creation, session snapshot/restore
 * and renderer-crash recovery. All calls are main-thread.
 */
class TabManager(
    private val ctx: Context,
    private val container: ViewGroup,
    private val clientFactory: ClientFactory,
) {
    interface ClientFactory {
        fun attach(tab: Tab, webView: WebView)
    }

    val registry = TabRegistry<Tab>()
    var desktopUa: Boolean = true
        set(v) { field = v; registry.all().forEach { t -> t.webView?.let { applyUserAgent(it) } } }

    val active: Tab? get() = registry.active
    val count: Int get() = registry.size

    fun newTab(url: String = UrlInput.HOME, activate: Boolean = true): Tab? {
        val tab = Tab(url)
        if (!registry.add(tab, activate)) return null
        if (activate) ensureWebView(tab)
        refreshVisibility()
        return tab
    }

    /** Re-applies "only the active, non-home tab's WebView is visible". */
    fun refreshVisibility() {
        registry.active?.let { showOnly(it) }
    }

    fun switchTo(index: Int): Tab? {
        val tab = registry.activate(index) ?: return null
        ensureWebView(tab)
        showOnly(tab)
        return tab
    }

    /** Closes a tab. If it was the last one a fresh home tab is created. Returns the new active tab. */
    fun closeTab(index: Int): Tab? {
        val removed = registry.remove(index) ?: return active
        destroyWebView(removed)
        if (registry.size == 0) {
            return newTab(UrlInput.HOME)
        }
        val now = registry.active ?: return null
        ensureWebView(now)
        showOnly(now)
        return now
    }

    fun load(tab: Tab, url: String) {
        tab.url = url
        if (UrlInput.isHome(url)) {
            tab.pendingUrl = null
            tab.title = ""
            refreshVisibility()
            return
        }
        val wv = ensureWebView(tab)
        tab.pendingUrl = null
        wv.loadUrl(url)
        showOnly(tab)
    }

    /**
     * Replace a tab's WebView after a renderer crash. The page is reloaded once; if the same tab
     * crashes again within CRASH_LOOP_WINDOW_MS (or the URL is a chrome:// page) it goes home
     * instead, so a page that kills the renderer cannot loop forever.
     */
    fun recover(tab: Tab): WebView {
        val lastUrl = tab.webView?.url ?: tab.url
        val now = android.os.SystemClock.uptimeMillis()
        val looping = now - tab.lastCrashAt < CRASH_LOOP_WINDOW_MS || lastUrl.startsWith("chrome://")
        tab.lastCrashAt = now
        destroyWebView(tab)
        if (looping || UrlInput.isHome(lastUrl)) {
            tab.url = UrlInput.HOME
            tab.pendingUrl = null
            tab.title = ""
        } else {
            tab.url = lastUrl
            tab.pendingUrl = lastUrl
        }
        val wv = ensureWebView(tab)
        refreshVisibility()
        return wv
    }

    fun snapshot(): SavedSession {
        val tabs = registry.all().map { t ->
            val wv = t.webView
            val url = wv?.url?.takeIf { it.isNotBlank() } ?: t.url
            SavedTab(url, wv?.title ?: t.title)
        }
        return SavedSession(tabs, registry.activeIndex.coerceAtLeast(0))
    }

    fun restore(s: SavedSession) {
        for (t in s.tabs) {
            val tab = Tab(t.url, t.title)
            if (!registry.add(tab, activate = false)) break
        }
        if (registry.size == 0) {
            newTab(UrlInput.HOME)
            return
        }
        switchTo(s.activeIndex.coerceIn(0, registry.size - 1))
    }

    fun destroyAll() {
        registry.all().forEach { destroyWebView(it) }
    }

    fun ensureWebView(tab: Tab): WebView {
        tab.webView?.let { return it }
        val wv = createWebView()
        tab.webView = wv
        clientFactory.attach(tab, wv)
        container.addView(wv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val pending = tab.pendingUrl
        if (pending != null) {
            tab.pendingUrl = null
            wv.loadUrl(pending)
        }
        return wv
    }

    private fun showOnly(tab: Tab) {
        for (t in registry.all()) {
            val wv = t.webView ?: continue
            val visible = t === tab && !t.isHome
            wv.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                wv.onResume()
                wv.resumeTimers()
            } else {
                wv.onPause()
            }
        }
    }

    private fun destroyWebView(tab: Tab) {
        val wv = tab.webView ?: return
        tab.webView = null
        container.removeView(wv)
        wv.stopLoading()
        wv.loadUrl("about:blank")
        wv.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val wv = WebView(ctx)
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportMultipleWindows(false)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.setGeolocationEnabled(false)
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.textZoom = 100
        applyUserAgent(wv)
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(wv, true)
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true
        wv.setBackgroundColor(0xFF101418.toInt())
        wv.isVerticalScrollBarEnabled = true
        return wv
    }

    companion object {
        const val CRASH_LOOP_WINDOW_MS = 15_000L
    }

    private fun applyUserAgent(wv: WebView) {
        val s = wv.settings
        s.userAgentString = if (desktopUa) UserAgents.desktopFrom(WebSettings.getDefaultUserAgent(ctx)) else null
    }
}
