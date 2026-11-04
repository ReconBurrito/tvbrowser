# Deployment and rollback

## Deploy (new version)
1. Bump versionCode (+1) and versionName in app\build.gradle.kts. versionCode must increase or Android refuses the update.
2. Run scripts\Invoke-Tests.ps1 and require green.
3. Run scripts\Build-Release.ps1. It verifies the signature, copies the APK to releases\tvbrowser-<version>-<stamp>.apk with a .sha256 beside it, and keeps the newest three.
4. Copy the APK to a FAT32 or exFAT USB drive.
5. Plug the drive into the Shield. Open the file manager app, open the drive, select the APK, choose Install. The first time, Android TV asks to allow the file manager to install unknown apps; that is a Shield system-settings change and needs approval under the project rules before it is toggled. It is the only Shield setting this project touches.
6. The install is an in-place update when the signing certificate matches; bookmarks, history, cookies and session survive. If Android reports "App not installed" the certificate differs (different keystore) or versionCode went down; do not uninstall without approval, fix the build instead.
7. Launch from the Shield home row, run the smoke test (TEST-PLAN.md section D1 to D5).

## Rollback
1. Take the previous APK from releases\ (or rebuild the tagged version) and confirm its .sha256.
2. Android does not allow installing a lower versionCode over a higher one. Rollback is therefore either: (a) rebuild the previous source at the previous git tag but with a versionCode one higher than the bad release, sign with the same keystore, install over the top; data is preserved. Or (b) uninstall the app on the Shield and install the old APK; this deletes bookmarks, history and session, so run More > Export first and get approval, because uninstalling is on the approval list.
3. After rollback, run D1 to D5 again.

## Keystore loss
If %USERPROFILE%\tvbrowser-signing is lost, no future build can update the installed app; the only path is uninstall plus reinstall with a new keystore (approval required, data loss). The folder must be backed up after T06.

## Emulator and test data
Emulator wipes, debug-build reinstalls on the emulator, and gradlew clean are routine and need no approval. Nothing in this project touches any lab host, the network, OPNsense or PiHole.
