# Data Model and Room Schema

This document is a design baseline. Do not treat it as evidence that the schema has already been compiled or tested.

## Storage conventions

- UUID strings for user-created primary keys.
- Unix epoch milliseconds (`Long`) for instants such as schedule and alarm times.
- ISO date strings (`YYYY-MM-DD`) for local habit calendar dates.
- Store the IANA time-zone ID alongside date-sensitive scheduling data (for example, `Asia/Manila`).
- Use explicit database status strings mapped to domain enums.
- Use transactions for multi-row operations.
- Prefer archiving/soft deletion where user history should be retained.
- Export Room schemas and create explicit migration tests. Never use destructive migration in production.

## Tables

### tasks
Stores task definitions independently of scheduled time.

Fields: `id`, `title`, `description`, `estimatedDurationMinutes`, `priority`, `energyLevel`, optional `deadlineAtEpochMillis`, `status`, optional `completedAtEpochMillis`, creation/update timestamps, optional `deletedAtEpochMillis`.

A task may be unscheduled or split across multiple schedule blocks. A deadline is not the scheduled start time.

### habits
Stores recurring habit definitions.

Fields: `id`, `title`, `description`, `frequencyPeriod` (`DAY` or `WEEK`), `targetCount`, `selectedDaysMask` (Monday bit 0 through Sunday bit 6; zero means any day), start/end local dates, optional preferred minute-of-day, `durationMinutes`, `energyLevel`, reminder setting, archive flag, timestamps.

Weekly targets are evaluated over the user's local calendar week, not consecutive days.

### habit_daily_progress
Stores daily completion counts and a snapshot of the target/frequency configuration.

Composite primary key: `habitId + localDateIso`.

Other fields: `timeZoneId`, `completionCount`, `targetCountAtTime`, `frequencyPeriodAtTime`, update timestamp.

A completion operation must be transactional and must define retry/idempotency behavior. If undo/audit or multiple independently addressable check-ins are required, add a separate immutable check-in table.

### schedule_blocks
Unified timeline records for tasks, habits, breaks, and external/fixed commitments.

Fields: `id`, `blockType`, optional `taskId` / `habitId`, `titleSnapshot`, start/end epoch milliseconds, `timeZoneId`, `status`, `source`, `isMovable`, timestamps.

Domain validation must ensure:
- end is later than start;
- TASK blocks reference a task only;
- HABIT blocks reference a habit only;
- BREAK/EXTERNAL blocks do not incorrectly reference task/habit records;
- fixed/completed blocks are not automatically moved;
- active blocks do not overlap unless a future explicit policy permits it.

An ordinary index does not enforce interval non-overlap. Check in the domain layer and again inside the schedule-application transaction.

### availability_windows
Recurring available/unavailable periods.

Fields: `id`, `title`, `windowType`, local start/end minutes, `daysOfWeekMask`, `timeZoneId`, optional validity range, hard-constraint flag, enabled flag.

Overnight intervals (for example, sleep from 22:00 to 06:00) must be expanded into adjacent local-day intervals before comparison.

### reminders
Persisted local reminder intent/state for a schedule block.

Fields: `id`, `scheduleBlockId`, `triggerAtEpochMillis`, `status`, optional snooze time, optional sound URI, vibration preference, snooze/max-playback configuration, stable PendingIntent request identifier, trigger/audit timestamps.

Initial design allows one reminder per schedule block. Change that uniqueness rule deliberately if multiple reminders are later supported.

Room stores intended reminder state. Android alarm registrations are external system state and must be reconciled after reboot, time changes, app updates, and permission changes.

### ai_schedule_proposals
Draft schedule proposal separate from the live schedule.

Fields: `id`, `engine`, optional `modelId`, status, `summary`, `inputSnapshotJson`, `baseDataFingerprint`, optional prompt version and timestamps.

The fingerprint must be calculated from a deterministic canonical representation of the relevant input data. If relevant data changes before approval, revalidate or regenerate the proposal.

### ai_proposal_blocks
Proposed blocks belonging to a proposal.

Fields: `id`, `proposalId`, `blockType`, optional task/habit reference snapshots, title snapshot, start/end epoch milliseconds, time-zone ID, rationale, optional confidence, display order.

Proposals remain inspectable if an original task is later removed. Validate all live references before applying.

## Relationships

- `habit_daily_progress.habitId -> habits.id`
- `schedule_blocks.taskId -> tasks.id` (nullable)
- `schedule_blocks.habitId -> habits.id` (nullable)
- `reminders.scheduleBlockId -> schedule_blocks.id`
- `ai_proposal_blocks.proposalId -> ai_schedule_proposals.id`

Use cascade deletion only when intentional. Normal user-facing removal should prefer archival/soft deletion.

## Atomic operations

### Habit completion
Read and update daily progress in a transaction. Make retries idempotent using a stable check-in/action ID or explicit duplicate-action handling.

### Apply schedule proposal
Within one Room transaction:
1. Load the draft proposal and its blocks.
2. Confirm proposal status remains DRAFT.
3. Compare/recompute the base-data fingerprint or fully revalidate current inputs.
4. Confirm referenced tasks/habits still exist and are eligible.
5. Validate durations, time zone, availability, fixed blocks, and overlap.
6. Preserve completed and fixed blocks.
7. Apply schedule changes.
8. Create/update reminder records.
9. Mark the proposal APPLIED.

If validation fails, roll back all schedule writes. After commit, reconcile Android alarms from persisted reminders. AlarmManager registration cannot participate in a Room transaction and must be retryable.
