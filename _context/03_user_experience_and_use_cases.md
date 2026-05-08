# User Experience And Use Cases

## How To Fill This File

- If the product has no visual UI, reinterpret UX as operator, API consumer, or workflow experience.
- Keep the scenarios directly traceable to the goals in `01_product_vision_and_scope.md`.
- Write behaviors the QA role can test without guessing intent.

## Experience Overview

- Experience channels: (1) the Android app UI and (2) Android system notifications (lock-screen and notification-shade Reminders with inline "Taken" / "Skipped" action buttons). No widget, no Wear OS / watch face, no email, no SMS, no voice assistant in MVP.
- Primary user journey this phase must support: the full first-week onboarding loop — Patient adds a Medication and configures its Dose Schedule → the Scheduler materializes upcoming Doses → Reminders fire at the configured times → Patient confirms each Dose as Taken or Skipped → after a few days, the Patient can review their Adherence history.
- UX success signal: behavioral and emotional in combination — the Patient does not have to remember on their own whether they took today's Dose (the app's daily list and Reminders are sufficient ground truth), and the Patient feels confident their treatment is on track without anxiety about missed Doses or doubling up after a forgotten one.

## Personas Or Actors

| Actor | Context | Goal | Skill Level | Accessibility Or Environment Notes |
| --- | --- | --- | --- | --- |
| Patient | At home, throughout the day, on their personal Android phone — to be prompted at each scheduled Dose time and to confirm taking or skipping the Dose without having to remember the schedule themselves. Sets up Medications and Dose Schedules occasionally; interacts with Reminders multiple times per day. | Take every prescribed Dose at its scheduled time, capture each Dose as Taken or Skipped without effort, and have a trustworthy record of Adherence to look back on. | Novice — design for low smartphone fluency; assume the Patient may be unfamiliar with notification actions, may not navigate menus deeply, and benefits from very explicit labels and few choices. | Offline-first (no network connectivity required); screen-reader compatible (TalkBack); low-light / glanceable (morning Doses may be taken before sunrise; lock-screen Reminders must be readable and actionable from a glance); one-handed friendly (Patient may be holding a water glass or pill bottle in the other hand); battery / Doze-tolerant (BR-004 requires Reminders to fire reliably under Doze; file 01 lists battery and Doze reliability as a key assumption to validate). |
| Not applicable | Not applicable — single-user app; file 01's "no multi-user / caregiver accounts" boundary excludes any secondary human actor for the MVP. | Not applicable. | Not applicable. | Not applicable. |

## Key Journeys

