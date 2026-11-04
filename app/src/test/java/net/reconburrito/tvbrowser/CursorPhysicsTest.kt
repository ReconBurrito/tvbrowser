package net.reconburrito.tvbrowser

import net.reconburrito.tvbrowser.input.CursorPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorPhysicsTest {

    @Test fun startsCentered() {
        val p = CursorPhysics(1920, 1080)
        assertEquals(960f, p.x, 0.01f)
        assertEquals(540f, p.y, 0.01f)
    }

    @Test fun speedAcceleratesAndCaps() {
        val p = CursorPhysics(1920, 1080)
        assertEquals(CursorPhysics.BASE_SPEED, p.speedFor(0), 0.01f)
        assertTrue(p.speedFor(500) > p.speedFor(0))
        assertEquals(CursorPhysics.MAX_SPEED, p.speedFor(60_000), 0.01f)
    }

    @Test fun stepMovesRightByDistance() {
        val p = CursorPhysics(1920, 1080)
        val s = p.step(1, 0, 0, 100)
        assertEquals(960f + CursorPhysics.BASE_SPEED * 0.1f, s.x, 0.5f)
        assertEquals(540f, s.y, 0.01f)
        assertEquals(0, s.scrollX)
        assertEquals(0, s.scrollY)
    }

    @Test fun edgeOverflowBecomesScroll() {
        val p = CursorPhysics(1920, 1080)
        p.moveTo(960f, 1079f)
        val s = p.step(0, 1, 0, 100)
        assertEquals(1080f, s.y, 0.01f)
        assertTrue("expected downward scroll, got ${s.scrollY}", s.scrollY > 0)
        p.moveTo(960f, 1f)
        val up = p.step(0, -1, 0, 100)
        assertEquals(0f, up.y, 0.01f)
        assertTrue(up.scrollY < 0)
    }

    @Test fun moveToClamps() {
        val p = CursorPhysics(100, 100)
        p.moveTo(-50f, 500f)
        assertEquals(0f, p.x, 0.01f)
        assertEquals(100f, p.y, 0.01f)
    }

    @Test fun setBoundsKeepsCursorInside() {
        val p = CursorPhysics(1920, 1080)
        p.moveTo(1900f, 1000f)
        p.setBounds(1280, 720)
        assertTrue(p.x <= 1280f)
        assertTrue(p.y <= 720f)
    }
}
