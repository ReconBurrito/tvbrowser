package net.reconburrito.tvbrowser.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Draws the virtual cursor (an arrow with a dark outline so it is visible on any page).
 * It is purely visual: touches pass through, focus is never taken.
 */
class CursorView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    var cursorX: Float = 0f
        private set
    var cursorY: Float = 0f
        private set
    var cursorPressed: Boolean = false
        set(v) { field = v; invalidate() }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; strokeJoin = Paint.Join.ROUND
    }
    private val pressedFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4FA3FF"); style = Paint.Style.FILL }
    private val path = Path()
    private val size = 34f

    init {
        isFocusable = false
        isClickable = false
        setWillNotDraw(false)
    }

    fun setPosition(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return
        path.reset()
        path.moveTo(cursorX, cursorY)
        path.lineTo(cursorX, cursorY + size)
        path.lineTo(cursorX + size * 0.28f, cursorY + size * 0.76f)
        path.lineTo(cursorX + size * 0.62f, cursorY + size * 0.72f)
        path.close()
        canvas.drawPath(path, if (cursorPressed) pressedFill else fill)
        canvas.drawPath(path, stroke)
    }
}