| Journey ID | Actor | Trigger | Happy Path Summary | Failure Or Alternate Paths |
| --- | --- | --- | --- | --- |
| `J-001` | Patient | Patient installs and opens NemoPill for the first time with a real prescription they want to track. | Patient grants Android notification permission, then adds a Medication (name, Strength, Form). Patient configures its Dose Schedule (Frequency, TimesOfDay, startDate, optional endDate). The Scheduler materializes Doses up to 3 days ahead (BR-009). Over the following days, Reminders fire at each TimesOfDay; the Patient taps "Taken" / "Skipped" inline or in-app, recording a Confirmation. After several Doses have passed, the Patient opens the Adherence view and sees a percentage and per-Dose history that match what they did. | Patient denies notification permission (app falls back to in-app Dose list only; warning banner shown). Patient enters an invalid Dose Schedule (BR-002 endDate < startDate, BR-003 zero/negative quantity) and is blocked at inline validation. Doze / battery throttling delays the first Reminder (BR-010 late-Reminder path engages, or Dose flips to `missed` and J-005 retroactive flow applies). Patient abandons setup partway (no Medication persisted; no Doses generated). |
| `J-002` | Patient | A scheduled Dose's `scheduledAt` arrives; the Scheduler fires the Reminder (`ReminderFired` event). | Reminder appears on the lock screen / notification shade with Medication name, DoseQuantity, and inline "Taken" / "Skipped" actions. Patient taps "Taken"; a Confirmation is recorded with `source = notification-action`; the Dose flips to `taken` (Dose aggregate atomicity); the Reminder is dismissed. Alternatively, Patient opens the app and confirms from the day's Dose list (`source = in-app`). | Patient ignores the Reminder; Dose remains `pending` until `scheduledAt + 1h`, then transitions to `missed` (BR-010). Patient dismisses the Reminder without acting (no Confirmation recorded). Patient taps the same notification action twice (idempotent — Dose ↔ Confirmation 1:1 invariant prevents duplicates). Duplicate Reminders prevented per BR-004 (`doseId` dedupe). |
| `J-003` | Patient | Patient's prescription changes (new Frequency, TimesOfDay, or DateRange) and they want NemoPill to match. | Patient opens the Medication and edits its Dose Schedule. On save, the prior Dose Schedule is deactivated and the new one activated atomically (BR-001). Future-pending Doses tied to the old Schedule are deleted; Doses are regenerated forward of the new `startDate` (BR-006, BR-009). Past Doses and Confirmations are preserved unchanged. The next Reminder fires at the new TimesOfDay. `DoseScheduleReplaced` event is emitted. | Atomic save fails (transaction rolls back; prior Schedule remains active; non-technical error surfaced). Invalid new Schedule blocked at inline validation (BR-002, BR-003). Patient cancels mid-edit (no changes persisted). |
| `J-004` | Patient | Patient has stopped taking a Medication (course complete, prescription discontinued). | Patient opens the Medication and chooses Archive. App shows a confirm prompt explaining that future-pending Doses will be removed but historical Doses and Adherence are preserved. On confirm, `isActive = false`, all Dose Schedules deactivate, and all `pending` Doses with `scheduledAt > now` are deleted in the same transaction (BR-007). The Medication disappears from the active list but remains in Adherence history. `MedicationArchived` event is emitted. | Atomic archive fails (rejected; Medication remains active; error surfaced). Patient archives by mistake (no MVP re-activation per file 02; Patient must create a new Medication). Patient archives a Medication with no remaining Doses (still works; idempotent). |
| `J-005` | Patient | Patient took or skipped a Dose without tapping the Reminder; the Dose has flipped to `missed` and the Patient wants the record to reflect reality. | Patient opens the day's Dose list (or yesterday's). For a `missed` Dose where `now ≤ scheduledAt + 24h`, the Dose card shows "Taken" / "Skipped" buttons (BR-011). Patient taps "Taken"; a Confirmation is recorded with `source = retroactive`, `confirmedAt = time of tap`. Dose status flips to `taken`; Adherence updates (retroactive `taken` counts identically per BR-008). | Dose where `now > scheduledAt + 24h`: action buttons are not rendered; card shows "This Dose can no longer be edited". Patient attempts retroactive logging via stale UI / race: service rejects with same non-technical message. |
| `J-006` | Patient (passive) and Scheduler (active) | Device was asleep, off, or Doze-throttled across one or more `Dose.scheduledAt` times; on wake / boot / Doze-exit / app-foreground, the Scheduler reconciles. | For each `pending` Dose where `now ≤ scheduledAt + 1h`, the Scheduler fires a late Reminder using distinct text indicating lateness (BR-010). For each `pending` Dose where `now > scheduledAt + 1h`, the Scheduler transitions status → `missed` and emits one silent low-priority notification (BR-010, BR-004). Multiple late/missed Doses across the device-off window are batched into a single summary notification at wake. Patient confirms late Reminders normally (J-002 path); `missed` Doses follow J-005 if still within 24h. | Late-fire path errors: Scheduler-fault event logged; the `pending → missed` transition runs on its normal schedule (BR-010 fallback). Device wakes after both 1h grace and 24h retroactive window: Doses locked at `missed`. OS-level wake suppression beyond app control: out of MVP scope per file 01 reliability assumption. |
| `J-007` | Patient (passive) and Scheduler (active) | Android emits `ACTION_TIMEZONE_CHANGED` (Patient traveled, manual TZ change, or DST shift). | A `BroadcastReceiver` hands the new IANA TZ to the timezone-change handler. In a single transaction: all `pending` Doses with `scheduledAt > now` are deleted and regenerated using TimesOfDay interpreted in the new timezone (BR-012). `Dose.generatedInTimezone` is set to the new TZ. Past Doses and Confirmations are preserved unchanged. The Patient's "8 AM Dose" continues to fire at 8 AM local time in the new location. `DeviceTimezoneChanged` event is emitted. | Regeneration cannot complete transactionally: handler logs a Scheduler-fault event; existing future-pending Doses remain in place; next daily horizon-extension job (BR-009) reconciles within 24h. DST spring-forward: a Dose at a non-existent local time shifts to first valid time after the gap (TimeOfDay validation, BR-012). DST fall-back: Reminder fires once at first occurrence. |

