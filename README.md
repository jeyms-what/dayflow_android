# Dayflow

**An offline-first Android planner combining tasks, habits, time-blocking, local AI assistance, and alarm-style reminders.**

Dayflow is designed around one promise: your schedule and habits stay available on your phone, even when the internet is not.

> **Project status:** The initial Android Gradle/Compose scaffold, Room schema/DAO source, domain contracts, Room-backed repositories, and initial unit tests are committed. The first clean build and device test have not yet been run. Use-case coverage, timeline features, deterministic scheduler, alarms, widgets, local AI runtime, and clean CI verification are still in progress. This is not yet a ready-to-install release APK.

## Product goals

- **Unified daily timeline:** Tasks, habits, breaks, and fixed commitments in one agenda.
- **Flexible habit tracking:** Daily and weekly targets, including goals such as three sessions per week.
- **On-device AI first:** Prefer compatible local models; never require cloud AI for core scheduling.
- **Safe smart scheduling:** Generate proposals, validate constraints locally, and apply only after user approval.
- **Alarm-style reminders:** Local audible/vibrating alerts with Complete, Snooze, and Dismiss actions.
- **Interactive widgets:** Quick habit completion and upcoming schedule information.
- **Local-first data:** Room is the source of truth for application data.

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
| Compose BOM | 2026.09.00 |
| Local database | Room 2.8.5 |
| Architecture | MVVM, Clean Architecture boundaries, unidirectional data flow |
| Local model runtime | LiteRT-LM (subject to model/device testing) |
| Scheduling fallback | Deterministic Kotlin engine |

Other libraries such as WorkManager, Glance, DataStore, and Hilt will be added when their features are implemented, with versions verified at that time.

## Repository layout

```text
app/
  src/main/
    java/com/dayflow/
      core/database/
        dao/
        entity/
      core/designsystem/theme/
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
- [Development roadmap](docs/roadmap.md)

## Primary test device

The initial target test device is the **Infinix NOTE 50 Pro+ 5G running Android 16**. Actual hardware variant, available memory, firmware, model compatibility, thermal behavior, and inference performance must be verified on-device. Do not assume Gemini Nano support.

## Build status

The Gradle app scaffold is present, but the Gradle wrapper scripts/JAR still need to be generated and the first clean build must be run. No successful compile or device test is claimed yet. Follow [docs/build-setup.md](docs/build-setup.md).

## Privacy and permissions

- No account or network is required for core features.
- A local model may require a one-time download before offline inference is possible.
- Explain model storage and processing to the user; do not silently download large models over mobile data.
- Notifications require runtime permission on Android 13+.
- Exact alarms require the applicable Android special access where used. Explain why it is needed and provide a fallback.
- Respect notification channel sound/vibration settings and device-level restrictions.

## License

No license has been selected yet. Until a license is added, normal copyright applies; public visibility does not grant permission to reuse the code.
