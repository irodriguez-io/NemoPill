# Product Vision And Scope

## How To Fill This File

- Replace every placeholder with project-specific language.
- Keep the vocabulary aligned with `02_domain_model_and_business_rules.md` and `03_user_experience_and_use_cases.md`.
- Describe the business problem and scope, not the implementation details.

## Project Snapshot

- Product or initiative name: NemoPill Medication Reminder
- One-sentence summary: NemoPill Medication Reminder sends scheduled reminders to adults self-managing recurring medication at home, helping them take the right dose at the right time and improving adherence to their prescribed treatment plan.
- Primary business problem: NemoPill addresses missed doses, poor treatment adherence, and caregiver burden, especially for patients who have difficulty remembering their medication schedule reliably.
- Primary users or customers: Adults under medical treatment who self-manage recurring medication at home.
- Key stakeholders: Not applicable (solo project; no formal stakeholders defined).
- Delivery context: Personal side project; the context is self-learning how to leverage AI into software development in a structured way.

## Goals And Success Measures

| Goal | Why It Matters | Success Metric | Target |
| --- | --- | --- | --- |
| Improve medication adherence for users self-managing recurring medication at home. | Without adherence, prescribed treatment plans don't work as intended — improving it is the core value the product must deliver. | Reminder-to-Confirmation rate — % of Reminders that result in a "taken" Confirmation. | ≥ 80% Reminder-to-Confirmation rate over a rolling 30-day window. |
| Validate the AI-assisted SDF (Software Design Framework) workflow itself. | This project is also a learning vehicle for AI-assisted, role-based software development; demonstrating that the framework can take a real product from vision to working code without skipping rigor is a goal in its own right. | Framework completeness during design phase (all `_context/` files filled, validator passing, ADRs logged for architectural decisions) AND code quality assessment after each milestone during the coding phase. | Design phase: 100% of `_context/` files contextualized and validator passes before any code is written. Coding phase: each milestone passes a self-administered quality review (tests-first discipline, clean architecture boundaries respected, no Apply Mode without `Approved for apply`) before proceeding to the next milestone. |

## In Scope

- Define a Medication and its Dose Schedule (drug name, dose, frequency, start/end dates).
- Deliver scheduled Reminders at the configured time for each Medication Dose.
- Confirm a Dose as taken or skipped.
- View Adherence history over time.

## Out Of Scope

- No multi-user / caregiver accounts — single-user only; no shared accounts, no caregiver remote view.
- No HIPAA-covered data handling guarantees — clarify the product is for personal use, not a clinical tool.
- No drug-interaction or allergy warnings — that's clinical decision support and out of scope.

## Users, Actors, And Their Jobs

| Actor | Description | Primary Job To Be Done | Frustrations Or Risks |
| --- | --- | --- | --- |
| Patient | An adult under medical treatment who self-manages one or more recurring medications at home, without a caregiver or clinician administering doses. | Take the right medication, in the right dose, at the right time, every day — without having to remember the schedule on their own. | Forgets doses, doubles up after missing one, or loses confidence in whether the day's doses were taken — leading to anxiety, treatment failures, and avoidable medical visits. Currently relies on memory, paper checklists, or generic phone alarms that don't model dose schedules and don't capture whether the dose was actually taken. |
| Scheduler | An internal time-based system actor that triggers Reminders at the configured Dose times, independent of the Patient opening the app. | Fire each scheduled Dose Reminder at its configured time, exactly once per Dose, even if the app is not open. | Risks include missed firings (device asleep, OS throttling background work), duplicate firings (re-scheduling bugs), and clock-drift or timezone errors that misplace Doses. |

## Ubiquitous Language

List the terms the team and the agent must use consistently. Remove duplicate synonyms.

| Term | Definition | Avoid Saying | Notes |
| --- | --- | --- | --- |
| Medication | A specific drug the Patient is prescribed to take, identified by name, strength (e.g., 10 mg), and form (e.g., tablet, capsule, liquid). A Medication is the *what*; its Dose Schedule is the *when*. | Drug, pill, prescription, Rx, med — use "Medication" consistently. | Always capitalize "Medication" when referring to the domain entity. Example: "The Patient adds a Medication and configures its Dose Schedule." |
| Dose | A single quantity of a Medication taken at a specific scheduled time (e.g., "10 mg at 8:00 AM"). | Pill, tablet (those refer to physical form, not the scheduled event). | A Dose is an *event*: it has a scheduled time, a Confirmation status, and belongs to a Medication. |
| Dose Schedule | The rule that generates Doses for a Medication: frequency, time-of-day, start date, and (optionally) end date. | Regimen, plan, prescription. | A Medication has exactly one active Dose Schedule at a time. Editing the schedule does not retroactively change past Doses. |
| Reminder | The notification fired by the Scheduler at a Dose's scheduled time, prompting the Patient to take that Dose. | Alert, alarm, notification (use "Reminder" consistently). | One Reminder corresponds to one Dose. Snoozing or dismissing the Reminder does not change the Dose's Confirmation status. |
| Confirmation | The Patient's recorded response that a Dose was *taken* or *skipped*. | Acknowledgement, response, check-in. | Confirmation states: `taken`, `skipped`, `pending` (no response yet). Adherence is computed only from confirmed Doses. |
| Adherence | The percentage of scheduled Doses confirmed `taken` over a defined window (e.g., rolling 30 days). | Compliance (clinical jargon, avoid), success rate. | The primary success metric is *Reminder-to-Confirmation rate*, which is related but distinct: it measures Reminder response, not Dose-taking outcome. |