## Use Cases

| Use Case ID | Name | Primary Actor | Preconditions | Outcome |
| --- | --- | --- | --- | --- |
| `UC-001` | Add Medication | Patient | App installed; Android notification permission ideally granted; Patient knows their Medication's name, Strength, Form, Frequency, TimesOfDay, startDate, and (optional) endDate. No restriction against multiple Medications with the same name/Strength. | A new active Medication exists with exactly one active Dose Schedule (BR-001 invariant). Doses are materialized for the next 3 days (BR-009). `MedicationCreated` event emitted. The Medication appears in the active Medications list and its first upcoming Dose appears in the day's Dose list. |
| `UC-002` | Replace Dose Schedule | Patient | An active (non-archived) Medication exists with exactly one active Dose Schedule. | Prior Dose Schedule deactivated and new Dose Schedule activated **atomically** (BR-001). Future-pending Doses tied to the old Schedule deleted; new Doses materialized forward of the new `startDate` (BR-006, BR-009). Past Doses and their Confirmations preserved unchanged. `DoseScheduleReplaced` event emitted. |
| `UC-003` | Archive Medication | Patient | An active Medication exists. | `Medication.isActive = false`. All Dose Schedules of the Medication deactivated. All `pending` Doses with `scheduledAt > now` deleted in the same transaction (BR-007). Past Doses and their Confirmations preserved unchanged. `MedicationArchived` event emitted. Medication disappears from the active list; remains visible in Adherence history. Re-activation is **not** permitted in MVP — to resume, Patient creates a new Medication. |
| `UC-004` | Confirm Dose from Reminder | Patient | A Reminder for a Dose with `status = pending` is currently displayed on the lock screen / notification shade with inline "Taken" / "Skipped" action buttons (notification permission was granted at setup; OS has not stripped the actions). | A Confirmation with `source = notification-action`, `status ∈ {taken, skipped}`, `confirmedAt = time of tap` is recorded. `Dose.status` flips to match (Dose aggregate atomicity per BR-005). Notification is dismissed. `DoseConfirmed` event emitted. |
| `UC-005` | Confirm Dose in-app | Patient | A Dose with `status = pending` is visible in the day's Dose list inside the app. | A Confirmation with `source = in-app`, `status ∈ {taken, skipped}`, `confirmedAt = time of tap` is recorded. `Dose.status` flips to match (BR-005). The Dose card updates inline. `DoseConfirmed` event emitted. |
| `UC-006` | Retroactively confirm a missed Dose | Patient | A Dose has `status = missed` AND `now ≤ Dose.scheduledAt + 24 hours` (BR-011). No Confirmation row exists for the Dose. | A Confirmation with `source = retroactive`, `status ∈ {taken, skipped}`, `confirmedAt = time of tap` is recorded. `Dose.status` flips to match. Retroactive `taken` counts toward Adherence identically to on-time `taken` per BR-008. `DoseConfirmed` event emitted. |
| `UC-007` | Correct an existing Confirmation | Patient | A Dose has `status ∈ {taken, skipped}` with an existing Confirmation row (i.e., the Patient previously confirmed but wants to change the answer). | The existing Confirmation row is mutated **in place** (Option A): `status` flipped, `confirmedAt` updated to time of correction. No new Confirmation row is created (Dose ↔ Confirmation 1:1 invariant). `Dose.status` flips to match. `DoseConfirmed` event re-emitted with the same `confirmationId`. |
| `UC-008` | View today's Dose list | Patient | Patient has at least one Medication (active or archived) with at least one materialized Dose for the current local day. | Patient sees, for the current local day, all Doses ordered by `scheduledAt`, each showing Medication name, DoseQuantity, scheduled time, and current status (`pending` / `taken` / `skipped` / `missed`). Doses eligible for action — `pending`, or `missed` within the BR-011 24-hour retroactive window — render inline "Taken" / "Skipped" buttons. Locked `missed` Doses (outside 24h) show a non-technical "This Dose can no longer be edited" message. |
| `UC-009` | View Adherence history | Patient | At least one Dose with `scheduledAt ≤ now` exists in the Patient's data (otherwise BR-008 returns 0% with no meaningful history). | Patient sees the Reminder-to-Confirmation rate over the rolling 30-day window (file 01 success metric) computed per BR-008, plus a per-Dose history (date, time, Medication, status). Computation is on demand per query — no precomputed rollups in MVP. Adherence covers archived Medications' past Doses too. |

