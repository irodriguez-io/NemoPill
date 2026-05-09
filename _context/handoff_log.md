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

## 2026-05-08T21:36:25Z — Architect — start-working

- **Outcome:** `_context/03_user_experience_and_use_cases.md` written and verified (zero unresolved placeholders in file 03).
- **Files written:** `_context/03_user_experience_and_use_cases.md`.
- **Optional files included this turn:** none (file 03 stands alone; the 5 optional sections in the template that touch design system, content, etc., were either filled or explicitly deferred to file 11).
- **Validator result:** Pass for files 01, 02, and 03 (no `{{REQUIRED}}` / `{{OPTIONAL}}` / `{{EXAMPLE}}` markers remain in any of them, no `// review` markers in file 03). Overall framework still incomplete — files 04–09 remain missing, as expected by the per-file session protocol.
- **Volume note:** the framework template's 48-placeholder skeleton was deliberately expanded during the interview (with the user's approval at each section) to **218 resolved placeholders** in the final file: 7 Key Journey rows (vs. template's 2), 9 Use Case rows (vs. 2), 21 BDD scenarios (vs. 1), 8 Acceptance Criteria (vs. 3), and 7 Edge Cases (vs. 2). Personas table row 2 captured as `Not applicable` per single-actor scope.
- **Decisions made worth carrying forward into later files:**
  - **Experience channels** locked at exactly two: (1) Android app UI and (2) Android system notifications (lock-screen + notification-shade Reminders with inline action buttons). No Wear OS, widget, email, SMS, or voice.
  - **Primary user journey** framed broadly as the **first-week onboarding loop** (add Medication → Schedule → Doses materialize → Reminders fire → Confirmations recorded → Adherence visible). This becomes the QA-testable contract for "the product works."
  - **UX success signal** is dual-framed: behavioral (Patient stops having to remember) AND emotional (Patient feels confident, no anxiety about missed Doses or doubling up).
  - **Single human persona: `Patient`**, designed as **novice** smartphone fluency. Five environment constraints to honor everywhere: offline-first, screen-reader (TalkBack) compatible, low-light / glanceable, one-handed friendly, battery / Doze-tolerant.
  - **Responsiveness scope expanded to fully responsive across phone, tablet, and foldables** — explicitly per user override of the initial proposal. This widens the layout / breakpoint surface area for the Developer role and should be reflected in `_context/04` UI architecture and `_context/11` design tokens.
  - **Seven Key Journeys (`J-001` through `J-007`)**: J-001 = primary onboarding, J-002 = steady-state daily loop, J-003 = Schedule replacement (BR-001 / BR-006), J-004 = Archive Medication (BR-007), J-005 = retroactive Confirmation (BR-011), J-006 = late-Reminder / device-off recovery (BR-010), J-007 = timezone change (BR-012). Each Journey traces explicitly to one or more of the file-02 Business Rules.
  - **Nine Use Cases (`UC-001` through `UC-009`)**: UC-004 (Confirm Dose from Reminder) and UC-005 (Confirm Dose in-app) are kept distinct so file 04 can model the two `Confirmation.source` paths separately even though both produce the same Dose state. UC-007 (Correct existing Confirmation) is the in-place mutation path per Option A.
  - **21 BDD scenarios** are the testable backbone — each maps to a Business Rule, an aggregate invariant, or an Adherence calculation. These are the QA contract.
  - **8 Acceptance Criteria** (AC-001 through AC-008) summarize the "Patient gets the product they were promised" view. AC-002 is the one most likely to be hard to verify under real OS conditions (Doze, battery saver) and should be flagged for early prototype validation.
  - **7 Edge Cases** preserved in the test suite, including manual clock changes (EC-003), DST fall-back (EC-005), and concurrent Confirmation taps (EC-006). EC-006 is the strongest argument for using a transactional write path for Dose ↔ Confirmation in file 04.
- **Open questions / review markers left in file 03:** none (zero `// review` markers).
- **Items deferred to later files (do not re-litigate):**
  - **`minSdk` choice** for Android — referenced in UX Constraints as "to be set in file 12 / `_build/`."
  - **Visual language tokens** (color palette, typography, spacing, component library) — explicitly deferred to file 11.
  - **Persistence-layer mapping** for `Instant` and IANA TZ ID (Room / SQLite types) — flagged in the prior handoff turn; lives in file 04.
  - **Concrete Reminder copy strings** (full localized wording) — lives in file 11.
- **ADR candidates surfaced in this turn (to backfill into `_context/09_decision_log.md` when contextualized):** none new beyond the three already identified in the 2026-05-06 open-question resolution turn (Late-Reminder Window, Retroactive Window, Timezone-Anchored Schedules). File 03 reused those three rules; it did not introduce additional durable architecture decisions.
- **Next role:** Architect — invoke `/start-working` in a new conversation to contextualize `_context/04_solution_architecture.md`. Per CLAUDE.md's per-file session protocol, this conversation stops here.

## 2026-05-09T20:12:34Z — Architect — start-working

- **Outcome:** `_context/04_solution_architecture.md` written and verified (zero unresolved placeholders in file 04, zero `// review` markers).
- **Files written:** `_context/04_solution_architecture.md`.
- **Optional files included this turn:** none (file 04 stands alone; the optional files agreed for the project — 10, 11, 12, 13 — are still pending and remain queued for their own per-file sessions).
- **Validator result:** Pass for files 01, 02, 03, and 04 (no `{{REQUIRED}}` / `{{OPTIONAL}}` / `{{EXAMPLE}}` markers remain in any of them; validator exits non-zero only because files 05–09 are still missing, as expected by the per-file session protocol).
- **Volume note:** the framework template's 59-placeholder skeleton was deliberately expanded during the interview (with the user's approval at each section) — most notably the Module Or Component Map (template's 2 rows → 6 rows), the Interface And Adapter Strategy (template's 2 rows → 8 rows), and the Technology And Runtime Decisions table (template's 2 rows → 9 rows). The Architecture Risks And Trade-Offs section similarly expanded from 2 to 4 bullets.
- **Decisions made worth carrying forward into later files:**
  - **Architecture style:** Clean Architecture; presentation pattern (MVVM vs. MVI) deliberately left unspecified at the layer level — Compose + `ViewModel` + `StateFlow` is the implementation pattern picked under Technology Decisions.
  - **Key architectural constraint** is **Reminder reliability under Android background-execution restrictions** (Doze, App Standby, battery optimization, reboot). When two design choices conflict, this is the tiebreaker.
  - **Four Clean Architecture layers**: `Presentation` / `Application` / `Domain` / `Infrastructure`. **Presentation owns OS callbacks** (`BroadcastReceiver`, `WorkManager.Worker`, notification-action handlers) — they are entry points, same as a screen tap. Domain is pure-Kotlin only; clock and timezone are *ports*, never read directly in Domain.
  - **Six Gradle modules** (feature-first + shell + shared kernel): `:app`, `:core`, `:medication-management`, `:scheduling`, `:notifications`, `:adherence-tracking`. The `scheduling-reminders` Bounded Context from file 01 is **split across two Gradle modules** — `:scheduling` for the lifecycle/clock-arithmetic side, `:notifications` for the user-facing presentation side; the boundary between them is Reminder *firing* (scheduling decides *that* a Reminder must fire; notifications decides *what the Patient sees*). This split was a user amendment during the interview and should be respected by file 06's integration map and file 11's content-and-tone guidance.
  - **Cross-module communication is event-driven only.** Feature modules never import each other's Domain or Application packages; they emit and subscribe to domain events on `DomainEventPublisher` (in `:core`). MVP transport is in-process (`MutableSharedFlow`), no message bus, no Room-as-outbox.
  - **`:core` is a `kotlin("jvm")` Gradle module** — pure Kotlin, zero Android dependency enforced by the build. Hosts `Result<T, E>`, ID newtypes, `ClockPort` / `TimezonePort` interfaces and their `SystemClockAdapter` / `SystemTimezoneAdapter` defaults, and the `DomainEvent` / `DomainEventPublisher` / `InProcessEventBus` triad. (User initially asked to collapse `:core` into `:app`; reverted to a separate `:core` module after the Repository Structure step revealed the dependency-cycle issue.)
  - **`MedicationRepository` is one repository per aggregate root** (DDD-correct). `Medication` and its `DoseSchedule`s are fronted by a single repository (file-02 `Medication aggregate`), and the BR-001 / BR-007 atomicity runs inside `room.withTransaction { }`. The original draft had separate `MedicationRepository` and `DoseScheduleRepository` ports; this was collapsed during the interview at the user's request.
  - **Eight ports (interfaces) total**: `MedicationRepository`, `DoseRepository`, `ConfirmationRepository`, `SchedulerPort`, `NotificationPort`, `ClockPort`, `TimezonePort`, `DomainEventPublisher`. Use cases (interactors) are *not* ports themselves — they are the Application layer's public API consumed directly by Presentation.
  - **Technology stack pinned (architecture-level only):** Kotlin (Kotlin/JVM); Jetpack Compose + `ViewModel` + `StateFlow`; Room with `withTransaction { }` and `TypeConverter`s for `Instant` / `ZoneId`; Kotlin Coroutines + Flow / StateFlow / SharedFlow; **`AlarmManager` (`setExactAndAllowWhileIdle` / `setAlarmClock`) for exact-time Reminder firing** + **`WorkManager` for the daily horizon-extension job and the `pending → missed` sweep fallback**; `BroadcastReceiver` (manifest-declared) for `BOOT_COMPLETED` and `ACTION_TIMEZONE_CHANGED`; **Hilt (KSP)** for DI; **JUnit 4 + kotlinx-coroutines-test + Truth + Turbine + Room in-memory + Compose UI Test + Robolectric** as the testing toolchain.
  - **Functional programming rules (5):** (1) Domain purity — every dependency including clock and timezone passed in as parameter; verification rule is "Domain compiles with only `kotlin-stdlib` + `junit` + `truth` on its classpath." (2) Immutability — `data class` with `val` only; state changes return new instances; immutable collections in Domain. (3) Side-effect isolation — all I/O at Application layer; Domain functions are non-suspending by default. (4) **Two-tier error policy** — sealed `Result<T, E>` in `:core` for expected business failures (every BR has a Result variant), exceptions only for unexpected/programmer-error situations and converted at the Application boundary to `Result.Err.Unexpected`; no `kotlin.Result` (rejected as exception-flavored). (5) Shared-utility admission — two-of-three rule; no `util` package; helpers private/internal to feature modules unless promoted to `:core`.
  - **Persistence-mapping question deferred from file 02 is now resolved:** `Instant` stored as UTC epoch millis via Room `TypeConverter`; `ZoneId` (IANA TZ ID) stored as the IANA string via Room `TypeConverter`. Single `NemoPillDatabase` instance for the whole app (declared in `:app::di::AppModule`), with all four feature DAOs registered on it — required to preserve the Dose-aggregate cross-module transaction.
  - **Cross-cutting Presentation screens** (the today's-Dose list and the Adherence history view) live in `:app::presentation/`, not in any feature module — they join data from multiple Bounded Contexts and are explicitly named as **non-feature-owned** views.
- **Open questions / review markers left in file 04:** none (zero `// review` markers).
- **Items deferred to later files (do not re-litigate):**
  - **`minSdk` / `targetSdk` / `compileSdk`**, specific library versions (`gradle/libs.versions.toml`), signing config, CI runner choice, log aggregation, crash reporting vendor, distribution channel — file 12.
  - **Material 3 vs. Material 2 token set, color/typography/spacing scale, exact Reminder copy strings (on-time, late, silent missed, batched summary), notification-channel display names** — file 11.
  - **`SCHEDULE_EXACT_ALARM` vs. `USE_EXACT_ALARM` permission flavor decision** for Android 13+ — file 12.
  - **`WorkManager` constraints** for the horizon-extension job (network/charging/idle) — file 12.
  - **Konsist / ArchUnit test for layer purity inside feature modules** — flagged as a possible future test addition; file 05 testing portfolio may pick this up.
- **ADR candidates surfaced in this turn (to backfill into `_context/09_decision_log.md` when contextualized):** in addition to the three already identified in the 2026-05-06 open-question resolution turn (Late-Reminder Window, Retroactive Window, Timezone-Anchored Schedules), file 04 introduces several new durable architecture decisions worth recording as ADRs:
  - **ADR — Six-module Gradle layout with `:core` as a pure-Kotlin shared kernel.** Rationale: compile-time enforcement of Bounded Context boundaries; pure-Kotlin shared kernel guarantees Domain-layer purity at build time. Alternative considered: single `:app` Gradle module with package-only separation (rejected at the user's explicit choice — Option B).
  - **ADR — Split `scheduling-reminders` into `:scheduling` (lifecycle/clock-arithmetic) and `:notifications` (user-facing presentation).** Rationale: the boundary makes BR-010's distinct text variants and the silent-missed / batched-summary copy presentational concerns, leaving the clock-arithmetic deciding on-time vs. late vs. missed in `:scheduling`.
  - **ADR — Dual-strategy background work: `AlarmManager` exact alarms for Reminder firing, `WorkManager` for deferred/periodic.** Rationale: Doze-resilience requires `AlarmManager`'s exactness for `BR-004`; `WorkManager`'s exactness is insufficient for sub-minute precision but its durability is the right fit for the daily horizon-extension job and the `pending → missed` sweep fallback.
  - **ADR — Hilt over Koin for DI.** Rationale: compile-time graph verification preferred over runtime-only resolution for a solo dev with no second pair of eyes.
  - **ADR — Two-tier error policy with custom sealed `Result<T, E>` in `:core` (rejecting `kotlin.Result`).** Rationale: typed error channel makes file-02's BR violations exhaustively patternable in `when` expressions; `kotlin.Result` is `Throwable`-based and therefore exception-flavored.
  - **ADR — Single Room `NemoPillDatabase` instance for the whole app** (rather than per-feature databases). Rationale: required to preserve the Dose-aggregate atomic transaction that spans `:scheduling` (Dose status) and `:adherence-tracking` (Confirmation).
  - **ADR — `ClockPort` and `TimezonePort` as shared kernel ports; Domain functions take `now: Instant` as a parameter.** Rationale: makes BR-008 / BR-009 / BR-010 / BR-011 / BR-012 deterministically testable without device-clock manipulation.
  - **ADR — Option-A in-place Confirmation mutation (carried forward from file 02 but with the file-04 architecture risk that the audit trail is lost).** Listed under Architecture Risks And Trade-Offs as a refactor candidate; the ADR can either be a fresh entry or a cross-reference to the file-02 decision.
- **Next role:** Architect — invoke `/start-working` in a new conversation to contextualize `_context/05_engineering_quality_security_and_compliance.md`. Per CLAUDE.md's per-file session protocol, this conversation stops here.
