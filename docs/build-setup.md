# Android Build Setup

## Pinned baseline

This repository uses the following initial toolchain:

| Component | Version | Reason |
|---|---:|---|
| Android Gradle Plugin | 9.5.0 | Current stable AGP baseline |
| Gradle | 9.6.0 | Required Gradle line for AGP 9.5 |
| JDK | 17 | AGP 9.5 documented JDK requirement |
| Kotlin | 2.4.21 | Stable Kotlin release |
| KSP | 2.3.12 | KSP release aligned with Kotlin 2.4.21 |
| Compose BOM | 2026.06.01 | Compose 1.11.4 generation |
| Room | 2.8.5 | Stable Room 2.x release |
| compileSdk | 36 | Stable Android 16 platform used by CI |
| targetSdk | 36 | Initial Android 16 target |
| minSdk | 26 | Initial product compatibility baseline |

Official references:
- [AGP release notes](https://developer.android.com/build/releases/gradle-plugin)
- [AGP and Gradle compatibility](https://developer.android.com/build/releases/about-agp)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [KSP quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [Compose UI release notes](https://developer.android.com/jetpack/androidx/releases/compose-ui)
- [Room release notes](https://developer.android.com/jetpack/androidx/releases/room)

AGP 9 enables built-in Kotlin support. The project intentionally does **not** apply `org.jetbrains.kotlin.android`. The Compose compiler plugin remains applied separately using the Kotlin version. KSP handles Room's annotation processor.

### Why compileSdk 36 is pinned

Compose 1.12.0 and newer require compileSdk 37. The CI runner currently cannot install API 37 as a stable SDK platform. The project therefore uses the Compose 1.11.4 generation with the stable 2026.06 BOM and compileSdk 36, which matches our initial Android 16 target. Upgrade the Compose BOM and compileSdk together when API 37 is generally available in the build environment. <https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler>

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
2. Install Android SDK Platform 36 and Android SDK Build-Tools 36.0.0.
3. Open the repository root in Android Studio.
4. Allow Gradle sync to finish.
5. Generate and commit the Gradle wrapper as described above.
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

The Gradle files have been added to GitHub, but the CI build has not completed successfully yet. Do not treat the scaffold as verified until the CI build and tests pass.