## BDD Scenarios

Use one scenario per observable behavior. Duplicate the pattern as needed.

### `BDD-001` — Patient adds a Medication and the next 3 days of Doses materialize
- Given the Patient has installed NemoPill, granted notification permission, and has no Medications yet
- When the Patient adds a Medication "Lisinopril, 10 mg, tablet" with Dose Schedule "1 tablet, daily, 08:00, starting today, no endDate"
- Then exactly one active `Medication` and one active `Dose Schedule` are persisted, and three `Dose` rows are materialized with `scheduledAt` = 08:00 today, 08:00 tomorrow, and 08:00 the day after, all with `status = pending`
- And a `MedicationCreated` domain event is emitted

### `BDD-002` — Reject a Dose Schedule whose `endDate` is before its `startDate`
- Given the Patient is configuring a new Medication's Dose Schedule
- When the Patient enters `startDate = 2026-06-01` and `endDate = 2026-05-15`
- Then save is blocked with an inline validation message per BR-002 and neither a Medication nor a Dose Schedule is persisted

### `BDD-003` — Reject a zero or negative Strength
- Given the Patient is configuring a new Medication
- When the Patient enters `Strength.amount = 0`
- Then save is blocked with inline validation per BR-003 and no Medication is persisted

### `BDD-004` — Replace Dose Schedule preserves past Doses and regenerates future Doses
- Given an active Medication "Lisinopril 10 mg" with active Dose Schedule "daily 08:00", 3 future-pending Doses materialized, and 5 past Doses (3 `taken`, 1 `skipped`, 1 `missed`) with their Confirmations
- When the Patient changes the Dose Schedule to "daily 09:00"
- Then the prior Dose Schedule is deactivated, the new Dose Schedule is active, the 3 future-pending Doses are deleted, new Doses are materialized at 09:00 forward of the new `startDate`, and all 5 past Doses with their Confirmations remain unchanged
- And a `DoseScheduleReplaced` domain event is emitted

### `BDD-005` — Failed atomic Schedule replacement leaves the prior Schedule active
- Given the Patient is replacing a Dose Schedule and the underlying transaction will fail
- When the Patient saves the new Schedule
- Then the prior Dose Schedule remains the only active Dose Schedule, no future-pending Doses are deleted, no new Dose Schedule is persisted, and a non-technical error is surfaced (BR-001 atomicity)

### `BDD-006` — Archive Medication deletes future-pending Doses and preserves past Doses
- Given an active Medication "Lisinopril 10 mg" with 3 future-pending Doses and 5 past Doses (3 `taken`, 1 `skipped`, 1 `missed`)
- When the Patient archives the Medication
- Then `Medication.isActive = false`, all Dose Schedules of this Medication are deactivated, the 3 future-pending Doses are deleted, all 5 past Doses with their Confirmations remain unchanged, and the Medication no longer appears in the active list (BR-007)
- And a `MedicationArchived` domain event is emitted

