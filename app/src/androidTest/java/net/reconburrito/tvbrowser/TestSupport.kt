package net.reconburrito.tvbrowser

import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario

object TestSupport {
    const val PAGE_HTML = """<!doctype html><html><head><title>TestPage</title></head>
<body style="margin:0"><h1 id="h">Hello</h1>
<a id="link" href="https://test.local/other" style="position:absolute;left:600px;top:300px;width:200px;height:60px;display:block">Other</a>
<button id="btn" style="position:absolute;left:100px;top:100px;width:200px;height:60px" onclick="document.title='Clicked'">Click</button>
<div style="height:4000px"></div></body></html>"""

    /** Polls on the test thread until the predicate (run on the UI thread) is true. */
    fun waitUntil(scenario: ActivityScenario<MainActivity>, timeoutMs: Long = 10_000, what: String, pred: (MainActivity) -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            var ok = false
            scenario.onActivity { ok = pred(it) }
            if (ok) return
            Thread.sleep(100)
        }
        throw AssertionError("timed out waiting for: $what")
    }

    fun press(scenario: ActivityScenario<MainActivity>, keyCode: Int, holdMs: Long = 0) {
        scenario.onActivity { a ->
            val t = SystemClock.uptimeMillis()
            a.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0))
        }
        if (holdMs > 0) Thread.sleep(holdMs)
        scenario.onActivity { a ->
            val t = SystemClock.uptimeMillis()
            a.dispatchKeyEvent(KeyEvent(t - holdMs, t, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }
}
