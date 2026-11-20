# Compose Today and Habits Smoke Test

Run this checklist on a clean install after the debug build succeeds.

## Offline task flow

- [ ] Enable Airplane mode before launching Dayflow.
- [ ] Open Today.
- [ ] Create a task with a non-empty title and a valid duration.
- [ ] Confirm the task appears without a network connection.
- [ ] Complete the task using its checkbox.
- [ ] Confirm it disappears from the active task list.
- [ ] Force-stop and relaunch the app while still offline.
- [ ] Confirm the task remains completed and is not shown as active.

## Offline habit flow

- [ ] Create a daily habit with a target of one per day.
- [ ] Log the habit once.
- [ ] Confirm the daily progress changes to 1 / 1 and the action becomes disabled.
- [ ] Create a weekly habit with a target of three times per week.
- [ ] Log one session and confirm progress changes to 1 / 3.
- [ ] Log two more sessions and confirm progress reaches 3 / 3.
- [ ] Confirm the weekly target remains independent of consecutive-day completion.

## Navigation and appearance

- [ ] Switch between Today, Habits, Plan, and Insights.
- [ ] Confirm Plan shows the daily task timeline and Insights is clearly marked as planned.
- [ ] Toggle Light and OLED Dark themes.
- [ ] Confirm scrolling content is not obscured by the top or bottom app bars.
- [ ] Confirm dialogs can be cancelled without creating records.

## Persistence

- [ ] Close and reopen Dayflow.
- [ ] Confirm task and habit records remain available.
- [ ] Repeat the checks with Airplane mode still enabled.

## Record evidence

Record device model, Android version, app version, build variant, steps, expected result, actual result, and any relevant logcat output. Do not mark the milestone complete based only on a successful compilation; the physical-device checks must also pass.


## Daily timeline flow

- [ ] Create two open tasks with different estimated durations.
- [ ] Open the Plan tab while offline.
- [ ] Schedule the first task at a valid 24-hour start time.
- [ ] Confirm the block appears in chronological order with the calculated end time.
- [ ] Confirm the scheduled task no longer appears under Unscheduled tasks.
- [ ] Try to schedule the second task into an overlapping time range.
- [ ] Confirm Room rejects the overlap and the app shows an error message.
- [ ] Schedule the second task in a free time range and confirm both blocks appear.
- [ ] Force-stop and relaunch the app; confirm the timeline remains present offline.


## Unified task and habit completion flow

- [ ] Create a daily habit with a 15-minute duration.
- [ ] Create a weekly habit with a target of three sessions.
- [ ] Open Plan while offline and schedule both habits at non-overlapping times.
- [ ] Confirm both habit blocks appear in chronological order.
- [ ] Schedule a task at a time that overlaps a habit block.
- [ ] Confirm Room rejects the conflicting block and the existing schedule remains unchanged.
- [ ] Complete a scheduled task block and confirm the task is marked completed in Today.
- [ ] Complete a scheduled daily habit block and confirm its daily progress increments once.
- [ ] Complete a scheduled weekly habit block and confirm its weekly progress increments once.
- [ ] Confirm a completed block cannot be completed a second time.
- [ ] If a reminder is attached to a block, confirm its persisted state becomes COMPLETED.
- [ ] Force-stop and relaunch while offline; confirm completed block and habit progress remain saved.
