# Dayflow product direction: Notes + Schedule + Training

Dayflow is evolving from a task-and-habit planner into one offline-first personal workspace that combines three connected jobs:

1. **Notes:** capture ideas, journaling, checklists, workout notes, and reference material.
2. **Schedule:** plan tasks, habits, workouts, breaks, and fixed commitments in one daily timeline.
3. **Training:** plan gym sessions, record exercises and sets, and review training consistency and progression.

Habits remain a first-class capability and connect all three areas: users can create a habit for training frequency, schedule a workout block, record the session, and see consistency over time.

## UI/UX references

The visual and interaction direction is informed by the user's requested references:
- **Streak habit tracker:** lightweight habit check-ins, clear weekly rhythm, streak feedback, compact progress visualization, and calm motivational language.
- **GymMane:** training-focused hierarchy, readable workout/session cards, exercise and set logging, and useful progress summaries.

These are references for interaction patterns and information hierarchy, not assets or interfaces to copy. Dayflow keeps its own visual identity: midnight navy surfaces, cool cyan/teal active states, restrained lavender, rounded cards, strong typography, and a tiny original slime-inspired mark.

## Navigation model

The primary destinations are:
- **Today:** a concise daily command center with tasks and habit check-ins.
- **Notes:** searchable, editable, pinnable local notes.
- **Plan:** the unified timeline for tasks and habit sessions; workout scheduling is the next integration step.
- **Train:** gym session logging and workout history.
- **Habits:** recurring routines and streaks, including training consistency.

## Current implementation slice

- Notes are stored locally in Room, support create/edit, search, pinning, and deletion.
- Training logs are stored locally in Room and support session title, exercise, muscle group, set count, reps, load in kg, and notes.
- Existing task, habit, schedule, and task-alarm flows remain in place.
- Database migration 1→2 adds the notes and workout tables without deleting existing user data.

## Next increments

1. Add workout templates and reusable exercise library.
2. Support multiple exercises per workout session and per-set weights/reps.
3. Add rest timer and workout-in-progress state.
4. Schedule workout sessions in the Plan timeline and add optional workout reminders.
5. Add personal records, volume trends, and weekly training summaries.
6. Connect workout sessions to habit progress without double-counting completions.
7. Add note links to tasks, schedule blocks, and workout sessions.
8. Add export/backup and migration tests.

## Product integrity

- Room remains the source of truth; all core notes, logs, tasks, and habits work offline.
- Do not imply a live workout timer, automatic exercise library, PR detection, or analytics until implemented.
- Weight is stored in kilograms in this milestone.
- The current workout logger records one exercise per session. Multi-exercise sessions and independent set values are planned follow-up work.
- All data migrations must preserve existing installations.
