# Runs JVM unit tests, boots the TV emulator, runs instrumented tests, writes reports under app\build\reports.
param([switch]$UnitOnly)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

& .\gradlew.bat --no-daemon testDebugUnitTest
if ($LASTEXITCODE -ne 0) { throw 'unit tests failed' }
if ($UnitOnly) { exit 0 }

$emulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$running = & $adb devices | Select-String -Pattern 'emulator-\d+\s+device'
if (-not $running) {
    Start-Process -FilePath $emulator -ArgumentList @('-avd', 'tv_1080p_api36', '-no-snapshot', '-no-boot-anim', '-gpu', 'swiftshader_indirect') -WindowStyle Minimized
    & $adb wait-for-device
    do {
        Start-Sleep -Seconds 3
        $booted = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
    } while ($booted -ne '1')
}
& $adb shell settings put global window_animation_scale 0
& $adb shell settings put global transition_animation_scale 0
& $adb shell settings put global animator_duration_scale 0

& .\gradlew.bat --no-daemon connectedDebugAndroidTest
$rc = $LASTEXITCODE
Write-Host "Reports: $repo\app\build\reports\tests\testDebugUnitTest\index.html and $repo\app\build\reports\androidTests\connected\debug\index.html"
if ($rc -ne 0) { throw 'instrumented tests failed' }
