# Development Roadmap

## Product invariants

- Core task, habit, timeline, and history functions work offline.
- Room is the local source of truth.
- On-device AI is preferred when supported and initialized.
- A deterministic Kotlin scheduler is always available as fallback.
- AI-generated schedules are proposals, validated locally and approved by the user.
- Reminders are device-local audible/vibrating alerts with Complete, Snooze, and Dismiss actions.
- Widgets operate on local data.
- Cloud AI and sync are optional, opt-in enhancements.

## Milestone 0 — Repository/build foundation

- [x] Create dedicated GitHub repository.
- [x] Document product and architecture baseline.
- [x] Add initial Android Gradle/Compose scaffold.
- [x] Pin initial Android build toolchain and version catalog.
- [ ] Generate and commit Gradle wrapper scripts/JAR.
- [x] Run hosted `:app:assembleDebug` and `:app:testDebugUnitTest` on the current Compose snapshot.
- [x] Add CI build/test workflow and verify debug build plus unit tests on PR #1.
- [ ] Select a project license.

Exit: clean checkout imports into Android Studio and passes a Gradle debug build.

## Milestone 1 — Local data foundation

- [x] Draft Room entities and DAOs.
- [ ] Compile and verify Room schema generation.
- [x] Implement initial Room-backed repositories and entity/domain mappers.
- [x] Add Hilt database and repository bindings.
- [ ] Export schemas and add migration tests.
- [x] Add initial task validation/save/complete use cases.
- [x] Add habit save, completion, and archive use cases.
- [x] Add daily/weekly progress calculation, pending-period-aware streak rules, and unit tests.
- [ ] Add schedule and availability persistence tests.
- [ ] Add reminder persistence tests.
- [x] Add initial pure-domain unit tests.

Exit: automated tests prove offline CRUD and persistence across process restarts.

## Milestone 2 — Core Compose experience

- [x] Add initial light and OLED dark palettes.
- [x] Add Today task/habit entry screen.
- [x] Add bottom navigation destinations.
- [x] Add quick task create and completion.
- [x] Add daily/weekly habit create and one-tap check-in.
- [x] Show habit period progress and current streak.
- [x] Add initial unified daily task timeline with manual task time-block creation and overlap rejection.
- [ ] Add habit details and completion heatmap.
- [x] Add habit time-blocking and scheduled-block completion actions with atomic task/habit progress updates.
- [ ] Add time-block editing, drag-and-drop, and rescheduling.
- [ ] Persist theme preference in DataStore.
- [ ] Add settings and accessibility review.

Exit: users can manage tasks and habits offline through the app UI.

## Milestone 3 — Scheduling engine

- [ ] Define hard constraints and soft preferences.
- [ ] Implement deterministic slot generation.
- [ ] Add task priority/deadline scoring.
- [ ] Add habit-frequency handling.
- [ ] Protect fixed blocks and prevent overlaps.
- [ ] Add proposal persistence and fingerprints.
- [ ] Add transactional proposal application.
- [ ] Add rescheduling when a task runs late.

Exit: valid schedules are produced for synthetic test cases without violating fixed commitments.

## Milestone 4 — On-device AI

- [ ] Check supported local inference runtimes.
- [ ] Add model capability state interface.
- [ ] Evaluate LiteRT-LM model size and device resource use.
- [ ] Add structured prompt and JSON parsing.
- [ ] Add natural-language task extraction.
- [ ] Add AI preference/ranking suggestions.
- [ ] Validate all model output deterministically.
- [ ] Add user-controlled model download/deletion.
- [ ] Verify airplane-mode inference after model setup.

Exit: AI-assisted interpretation/suggestions operate offline on a tested device/model with deterministic fallback.

## Milestone 5 — Alarm reminders and widgets

- [ ] Notification channels and runtime permission.
- [ ] Exact-alarm special access explanation and fallback.
- [ ] AlarmManager coordinator.
- [ ] Complete/Snooze/Dismiss actions.
- [ ] Reconciliation after boot/time/time-zone/app/permission changes.
- [ ] Glance habits widget.
- [ ] Glance upcoming schedule widget.
- [ ] Widget-to-Room consistency tests.

Exit: reminders and widgets reflect local state and recover after common lifecycle events.

## Milestone 6 — Quality and release

- [ ] Unit, DAO, migration, ViewModel, and Compose UI tests.
- [ ] Device tests for alarm permission/delivery.
- [ ] Airplane-mode test suite.
- [ ] Accessibility and dynamic font review.
- [ ] Battery/performance profiling.
- [ ] Privacy and data export review.
- [ ] Release build and signing configuration.
- [ ] User documentation and screenshots.

## Verification rule

Do not mark an item complete until it has been implemented and verified. Record device, Android version, build variant, steps, expected/actual results, and relevant logs for important test evidence.
