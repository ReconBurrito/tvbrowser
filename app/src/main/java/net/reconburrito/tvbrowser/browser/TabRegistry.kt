package net.reconburrito.tvbrowser.browser

/**
 * Ordered set of tabs with an active index and a hard cap. Generic and pure so the
 * tab-limit and switching rules are unit-testable without a WebView.
 */
class TabRegistry<T>(val maxTabs: Int = MAX_TABS) {
    private val items = ArrayList<T>()
    var activeIndex: Int = -1
        private set

    val size: Int get() = items.size
    val isFull: Boolean get() = items.size >= maxTabs
    val active: T? get() = items.getOrNull(activeIndex)
    fun all(): List<T> = items.toList()
    fun indexOf(t: T): Int = items.indexOf(t)
    fun get(i: Int): T = items[i]

    /** Adds and activates. Returns false when the cap is reached. */
    fun add(t: T, activate: Boolean = true): Boolean {
        if (isFull) return false
        items.add(t)
        if (activate || activeIndex < 0) activeIndex = items.size - 1
        return true
    }

    fun activate(i: Int): T? {
        if (i !in items.indices) return null
        activeIndex = i
        return items[i]
    }

    /** Removes and returns the removed item; active index moves to the previous tab. */
    fun remove(i: Int): T? {
        if (i !in items.indices) return null
        val removed = items.removeAt(i)
        activeIndex = when {
            items.isEmpty() -> -1
            i <= activeIndex -> (activeIndex - 1).coerceAtLeast(0)
            else -> activeIndex
        }
        return removed
    }

    fun replace(i: Int, t: T) {
        items[i] = t
    }

    companion object {
        const val MAX_TABS = 10
    }
}
