# Creates the Android TV 1080p emulator used for instrumented tests. Safe to re-run.
$ErrorActionPreference = 'Stop'
$avdmanager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\avdmanager.bat'
$name = 'tv_1080p_api36'
$existing = & $avdmanager list avd 2>$null | Select-String -Pattern "Name: $name"
if ($existing) { Write-Host "AVD $name already exists"; exit 0 }
'no' | & $avdmanager create avd --name $name --package 'system-images;android-36;android-tv;x86_64' --device 'tv_1080p' --force
if ($LASTEXITCODE -ne 0) { throw 'avdmanager create failed' }
$cfg = Join-Path $env:USERPROFILE ".android\avd\$name.avd\config.ini"
Add-Content $cfg 'hw.ramSize=2048'
Add-Content $cfg 'hw.keyboard=yes'
Add-Content $cfg 'hw.dPad=yes'
Add-Content $cfg 'disk.dataPartition.size=4G'
Write-Host "AVD $name created"
