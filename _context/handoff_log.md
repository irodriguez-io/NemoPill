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
