# Architecture

## Components
MainActivity (net.reconburrito.tvbrowser) is the single Activity. It owns the view tree, routes every remote key in dispatchKeyEvent, runs the cursor movement loop on the main Handler, synthesises touch events, hosts fullscreen video, shows dialogs and persists the session.

TabManager (browser) owns the WebViews. It wraps TabRegistry (pure, cap of 10, active index), creates WebViews lazily, applies WebSettings and the user agent, keeps exactly one WebView visible, destroys WebViews on close, snapshots and restores the session, and rebuilds a WebView after a renderer crash with a crash-loop guard.

TvWebViewClient and TvWebChromeClient (browser) hold the security policy: scheme allow-list, SSL cancel, popup refusal, permission denial, geolocation denial, fullscreen hand-off, progress and title relay. They talk to the Activity through two small Events interfaces so they are testable without the Activity.

PageScripts (browser) are the three JavaScript snippets evaluated in the page: nearest-clickable in a direction (snap), link-under-point (context menu), and video play/pause.

CursorPhysics (input) is the pure movement model (acceleration, clamping, edge overflow to scroll). CursorView (input) draws the arrow and nothing else.

BrowserDb (data) is a SQLiteOpenHelper with bookmarks and history tables, history pruning inside the insert transaction, and JSON export. Prefs (data) is SharedPreferences for the UA choice and the session JSON. SessionCodec (data) is the JSON codec.

UrlInput and UserAgents (util) are pure string logic. CrashLogger (util) is the uncaught-exception hook and append-only capped log. TvBrowserApp installs it.

## Data flow
Remote key -> MainActivity.dispatchKeyEvent -> one of four modes: fullscreen, toolbar focused, home grid focused, cursor. In cursor mode D-pad keys feed heldDirs; the 16 ms move loop asks CursorPhysics for the next position and either moves CursorView or calls WebView.scrollBy with the overflow. A D-pad release under 160 ms evaluates PageScripts.snapTarget in the WebView; the result (CSS px) is multiplied by WebView.scale and becomes the new cursor position. Select down starts the long-press timer and Select up without long-press dispatches ACTION_DOWN then ACTION_UP MotionEvents at the cursor to the WebView (or the fullscreen video view). Long-press evaluates PageScripts.linkAt and opens the context dialog.

Address bar text -> UrlInput.toLoadable -> TabManager.load -> WebView.loadUrl. WebView callbacks -> TvWebViewClient -> MainActivity.onPageFinished -> BrowserDb.addHistory (prunes to 5000 in the same transaction) -> refreshUi.

Session: MainActivity.onStop -> TabManager.snapshot -> SessionCodec.encode -> Prefs. onCreate reverses it; only the active tab's WebView is created, other tabs keep pendingUrl until switched to.

Crash: WebView renderer dies -> onRenderProcessGone -> TabManager.recover (destroy, recreate, reload once or go home) -> CrashLogger.append. Java exception -> CrashLogger handler -> crash.log -> default handler (process ends, Android relaunches on next open, session restores).

## Storage
/data/data/net.reconburrito.tvbrowser/databases/tvbrowser.db (bookmarks, history)
/data/data/net.reconburrito.tvbrowser/shared_prefs/tvbrowser.xml (desktop_ua, session)
/sdcard/Android/data/net.reconburrito.tvbrowser/files/crash.log
WebView cookies, cache and web storage in the app's app_webview directory (managed by WebView).
Exports land in the shared Downloads collection via MediaStore.

## Network and ports
Outbound only: WebView page loads over http/https to whatever the user types, plus DuckDuckGo for searches. No listening ports, no background services, no telemetry, no update checks. Permissions: INTERNET, ACCESS_NETWORK_STATE. Cleartext is allowed because lab services on LAN IPs are http.

## Build and signing
Gradle 8.13, AGP 8.11.1, Kotlin 2.1.20, compileSdk 35, minSdk 30, targetSdk 35, JDK 17. No androidx runtime libraries. Release signing config is read from %USERPROFILE%\tvbrowser-signing\keystore.properties; when the file is absent the release build is unsigned and Build-Release.ps1 refuses to proceed. Debug builds use applicationId suffix .debug so both can coexist on the emulator.

## Test topology
JVM unit tests (app/src/test) cover the pure classes. Instrumented tests (app/src/androidTest) run on the tv_1080p_api36 emulator against the debug APK and exercise the Activity through its public test hooks (loadTestHtml, setCursorForTest, cursorPosition, isOnHome, isToolbarVisible, tabCount, database, loadInActive). Nothing in the test packages touches the network; pages are inline HTML loaded with a https://test.local base URL. The Shield itself is tested manually with the on-device checklist in TEST-PLAN.md.
