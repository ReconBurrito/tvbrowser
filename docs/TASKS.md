# Task list (dependencies in brackets)

T01 Install toolchain on the Windows PC with scripts\Install-Toolchain.ps1, open a new PowerShell, confirm java -version shows 17 and sdkmanager --list works. [none]
T02 Clone or copy the repo to C:\src\tvbrowser, run gradlew.bat assembleDebug. This is the compile gate; fix any compile errors before anything else. [T01]
T03 Run scripts\Invoke-Tests.ps1 -UnitOnly; all JVM tests green. [T02]
T04 Create the emulator with scripts\New-TvAvd.ps1 and boot it once by hand to confirm it starts with hardware acceleration. [T01]
T05 Run scripts\Invoke-Tests.ps1 (full); all instrumented tests green on the emulator. [T03, T04]
T06 Create the release keystore with scripts\New-Keystore.ps1 and back up %USERPROFILE%\tvbrowser-signing to a second location. [T01]
T07 Build the signed release with scripts\Build-Release.ps1; confirm apksigner verify prints the certificate and releases\ holds the APK and .sha256. [T05, T06]
T08 Copy the APK to a USB drive, install on the Shield via the file manager (requires the one-time Unknown sources toggle for the file manager app; get approval first). [T07]
T09 Run the on-device checklist in TEST-PLAN.md section D on the Shield. [T08]
T10 Initialise git, commit, push to the public GitHub repo; confirm .gitignore keeps keystore.properties, *.jks and releases\ out. [T02]
T11 Hand SPEC.md, the source tree and TEST-PLAN.md to Claude Fable 5.1 with docs\FABLE-REVIEWER-PROMPT.md; fix findings; repeat until no High or Medium remain. [T05, T10]
T12 Tag v1.0.0 and archive the release APK; record the SHA-256 in the tag message. [T09, T11]
