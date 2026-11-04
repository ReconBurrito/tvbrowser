package net.reconburrito.tvbrowser.browser

import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import net.reconburrito.tvbrowser.util.UrlInput

class TvWebViewClient(private val tab: Tab, private val events: Events) : WebViewClient() {

    interface Events {
        fun onPageStarted(tab: Tab, url: String)
        fun onPageFinished(tab: Tab, url: String, title: String)
        fun onGoHome(tab: Tab)
        fun onSslBlocked(tab: Tab, url: String)
        fun onRendererGone(tab: Tab, crashed: Boolean): Boolean
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (UrlInput.isHome(url)) {
            events.onGoHome(tab)
            return true
        }
        // Block intent://, market://, javascript:, file:, content: and any other scheme the app
        // does not render. Nothing ever leaves the WebView into another app.
        return !UrlInput.isLoadableScheme(url)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        tab.url = url
        events.onPageStarted(tab, url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        tab.url = url
        tab.title = view.title ?: ""
        events.onPageFinished(tab, url, tab.title)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        decideSslError(error.url ?: view.url ?: "", cancel = { handler.cancel() }, proceed = { handler.proceed() })
    }

    /** Certificate policy: never proceed. Split out so tests can verify it without a real SslErrorHandler. */
    fun decideSslError(url: String, cancel: () -> Unit, proceed: () -> Unit) {
        cancel()
        events.onSslBlocked(tab, url)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return events.onRendererGone(tab, detail.didCrash())
    }
}