### `BDD-007` — Archived Medication remains in Adherence history
- Given the Patient archived "Lisinopril 10 mg" yesterday after it had 5 past Doses (3 `taken`, 1 `skipped`, 1 `missed`) and 1 Confirmation correction
- When the Patient opens the Adherence history view
- Then those 5 past Doses still appear in the per-Dose history with the Medication name rendered, and they contribute to the 30-day Adherence percentage per BR-008

### `BDD-008` — Confirm Taken from a notification action
- Given a Reminder for a `pending` Dose at 08:00 is currently visible on the lock screen with inline "Taken" and "Skipped" actions
- When the Patient taps "Taken"
- Then a Confirmation is recorded with `source = notification-action`, `status = taken`, `confirmedAt = time of tap`; `Dose.status` flips to `taken`; the notification dismisses
- And a `DoseConfirmed` domain event is emitted

### `BDD-009` — Confirm Skipped in-app
- Given a `pending` Dose at 08:00 today is visible on the day's Dose list inside the app
- When the Patient taps "Skipped" on that Dose card
- Then a Confirmation is recorded with `source = in-app`, `status = skipped`, `confirmedAt = time of tap`; `Dose.status` flips to `skipped`; the Dose card updates inline
- And a `DoseConfirmed` domain event is emitted

### `BDD-010` — Retroactive Taken on a missed Dose, within 24h
- Given a Dose has `status = missed` and `now = scheduledAt + 6 hours`
- When the Patient taps "Taken" on that Dose's card
- Then a Confirmation is recorded with `source = retroactive`, `status = taken`, `confirmedAt = now`; `Dose.status` flips to `taken`; that Dose now contributes to the numerator of Adherence per BR-008 (BR-011)

### `BDD-011` — Retroactive command rejected after 24h
- Given a Dose has `status = missed` and `now = scheduledAt + 25 hours`
- When the Patient opens that Dose's card
- Then no "Taken" / "Skipped" action buttons are rendered, a "This Dose can no longer be edited" message is shown, and any service-level retroactive command is rejected (BR-011)

### `BDD-012` — Correct an existing Confirmation in place (taken → skipped)
- Given a Dose has `status = taken` with an existing Confirmation row (`status = taken`, `confirmedAt = T1`)
- When the Patient taps "Skipped" on that Dose card to correct the answer
- Then the same Confirmation row is updated in place to `status = skipped`, `confirmedAt = T2`; no new Confirmation row is created (Dose ↔ Confirmation 1:1 invariant); `Dose.status` flips to `skipped`
- And a `DoseConfirmed` domain event is re-emitted with the same `confirmationId`

### `BDD-013` — Today's Dose list shows correct status and action buttons for each
- Given the Patient has 4 Doses today: a `taken` Dose at 06:00, a `pending` Dose at the current time, a `skipped` Dose earlier, and a `missed` Dose 4 hours ago (within the 24h retroactive window)
- When the Patient opens the day's Dose list
- Then each Dose displays its current status, and only the `pending` Dose and the within-24h `missed` Dose render inline "Taken" / "Skipped" action buttons (BR-011)

### `BDD-014` — Adherence calculation matches BR-008 formula
- Given the Patient has, in the past 30 days: 60 Doses `taken`, 10 `skipped`, 5 `missed`, 5 `pending` with `scheduledAt ≤ now`
- When the Patient views Adherence
- Then Adherence = 60 / (60 + 10 + 5 + 5) × 100 = 75%, computed on demand per BR-008

### `BDD-015` — Adherence returns 0% with no scheduled Doses (zero-denominator edge case)
- Given the Patient has just installed NemoPill and has configured no Medications
- When the Patient opens the Adherence view
- Then Adherence shows 0% and the per-Dose history is empty (BR-008 zero-denominator behavior; never throws)

### `BDD-016` — Late Reminder fires within the 1h grace window
- Given a Dose at `scheduledAt = 08:00` with `status = pending`, the device was off across 08:00, and the device boots at 08:30 (still within `scheduledAt + 1h`)
- When the boot-complete + Doze-exit reconciliation runs
- Then a late Reminder for that Dose fires using distinct text indicating lateness, and `Dose.status` remains `pending` (BR-010)

