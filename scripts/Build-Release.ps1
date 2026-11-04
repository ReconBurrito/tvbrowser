# Builds the signed release APK, verifies the signature, archives it under releases\ and keeps the last 3.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

if (-not (Test-Path (Join-Path $env:USERPROFILE 'tvbrowser-signing\keystore.properties'))) {
    throw 'keystore.properties missing. Run scripts\New-Keystore.ps1 first.'
}

& .\gradlew.bat --no-daemon clean assembleRelease
if ($LASTEXITCODE -ne 0) { throw 'assembleRelease failed' }

$apk = Join-Path $repo 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $apk)) { throw "APK not found at $apk (unsigned build means keystore.properties was not read)" }

$buildTools = Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Directory | Sort-Object Name -Descending | Select-Object -First 1
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
& $apksigner verify --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'apksigner verify failed' }

$version = (Select-String -Path 'app\build.gradle.kts' -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$releases = Join-Path $repo 'releases'
New-Item -ItemType Directory -Force -Path $releases | Out-Null
$out = Join-Path $releases "tvbrowser-$version-$stamp.apk"
Copy-Item $apk $out
(Get-FileHash $out -Algorithm SHA256).Hash | Set-Content "$out.sha256"

Get-ChildItem $releases -Filter '*.apk' | Sort-Object LastWriteTime -Descending | Select-Object -Skip 3 | ForEach-Object {
    Remove-Item $_.FullName -Force
    Remove-Item "$($_.FullName).sha256" -Force -ErrorAction SilentlyContinue
}
Write-Host "Release APK: $out"
