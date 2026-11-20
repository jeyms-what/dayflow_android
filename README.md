# Dayflow

**An offline-first Android planner combining tasks, habits, time-blocking, local AI assistance, and alarm-style reminders.**

Dayflow is designed around one promise: your schedule and habits stay available on your phone, even when the internet is not.

> **Current status:** The Android Gradle/Compose scaffold, Room data layer, Hilt bindings, task/habit use cases, and first interactive Compose task/habit experience are committed. The screen reads and writes local Room data. The Plan tab includes a Room-backed daily task-and-habit timeline, manual time-block creation, transactional overlap rejection, and completion actions that update linked task/habit progress. Task blocks can opt into persisted alarm reminders with Android AlarmManager delivery and Complete/Snooze/Dismiss notification actions. CI and physical-device alarm behavior are still being verified. Analytics, automatic/dynamic scheduling, Glance widgets, and on-device AI runtime remain in progress. The debug build and unit tests passed in GitHub Actions for PR #1; physical-device testing is still pending. Gradle wrapper scripts/JAR are not yet included. This is not a release-ready APK.

## Product goals

- **Unified daily timeline:** Tasks, habits, breaks, and fixed commitments in one agenda.
- **Flexible habit tracking:** Daily and weekly targets, including goals such as three sessions per week.
- **On-device AI first:** Prefer compatible local models; never require cloud AI for core scheduling.
- **Safe smart scheduling:** Generate proposals, validate constraints locally, and apply only after user approval.
- **Alarm-style reminders:** Local audible/vibrating alerts with Complete, Snooze, and Dismiss actions.
- **Interactive widgets:** Quick habit completion and upcoming schedule information.
- **Local-first data:** Room is the source of truth for application data.

## Implemented in the current milestone

- Create tasks with a title and estimated duration.
- Complete tasks from the Today screen.
- Create daily or weekly habits.
- Add open tasks and active habits to today's timeline using their estimated durations.
- Complete scheduled activities from the timeline; task, habit progress, block status, and linked reminder state are updated in a local transaction.
- Log a habit completion directly from the habit card.
- Calculate daily/weekly progress and current streaks from Room history.
- Switch between light and OLED-black dark themes for the current app session.
- Browse Today and Habits destinations; Planner and Insights are clearly marked as planned.

## Non-negotiable principles

1. **Offline is a first-class mode.** Creating tasks, completing habits, viewing history, and deterministic scheduling must work without a network.
2. **Local AI is capability-dependent.** Check model/runtime availability at runtime. Keep a deterministic Kotlin scheduler available when a generative model is absent or cannot run.
3. **AI does not own the schedule.** Treat model output as untrusted. Validate overlaps, durations, deadlines, availability, and fixed blocks locally. Require approval before applying a proposal.
4. **Reminders are local.** Persist reminder intent in Room and reconcile Android alarm registrations from those records.
5. **No API secrets in the app.** Any future cloud AI or sync feature must be optional, opt-in, and accessed through a secure backend.

## Technology baseline

| Area | Selected baseline |
|---|---|
| Language | Kotlin 2.4.21 |
| Android Gradle Plugin | 9.5.0 |
| Gradle | 9.6.0 |
| JDK | 17 |
| Kotlin Symbol Processing | KSP 2.3.12 |
| UI | Jetpack Compose + Material 3 |
| Activity Compose | 1.12.4 |
| Lifecycle ViewModel | 2.10.0 |
| Compose BOM | 2026.06.01 |
| Local database | Room 2.8.5 |
| Architecture | MVVM, Clean Architecture boundaries, unidirectional data flow |
| Local model runtime | LiteRT-LM (subject to model/device testing) |
| Scheduling fallback | Deterministic Kotlin engine |

Hilt 2.60.1 is used for dependency injection. WorkManager, Glance, and DataStore will be added when their features are implemented, with versions verified at that time.

## Repository layout

```text
app/
  src/main/
    java/com/dayflow/
      core/database/
      core/designsystem/theme/
      data/
      di/
      domain/
      feature/today/presentation/
    AndroidManifest.xml
  schemas/
docs/
  architecture.md
  data-model.md
  build-setup.md
  roadmap.md
gradle/
  libs.versions.toml
```

See:
- [Architecture decisions](docs/architecture.md)
- [Data model and Room schema](docs/data-model.md)
- [Build setup](docs/build-setup.md)
- [Download and install the debug APK](docs/debug-apk-artifact.md)
- [Development roadmap](docs/roadmap.md)

## Primary test device

The initial target test device is the **Infinix NOTE 50 Pro+ 5G running Android 16**. Actual hardware variant, available memory, firmware, model compatibility, thermal behavior, and inference performance must be verified on-device. Do not assume Gemini Nano support.

## Build status

The current task timeline snapshot passed GitHub Actions run #41 for PR #3: `:app:assembleDebug` and `:app:testDebugUnitTest` completed successfully with JDK 17, Gradle 9.6.0, and Android SDK 36. The CI workflow now uploads `dayflow-debug-apk` for 14 days after successful builds. This is hosted build verification only; installation and airplane-mode behavior still need testing on the Infinix NOTE 50 Pro+ 5G. The Gradle wrapper scripts/JAR are still missing. See [docs/build-setup.md](docs/build-setup.md) for local setup and artifact retrieval.

## Privacy and permissions

- No account or network is required for core features.
- A local model may require a one-time download before offline inference is possible.
- Explain model storage and processing to the user; do not silently download large models over mobile data.
- Notifications require runtime permission on Android 13+.
- Exact alarms require the applicable Android special access where used. Explain why it is needed and provide a fallback.
- Respect notification channel sound/vibration settings and device-level restrictions.

## License

No license has been selected yet. Until a license is added, normal copyright applies; public visibility does not grant permission to reuse the code.
