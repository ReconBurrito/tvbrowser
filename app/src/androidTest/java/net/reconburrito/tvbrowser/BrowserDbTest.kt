package net.reconburrito.tvbrowser

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.reconburrito.tvbrowser.data.BrowserDb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserDbTest {
    private lateinit var db: BrowserDb

    @Before fun open() {
        db = BrowserDb(ApplicationProvider.getApplicationContext(), null) // in-memory
    }

    @After fun close() { db.close() }

    @Test fun bookmarksAreUniqueByUrl() {
        assertTrue(db.addBookmark("A", "https://a.b"))
        assertFalse(db.addBookmark("A again", "https://a.b"))
        assertEquals(1, db.listBookmarks().size)
        assertTrue(db.isBookmarked("https://a.b"))
        assertEquals(1, db.removeBookmark("https://a.b"))
        assertFalse(db.isBookmarked("https://a.b"))
    }

    @Test fun historyIsCappedAt5000() {
        for (i in 0 until 5100) db.addHistory("t$i", "https://h/$i")
        assertEquals(5000L, db.historyCount())
        val newest = db.listHistory(1).first()
        assertEquals("https://h/5099", newest.url)
    }

    @Test fun historyIgnoresHomeAndBlank() {
        db.addHistory("x", "tvbrowser://home")
        db.addHistory("x", "about:blank")
        db.addHistory("x", "")
        assertEquals(0L, db.historyCount())
    }

    @Test fun clearHistoryEmptiesTable() {
        db.addHistory("a", "https://a")
        db.addHistory("b", "https://b")
        assertEquals(2, db.clearHistory())
        assertEquals(0L, db.historyCount())
    }

    @Test fun exportContainsBothTables() {
        db.addBookmark("A", "https://a.b")
        db.addHistory("H", "https://h.i")
        val json = org.json.JSONObject(db.exportJson())
        assertEquals(1, json.getJSONArray("bookmarks").length())
        assertEquals(1, json.getJSONArray("history").length())
    }
}
