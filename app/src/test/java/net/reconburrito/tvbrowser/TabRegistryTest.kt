package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.browser.TabRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabRegistryTest {

    @Test fun capIsTen() {
        val r = TabRegistry<String>()
        for (i in 1..10) assertTrue(r.add("t$i"))
        assertTrue(r.isFull)
        assertFalse(r.add("t11"))
        assertEquals(10, r.size)
    }

    @Test fun addActivatesByDefault() {
        val r = TabRegistry<String>()
        r.add("a")
        r.add("b")
        assertEquals("b", r.active)
        r.add("c", activate = false)
        assertEquals("b", r.active)
        assertEquals(3, r.size)
    }

    @Test fun removeMovesActiveToPrevious() {
        val r = TabRegistry<String>()
        r.add("a"); r.add("b"); r.add("c")
        assertEquals(2, r.activeIndex)
        r.remove(2)
        assertEquals("b", r.active)
        r.remove(0)
        assertEquals("b", r.active)
        assertEquals(0, r.activeIndex)
        r.remove(0)
        assertNull(r.active)
        assertEquals(-1, r.activeIndex)
    }

    @Test fun removeOutOfRangeIsNoop() {
        val r = TabRegistry<String>()
        r.add("a")
        assertNull(r.remove(5))
        assertEquals(1, r.size)
    }

    @Test fun activateOutOfRangeReturnsNull() {
        val r = TabRegistry<String>()
        r.add("a")
        assertNull(r.activate(3))
        assertEquals("a", r.activate(0))
    }
}
