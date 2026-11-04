# Installs JDK 17, Android Studio, Android command-line tools, platform-tools, SDK 35, build-tools,
# the emulator and the Android TV system image. Run as the current user from an elevated PowerShell.
# Idempotent: re-running only installs what is missing.
$ErrorActionPreference = 'Stop'

$SdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
# Check https://developer.android.com/studio#command-line-tools-only for the current build number if this 404s.
$CmdlineToolsUrl = 'https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip'

winget install --id Microsoft.OpenJDK.17 --exact --silent --accept-package-agreements --accept-source-agreements
winget install --id Google.AndroidStudio --exact --silent --accept-package-agreements --accept-source-agreements

$jdk = Get-ChildItem 'C:\Program Files\Microsoft\jdk-17*' -Directory | Select-Object -First 1
if (-not $jdk) { throw 'JDK 17 not found under C:\Program Files\Microsoft' }
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk.FullName, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $SdkRoot, 'User')
$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = $SdkRoot

New-Item -ItemType Directory -Force -Path (Join-Path $SdkRoot 'cmdline-tools') | Out-Null
$latest = Join-Path $SdkRoot 'cmdline-tools\latest'
if (-not (Test-Path (Join-Path $latest 'bin\sdkmanager.bat'))) {
    $zip = Join-Path $env:TEMP 'cmdline-tools.zip'
    Invoke-WebRequest -Uri $CmdlineToolsUrl -OutFile $zip
    $tmp = Join-Path $env:TEMP 'cmdline-tools-extract'
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp
    Move-Item -Path (Join-Path $tmp 'cmdline-tools') -Destination $latest
}

$sdkmanager = Join-Path $latest 'bin\sdkmanager.bat'
$packages = @(
    'platform-tools',
    'platforms;android-35',
    'build-tools;35.0.0',
    'emulator',
    'system-images;android-36;android-tv;x86_64'
)
# Accept licenses non-interactively, then install.
$yes = ('y' + [Environment]::NewLine) * 20
$yes | & $sdkmanager --sdk_root="$SdkRoot" --licenses
& $sdkmanager --sdk_root="$SdkRoot" @packages

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
foreach ($p in @((Join-Path $SdkRoot 'platform-tools'), (Join-Path $SdkRoot 'emulator'), (Join-Path $latest 'bin'), (Join-Path $jdk.FullName 'bin'))) {
    if ($userPath -notlike "*$p*") { $userPath = "$userPath;$p" }
}
[Environment]::SetEnvironmentVariable('Path', $userPath, 'User')
Write-Host 'Toolchain installed. Open a new PowerShell window so PATH, JAVA_HOME and ANDROID_HOME take effect.'