### `BDD-017` — Beyond 1h grace, Dose transitions to missed with a silent notification
- Given a `pending` Dose at `scheduledAt = 08:00` and the device boots at 09:30 (`scheduledAt + 1.5h`)
- When the boot-complete reconciliation runs
- Then `Dose.status` transitions to `missed`, a single silent low-priority notification is shown, and a `DoseMissed` domain event is emitted (BR-010)

### `BDD-018` — Multiple late/missed Doses during device-off batch into one summary notification
- Given pending Doses at 06:00, 08:00, and 12:00 today, with the device off from 05:30 to 13:30
- When the device wakes at 13:30 and reconciliation runs
- Then a single summary notification is shown describing the multiple late / missed Doses (not three separate notifications), all three Doses are now `missed` (each emitting a `DoseMissed` event), and the day's Dose list reflects the three `missed` statuses (BR-010)

### `BDD-019` — Timezone change regenerates future-pending Doses in the new timezone
- Given the device is in `America/Los_Angeles` with 3 future-pending Doses scheduled for tomorrow at 08:00 / 12:00 / 18:00 LA time (`Dose.generatedInTimezone = America/Los_Angeles`) and 5 past Doses with Confirmations
- When the device timezone changes to `America/New_York` and `ACTION_TIMEZONE_CHANGED` fires
- Then the 3 future-pending Doses are deleted and regenerated for tomorrow at 08:00 / 12:00 / 18:00 New York time, each with `Dose.generatedInTimezone = America/New_York`; the 5 past Doses are unchanged
- And a `DeviceTimezoneChanged` domain event is emitted (BR-012)

### `BDD-020` — Past Doses are unaffected by a timezone change
- Given the Patient has a `taken` Dose from yesterday at 08:00 LA time with `Dose.generatedInTimezone = America/Los_Angeles` and a Confirmation row
- When the device timezone changes to `America/New_York`
- Then the past Dose's `scheduledAt`, `generatedInTimezone`, and Confirmation row are all unchanged (BR-012 preserves past Doses)

### `BDD-021` — DST spring-forward shifts a non-existent local time to the first valid time
- Given the device is in `America/Los_Angeles` with a daily Dose Schedule at 02:30 AM
- When DST spring-forward occurs (02:00 → 03:00, so 02:30 does not exist that day)
- Then the Dose for that day is materialized at 03:00 — the first valid time after the gap — per the `TimeOfDay` DST handling rule in BR-012

## UX Constraints And Rules

