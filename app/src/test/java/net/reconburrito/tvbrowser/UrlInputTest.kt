package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.util.UrlInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlInputTest {

    @Test fun emptyGoesHome() {
        assertEquals(UrlInput.HOME, UrlInput.toLoadable(""))
        assertEquals(UrlInput.HOME, UrlInput.toLoadable("   "))
        assertEquals(UrlInput.HOME, UrlInput.toLoadable(UrlInput.HOME))
    }

    @Test fun fullUrlsPassThrough() {
        assertEquals("https://example.com/a?b=1", UrlInput.toLoadable("https://example.com/a?b=1"))
        assertEquals("http://192.168.1.10:8080/", UrlInput.toLoadable("http://192.168.1.10:8080/"))
        assertEquals("about:blank", UrlInput.toLoadable("about:blank"))
    }

    @Test fun bareHostGetsHttps() {
        assertEquals("https://example.com", UrlInput.toLoadable("example.com"))
        assertEquals("https://seerr.reconburrito.net/login", UrlInput.toLoadable("seerr.reconburrito.net/login"))
        assertEquals("https://www.youtube.com", UrlInput.toLoadable("  www.youtube.com  "))
    }

    @Test fun lanAddressesGetHttp() {
        assertEquals("http://192.168.1.5", UrlInput.toLoadable("192.168.1.5"))
        assertEquals("http://192.168.1.5:8096/web", UrlInput.toLoadable("192.168.1.5:8096/web"))
        assertEquals("http://localhost:3000", UrlInput.toLoadable("localhost:3000"))
        assertEquals("https://seerr.reconburrito.net:5055/", UrlInput.toLoadable("seerr.reconburrito.net:5055/"))
    }

    @Test fun portIsNotAScheme() {
        assertFalse(UrlInput.isLoadableScheme("localhost:3000"))
        assertFalse(UrlInput.isLoadableScheme("example.com:8080/x"))
        assertTrue(UrlInput.isLoadableScheme("http://example.com:8080/x"))
    }

    @Test fun wordsBecomeSearch() {
        assertEquals(UrlInput.SEARCH_BASE + "shield+tv+browser", UrlInput.toLoadable("shield tv browser"))
        assertEquals(UrlInput.SEARCH_BASE + "hello", UrlInput.toLoadable("hello"))
    }

    @Test fun searchEncodesSpecials() {
        assertEquals(UrlInput.SEARCH_BASE + "a%26b%3Dc", UrlInput.search("a&b=c"))
        assertEquals(UrlInput.SEARCH_BASE + "caf%C3%A9", UrlInput.search("café"))
    }

    @Test fun dangerousSchemesBecomeSearch() {
        assertTrue(UrlInput.toLoadable("javascript:alert(1)").startsWith(UrlInput.SEARCH_BASE))
        assertTrue(UrlInput.toLoadable("file:///etc/hosts").startsWith(UrlInput.SEARCH_BASE))
        assertTrue(UrlInput.toLoadable("content://media/x").startsWith(UrlInput.SEARCH_BASE))
        assertTrue(UrlInput.toLoadable("intent://scan/#Intent;scheme=zxing;end").startsWith(UrlInput.SEARCH_BASE))
    }

    @Test fun loadableSchemeGate() {
        assertTrue(UrlInput.isLoadableScheme("https://x.y"))
        assertTrue(UrlInput.isLoadableScheme("http://x.y"))
        assertTrue(UrlInput.isLoadableScheme("about:blank"))
        assertFalse(UrlInput.isLoadableScheme("javascript:void(0)"))
        assertFalse(UrlInput.isLoadableScheme("file:///sdcard/x"))
        assertFalse(UrlInput.isLoadableScheme("intent://x"))
        assertFalse(UrlInput.isLoadableScheme("market://details?id=x"))
        assertFalse(UrlInput.isLoadableScheme("no-scheme-here"))
    }

    @Test fun displayTextHidesHome() {
        assertEquals("", UrlInput.displayText(UrlInput.HOME))
        assertEquals("", UrlInput.displayText(null))
        assertEquals("https://a.b", UrlInput.displayText("https://a.b"))
    }
}
