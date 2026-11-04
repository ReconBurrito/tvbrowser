# TV Browser for NVIDIA Shield TV Pro 2019 - Specification v1.0

## Goal
A sideloaded Android TV app (APK) that lets a household browse the web on a Shield TV Pro 2019 using only the 2019 remote, with a virtual cursor that feels natural: accelerating movement, short-tap link snapping, edge scrolling, Select to click, hold Select for a link menu, Menu for the address bar, Back for page history and exit.

## Requirements
REQ-01 The app installs from a USB-delivered APK on Android TV 11 (API 30) and launches from the Shield home screen with a banner.
REQ-02 The app renders web pages with the system Android WebView; JavaScript, DOM storage, first- and third-party cookies are enabled.
REQ-03 The Menu button toggles a toolbar containing the address bar and buttons for Back, Forward, Reload, Home, Tabs, Star, Bookmarks, History, UA toggle, Zoom+, Zoom-, More. Back or Menu while the toolbar is visible hides it; Down from the toolbar hides it.
REQ-04 The toolbar is hidden by default; browsing takes the full screen.
REQ-05 D-pad moves a visible cursor. Movement accelerates from 450 px/s to 2600 px/s while held. When the cursor is against a screen edge, further D-pad input scrolls the page in that direction at the cursor speed.
REQ-06 Select clicks at the cursor by synthesising a touch tap. Holding Select for the system long-press timeout opens a context menu for the link under the cursor (Open, Open in new tab, Bookmark link, Copy link) or for the page (Open in new tab, Bookmark page, Copy URL).
REQ-07 Every finished top-level page load is recorded in history (title, URL, time). History is capped at 5000 entries; older entries are pruned automatically. History can be cleared.
REQ-08 Pages can be bookmarked and unbookmarked from the toolbar Star; bookmarks are unique by URL and appear as a grid on the start page.
REQ-09 The start page (tvbrowser://home) is native: a D-pad-navigable grid of bookmarks, long-press deletes a bookmark. An empty state explains how to add one.
REQ-10 Up to 10 tabs. A tab switcher lists tabs, creates one, closes the current one. Open tabs and the active tab index are saved when the app stops and restored on launch. Inactive tabs do not load until switched to.
REQ-11 A WebView renderer crash replaces that tab's WebView and reloads the page once; a second crash within 15 s, or a chrome:// URL, sends the tab home instead. The event is written to the crash log.
REQ-12 Uncaught exceptions are appended to crash.log (external files dir, 200 KB cap) and shown in-app via More > Show crash log.
REQ-13 A short D-pad tap (under 160 ms) snaps the cursor to the nearest clickable element in that direction within the viewport; if none, the cursor nudges 48 px, or scrolls 160 px when already at the edge.
REQ-14 Back goes back in the tab's page history; if none, it returns to the start page; on the start page a second Back within 2 s exits.
REQ-15 Fullscreen video from the page is supported (onShowCustomView). Back exits fullscreen; Play/Pause on the remote toggles the first video element; the cursor still works over the video.
REQ-16 The address bar accepts URLs, host names, LAN IPs and search terms. Bare hosts get https, LAN IPs and localhost get http, anything else becomes a DuckDuckGo search. Text entry uses the system TV keyboard; Go submits.
REQ-17 The user agent defaults to a desktop UA derived from the WebView default (Chrome version preserved). The toolbar UA button toggles desktop/mobile and reloads.
REQ-18 Zoom+ and Zoom- change the WebView zoom; zoom controls are never drawn on screen.
REQ-19 Popups and new windows are refused. Camera, microphone, MIDI, protected-media and geolocation permission prompts are denied without asking. Certificate errors cancel the load and show a toast; there is no "proceed anyway".
REQ-20 Only http, https, about and chrome URLs may load. javascript:, file:, content:, intent:, market: and every other scheme are dropped; the app never hands a URL to another app.
REQ-21 The app declares INTERNET and ACCESS_NETWORK_STATE only. It opens no listening sockets and makes no network requests other than page loads.
REQ-22 More > Export writes bookmarks and history as JSON into the Shield's Downloads collection without any runtime permission.
REQ-23 More > Clear cookies and site data removes all cookies, web storage and cache after confirmation.
REQ-24 Release builds are signed with a local keystore whose credentials live in %USERPROFILE%\tvbrowser-signing\keystore.properties, outside the repo and gitignored. Unsigned release builds fail the release script.
REQ-25 The APK has no third-party runtime dependencies (Kotlin stdlib only); test dependencies are junit, androidx.test and org.json.

## Non-goals
Downloads, ad blocking, bookmark sync or import, extensions, password saving, per-site permission prompts, voice search, custom on-screen keyboard, Leanback UI, Play Store distribution, support for the 2015/2017 Shield remote or Android 9, touch devices, landscape/portrait handling, multi-user.

## Assumptions
The Shield TV Pro 2019 runs Shield Experience 9.x (Android TV 11, API 30) with the Google Play-updated Android System WebView.
The Windows PC has no Android tooling yet; JDK 17, Android Studio and the command-line tools are installed by the provided script.
Gboard TV is the installed IME and handles text entry with the remote.
The Shield's file manager already has the Unknown sources toggle for installing APKs; enabling it is a one-time Shield setting change and is called out in the deploy procedure for approval.
The emulator image system-images;android-36;android-tv;x86_64 is available from sdkmanager; the Shield itself is API 30, so instrumented tests verify behaviour on API 36 and the on-device checklist verifies API 30.
Shield developer options and network ADB stay off; crash logs are read in-app, not pulled.

## Open risks
WebView.getScale() is deprecated; if a future WebView returns 0 the code falls back to scale 1 and snapping will mis-target on zoomed pages.
Synthesised touch events are a documented-but-unofficial way to click in WebView; some pages with pointer-event detection may treat the tap as a mouse-less touch. TV Bro uses the same technique.
The system long-press timeout (ViewConfiguration) is 400 ms on stock Android TV; a slow Select release can turn a click into a context menu.
Gboard TV captures D-pad while the keyboard is open, so toolbar key handling is unreachable until the keyboard is dismissed; that is intended but may surprise a first-time user.
Session restore stores URLs only, not scroll position or form state.
The commandlinetools zip build number in Install-Toolchain.ps1 changes a few times a year; the script states where to look it up if the URL 404s.
Compile verification in the authoring sandbox was not possible (Google and Maven repositories are blocked there); the first gradlew build on the Windows PC is the compile gate and is the first task in TASKS.md.
