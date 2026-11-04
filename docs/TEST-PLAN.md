# Test plan

Run everything with scripts\Invoke-Tests.ps1 unless a row names a different command. Reports: app\build\reports\tests\testDebugUnitTest\index.html and app\build\reports\androidTests\connected\debug\index.html. A requirement passes only when every row mapped to it passes.

## A. Requirement to test map
REQ-01 D1 (install and launch on Shield), AppFlowTest.launchesOnHomeWithToolbarHidden (emulator)
REQ-02 AppFlowTest.loadsPageAndRecordsHistory, D2 (YouTube, news site), D6 (Keycloak login keeps cookies)
REQ-03 AppFlowTest.menuShowsToolbarAndBackHidesIt, D3
REQ-04 AppFlowTest.launchesOnHomeWithToolbarHidden
REQ-05 CursorPhysicsTest (all), AppFlowTest.dpadMovesCursorAndSelectClicks, D4 (acceleration and edge scroll by feel)
REQ-06 AppFlowTest.dpadMovesCursorAndSelectClicks, D5 (hold Select on a link opens menu)
REQ-07 BrowserDbTest.historyIsCappedAt5000, BrowserDbTest.historyIgnoresHomeAndBlank, BrowserDbTest.clearHistoryEmptiesTable, AppFlowTest.loadsPageAndRecordsHistory
REQ-08 BrowserDbTest.bookmarksAreUniqueByUrl, AppFlowTest.bookmarkRoundTrip, D7
REQ-09 AppFlowTest.launchesOnHomeWithToolbarHidden, D7
REQ-10 TabRegistryTest (all), SessionCodecTest (all), AppFlowTest.restoresSavedSession, D8
REQ-11 AppFlowTest.rendererCrashRecoversAndDoesNotLoop
REQ-12 AppFlowTest.rendererCrashRecoversAndDoesNotLoop (log written), D9 (More > Show crash log)
REQ-13 PageScriptsTest.scriptsInterpolateCoordinates, AppFlowTest.dpadMovesCursorAndSelectClicks, D4 (short taps hop between links)
REQ-14 D10
REQ-15 D11 (fullscreen video, Back exits, Play/Pause toggles)
REQ-16 UrlInputTest (all), D3
REQ-17 UserAgentsTest (all), D12
REQ-18 D13
REQ-19 ClientPolicyTest.sslErrorsAreCancelled, ClientPolicyTest.geolocationAndMediaPermissionsDenied, ClientPolicyTest.popupsRefused, S3, S4
REQ-20 UrlInputTest.dangerousSchemesBecomeSearch, UrlInputTest.loadableSchemeGate, ClientPolicyTest.blocksNonWebSchemes, S2
REQ-21 S1 (manifest permissions), S5 (no listening sockets)
REQ-22 BrowserDbTest.exportContainsBothTables, D14
REQ-23 D15
REQ-24 S6, S7
REQ-25 S8

## B. Unit and integration tests
Unit (JVM): UrlInputTest, CursorPhysicsTest, TabRegistryTest, SessionCodecTest, UserAgentsTest, PageScriptsTest.
Integration (emulator): BrowserDbTest, ClientPolicyTest, AppFlowTest.

## C. End-to-end smoke (emulator, automated)
AppFlowTest in order: launch, menu/back, load page + history, cursor + click, session restore, renderer crash, bookmark. Command: scripts\Invoke-Tests.ps1

## D. On-device checklist (Shield TV Pro 2019, manual, each row pass/fail)
D1 Install from USB, launch from home row; start page shows with the empty-state text; no toolbar visible.
D2 Menu, type youtube.com, Go. Page loads with the desktop layout. Repeat for a news site (e.g. apnews.com) and confirm text is readable at couch distance.
D3 Menu shows the toolbar with the address bar focused; Select opens Gboard; Back closes Gboard; Back again hides the toolbar; Down from the toolbar hides it.
D4 Hold Right for 2 s: cursor accelerates and reaches the edge; keep holding: page scrolls right or stops cleanly at the page edge. Hold Down at the bottom edge: page scrolls, faster the longer held. Tap Down repeatedly: cursor hops to the next link each tap.
D5 Place cursor on a link, hold Select: context menu with Open, Open in new tab, Bookmark link, Copy link. Choose Open in new tab; Tabs button shows 2/10.
D6 Open seerr.reconburrito.net, sign in through auth.reconburrito.net, confirm redirect back to Seerr signed in; close app (Back twice), relaunch, open Seerr again, still signed in.
D7 On a page press Star; toast Bookmark added; Home; the bookmark tile is on the grid; Select on it opens the page; hold Select on it prompts to delete; delete it.
D8 Open 3 tabs, close the app with Back twice, relaunch: Tabs shows 3/10, the active tab is the one that was active, switching to another tab loads it on demand.
D9 Type chrome://crash and Go: toast Page crashed and was reloaded, tab returns to the start page; More > Show crash log contains "renderer gone".
D10 On a page navigated two links deep, Back twice returns through history, third Back goes to the start page, Back on the start page shows the exit toast, Back again exits.
D11 Open a YouTube video, click fullscreen in the player: fullscreen fills the TV; Play/Pause on the remote pauses and resumes; cursor can click the player controls; Back leaves fullscreen.
D12 UA button switches to Mobile and reloads with the mobile layout; again returns to Desktop.
D13 Zoom+ twice enlarges the page; Zoom- twice returns; no zoom buttons are drawn on screen.
D14 More > Export: toast names the file; open the Shield file manager, Downloads contains the JSON.
D15 More > Clear cookies and site data, confirm; reopen Seerr: signed out.
D16 Tab limit: open tabs until Tabs shows 10/10; New tab shows the limit toast.
D17 Memory: with 10 tabs open for 10 minutes the Shield stays responsive and the app is not killed when switching to another app and back.

