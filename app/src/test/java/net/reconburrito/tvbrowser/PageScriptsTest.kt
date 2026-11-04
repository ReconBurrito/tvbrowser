package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.browser.PageScripts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageScriptsTest {

    @Test fun unquoteHandlesJsResults() {
        assertEquals("", PageScripts.unquote(null))
        assertEquals("", PageScripts.unquote("null"))
        assertEquals("", PageScripts.unquote("\"\""))
        assertEquals("https://a.b/c", PageScripts.unquote("\"https:\\/\\/a.b\\/c\""))
        assertEquals("12.5,40", PageScripts.unquote("\"12.5,40\""))
    }

    @Test fun scriptsInterpolateCoordinates() {
        val s = PageScripts.snapTarget(10.5f, 20f, 1, 0)
        assertTrue(s.contains("10.5"))
        assertTrue(s.contains("elementFromPoint") || s.contains("querySelectorAll"))
        val l = PageScripts.linkAt(3f, 4f)
        assertTrue(l.contains("elementFromPoint(3.0,4.0)"))
    }
}
