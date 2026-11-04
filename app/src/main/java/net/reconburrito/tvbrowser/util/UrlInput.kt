package net.reconburrito.tvbrowser.util

/**
 * Turns whatever the user typed in the address bar into something safe to load.
 * Pure Kotlin so it is unit-testable on the JVM.
 */
object UrlInput {
    const val HOME = "tvbrowser://home"
    const val SEARCH_BASE = "https://duckduckgo.com/?q="

    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*:")
    private val allowedSchemes = setOf("http", "https", "about", "chrome")
    private val hostLike = Regex("^[A-Za-z0-9][A-Za-z0-9\\-]*(\\.[A-Za-z0-9\\-]+)+(:\\d{1,5})?(/.*)?$")
    private val ipLike = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d{1,5})?(/.*)?$")
    private val localhostLike = Regex("^localhost(:\\d{1,5})?(/.*)?$", RegexOption.IGNORE_CASE)
    private val portTail = Regex("^\\d{1,5}(/.*)?$")

    /** A "scheme:" prefix followed only by a port number (host:8080/...) is not a scheme. */
    private fun schemeOf(t: String): String? {
        val m = schemeRegex.find(t) ?: return null
        val rest = t.substring(m.range.last + 1)
        if (portTail.matches(rest)) return null
        return m.value.dropLast(1).lowercase()
    }

    fun isHome(url: String?): Boolean = url == null || url.isEmpty() || url == HOME

    /** Schemes the WebView is allowed to navigate to. Everything else is dropped. */
    fun isLoadableScheme(url: String): Boolean {
        val scheme = schemeOf(url) ?: return false
        return scheme in allowedSchemes
    }

    fun toLoadable(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty() || t == HOME) return HOME
        val scheme = schemeOf(t)
        if (scheme != null) {
            return if (scheme in allowedSchemes) t else search(t)
        }
        if (t.contains(' ')) return search(t)
        if (localhostLike.matches(t) || ipLike.matches(t)) return "http://$t"
        if (hostLike.matches(t)) return "https://$t"
        return search(t)
    }

    fun search(terms: String): String = SEARCH_BASE + encode(terms)

    private fun encode(s: String): String {
        val sb = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            val ch = c.toChar()
            if (ch.isLetterOrDigit() && c < 128 || ch == '-' || ch == '_' || ch == '.' || ch == '~') sb.append(ch)
            else if (ch == ' ') sb.append('+')
            else sb.append('%').append(String.format("%02X", c))
        }
        return sb.toString()
    }

    /** What to show in the address bar for a given tab URL. */
    fun displayText(url: String?): String = if (isHome(url)) "" else url ?: ""
}
