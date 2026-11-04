package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.data.SavedSession
import net.reconburrito.tvbrowser.data.SavedTab
import net.reconburrito.tvbrowser.data.SessionCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {

    @Test fun roundTrip() {
        val s = SavedSession(listOf(SavedTab("https://a.b/x?y=1", "A"), SavedTab("tvbrowser://home", "")), 1)
        val back = SessionCodec.decode(SessionCodec.encode(s))
        assertEquals(s, back)
    }

    @Test fun garbageIsNull() {
        assertNull(SessionCodec.decode(null))
        assertNull(SessionCodec.decode(""))
        assertNull(SessionCodec.decode("{not json"))
        assertNull(SessionCodec.decode("{\"tabs\":[]}"))
    }

    @Test fun activeIndexClamped() {
        val json = "{\"tabs\":[{\"url\":\"https://a\",\"title\":\"\"}],\"active\":7}"
        assertEquals(0, SessionCodec.decode(json)!!.activeIndex)
    }
}