## Bounded Contexts

| Context | Purpose | Owned Concepts | Inputs | Outputs |
| --- | --- | --- | --- | --- |
| Medication Management | Maintain the Patient's catalog of Medications and the Dose Schedule rules that define when each Medication's Doses occur. | `Medication` entity, `Dose Schedule` entity, schedule-validation rules (e.g., a Medication has exactly one active Schedule). | Patient inputs (add/edit/discontinue Medication; configure Schedule). | Schedule changes published to **Scheduling & Reminders** (so it can regenerate upcoming Doses). |
| Scheduling & Reminders | Materialize upcoming Doses from active Dose Schedules and fire a Reminder for each Dose at its scheduled time. | `Dose` entity (scheduled time, status), `Reminder` event, scheduling/firing logic, snooze rules. | Schedule changes from **Medication Management**; system clock; OS notification permissions. | Reminder notifications delivered to the Patient; `Dose` records (with `pending` status) handed to **Adherence Tracking**. |
| Adherence Tracking | Record Patient Confirmations against Doses and compute Adherence metrics (including Reminder-to-Confirmation rate). | `Confirmation` entity (`taken`/`skipped`/`pending`), Adherence calculations, history queries. | Patient Confirmations (taken/skipped); `Dose` records from **Scheduling & Reminders**. | Adherence metrics and history views surfaced to the Patient. |

## Business Constraints And Assumptions

- Regulatory or legal constraints: Not a regulated medical device under FDA Software as a Medical Device (SaMD) guidance, since it provides no clinical decision support and offers no diagnostic, therapeutic, or treatment recommendations. Not HIPAA-covered (no covered entity, no PHI exchange with providers). Privacy: medication data is sensitive personal data; the app must not transmit or share it without explicit Patient consent. App-store policies (Apple HealthKit / Google Play health-data rules) may still apply if storing data locally on those platforms.
- Operational constraints: Solo developed Android application. No formal support hours, no on-call. Reminders must fire reliably even when the developer is unavailable, since missed Reminders directly harm the Patient's Adherence — reliability of the Scheduler is more important than feature velocity.
- Commercial constraints: Zero budget. No paid services, vendors, or third-party SaaS dependencies unless they offer a free tier sufficient for a single-user app. No commercial distribution or monetization in this phase.
- Assumptions the team is making: (1) Android's local notification + scheduled-alarm APIs (e.g., AlarmManager / WorkManager) are reliable enough to fire Reminders on time even when the app is backgrounded or the device is in Doze mode. (2) The Patient will keep the device charged and within reach during their scheduled Dose times. (3) Local on-device storage is sufficient — no cloud sync required for the MVP. (4) The Patient is willing to manually configure their Medications and Dose Schedules without import from prescriptions.

## Resolved Product Decisions

These were the open product questions raised during file 01 contextualization, resolved in a later session (2026-05-06). ADRs to be backfilled in `_context/09_decision_log.md` when that file is contextualized.

- **Q1 — Reminder-misfire / device-off behavior.** Resolved as a **late-Reminder window of `G = 1 hour`**. If a Dose is still `pending` and `now ≤ Dose.scheduledAt + 1 hour`, the Reminder fires at the next OS opportunity (boot complete, Doze exit, app foreground) using a "late" notification text distinct from the on-time text. After `scheduledAt + 1 hour`, the Dose flips to `missed` and a single silent low-priority notification is shown. Multiple late Doses queued during a device-off period are batched into one summary notification at wake; within-window Doses still appear in the day's list as `pending` until taken.
- **Q2 — Retroactive Dose logging.** Resolved as **allowed within `R = 24 hours` of `Dose.scheduledAt`**. The Patient can convert `missed → taken/skipped` up to 24 hours after the scheduled time; beyond that the Dose is locked at `missed` and action buttons are removed from the Dose card. The Confirmation gets `source = retroactive` and `confirmedAt = time of tap` (no manual time entry in MVP). Retroactive `taken` Confirmations count toward Adherence the same as on-time Confirmations.
- **Q3 — `TimeOfDay` semantics across timezones and DST** (raised during file 02 contextualization). Resolved as **local wall-clock, follows device timezone**. `Dose.scheduledAt` is stored as UTC `Instant`, materialized from the device's current timezone at generation time and recorded in a new `Dose.generatedInTimezone` (IANA TZ ID). On `ACTION_TIMEZONE_CHANGED`, future-pending Doses are deleted and regenerated for the new timezone (transactional). DST: spring-forward shifts a Dose to the first valid time after the gap (e.g., 02:30 → 03:30); fall-back fires the Reminder once at the first occurrence.