- Navigation or workflow rules: Destructive or irreversible actions must be guarded by an explicit confirm step that names the consequence in the Patient's terms — specifically: (a) **Archiving a Medication** shows a confirm prompt explaining "Future-pending Doses will be removed; past Doses and Adherence history are preserved" before BR-007 runs; (b) **Replacing a Dose Schedule** shows a confirm prompt explaining "Past Doses are kept; upcoming Doses will be regenerated for the new schedule" before BR-001 / BR-006 runs. **Retroactive Confirmation** is silently disabled (no buttons) — not blocked with a popup — once `now > scheduledAt + 24h` per BR-011. **Re-activation of an archived Medication** is not exposed in the UI (file 02 forbids it). The day's Dose list and the Adherence history must be reachable from the app's primary navigation in **at most two taps** to support the novice persona.
- Content or copy rules: User-visible text must use the Ubiquitous Language from file 01 exactly: `Medication`, `Dose`, `Dose Schedule`, `Reminder`, `Confirmation`, `Adherence`. **Avoid in copy:** "drug", "pill", "med", "Rx", "regimen", "plan", "prescription", "alert", "alarm", "notification" (use `Reminder`), "compliance" (use `Adherence`), "acknowledgement" / "check-in" (use `Confirmation`). Reminder text has two distinct variants per BR-010: on-time ("Take your 08:00 Lisinopril") and late ("Late: Take your 08:00 Lisinopril"). The locked-Dose message (BR-011 outside 24h) reads "This Dose can no longer be edited." Validation errors must be inline next to the offending input and phrased without technical jargon (e.g., "End date must be on or after start date" rather than "BR-002 violated"). **Localization: English-only for MVP** — no other locales planned in this phase.
- Responsiveness or device constraints: The app must be **fully responsive across Android phone and tablet form factors**, including standard portrait, landscape, and foldable / dual-screen layouts. The day's Dose list, the Add/Edit Medication flow, the Adherence history, and individual Dose cards must all reflow gracefully across screen widths from compact phone (≤ 600 dp) through medium (600–840 dp) up to expanded tablet/foldable (≥ 840 dp), without text truncation, overflowing controls, or fixed-pixel layouts. Both portrait and landscape orientations are supported; orientation changes do not lose form input or in-progress Confirmation state. The app honors the system font-size setting (per the accessibility section) and adapts at all supported screen sizes. Minimum target: a contemporary Android version that supports notification actions reliably (specific `minSdk` to be set in file 12 / `_build/`). The app is **fully offline-first** — no functionality requires network connectivity, and there is no loading state for "fetching from server." The lock-screen Reminder must be **glanceable in under 2 seconds** and actionable from the lock screen without unlocking the device (i.e., notification actions must work on the lock screen). Out of scope for the MVP: Wear OS, Android Auto, Android TV, and Chromebook-specific behaviors.
- Accessibility expectations: The app must support TalkBack screen-reader navigation: every actionable element has a programmatic role and accessible label using the Ubiquitous Language ("Confirm Dose taken" rather than just "Taken"); status changes (a Dose flipping `pending → taken`, or a Reminder firing) are announced via accessibility live regions. Color contrast meets WCAG 2.1 AA (≥ 4.5:1 for normal text, ≥ 3:1 for large text and UI controls). All touch targets are ≥ 48 dp × 48 dp. The UI honors the Patient's system font-size setting up to and including the Android "Largest" setting without text truncation in the day's Dose list, the Reminder body, or the Adherence summary. Validation error messages are programmatically associated with their input field (`labelFor` / `accessibilityLabeledBy`) so screen readers announce them on focus. Reminders provide a vibration pattern in addition to sound for hearing-impaired Patients and respect system "Do Not Disturb" exemptions only insofar as Android allows for time-critical Reminders.
- Design system or visual language rules: Deferred to `_context/11_visual_language_and_design_system.md` (which is in the planned optional set per the file-01 handoff log but not yet contextualized). File 03 captures *behavior* and *content rules* in the Ubiquitous Language; visual tokens (color palette, typography, spacing scale, component library) belong in file 11. Until file 11 is contextualized, the Developer role should follow Material Design 3 defaults for an Android app and not invent custom visual language.

## User-Visible Acceptance Criteria

- `AC-001` — Patient can complete the primary journey end-to-end within the first week. From a clean install, the Patient can add a Medication, configure its Dose Schedule, receive a Reminder at the configured time, confirm a Dose Taken from the Reminder, and view that Confirmation in their Adherence history — without needing external help or documentation.
- `AC-002` — Reminders fire reliably and recover from device-off / Doze. Reminders fire at `Dose.scheduledAt` within a tolerance acceptable for typical morning, midday, and evening Dose times, surviving device reboot, Doze idle, and app updates (BR-004). When the device was off across one or more `scheduledAt` times, on wake the Patient sees either a late Reminder (within 1h, BR-010) or a single batched summary of late / missed Doses — never duplicates and never silent failures.
- `AC-003` — Confirmation has parity across channels. The Patient can confirm a Dose Taken or Skipped from either the lock-screen Reminder or the in-app Dose list, and the resulting Adherence history shows the same Dose status regardless of which channel was used (UC-004 vs UC-005).
- `AC-004` — Past data is never silently mutated. When the Patient replaces a Dose Schedule (UC-002) or archives a Medication (UC-003), all past Doses and their Confirmations remain visible and unchanged in the Adherence history — the Patient's record of what they did is never rewritten (BR-006, BR-007).
- `AC-005` — Retroactive Confirmation works within 24h and locks afterward. For up to 24 hours after a Dose's `scheduledAt`, the Patient can mark a `missed` Dose Taken or Skipped from the day's Dose list. After 24 hours, action buttons disappear and the Dose card displays "This Dose can no longer be edited" (BR-011).
- `AC-006` — Adherence numbers are correct and computed on demand. The Adherence percentage shown to the Patient over any window matches the BR-008 formula (`taken ÷ (taken + skipped + missed + past-pending) × 100`), is recomputed from persisted Dose data on view, and shows 0% honestly when no scheduled Doses have occurred yet.
- `AC-007` — Travel doesn't break tomorrow's Reminders. After the Patient crosses timezones (or DST shifts the device clock), upcoming Doses fire at the same wall-clock times in the new local timezone (BR-012), and past Doses' history is unchanged.
- `AC-008` — Offline-first works at every step. At every step of the primary journey — adding Medications, firing Reminders, recording Confirmations, viewing Adherence — the app functions correctly with no network connectivity. There is no "no internet" failure state and no loading spinner waiting on a server.

