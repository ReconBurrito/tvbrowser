# Creates the release keystore and keystore.properties OUTSIDE the repo at %USERPROFILE%\tvbrowser-signing.
# Losing this keystore means future builds cannot update the installed app; back the folder up.
$ErrorActionPreference = 'Stop'
$dir = Join-Path $env:USERPROFILE 'tvbrowser-signing'
$ks = Join-Path $dir 'tvbrowser-release.jks'
$props = Join-Path $dir 'keystore.properties'
if (Test-Path $ks) { throw "Keystore already exists at $ks. Refusing to overwrite." }
New-Item -ItemType Directory -Force -Path $dir | Out-Null

$storePass = Read-Host -AsSecureString 'Keystore password (min 6 chars)'
$keyPass = Read-Host -AsSecureString 'Key password (min 6 chars)'
$sp = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePass))
$kp = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPass))

$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
& $keytool -genkeypair -v -keystore $ks -alias tvbrowser -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $sp -keypass $kp -dname 'CN=TV Browser, OU=Home Lab, O=reconburrito.net, L=Santa Clara, S=CA, C=US'
if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }

$ksForward = $ks -replace '\\', '/'
@(
    "storeFile=$ksForward",
    "storePassword=$sp",
    "keyAlias=tvbrowser",
    "keyPassword=$kp"
) | Set-Content -Path $props -Encoding ascii

# Lock the folder down to the current user only.
icacls $dir /inheritance:r /grant:r "${env:USERNAME}:(OI)(CI)F" | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'icacls failed; keystore folder is NOT locked down' }
Write-Host "Keystore and keystore.properties written to $dir (not in the repo, ACL restricted to $env:USERNAME)."
