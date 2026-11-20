# Android Build Setup

## Pinned baseline

This repository uses the following initial toolchain:

| Component | Version | Reason |
|---|---:|---|
| Android Gradle Plugin | 9.5.0 | Stable AGP release supporting Android API 37 |
| Gradle | 9.6.0 | Required Gradle line for AGP 9.5 |
| JDK | 17 | AGP 9.5 documented JDK requirement |
| Kotlin | 2.4.21 | Stable Kotlin release |
| KSP | 2.3.12 | Current KSP line; documented with Kotlin 2.4.21 |
| Compose BOM | 2026.09.00 | Stable Compose dependency alignment |
| Room | 2.8.5 | Stable Room 2.x release |
| compileSdk | 37 | Supported by AGP 9.5 |
| targetSdk | 36 | Initial target while validating behavior on Android 16 |
| minSdk | 26 | Initial product compatibility baseline |

Official references:
- [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes)
- [AGP and Gradle compatibility](https://developer.android.com/build/releases/about-agp)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [Room release notes](https://developer.android.com/jetpack/androidx/releases/room)

AGP 9 enables built-in Kotlin support. The project intentionally does **not** apply `org.jetbrains.kotlin.android`. The Compose compiler plugin remains applied separately and uses the Kotlin version. KSP is configured for Room's annotation processor.

## Gradle wrapper

The wrapper distribution is configured in `gradle/wrapper/gradle-wrapper.properties`. The wrapper scripts and wrapper JAR are not included yet.

From the project root, after installing Gradle 9.6.0, generate the wrapper:

```powershell
gradle wrapper --gradle-version 9.6.0 --distribution-type bin
```

Commit the generated files:
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

After that, use the wrapper for reproducible builds.

## Windows development setup

1. Install Android Studio and select JDK 17 as the Gradle JDK.
2. Install Android SDK Platform 37 and the Android 16 platform/build tools needed for device testing.
3. Open the repository root in Android Studio.
4. Allow Gradle sync to finish.
5. Generate/commit the Gradle wrapper as described above.
6. Connect the Infinix NOTE 50 Pro+ 5G with USB debugging enabled.

VS Code can be used as the editor, but Android Studio is recommended for SDK management, Compose previews, device debugging, profiling, and Gradle sync.

## Initial verification commands

After wrapper generation:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Then install the debug APK on the test phone and record the exact build, Android version, test steps, and observed results.

## Memory-conscious development

The primary development PC has 8 GB RAM. Gradle's JVM heap is initially limited to 2 GB and parallel Gradle execution is disabled. Avoid running a heavy emulator and several IDEs simultaneously; a physical device is preferred for early tests.

## Build verification status

The Gradle files have been added to GitHub, but no clean build has been executed in this environment. Do not treat the scaffold as verified until the commands above pass.