## Edge Cases To Preserve In Tests

- `EC-001` — **Notification permission revoked after initial grant.** The Patient grants notification permission at first launch but later revokes it in Android system settings. Subsequent Reminders cannot be delivered as system notifications, but the in-app Dose list must continue to surface `pending` Doses with action buttons. The app should detect the missing permission on next foreground and show a non-blocking banner offering to re-request it, never silently leaving Doses unreminded.
- `EC-002` — **Two Medications scheduled at the same wall-clock time.** The Patient has Lisinopril 10 mg at 08:00 and Metformin 500 mg at 08:00. At 08:00 the Scheduler must fire **two distinct Reminders**, each tied to its own `Dose`, each with independent action buttons. Tapping "Taken" on one must not affect the other Dose's `status`. Adherence counts each Dose independently per BR-008.
- `EC-003` — **Patient manually changes the device clock without crossing a timezone.** The Patient opens Android system settings and rolls the clock forward 6 hours (or back). `ACTION_TIMEZONE_CHANGED` is **not** emitted in this case, so BR-012's regeneration path does not run. The Scheduler should still detect that real time has jumped and reconcile via the boot-complete / Doze-exit / app-foreground sweep paths defined in BR-004 and BR-010 (firing late Reminders within the 1h grace and transitioning otherwise to `missed`). Tests should confirm that no duplicate Reminders are produced for the affected Doses.
- `EC-004` — **App update during a pending Reminder.** A `Dose` with `scheduledAt = 08:00` and `status = pending` exists; the app is updated (e.g., via Play Store) at 07:55, which clears the in-process state of `AlarmManager` / `WorkManager`. On the post-update first-launch path, the Scheduler must re-register the alarm so the 08:00 Reminder still fires (BR-004 "surviving app updates"). Tests should confirm exactly one Reminder per Dose, no duplicates, no missed firings.
- `EC-005` — **DST fall-back duplicate hour fires the Reminder exactly once.** On DST fall-back day, the local clock reads "01:30" twice (once in DST, once in standard time). A Dose scheduled at 01:30 must materialize and fire **once** at the first occurrence per BR-012 — not zero times, not twice. Tests should pin this behavior because it is a known DST trap.
- `EC-006` — **Concurrent Confirmation taps on the same Dose.** The Patient taps "Taken" on the lock-screen Reminder and almost simultaneously the same Dose's "Taken" button on the in-app card (or rapidly double-taps a single button). The Dose ↔ Confirmation 1:1 invariant must hold — exactly one Confirmation row exists, and both taps converge on the same final state without producing two rows or a transient "two Confirmations" window.
- `EC-007` — **Dose Schedule's last day generates final Doses but nothing beyond `endDate`.** The Patient sets a Dose Schedule with `endDate = 2026-06-15`. On 2026-06-15, the daily horizon-extension job (BR-009) materializes any remaining Doses up to but not after 23:59 local time on that day. From 2026-06-16 onward, no further Doses are generated for this Schedule, even though the daily job continues to run for other Medications. Past Doses from this Schedule remain in Adherence history.
