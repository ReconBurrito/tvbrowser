package net.reconburrito.tvbrowser.browser

import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

class TvWebChromeClient(private val tab: Tab, private val events: Events) : WebChromeClient() {

    interface Events {
        fun onProgress(tab: Tab, percent: Int)
        fun onTitle(tab: Tab, title: String)
        fun onEnterFullscreen(view: View, callback: CustomViewCallback)
        fun onExitFullscreen()
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        events.onProgress(tab, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        tab.title = title ?: ""
        events.onTitle(tab, tab.title)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        events.onEnterFullscreen(view, callback)
    }

    override fun onHideCustomView() {
        events.onExitFullscreen()
    }

    /** Camera, microphone, MIDI, protected media: always denied. */
    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    /** Location: always denied, never remembered as allowed. */
    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        callback.invoke(origin, false, false)
    }

    /** No popups. */
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
        return false
    }
}
