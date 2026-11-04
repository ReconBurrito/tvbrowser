package net.reconburrito.tvbrowser

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import net.reconburrito.tvbrowser.browser.PageScripts
import net.reconburrito.tvbrowser.browser.Tab
import net.reconburrito.tvbrowser.browser.TabManager
import net.reconburrito.tvbrowser.browser.TvWebChromeClient
import net.reconburrito.tvbrowser.browser.TvWebViewClient
import net.reconburrito.tvbrowser.data.BrowserDb
import net.reconburrito.tvbrowser.data.Prefs
import net.reconburrito.tvbrowser.data.SessionCodec
import net.reconburrito.tvbrowser.input.CursorPhysics
import net.reconburrito.tvbrowser.input.CursorView
import net.reconburrito.tvbrowser.ui.BookmarkGridAdapter
import net.reconburrito.tvbrowser.util.CrashLogger
import net.reconburrito.tvbrowser.util.UrlInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TabManager.ClientFactory, TvWebViewClient.Events, TvWebChromeClient.Events {

    private lateinit var root: FrameLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var homeContainer: FrameLayout
    private lateinit var homeGrid: GridView
    private lateinit var homeEmpty: TextView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var cursorView: CursorView
    private lateinit var toolbar: HorizontalScrollView
    private lateinit var urlInput: EditText
    private lateinit var progress: ProgressBar
    private lateinit var btnTabs: Button
    private lateinit var btnBookmark: Button
    private lateinit var btnUa: Button

    private lateinit var tabs: TabManager
    private lateinit var db: BrowserDb
    private lateinit var prefs: Prefs
    private lateinit var physics: CursorPhysics
    private val gridAdapter by lazy { BookmarkGridAdapter(this) }

    private val handler = Handler(Looper.getMainLooper())
    private val heldDirs = LinkedHashMap<Int, Long>()
    private var lastFrameTime = 0L
    private var moveLoopPosted = false

    private var selectLongHandled = false
    private var lastBackPress = 0L

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        webContainer = findViewById(R.id.web_container)
        homeContainer = findViewById(R.id.home_container)
        homeGrid = findViewById(R.id.home_grid)
        homeEmpty = findViewById(R.id.home_empty)
        fullscreenContainer = findViewById(R.id.fullscreen_container)
        cursorView = findViewById(R.id.cursor)
        toolbar = findViewById(R.id.toolbar)
        urlInput = findViewById(R.id.url_input)
        progress = findViewById(R.id.progress)
        btnTabs = findViewById(R.id.btn_tabs)
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnUa = findViewById(R.id.btn_ua)

        prefs = Prefs(this)
        db = BrowserDb(this)
        physics = CursorPhysics(1920, 1080)
        root.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            if (v.width > 0 && v.height > 0 && (physics.width != v.width || physics.height != v.height)) {
                physics.setBounds(v.width, v.height)
                physics.center()
                cursorView.setPosition(physics.x, physics.y)
            }
        }

        tabs = TabManager(this, webContainer, this)
        tabs.desktopUa = prefs.desktopUa

        setupToolbar()
        setupHome()

        val restored = SessionCodec.decode(prefs.sessionJson)
        if (restored != null) tabs.restore(restored) else tabs.newTab(UrlInput.HOME)
        handleViewIntent(intent)
        refreshUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
        refreshUi()
    }

    private fun handleViewIntent(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_VIEW) return
        val data = i.data?.toString() ?: return
        if (!UrlInput.isLoadableScheme(data)) return
        val tab = tabs.newTab(data) ?: tabs.active ?: return
        if (tab.url != data) tabs.load(tab, data)
    }

    override fun onStop() {
        super.onStop()
        saveSession()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tabs.destroyAll()
        db.close()
        super.onDestroy()
    }

    private fun saveSession() {
        prefs.sessionJson = SessionCodec.encode(tabs.snapshot())
    }

    // ------------------------------------------------------------------ setup

    private fun setupToolbar() {
        findViewById<Button>(R.id.btn_back).setOnClickListener { goBack() }
        findViewById<Button>(R.id.btn_forward).setOnClickListener { activeWebView()?.let { if (it.canGoForward()) it.goForward() } }
        findViewById<Button>(R.id.btn_reload).setOnClickListener { activeWebView()?.reload(); hideToolbar() }
        findViewById<Button>(R.id.btn_home).setOnClickListener { tabs.active?.let { goHome(it) }; hideToolbar() }
        btnTabs.setOnClickListener { showTabsDialog() }
        btnBookmark.setOnClickListener { toggleBookmark() }
        findViewById<Button>(R.id.btn_bookmarks).setOnClickListener { showBookmarksDialog() }
        findViewById<Button>(R.id.btn_history).setOnClickListener { showHistoryDialog() }
        btnUa.setOnClickListener { toggleUserAgent() }
        findViewById<Button>(R.id.btn_zoom_in).setOnClickListener { activeWebView()?.zoomIn() }
        findViewById<Button>(R.id.btn_zoom_out).setOnClickListener { activeWebView()?.zoomOut() }
        findViewById<Button>(R.id.btn_more).setOnClickListener { showMoreDialog() }

        urlInput.setOnEditorActionListener { _, actionId, event ->
            val enter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || enter) {
                navigateFromUrlBar()
                true
            } else false
        }
    }

    private fun setupHome() {
        homeGrid.adapter = gridAdapter
        homeGrid.setOnItemClickListener { _, _, position, _ ->
            val b = gridAdapter.getItem(position)
            loadInActive(b.url)
        }
        homeGrid.setOnItemLongClickListener { _, _, position, _ ->
            val b = gridAdapter.getItem(position)
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.action_delete_bookmark)
                .setMessage(b.title + "\n" + b.url)
                .setPositiveButton(R.string.yes) { _, _ -> db.removeBookmark(b.url); refreshUi() }
                .setNegativeButton(R.string.no, null)
                .show()
            true
        }
    }

    // ------------------------------------------------------------------ UI state

    fun refreshUi() {
        val tab = tabs.active
        val onHome = tab == null || tab.isHome
        if (onHome) {
            val list = db.listBookmarks()
            gridAdapter.submit(list)
            homeEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            homeContainer.visibility = View.VISIBLE
            cursorView.visibility = View.GONE
            if (!isToolbarVisible()) homeGrid.requestFocus()
        } else {
            homeContainer.visibility = View.GONE
            cursorView.visibility = View.VISIBLE
        }
        btnTabs.text = "Tabs ${tabs.count}/${tabs.registry.maxTabs}"
        val url = tab?.webView?.url ?: tab?.url
        btnBookmark.text = if (url != null && !UrlInput.isHome(url) && db.isBookmarked(url)) "Unstar" else getString(R.string.btn_bookmark)
        btnUa.text = if (prefs.desktopUa) "Desktop" else "Mobile"
        if (!urlInput.hasFocus()) urlInput.setText(UrlInput.displayText(url))
    }

    fun isToolbarVisible(): Boolean = toolbar.visibility == View.VISIBLE

    private fun showToolbar() {
        if (fullscreenView != null) return
        toolbar.visibility = View.VISIBLE
        urlInput.setText(UrlInput.displayText(tabs.active?.webView?.url ?: tabs.active?.url))
        urlInput.requestFocus()
        urlInput.selectAll()
    }

    private fun hideToolbar() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
        urlInput.clearFocus()
        toolbar.visibility = View.GONE
        if (tabs.active?.isHome == true) homeGrid.requestFocus() else activeWebView()?.requestFocus()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun activeWebView(): WebView? = tabs.active?.takeIf { !it.isHome }?.webView

    // ------------------------------------------------------------------ navigation

    private fun navigateFromUrlBar() {
        val target = UrlInput.toLoadable(urlInput.text.toString())
        hideToolbar()
        loadInActive(target)
    }

    fun loadInActive(url: String) {
        val tab = tabs.active ?: tabs.newTab(UrlInput.HOME) ?: return
        if (UrlInput.isHome(url)) goHome(tab) else tabs.load(tab, url)
        refreshUi()
    }

    private fun goHome(tab: Tab) {
        tabs.load(tab, UrlInput.HOME)
        refreshUi()
    }

    private fun goBack() {
        if (fullscreenView != null) { exitFullscreen(); return }
        if (isToolbarVisible()) { hideToolbar(); return }
        val tab = tabs.active
        val wv = tab?.webView
        when {
            tab == null -> promptExit()
            !tab.isHome && wv != null && wv.canGoBack() -> wv.goBack()
            !tab.isHome -> goHome(tab)
            else -> promptExit()
        }
    }

    private fun promptExit() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackPress < 2000L) {
            saveSession()
            finish()
        } else {
            lastBackPress = now
            toast(getString(R.string.press_back_again))
        }
    }

    private fun toggleBookmark() {
        val url = activeWebView()?.url ?: tabs.active?.url
        if (url == null || UrlInput.isHome(url)) return
        if (db.isBookmarked(url)) {
            db.removeBookmark(url)
            toast(getString(R.string.bookmark_removed))
        } else {
            db.addBookmark(activeWebView()?.title ?: tabs.active?.title ?: url, url)
            toast(getString(R.string.bookmark_added))
        }
        refreshUi()
    }

    private fun toggleUserAgent() {
        prefs.desktopUa = !prefs.desktopUa
        tabs.desktopUa = prefs.desktopUa
        activeWebView()?.reload()
        refreshUi()
        toast(getString(if (prefs.desktopUa) R.string.ua_desktop else R.string.ua_mobile))
    }

    // ------------------------------------------------------------------ dialogs

    private fun dialog(): AlertDialog.Builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)

    private fun showTabsDialog() {
        val list = tabs.registry.all()
        val labels = ArrayList<String>()
        list.forEachIndexed { i, t -> labels.add("${i + 1}. ${t.displayTitle}") }
        labels.add("+ " + getString(R.string.action_new_tab))
        labels.add("x " + getString(R.string.action_close_tab))
        dialog().setTitle(R.string.dialog_tabs)
            .setItems(labels.toTypedArray()) { _, which ->
                when {
                    which < list.size -> tabs.switchTo(which)
                    which == list.size -> if (tabs.newTab(UrlInput.HOME) == null) toast(getString(R.string.tab_limit))
                    else -> tabs.closeTab(tabs.registry.activeIndex)
                }
                hideToolbar()
                refreshUi()
            }.show()
    }

    private fun showBookmarksDialog() {
        val list = db.listBookmarks()
        val labels = list.map { it.title }.toTypedArray()
        dialog().setTitle(R.string.dialog_bookmarks)
            .setItems(labels) { _, which -> hideToolbar(); loadInActive(list[which].url) }
            .show()
    }

    private fun showHistoryDialog() {
        val list = db.listHistory(200)
        val labels = ArrayList<String>()
        labels.add("x " + getString(R.string.action_clear_history))
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        list.forEach { labels.add(fmt.format(Date(it.visited)) + "  " + it.title) }
        dialog().setTitle(R.string.dialog_history)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == 0) { db.clearHistory(); refreshUi(); return@setItems }
                hideToolbar()
                loadInActive(list[which - 1].url)
            }.show()
    }

    private fun showMoreDialog() {
        val items = arrayOf(
            getString(R.string.action_export),
            getString(R.string.action_show_crash_log),
            getString(R.string.action_clear_data),
        )
        dialog().setTitle(R.string.dialog_more)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> exportData()
                    1 -> showCrashLog()
                    2 -> clearSiteData()
                }
            }.show()
    }

    private fun showCrashLog() {
        val text = CrashLogger.read(this)
        dialog().setTitle(R.string.action_show_crash_log)
            .setMessage(text ?: getString(R.string.no_crash_log))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun clearSiteData() {
        dialog().setTitle(R.string.action_clear_data)
            .setPositiveButton(R.string.yes) { _, _ ->
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                tabs.registry.all().forEach { it.webView?.clearCache(true) }
                activeWebView()?.reload()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun exportData() {
        try {
            val name = "tvbrowser-export-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("insert returned null")
            contentResolver.openOutputStream(uri)?.use { it.write(db.exportJson().toByteArray(Charsets.UTF_8)) }
                ?: throw IllegalStateException("no output stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            toast(getString(R.string.exported_to, name))
        } catch (e: Exception) {
            CrashLogger.append(this, "export failed", e)
            toast(getString(R.string.export_failed))
        }
    }

    private fun showLinkContextMenu(href: String) {
        val wv = activeWebView() ?: return
        val isLink = href.isNotBlank()
        val url = if (isLink) href else (wv.url ?: return)
        val items = if (isLink) arrayOf(
            getString(R.string.ctx_open), getString(R.string.ctx_open_new_tab),
            getString(R.string.ctx_bookmark_link), getString(R.string.ctx_copy_link)
        ) else arrayOf(getString(R.string.ctx_open_new_tab), getString(R.string.ctx_bookmark_link), getString(R.string.ctx_copy_link))
        dialog().setTitle(url)
            .setItems(items) { _, which ->
                val action = if (isLink) which else which + 1
                when (action) {
                    0 -> loadInActive(url)
                    1 -> if (tabs.newTab(url) == null) toast(getString(R.string.tab_limit)) else refreshUi()
                    2 -> { db.addBookmark(url, url); toast(getString(R.string.bookmark_added)); refreshUi() }
                    3 -> {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("url", url))
                    }
                }
            }.show()
    }

    // ------------------------------------------------------------------ fullscreen video

    override fun onEnterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreenView != null) {
            callback.onCustomViewHidden()
            return
        }
        hideToolbar()
        fullscreenView = view
        fullscreenCallback = callback
        fullscreenContainer.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        fullscreenContainer.visibility = View.VISIBLE
        webContainer.visibility = View.INVISIBLE
        cursorView.visibility = View.VISIBLE
        cursorView.bringToFront()
    }

    override fun onExitFullscreen() {
        val v = fullscreenView ?: return
        fullscreenView = null
        fullscreenCallback = null
        fullscreenContainer.removeView(v)
        fullscreenContainer.visibility = View.GONE
        webContainer.visibility = View.VISIBLE
        refreshUi()
    }

    private fun exitFullscreen() {
        val cb = fullscreenCallback
        if (cb != null) cb.onCustomViewHidden() else onExitFullscreen()
    }

    private fun toggleVideo() {
        activeWebView()?.evaluateJavascript(PageScripts.TOGGLE_VIDEO, null)
    }

    // ------------------------------------------------------------------ key handling

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val down = event.action == KeyEvent.ACTION_DOWN
        val first = down && event.repeatCount == 0

        if (fullscreenView != null) {
            if (code == KeyEvent.KEYCODE_BACK) { if (first) exitFullscreen(); return true }
            if (isMediaKey(code)) { if (first) toggleVideo(); return true }
            if (handleCursorKey(event)) return true
            return super.dispatchKeyEvent(event)
        }

        if (isToolbarVisible()) {
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) { if (first) hideToolbar(); return true }
            if (code == KeyEvent.KEYCODE_DPAD_DOWN && down) {
                // Nothing lives below the toolbar row, so Down leaves it.
                hideToolbar()
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (homeContainer.visibility == View.VISIBLE) {
            if (code == KeyEvent.KEYCODE_MENU) { if (first) showToolbar(); return true }
            if (code == KeyEvent.KEYCODE_BACK) { if (first) goBack(); return true }
            return super.dispatchKeyEvent(event)
        }

        if (code == KeyEvent.KEYCODE_MENU) { if (first) showToolbar(); return true }
        if (code == KeyEvent.KEYCODE_BACK) { if (first) goBack(); return true }
        if (isMediaKey(code)) { if (first) toggleVideo(); return true }
        if (handleCursorKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun isMediaKey(code: Int) =
        code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == KeyEvent.KEYCODE_MEDIA_PLAY || code == KeyEvent.KEYCODE_MEDIA_PAUSE

    private fun directionOf(code: Int): Pair<Int, Int>? = when (code) {
        KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
        KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
        KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
        KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
        else -> null
    }

    private fun isSelectKey(code: Int) =
        code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
            code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

    /** Returns true when the event was consumed by the virtual cursor. */
    private fun handleCursorKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        val dir = directionOf(code)
        if (dir != null) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                    heldDirs[code] = event.eventTime
                    startMoveLoop()
                }
                KeyEvent.ACTION_UP -> {
                    val t = heldDirs.remove(code)
                    if (heldDirs.isEmpty()) stopMoveLoop()
                    if (t != null && event.eventTime - t < CursorPhysics.TAP_THRESHOLD_MS) snapOrNudge(dir.first, dir.second)
                }
            }
            return true
        }
        if (isSelectKey(code)) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                    selectLongHandled = false
                    cursorView.cursorPressed = true
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                }
                KeyEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    cursorView.cursorPressed = false
                    if (!selectLongHandled && !event.isCanceled) tapAtCursor()
                }
            }
            return true
        }
        return false
    }

    private val longPressRunnable = Runnable {
        selectLongHandled = true
        cursorView.cursorPressed = false
        val wv = activeWebView()
        if (wv == null || fullscreenView != null) return@Runnable
        val scale = wv.scale.takeIf { it > 0f } ?: 1f
        wv.evaluateJavascript(PageScripts.linkAt(physics.x / scale, physics.y / scale)) { r ->
            showLinkContextMenu(PageScripts.unquote(r))
        }
    }

    private val moveRunnable = object : Runnable {
        override fun run() {
            if (heldDirs.isEmpty()) { moveLoopPosted = false; return }
            val now = SystemClock.uptimeMillis()
            val dt = (now - lastFrameTime).coerceIn(1L, 50L)
            lastFrameTime = now
            var dx = 0
            var dy = 0
            var earliest = now
            for ((code, t) in heldDirs) {
                val d = directionOf(code) ?: continue
                dx += d.first
                dy += d.second
                if (t < earliest) earliest = t
            }
            dx = dx.coerceIn(-1, 1)
            dy = dy.coerceIn(-1, 1)
            val step = physics.step(dx, dy, now - earliest, dt)
            cursorView.setPosition(step.x, step.y)
            if (fullscreenView == null && (step.scrollX != 0 || step.scrollY != 0)) {
                activeWebView()?.scrollBy(step.scrollX, step.scrollY)
            }
            handler.postDelayed(this, CursorPhysics.FRAME_MS)
        }
    }

    private fun startMoveLoop() {
        if (moveLoopPosted) return
        moveLoopPosted = true
        lastFrameTime = SystemClock.uptimeMillis()
        handler.post(moveRunnable)
    }

    private fun stopMoveLoop() {
        handler.removeCallbacks(moveRunnable)
        moveLoopPosted = false
    }

    /** Short D-pad tap: jump to the nearest clickable element in that direction, else nudge. */
    private fun snapOrNudge(dx: Int, dy: Int) {
        val wv = activeWebView()
        if (wv == null || fullscreenView != null) { nudge(dx, dy); return }
        val scale = wv.scale.takeIf { it > 0f } ?: 1f
        wv.evaluateJavascript(PageScripts.snapTarget(physics.x / scale, physics.y / scale, dx, dy)) { r ->
            val s = PageScripts.unquote(r)
            val parts = s.split(',')
            val cx = parts.getOrNull(0)?.toFloatOrNull()
            val cy = parts.getOrNull(1)?.toFloatOrNull()
            if (cx != null && cy != null) {
                physics.moveTo(cx * scale, cy * scale)
                cursorView.setPosition(physics.x, physics.y)
            } else nudge(dx, dy)
        }
    }

    private fun nudge(dx: Int, dy: Int) {
        val before = physics.x to physics.y
        physics.moveTo(physics.x + dx * NUDGE_PX, physics.y + dy * NUDGE_PX)
        cursorView.setPosition(physics.x, physics.y)
        if (before == (physics.x to physics.y) && fullscreenView == null) {
            activeWebView()?.scrollBy(dx * NUDGE_SCROLL_PX, dy * NUDGE_SCROLL_PX)
        }
    }

    private fun tapAtCursor() {
        val target: View = fullscreenView ?: activeWebView() ?: return
        val x = physics.x
        val y = physics.y
        val downTime = SystemClock.uptimeMillis()
        dispatchTouch(target, MotionEvent.ACTION_DOWN, downTime, downTime, x, y)
        handler.postDelayed({
            dispatchTouch(target, MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), x, y)
        }, 60L)
    }

    private fun dispatchTouch(target: View, action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) {
        val ev = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        ev.source = InputDevice.SOURCE_TOUCHSCREEN
        target.dispatchTouchEvent(ev)
        ev.recycle()
    }

    // ------------------------------------------------------------------ TabManager.ClientFactory

    override fun attach(tab: Tab, webView: WebView) {
        webView.webViewClient = TvWebViewClient(tab, this)
        webView.webChromeClient = TvWebChromeClient(tab, this)
    }

    // ------------------------------------------------------------------ TvWebViewClient.Events

    override fun onPageStarted(tab: Tab, url: String) {
        if (tab === tabs.active) {
            progress.visibility = View.VISIBLE
            progress.progress = 5
            if (!urlInput.hasFocus()) urlInput.setText(UrlInput.displayText(url))
        }
    }

    override fun onPageFinished(tab: Tab, url: String, title: String) {
        db.addHistory(title, url)
        if (tab === tabs.active) {
            progress.visibility = View.GONE
            refreshUi()
        }
    }

    override fun onGoHome(tab: Tab) {
        goHome(tab)
    }

    override fun onSslBlocked(tab: Tab, url: String) {
        if (tab === tabs.active) toast(getString(R.string.ssl_error))
    }

    override fun onRendererGone(tab: Tab, crashed: Boolean): Boolean {
        CrashLogger.append(this, "WebView renderer gone (crashed=$crashed) url=${tab.url}")
        tabs.recover(tab)
        if (tab === tabs.active) {
            toast(getString(R.string.tab_recovered))
            refreshUi()
        }
        return true
    }

    // ------------------------------------------------------------------ TvWebChromeClient.Events

    override fun onProgress(tab: Tab, percent: Int) {
        if (tab !== tabs.active) return
        progress.progress = percent
        progress.visibility = if (percent in 1..99) View.VISIBLE else View.GONE
    }

    override fun onTitle(tab: Tab, title: String) {
        if (tab === tabs.active) refreshUi()
    }

    // ------------------------------------------------------------------ test hooks

    fun tabCount(): Int = tabs.count
    fun cursorPosition(): Pair<Float, Float> = physics.x to physics.y
    fun isOnHome(): Boolean = homeContainer.visibility == View.VISIBLE
    fun database(): BrowserDb = db
    fun setCursorForTest(x: Float, y: Float) {
        physics.moveTo(x, y)
        cursorView.setPosition(physics.x, physics.y)
    }

    /** Loads inline HTML into the active tab. Used by instrumented tests to stay off the network. */
    fun loadTestHtml(html: String, baseUrl: String = "https://test.local/page") {
        val tab = tabs.active ?: tabs.newTab(UrlInput.HOME) ?: return
        tab.url = baseUrl
        tab.pendingUrl = null
        val wv = tabs.ensureWebView(tab)
        tabs.refreshVisibility()
        wv.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", baseUrl)
        refreshUi()
    }

    companion object {
        const val NUDGE_PX = 48f
        const val NUDGE_SCROLL_PX = 160
    }
}
