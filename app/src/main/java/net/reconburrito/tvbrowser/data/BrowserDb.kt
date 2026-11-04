package net.reconburrito.tvbrowser.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val id: Long, val title: String, val url: String, val created: Long)
data class HistoryEntry(val id: Long, val title: String, val url: String, val visited: Long)

/**
 * App-private SQLite store for bookmarks and history. No androidx Room on purpose.
 * History is capped at MAX_HISTORY rows; oldest rows are pruned after every insert.
 */
class BrowserDb(ctx: Context, name: String? = DB_NAME) : SQLiteOpenHelper(ctx.applicationContext, name, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE bookmarks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "url TEXT NOT NULL UNIQUE, " +
                "created INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "url TEXT NOT NULL, " +
                "visited INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_history_visited ON history(visited)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 only. Future migrations go here; never drop user data.
    }

    // ---- bookmarks ----

    fun addBookmark(title: String, url: String): Boolean {
        val v = ContentValues().apply {
            put("title", title.ifBlank { url })
            put("url", url)
            put("created", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict("bookmarks", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun removeBookmark(url: String): Int = writableDatabase.delete("bookmarks", "url = ?", arrayOf(url))

    fun isBookmarked(url: String): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM bookmarks WHERE url = ? LIMIT 1", arrayOf(url)).use { c ->
            return c.moveToFirst()
        }
    }

    fun listBookmarks(): List<Bookmark> {
        val out = ArrayList<Bookmark>()
        readableDatabase.rawQuery("SELECT id, title, url, created FROM bookmarks ORDER BY created ASC", null).use { c ->
            while (c.moveToNext()) out.add(Bookmark(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
        return out
    }

    // ---- history ----

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("tvbrowser://") || url == "about:blank") return
        val db = writableDatabase
        db.beginTransaction()
        try {
            // collapse repeated reloads of the same URL within the last minute
            db.delete("history", "url = ? AND visited > ?", arrayOf(url, (System.currentTimeMillis() - 60_000L).toString()))
            val v = ContentValues().apply {
                put("title", title.ifBlank { url })
                put("url", url)
                put("visited", System.currentTimeMillis())
            }
            db.insert("history", null, v)
            pruneHistoryLocked(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun pruneHistoryLocked(db: SQLiteDatabase) {
        db.execSQL(
            "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY visited DESC, id DESC LIMIT $MAX_HISTORY)"
        )
    }

    fun pruneHistory() {
        pruneHistoryLocked(writableDatabase)
    }

    fun historyCount(): Long {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM history", null).use { c ->
            c.moveToFirst()
            return c.getLong(0)
        }
    }

    fun listHistory(limit: Int = 300): List<HistoryEntry> {
        val out = ArrayList<HistoryEntry>()
        readableDatabase.rawQuery(
            "SELECT id, title, url, visited FROM history ORDER BY visited DESC, id DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out.add(HistoryEntry(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
        return out
    }

    fun clearHistory(): Int = writableDatabase.delete("history", null, null)

    // ---- export ----

    fun exportJson(): String {
        val root = JSONObject()
        val b = JSONArray()
        for (x in listBookmarks()) b.put(JSONObject().put("title", x.title).put("url", x.url).put("created", x.created))
        val h = JSONArray()
        for (x in listHistory(MAX_HISTORY)) h.put(JSONObject().put("title", x.title).put("url", x.url).put("visited", x.visited))
        root.put("exportedAt", System.currentTimeMillis())
        root.put("bookmarks", b)
        root.put("history", h)
        return root.toString(2)
    }

    companion object {
        const val DB_NAME = "tvbrowser.db"
        const val DB_VERSION = 1
        const val MAX_HISTORY = 5000
    }
}
