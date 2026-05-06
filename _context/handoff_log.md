# Handoff Log

Structured record of role turns. Newest entries appended at the bottom.

## 2026-05-06T12:45:19Z — Architect — start-working

- **Outcome:** `_context/01_product_vision_and_scope.md` written and verified (zero unresolved placeholders).
- **Files written:** `_context/01_product_vision_and_scope.md`
- **Optional files agreed for inclusion (full project plan):** 10, 11, 12, 13. Skipping 14 and 15.
- **Validator result:** Not yet run (will be run after all files are contextualized).
- **Open questions / review markers:**
  - Open Product Q1 captured in file 01: behavior when device is off / Reminder fails to fire (rollover vs. missed vs. late-reminder-on-wake).
  - Open Product Q2 captured in file 01: retroactive Dose logging (yes/no, and time window if yes).
- **Decisions made worth carrying forward into later files:**
  - Project name: **NemoPill Medication Reminder**.
  - Three Bounded Contexts: **Medication Management**, **Scheduling & Reminders**, **Adherence Tracking**.
  - Ubiquitous Language fixed: Patient, Medication, Dose, Dose Schedule, Reminder, Confirmation, Adherence (plus Scheduler as a system actor).
  - Primary success metric: **Reminder-to-Confirmation rate ≥ 80%** over a rolling 30-day window.
  - Secondary success metric: framework completeness (design phase) + per-milestone code-quality review (coding phase).
  - Delivery context: **solo-developed Android application** — no cloud, no multi-user, no caregiver features.
  - Out of scope (hard boundaries): multi-user/caregiver accounts; HIPAA-covered handling; drug-interaction or allergy warnings.
  - Key assumption to validate early: Android `AlarmManager` / `WorkManager` reliability under Doze mode.
- **Process change applied this session:** `CLAUDE.md` updated with a "Contextualization Sessions: One File Per Conversation" section that mandates per-file scope, prior-state detection at session start, and a clean stop after each file. Future `/start-working` invocations should follow that protocol.
- **Next role:** Architect — invoke `/start-working` in a new conversation to contextualize `_context/02_domain_model_and_business_rules.md`.

## 2026-05-06T21:01:23Z — Architect — start-working

- **Outcome:** `_context/02_domain_model_and_business_rules.md` written and verified (zero unresolved placeholders in file 02).
- **Files written:** `_context/02_domain_model_and_business_rules.md`
- **Validator result:** Pass for file 02 (no `{{REQUIRED}}` / `{{OPTIONAL}}` / `{{EXAMPLE}}` markers remain). Overall framework still incomplete — files 03–09 remain missing, as expected by the per-file session protocol.
- **Decisions made worth carrying forward into later files:**
  - **Entities (4):** `Medication`, `Dose Schedule`, `Dose`, `Confirmation`. `Patient` was considered and explicitly **rejected** as an entity for MVP; multi-user remains out of scope per file 01.
  - **Value Objects (6):** `Strength`, `Form`, `TimeOfDay`, `DateRange`, `Frequency` (tagged-union: `daily` / `every-n-hours` / `weekly-on-days` / `every-n-days`), `DoseQuantity`.
  - **Aggregates (2):** `Medication aggregate` (root: `Medication`), `Dose aggregate` (root: `Dose`).
  - **Confirmation cardinality decision: Option A — strict 1:1 mutable.** A correction overwrites the Confirmation in place. No audit trail in MVP. (Drove the Dose ↔ Confirmation invariant.)
  - **Business rules:** 9 rules, `BR-001` through `BR-009`, including the binding definition of Adherence (BR-008) that ties to file 01's primary success metric.
  - **Dose generation horizon: 3 days,** capped at `Dose Schedule.endDate` (BR-009). Daily horizon-extension job.
  - **Archive behavior: future-pending Doses are deleted** (no `cancelled` status added) (BR-007). Past Doses preserved.
  - **Re-activation of an archived Medication: not allowed in MVP (Option Y).** To resume a discontinued Medication, the Patient creates a new Medication.
  - **Domain events: 7 events** declared (`MedicationCreated`, `DoseScheduleReplaced`, `MedicationArchived`, `DoseMaterialized`, `ReminderFired`, `DoseConfirmed`, `DoseMissed`) — logical cross-context contracts; transport is in-process for MVP, no message bus.
  - **Vocabulary: `Dose Schedule` is canonical** (matches file 01); `Schedule` was considered and rejected to preserve the Ubiquitous Language.