## E. Negative and failure-injection tests
N1 Type javascript:alert(1): becomes a DuckDuckGo search (UrlInputTest.dangerousSchemesBecomeSearch).
N2 A page link to intent://... or market://... does nothing (ClientPolicyTest.blocksNonWebSchemes; manual: https://example.com page with such a link).
N3 https://self-signed.badssl.com and https://expired.badssl.com show the certificate toast and do not render (manual, Shield).
N4 Renderer crash loop: chrome://crash returns to home, no infinite reload (AppFlowTest.rendererCrashRecoversAndDoesNotLoop).
N5 Corrupt session: write {"tabs":"x"} into the tvbrowser prefs session key, relaunch; app starts on a single home tab (SessionCodecTest.garbageIsNull covers the codec; manual on emulator via adb shell).
N6 Network down: unplug Ethernet on the Shield, load a page; WebView error page appears, Back returns, app does not crash (manual).
N7 Disk full for export: unlikely on Shield; covered by the try/catch that toasts Export failed and logs the exception (code review item).
N8 Window resize: on the emulator rotate or change density; cursor stays within bounds (CursorPhysicsTest.setBoundsKeepsCursorInside).
N9 Key spam: hold all four D-pad keys at once; cursor stands still or moves diagonally, never throws (manual, emulator; moveRunnable clamps dx, dy).
N10 Uncaught exception injection: on the emulator use adb shell am crash net.reconburrito.tvbrowser.debug and confirm crash.log gets an entry and the app restores the session on relaunch.

## F. Security checks (pass/fail, exact commands, run in PowerShell from the repo root after a build)
S1 Manifest permissions are exactly INTERNET and ACCESS_NETWORK_STATE:
    & (Get-ChildItem "$env:ANDROID_HOME\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName\aapt.exe dump permissions app\build\outputs\apk\release\app-release.apk
S2 No non-web scheme reaches the WebView: .\gradlew.bat connectedDebugAndroidTest --tests net.reconburrito.tvbrowser.ClientPolicyTest
S3 Permission and geolocation prompts are denied: same command as S2 (geolocationAndMediaPermissionsDenied).
S4 Certificate errors cancel: same command as S2 (sslErrorsAreCancelled).
S5 No listening sockets while running on the emulator: & "$env:ANDROID_HOME\platform-tools\adb.exe" shell "cat /proc/net/tcp /proc/net/tcp6 | grep -i ' 0A '" must list nothing owned by the app's uid (get the uid with adb shell dumpsys package net.reconburrito.tvbrowser.debug | findstr userId).
S6 No secrets in the repo: git grep -n -i -E "storePassword|keyPassword|BEGIN (RSA|PRIVATE)" must return nothing; git ls-files | findstr /i "jks keystore.properties" must return nothing.
S7 Release APK is signed with the release key and v2/v3 schemes: & "<build-tools>\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk shows Verified using v2 scheme: true and the CN=TV Browser certificate.
S8 No third-party runtime dependencies: .\gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath shows only org.jetbrains.kotlin and org.jetbrains annotations.
S9 Cleartext is confined to intent: review AndroidManifest.xml usesCleartextTraffic=true is deliberate for LAN http; no network_security_config widening beyond that.
S10 WebView file and content access are off: review TabManager.createWebView sets allowFileAccess=false and allowContentAccess=false; ClientPolicyTest.blocksNonWebSchemes blocks file: URLs.

## G. Pass/fail checklist with commands
G1 Unit tests: .\gradlew.bat --no-daemon testDebugUnitTest  (pass = BUILD SUCCESSFUL and report shows 0 failures)
G2 Instrumented tests: .\scripts\Invoke-Tests.ps1  (pass = BUILD SUCCESSFUL and connected report shows 0 failures)
G3 Release build and signature: .\scripts\Build-Release.ps1  (pass = prints Release APK path and apksigner output)
G4 Permissions: S1 command  (pass = exactly two uses-permission lines)
G5 Secrets: S6 commands  (pass = no output)
G6 Dependencies: S8 command  (pass = only org.jetbrains entries)
G7 Sockets: S5 command  (pass = no app-owned LISTEN entries)
G8 On-device: D1 to D17 all pass on the Shield.
G9 Negative: N1 to N10 all behave as described.
