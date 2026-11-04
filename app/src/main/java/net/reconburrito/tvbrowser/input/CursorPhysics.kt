package net.reconburrito.tvbrowser.input

import kotlin.math.max
import kotlin.math.min

/**
 * Pure cursor movement model. Holding a direction accelerates from BASE to MAX speed.
 * When the cursor is pinned against an edge, the overflow is returned as a scroll request,
 * so holding the D-pad at the edge scrolls the page, faster the longer it is held.
 */
class CursorPhysics(width: Int, height: Int) {
    var width: Int = max(1, width); private set
    var height: Int = max(1, height); private set
    var x: Float = this.width / 2f; private set
    var y: Float = this.height / 2f; private set

    data class Step(val x: Float, val y: Float, val scrollX: Int, val scrollY: Int)

    fun setBounds(w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        x = x.coerceIn(0f, width.toFloat())
        y = y.coerceIn(0f, height.toFloat())
    }

    fun moveTo(nx: Float, ny: Float) {
        x = nx.coerceIn(0f, width.toFloat())
        y = ny.coerceIn(0f, height.toFloat())
    }

    fun center() = moveTo(width / 2f, height / 2f)

    /** Pixels per second after the direction has been held for heldMs. */
    fun speedFor(heldMs: Long): Float {
        val s = BASE_SPEED + ACCEL * (heldMs / 1000f)
        return min(MAX_SPEED, s)
    }

    /**
     * Advance one frame. dx/dy are -1, 0 or 1. Returns the new position and any scroll overflow
     * (positive scrollY means the page should scroll down).
     */
    fun step(dx: Int, dy: Int, heldMs: Long, dtMs: Long): Step {
        val speed = speedFor(heldMs)
        val dist = speed * (dtMs / 1000f)
        var nx = x + dx * dist
        var ny = y + dy * dist
        var sx = 0
        var sy = 0
        if (nx < 0f) { sx = nx.toInt(); nx = 0f }
        if (nx > width) { sx = (nx - width).toInt(); nx = width.toFloat() }
        if (ny < 0f) { sy = ny.toInt(); ny = 0f }
        if (ny > height) { sy = (ny - height).toInt(); ny = height.toFloat() }
        x = nx
        y = ny
        return Step(nx, ny, sx, sy)
    }

    companion object {
        const val BASE_SPEED = 450f
        const val ACCEL = 1500f
        const val MAX_SPEED = 2600f
        const val TAP_THRESHOLD_MS = 160L
        const val FRAME_MS = 16L
    }
}
