package net.reconburrito.tvbrowser

import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.reconburrito.tvbrowser.TestSupport.PAGE_HTML
import net.reconburrito.tvbrowser.TestSupport.press
import net.reconburrito.tvbrowser.TestSupport.waitUntil
import net.reconburrito.tvbrowser.data.Prefs
import net.reconburrito.tvbrowser.data.SavedSession
import net.reconburrito.tvbrowser.data.SavedTab
import net.reconburrito.tvbrowser.data.SessionCodec
import net.reconburrito.tvbrowser.util.CrashLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun resetState() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Prefs(ctx).sessionJson = null
        ctx.deleteDatabase("tvbrowser.db")
        CrashLogger.logFile(ctx).delete()
    }

    @After fun close() {
        if (::scenario.isInitialized) scenario.close()
    }

    // REQ-01 launch, REQ-09 start page, REQ-04 toolbar hidden by default
    @Test fun launchesOnHomeWithToolbarHidden() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitUntil(scenario, what = "home visible") { it.isOnHome() }
        scenario.onActivity {
            assertFalse(it.isToolbarVisible())
            assertEquals(1, it.tabCount())
        }
    }

    // REQ-03 Menu toggles toolbar, Back hides it
    @Test fun menuShowsToolbarAndBackHidesIt() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitUntil(scenario, what = "home visible") { it.isOnHome() }
        press(scenario, KeyEvent.KEYCODE_MENU)
        waitUntil(scenario, what = "toolbar visible") { it.isToolbarVisible() }
        press(scenario, KeyEvent.KEYCODE_BACK)
        waitUntil(scenario, what = "toolbar hidden") { !it.isToolbarVisible() }
    }

    // REQ-02 page load, REQ-07 history recorded
    @Test fun loadsPageAndRecordsHistory() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.loadTestHtml(PAGE_HTML) }
        waitUntil(scenario, what = "title TestPage") { it.activeWebView()?.title == "TestPage" }
        waitUntil(scenario, what = "history entry") {
            it.database().listHistory(10).any { h -> h.url == "https://test.local/page" }
        }
        scenario.onActivity { assertFalse(it.isOnHome()) }
    }

    // REQ-05 cursor moves with D-pad, REQ-06 click lands on page content
    @Test fun dpadMovesCursorAndSelectClicks() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.loadTestHtml(PAGE_HTML) }
        waitUntil(scenario, what = "title TestPage") { it.activeWebView()?.title == "TestPage" }
        var start = 0f to 0f
        scenario.onActivity { start = it.cursorPosition() }
        press(scenario, KeyEvent.KEYCODE_DPAD_RIGHT, holdMs = 300)
        waitUntil(scenario, what = "cursor moved right") { it.cursorPosition().first > start.first }
        // move the cursor onto the button via JS-free path: hold down then left into the button box
        scenario.onActivity { a ->
            @Suppress("DEPRECATION")
            val scale = a.activeWebView()!!.scale
            // button center in CSS px is (200, 130); place cursor there in view px
            a.setCursorForTest(200f * scale, 130f * scale)
        }
        press(scenario, KeyEvent.KEYCODE_DPAD_CENTER)
        waitUntil(scenario, what = "button clicked (title Clicked)") { it.activeWebView()?.title == "Clicked" }
    }

    // REQ-10 session restore
    @Test fun restoresSavedSession() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Prefs(ctx).sessionJson = SessionCodec.encode(
            SavedSession(listOf(SavedTab("about:blank", "one"), SavedTab("about:blank", "two")), 1)
        )
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitUntil(scenario, what = "two tabs") { it.tabCount() == 2 }
        scenario.onActivity { assertFalse(it.isOnHome()) }
    }

    // REQ-11 renderer crash recovery with loop guard
    @Test fun rendererCrashRecoversAndDoesNotLoop() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.loadInActive("chrome://crash") }
        // first crash reloads chrome://crash once (within 15 s) -> second crash -> home
        waitUntil(scenario, timeoutMs = 30_000, what = "tab back on home after crash loop guard") { it.isOnHome() }
        scenario.onActivity {
            assertEquals(1, it.tabCount())
            val log = CrashLogger.read(it)
            assertNotNull(log)
            assertTrue(log!!.contains("renderer gone"))
        }
    }

    // REQ-08 bookmark toggle persists and shows on the home grid
    @Test fun bookmarkRoundTrip() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.loadTestHtml(PAGE_HTML) }
        waitUntil(scenario, what = "title TestPage") { it.activeWebView()?.title == "TestPage" }
        scenario.onActivity {
            assertTrue(it.database().addBookmark("TestPage", "https://test.local/page"))
            assertTrue(it.database().isBookmarked("https://test.local/page"))
            it.loadInActive("tvbrowser://home")
        }
        waitUntil(scenario, what = "home visible") { it.isOnHome() }
        scenario.onActivity {
            assertEquals(1, it.database().listBookmarks().size)
            assertEquals(1, it.database().removeBookmark("https://test.local/page"))
        }
    }
}
