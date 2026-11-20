# Architecture

## Architectural goals

Dayflow uses a layered architecture with MVVM and unidirectional data flow (UDF). Start with a single Gradle application module and package boundaries. Split Gradle modules only when there is a concrete build, ownership, or reuse benefit.

Rules:
- Room is the source of truth for user data.
- UI observes repositories; composables do not query Room.
- ViewModels expose immutable UI state and receive user actions.
- Domain logic does not depend on Android framework classes.
- AI adapters return proposals; they cannot write the live schedule directly.
- AlarmManager and widgets are Android adapters around persisted local data.
- Network access is never required for core use cases.

## Layer responsibilities

### Presentation
Compose screens, reusable components, navigation, ViewModels, and UI state.

Suggested features: `today`, `tasks`, `habits`, `schedule`, `insights`, `settings`.

Use `StateFlow` in ViewModels and lifecycle-aware collection in Compose. Keep composables stateless where practical.

### Domain
Pure Kotlin models, repository interfaces, use cases, scheduling contracts, and validation rules.

Examples:
- `CreateTask`
- `CompleteTask`
- `CompleteHabit`
- `GenerateScheduleProposal`
- `ValidateScheduleProposal`
- `ApplyScheduleProposal`
- `SnoozeReminder`
- `CalculateHabitProgress`

The domain layer must not import Room, Compose, AlarmManager, LiteRT-LM, or Retrofit types.

### Data
Room entities/DAOs, DataStore, mappers, repository implementations, and optional remote adapters. Repository implementations map storage models to domain models.

### Android integrations
- `notifications/`: channels, actions, receivers, and alarm coordination.
- `widget/`: Glance widgets and refresh operations.
- `worker/`: deferrable/retryable work and optional sync.
- `ai/local/`: local runtime adapters and model capability checks.

## Local-first data flow

```text
User action
   -> Compose
   -> ViewModel
   -> Use case
   -> Repository
   -> Room write
   -> Room observable Flow
   -> ViewModel StateFlow
   -> Compose UI
```

Successful local writes update the UI without waiting for a server. Future cloud synchronization, if implemented, synchronizes local records; it does not become the primary read path.

## Scheduling and AI boundaries

Use a common domain contract such as `SchedulingEngine`. Implementations may include:
- `RuleBasedSchedulingEngine`: deterministic, always available, fully offline.
- `LiteRtLocalAiEngine`: optional model-based language understanding and schedule suggestions.
- Future local runtime adapters behind the same contract.
- An optional cloud adapter, disabled by default.

Workflow:
1. Read tasks, habits, availability, fixed blocks, and preferences locally.
2. Build a bounded scheduling input snapshot.
3. Ask the local model to interpret natural language and/or rank preferences.
4. Parse strictly typed structured output.
5. Validate every block using deterministic Kotlin rules.
6. Save a draft proposal and input fingerprint.
7. Present it for user review.
8. On approval, revalidate against current data inside a Room transaction.
9. Apply the changes and persist reminder records.
10. Reconcile Android alarm registrations after commit.

An LLM is not a constraint solver. Never rely on its output to enforce no-overlap rules, fixed events, or deadlines.

## Alarm reminder boundary

Persist reminder intent/state in Room. An Android alarm coordinator translates records into system alarms and notification actions.

- Use AlarmManager for time-sensitive reminders when user-authorized exact-alarm access is available and genuinely needed.
- Check exact-alarm capability before scheduling; do not assume special access.
- Use inexact scheduling where precise delivery is not essential or access is unavailable.
- Respect notification-channel settings for sound and vibration.
- Notification actions must be idempotent and update Room before reconciliation.
- Reconcile after boot, time/time-zone changes, app update, and relevant permission changes.
- WorkManager is for deferrable work, not a replacement for precise alarm delivery.
- Do not promise uninterrupted/infinite looping audio; Android background-execution and foreground-service rules apply.

## Offline AI lifecycle

A local model must be installed and initialized before offline inference is possible. Expose capability states such as:
- `AVAILABLE`
- `MODEL_NOT_INSTALLED`
- `INITIALIZING`
- `UNSUPPORTED_DEVICE`
- `INSUFFICIENT_RESOURCES`
- `ERROR`

Explain any one-time download and storage requirement. Verify runtime/device support; do not infer Gemini Nano support from Android version alone.

## Initial package layout

```text
com.dayflow/
  DayflowApp.kt
  MainActivity.kt
  core/
    common/
    designsystem/
    database/
      dao/
      entity/
      migration/
    preferences/
  data/
    mapper/
    repository/
    local/
    remote/              # optional; never required for core use
  domain/
    model/
    repository/
    usecase/
    scheduler/
  feature/
    today/
    tasks/
    habits/
    schedule/
    insights/
    settings/
  ai/
    local/
    prompt/
    parser/
  notifications/
    receiver/
    scheduler/
  widget/
    today/
    habits/
  worker/
  di/
```

## Testing boundaries

- Domain unit tests for habit frequency, streaks, availability, deadlines, overlaps, and schedule decisions.
- DAO tests using an in-memory Room database.
- Migration tests for every schema transition.
- Repository tests for mapping and persistence.
- ViewModel tests with fake repositories and coroutine test dispatchers.
- Compose UI tests for important flows and accessibility.
- Device tests for alarms, reboot/time-zone reconciliation, notification actions, widgets, and local-model performance.
