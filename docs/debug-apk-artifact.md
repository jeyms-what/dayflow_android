# Downloading the Dayflow debug APK

After a successful Android CI run, GitHub Actions publishes an artifact named **dayflow-debug-apk**.

## Download

1. Open the repository's **Actions** tab.
2. Select a successful **Android CI** workflow run.
3. Scroll to the **Artifacts** section.
4. Download **dayflow-debug-apk** and extract the ZIP.
5. Install `app-debug.apk` on an Android test device.

Artifacts are retained for 14 days. A new successful workflow run creates a fresh artifact.

## Install with ADB

With USB debugging enabled and the phone connected:

```powershell
adb install -r .\app-debug.apk
```

Alternatively, transfer the APK to the phone and open it with the package installer. Android may ask you to allow installation from that source.

## Important

This is a **debug build for testing**, not a Play Store release. It is not a production-signed APK. Keep USB debugging and unknown-app installation controls appropriately restricted after testing.

A successful hosted build proves compilation and the automated unit tests for that source snapshot. It does not prove physical-device behavior. Run the offline task/habit checklist in [ui-smoke-test.md](ui-smoke-test.md) on the Infinix NOTE 50 Pro+ 5G running Android 16 and record the observed results.
