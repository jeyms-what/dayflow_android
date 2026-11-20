# Android Build Setup

The repository currently documents the intended stack but does not yet pin a complete Android build toolchain.

Before adding Gradle files, verify the compatibility matrix together for:
- Android Gradle Plugin
- Gradle wrapper
- JDK
- Kotlin Gradle Plugin
- KSP (must match the selected Kotlin toolchain)
- Compose compiler plugin and Compose BOM
- compileSdk / targetSdk
- Room and Hilt processors

Use official Android/Kotlin release documentation and pin the compatible versions in `gradle/libs.versions.toml`.

## Local machine guidance

The primary developer environment is Windows with VS Code preference and an Android 16 Infinix NOTE 50 Pro+ 5G for device testing. Android Studio is still recommended for Android SDK management, Compose previews, emulator/device tooling, profiling, and Gradle sync. VS Code may be used as the editor with Gradle commands run from its terminal.

## Required verification before first app release

- Clean Gradle sync on Windows.
- `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows).
- `./gradlew testDebugUnitTest`.
- Install/debug on the Infinix device.
- Record the exact SDK, Gradle, JDK, AGP, Kotlin and KSP versions in the README.

Do not claim the project builds until these checks have passed.