- **Open questions / review markers left in file 02:**
  - **Open Q1 (Reminder-misfire / device-off behavior)** — already deferred from file 01. Surfaces in: `BR-004` failure-behavior; the `pending → missed` policy's grace period `G`; the corresponding `Dose: pending → missed` state transition; and the `DoseMissed` domain event trigger.
  - **Open Q2 (retroactive Dose logging — yes/no, time window)** — already deferred from file 01. Surfaces in: `Confirmation.source` enum (`retroactive` value); the `Dose: missed → taken/skipped` state transition.
  - **New: `TimeOfDay` timezone/DST behavior** — flagged as a candidate Open Q. Currently stored in device local timezone; travel/DST edge cases not yet specified. Likely deserves explicit treatment in file 10 (NFRs) or a dedicated ADR.
- **Next role:** Architect — invoke `/start-working` in a new conversation to contextualize `_context/03_user_experience_and_use_cases.md`. Per CLAUDE.md's per-file session protocol, this conversation stops here.

## 2026-05-06T22:05:22Z — Architect — open-question resolution (Q1, Q2, Q3)

- **Outcome:** Resolved three open product questions and updated already-written context files accordingly. No new framework file was contextualized in this turn — this was a follow-up resolution turn that cleared review markers in files 01 and 02.
- **Files modified:** `_context/01_product_vision_and_scope.md`, `_context/02_domain_model_and_business_rules.md`.
- **Validator result:** Pass for files 01 and 02 (zero `{{REQUIRED}}` / `{{OPTIONAL}}` / `{{EXAMPLE}}` markers and zero `// review` markers in either file). Overall framework still incomplete — files 03–09 remain missing, as expected.
- **Resolutions captured:**
  - **Q1 — Reminder-misfire / device-off behavior:** Late-Reminder window of `G = 1 hour`. Within window, fire late Reminder with distinct text at next OS opportunity. Beyond window, transition to `missed` with a single silent low-priority notification. Multiple late/missed Doses queued during a device-off period are batched into one summary at wake.
  - **Q2 — Retroactive Dose logging:** Allowed within `R = 24 hours` of `Dose.scheduledAt`. Confirmation gets `source = retroactive`, `confirmedAt = time of tap` (no manual time entry in MVP). Retroactive `taken` Confirmations count toward Adherence identically to on-time Confirmations.
  - **Q3 — `TimeOfDay` semantics across timezones and DST** (newly raised during file-02 contextualization, resolved in same session): Local wall-clock follows device timezone. `Dose.scheduledAt` stored as UTC `Instant`; new `Dose.generatedInTimezone` (IANA TZ ID) added. On `ACTION_TIMEZONE_CHANGED`, future-pending Doses are deleted and regenerated in the new timezone (transactional). DST: spring-forward shifts to first valid time after the gap; fall-back fires once at the first occurrence.
- **New rules and events added to file 02:**
  - `BR-010` Late-Reminder Window.
  - `BR-011` Retroactive Logging Window.
  - `BR-012` Device Timezone Change Regenerates Future-Pending Doses.
  - `DeviceTimezoneChanged` domain event.
  - `Dose.generatedInTimezone` attribute added to the `Dose` entity.
- **Review markers cleared in file 02 (9 spots, mapped to the resolutions):**
  - `Confirmation.source` enum (Q2), `Dose` lifecycle notes (Q2), `TimeOfDay` validation rule (Q3), `BR-004` failure behavior (Q1), `pending → missed` policy notes (Q1), `Dose: pending → missed` state-transition guard (Q1), `Dose: missed → taken/skipped` state-transition guard (Q2), `Confirmation: (no row) → row exists` state-transition guard (Q2), `DoseMissed` event trigger (Q1).
- **ADRs to backfill when `_context/09_decision_log.md` is contextualized** (3 candidates):
  - **ADR — Late-Reminder Window of 1 hour and `pending → missed` policy.** Captures Q1 resolution; rationale: clinical actionability without colliding with next typical Dose.
  - **ADR — Retroactive Confirmation Window of 24 hours.** Captures Q2 resolution; rationale: same-day correction support, prevents stale back-edits inflating Adherence.
  - **ADR — Device-Timezone-Anchored Dose Schedules with Regenerate-on-Change.** Captures Q3 resolution; rationale: preserves Patient mental model of "morning meds in my morning"; cost is bent drug-spacing on travel days, accepted for MVP.
- **Still open after this turn:**
  - `Dose.scheduledAt` storage in code is now nailed (UTC `Instant`), but the persistence-layer mapping (Room/SQLite type for `Instant` and IANA TZ ID) is an implementation choice for file 04 (Solution Architecture).
  - The Patient-facing copy for the late-Reminder text and the silent missed notification ("Late: Take your 8 AM Lisinopril", etc.) is illustrative; final wording belongs in file 11 (Visual Language and Design System).
- **Next role:** Architect — invoke `/start-working` in a new conversation to contextualize `_context/03_user_experience_and_use_cases.md`. Per CLAUDE.md's per-file session protocol, this conversation stops here.
