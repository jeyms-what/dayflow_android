# Dayflow

**An offline-first Android planner combining tasks, habits, time-blocking, local AI assistance, and alarm-style reminders.**

Dayflow is designed around one promise: your schedule and habits stay available on your phone, even when the internet is not.

> **Project status:** Architecture and schema design are documented. The Android application scaffold and executable feature implementation are still being assembled. This repository is not yet a ready-to-install APK.

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

## Planned technology

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, Clean Architecture boundaries, unidirectional data flow |
| Local database | Room |
| Preferences | DataStore |
| Dependency injection | Hilt |
| Background work | WorkManager |
| Time-sensitive reminders | AlarmManager + BroadcastReceiver + NotificationManager |
| Home-screen widgets | Jetpack Glance |
| Local model runtime | LiteRT-LM (subject to model/device testing) |
| Scheduling fallback | Deterministic Kotlin constraint-based engine |
| Tests | JUnit, coroutine tests, Room tests, Compose UI tests |

Dependency versions will be pinned in Gradle version catalogs when the Android build scaffold is added. Verify the current compatibility matrix before upgrading Android Gradle Plugin, Kotlin, KSP, Compose, or AndroidX artifacts.

## Architecture overview

```text
Compose UI -> ViewModels -> Domain use cases
                          -> Repository interfaces
                                 |
                          Room-backed repositories
                                 |
                           Room (source of truth)

Local AI adapter -> Structured proposal -> Kotlin validator
                                      -> User approval
                                      -> Room transaction
                                      -> Reminder reconciliation
```

See:
- [Architecture decisions](docs/architecture.md)
- [Data model and Room schema](docs/data-model.md)
- [Development roadmap](docs/roadmap.md)

## Primary test device

The initial target test device is the user's **Infinix NOTE 50 Pro+ 5G running Android 16**. Actual hardware variant, memory availability, firmware, model compatibility, thermal behavior, and inference performance must be verified on-device. Do not assume Gemini Nano support.

## Initial delivery sequence

1. Build a clean Android Gradle scaffold.
2. Implement and test Room entities, DAOs, repositories, and migrations.
3. Build the Compose task/habit/timeline experience.
4. Implement the deterministic local scheduling engine.
5. Add local alarm-style reminders and recovery/reconciliation.
6. Evaluate and integrate a compatible on-device AI model.
7. Add Glance widgets and performance/accessibility tests.

## Privacy and permissions

- No account or network is required for core features.
- A local model may require a one-time download before offline inference is possible.
- Explain model storage and processing to the user; do not silently download large models over mobile data.
- Notifications require runtime permission on Android 13+.
- Exact alarms require the applicable Android special access where used. Explain why it is needed and provide a fallback.
- Respect notification channel sound/vibration settings and device-level restrictions.

## Build status

The Gradle project scaffold is not yet committed. Once added, this section will document the exact JDK, Android Gradle Plugin, Kotlin, SDK, and build/test commands. No compile or device test is claimed until it has actually been run.

## License

No license has been selected yet. Until a license is added, normal copyright applies; public visibility does not grant permission to reuse the code.
