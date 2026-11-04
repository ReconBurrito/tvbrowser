package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.util.UserAgents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentsTest {
    private val shieldDefault =
        "Mozilla/5.0 (Linux; Android 11; SHIELD Android TV Build/RQ1A.210105.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.107 Mobile Safari/537.36"

    @Test fun desktopUaDropsAndroidMarkers() {
        val ua = UserAgents.desktopFrom(shieldDefault)
        assertTrue(ua.contains("(X11; Linux x86_64)"))
        assertFalse(ua.contains("Android"))
        assertFalse(ua.contains("Mobile"))
        assertFalse(ua.contains("wv"))
        assertTrue("Chrome version must be preserved", ua.contains("Chrome/130.0.6723.107"))
        assertTrue(UserAgents.isDesktop(ua))
    }

    @Test fun unexpectedDefaultFallsBackToKnownDesktopUa() {
        val ua = UserAgents.desktopFrom("weird")
        assertTrue(ua.startsWith("Mozilla/5.0 (X11; Linux x86_64)"))
    }

    @Test fun mobileDefaultIsNotDesktop() {
        assertFalse(UserAgents.isDesktop(shieldDefault))
        assertEquals(false, UserAgents.isDesktop("Mozilla/5.0 (Linux; Android 11) Mobile"))
    }
}
