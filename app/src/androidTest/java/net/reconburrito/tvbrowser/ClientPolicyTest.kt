package net.reconburrito.tvbrowser

import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.reconburrito.tvbrowser.browser.Tab
import net.reconburrito.tvbrowser.browser.TvWebChromeClient
import net.reconburrito.tvbrowser.browser.TvWebViewClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Security policy checks on the WebView clients, independent of the Activity. */
@RunWith(AndroidJUnit4::class)
class ClientPolicyTest {

    private class Events : TvWebViewClient.Events, TvWebChromeClient.Events {
        var sslBlocked = 0
        var home = 0
        override fun onPageStarted(tab: Tab, url: String) {}
        override fun onPageFinished(tab: Tab, url: String, title: String) {}
        override fun onGoHome(tab: Tab) { home++ }
        override fun onSslBlocked(tab: Tab, url: String) { sslBlocked++ }
        override fun onRendererGone(tab: Tab, crashed: Boolean): Boolean = true
        override fun onProgress(tab: Tab, percent: Int) {}
        override fun onTitle(tab: Tab, title: String) {}
        override fun onEnterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {}
        override fun onExitFullscreen() {}
    }

    private fun request(url: String) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = true
        override fun isRedirect() = false
        override fun hasGesture() = true
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): MutableMap<String, String> = mutableMapOf()
    }

    private fun <T> onMain(block: () -> T): T {
        var out: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { out = block() }
        @Suppress("UNCHECKED_CAST")
        return out as T
    }

    @Test fun blocksNonWebSchemes() {
        onMain {
            val wv = WebView(ApplicationProvider.getApplicationContext())
            val ev = Events()
            val client = TvWebViewClient(Tab(), ev)
            assertTrue(client.shouldOverrideUrlLoading(wv, request("intent://scan/#Intent;scheme=zxing;end")))
            assertTrue(client.shouldOverrideUrlLoading(wv, request("market://details?id=x")))
            assertTrue(client.shouldOverrideUrlLoading(wv, request("javascript:alert(1)")))
            assertTrue(client.shouldOverrideUrlLoading(wv, request("file:///sdcard/x")))
            assertFalse(client.shouldOverrideUrlLoading(wv, request("https://example.com")))
            assertFalse(client.shouldOverrideUrlLoading(wv, request("http://192.168.1.1")))
            assertTrue(client.shouldOverrideUrlLoading(wv, request("tvbrowser://home")))
            assertEquals(1, ev.home)
            wv.destroy()
        }
    }

    @Test fun sslErrorsAreCancelled() {
        val ev = Events()
        val client = TvWebViewClient(Tab(), ev)
        var cancelled = 0
        var proceeded = 0
        client.decideSslError("https://bad.example", cancel = { cancelled++ }, proceed = { proceeded++ })
        assertEquals(1, cancelled)
        assertEquals(0, proceeded)
        assertEquals(1, ev.sslBlocked)
    }

    @Test fun geolocationAndMediaPermissionsDenied() {
        val client = TvWebChromeClient(Tab(), Events())
        var allowed: Boolean? = null
        var retained: Boolean? = null
        client.onGeolocationPermissionsShowPrompt("https://x", GeolocationPermissions.Callback { _, a, r -> allowed = a; retained = r })
        assertEquals(false, allowed)
        assertEquals(false, retained)

        var denied = 0
        var granted = 0
        val req = object : PermissionRequest() {
            override fun getOrigin(): Uri = Uri.parse("https://x")
            override fun getResources(): Array<String> = arrayOf(RESOURCE_VIDEO_CAPTURE)
            override fun grant(resources: Array<out String>?) { granted++ }
            override fun deny() { denied++ }
        }
        client.onPermissionRequest(req)
        assertEquals(1, denied)
        assertEquals(0, granted)
    }

    @Test fun popupsRefused() {
        onMain {
            val wv = WebView(ApplicationProvider.getApplicationContext())
            val client = TvWebChromeClient(Tab(), Events())
            assertFalse(client.onCreateWindow(wv, false, true, null))
            wv.destroy()
        }
    }
}
