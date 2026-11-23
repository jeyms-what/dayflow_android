# Dayflow UI visual direction

The current Compose UI uses a calm, dark-first productivity design with a restrained fantasy influence.

## Visual system

- **Base:** midnight navy background with slightly raised blue surfaces rather than pure black.
- **Primary accent:** cool cyan for active states, buttons, and small emphasis.
- **Supporting accents:** muted teal for habit progress and soft lavender for secondary emphasis.
- **Shape:** rounded cards and controls with quiet outlines; avoid heavy shadows and neon effects.
- **Mascot:** a small original blue slime-inspired mark in the app header. It is decorative and does not use anime character artwork.
- **Typography:** clear Material 3 hierarchy, with short overlines, strong screen titles, and readable supporting text.
- **Theme:** dark mode is the first-launch default. The header retains a light/dark switch.

## Interaction references and expanded destinations

Use the user's Streak habit tracker reference for glanceable weekly consistency, quick check-ins, streak context, and restrained progress feedback. Use GymMane as a reference for training-focused session hierarchy, exercise/set logging, and workout history. Do not reproduce either app's exact layout, assets, or branding.

Primary navigation is now Today / Notes / Plan / Train / Habits:
- **Today:** next actions and quick habit check-ins.
- **Notes:** searchable note cards, pinned notes, and a focused editor.
- **Plan:** unified daily timeline; workout scheduling will be integrated in a later increment.
- **Train:** workout session logging and recent training history.
- **Habits:** daily/weekly routines and streaks.

## Current screen scope

- **Today:** daily overview, open tasks, completion actions, and a short habit list.
- **Habits:** active habits, daily/weekly progress, streaks, and logging.
- **Plan:** today's schedule timeline, unscheduled tasks, habit sessions, and task-start reminder setup.
- **Insights:** explicitly marked as planned until real analytics are implemented.

The UI must not imply that the future on-device AI assistant or analytics are already available.

## Device smoke-test checklist

Run on a real Android device after installing the CI debug APK:

1. Launch Dayflow and confirm the dark theme is selected by default.
2. Switch to light theme, move between Today / Habits / Plan / Insights, and switch back to dark theme.
3. Add a task, complete it, and confirm it disappears or updates according to the task list.
4. Add a daily habit and a weekly habit; log each and confirm its progress/streak display updates.
5. Schedule a task with its reminder disabled and confirm the block appears in today's Plan.
6. Schedule a task with **Ring at task start** enabled; complete the notification and exact-alarm permission flows as requested by Android.
7. Complete a scheduled task and confirm its alarm is cancelled.
8. Schedule a habit session and confirm it appears in the timeline.
9. Check the layout with long task/habit names and with empty task/habit lists.
10. Repeat the reminder checks with network connectivity disabled to confirm the local data and alarm path do not depend on internet access.

The GitHub Actions debug APK is the installable test build. A successful CI build verifies compilation and JVM unit tests; it does not replace the physical-device checks above.


## New feature smoke checks

11. Create a note with a title and body, save it, leave the screen, return, and confirm it persists.
12. Search for a word in a note, pin it, edit it, and delete it.
13. Log a workout with an exercise, muscle group, sets, reps, and kg; confirm it appears in recent sessions after navigating away and back.
14. Verify the database migration on an installation containing existing tasks, habits, schedule blocks, and reminders. Confirm all previous records remain present after upgrade.
15. Repeat notes and workout creation in Airplane Mode.
