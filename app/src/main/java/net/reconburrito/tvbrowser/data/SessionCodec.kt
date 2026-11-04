package net.reconburrito.tvbrowser.data

import org.json.JSONArray
import org.json.JSONObject

data class SavedTab(val url: String, val title: String)
data class SavedSession(val tabs: List<SavedTab>, val activeIndex: Int)

/** JSON (de)serialisation of the open-tab session. */
object SessionCodec {
    fun encode(s: SavedSession): String {
        val arr = JSONArray()
        for (t in s.tabs) arr.put(JSONObject().put("url", t.url).put("title", t.title))
        return JSONObject().put("tabs", arr).put("active", s.activeIndex).toString()
    }

    fun decode(json: String?): SavedSession? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            val arr = o.getJSONArray("tabs")
            val tabs = ArrayList<SavedTab>()
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                tabs.add(SavedTab(t.optString("url", ""), t.optString("title", "")))
            }
            if (tabs.isEmpty()) return null
            val active = o.optInt("active", 0).coerceIn(0, tabs.size - 1)
            SavedSession(tabs, active)
        } catch (e: Exception) {
            null
        }
    }
}
