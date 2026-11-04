# TV Browser

Remote-friendly web browser for the NVIDIA Shield TV Pro 2019. Android WebView, virtual cursor with acceleration and link snapping, tabs, bookmarks, history, fullscreen video, desktop/mobile UA toggle. No third-party runtime dependencies, no accounts, no telemetry, no listening ports.

Docs: docs/SPEC.md, docs/ARCHITECTURE.md, docs/TASKS.md, docs/TEST-PLAN.md, docs/DEPLOY-ROLLBACK.md, docs/FABLE-REVIEWER-PROMPT.md.

Build on Windows: scripts\Install-Toolchain.ps1, then gradlew.bat assembleDebug. Release: scripts\New-Keystore.ps1 once, then scripts\Build-Release.ps1. Tests: scripts\New-TvAvd.ps1 once, then scripts\Invoke-Tests.ps1.

Remote mapping: D-pad moves the cursor (hold to accelerate, tap to hop to the next link, hold at an edge to scroll). Select clicks, hold Select for the link menu. Menu toggles the address bar. Back goes back, then home, then exits on a second press. Play/Pause toggles video.
