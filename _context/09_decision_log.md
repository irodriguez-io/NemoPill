# Decision Log

## How To Use This File

- Add one ADR entry for every durable decision that affects architecture, security, compliance, integrations, data boundaries, delivery process, or operating model.
- Keep the newest ADR at the top after the instructions section.
- If a decision changes later, add a new ADR that supersedes the old one instead of rewriting history.
- ADR IDs are zero-padded three-digit ascending integers assigned in oldest-to-newest order. The visual top of `## Current State` displays the highest-numbered (newest) ADR first; ADR-001 is the last entry in the section.
- All ADRs backfilled at T-003 close are `Status: Accepted` — every backfilled decision was already settled at section-acceptance time during prior `/start-working` sessions and ratified by the Human gatekeeper's `Approved for apply` flip. ADRs added after T-003 may sit at `Proposed` until approved.
- Each ADR's `Date` is the lock-date of the decision (the originating `/start-working` session timestamp or the date of the corresponding `_context/` file's contextualization, whichever is earlier).
- Each ADR's `Related milestone or task` row names the originating task ID, the originating `_context/` file references, and the originating `_context/handoff_log.md` entry timestamp(s) so the full traceability chain from the ~151 surfaced candidates to the consolidated ADR set is preserved.
- Inside `_context/` prose, references to the framework placeholder marker syntax must escape the braces (`\{\{REQUIRED:\}\}` / `\{\{OPTIONAL:\}\}` / `\{\{EXAMPLE:\}\}`) so the validator's regex does not false-fire — see ADR-039.

## ADR Status Values

- `Proposed`
- `Accepted`
- `Superseded`
- `Rejected`

## Current State

### ADR-103: `:adherence-tracking` Kover thresholds collapse to a single ≥ 90 % combined Domain+Application gate (ADR-086 constraint); end-to-end receiver test uses per-module test `@Database`s + a test-only `:notifications → :adherence-tracking` dependency + receiver `@VisibleForTesting` seams

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; QA; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/08_active_task_packet.md § In Scope / § Acceptance Criteria (AC-002 / AC-003 / AC-004 / AC-004a / AC-006)`; ADR-086 (report-level Kover filters); ADR-091 (`@DaggerGenerated` exclusion)

#### Context

The packet specifies two `:adherence-tracking` Kover thresholds — Domain ≥ 90 % and Application ≥ 80 %. Kover 0.8.x forbids per-rule filters (ADR-086): a report's `includes`/`excludes` are report-wide and a verify `rule` measures the whole filtered scope, so two different per-package thresholds in one module cannot be expressed directly. Separately, AC-004's end-to-end receiver test must drive `ConfirmFromNotificationReceiver` → `:core` gateway → use case → `RoomConfirmationRepository` → real Room, but the receiver lives in `:notifications`, the persistence path in `:adherence-tracking`, and the `@Database` aggregate (`NemoPillDatabase`) in `:app` — none reachable from a single library-module test by the production graph, and the receiver is OS-instantiated (no constructor injection).

#### Decision

(1) **Coverage:** one report-wide filter scoping `io.nemopill.adherencetracking.domain.*` + `application.*` (with the ADR-091 `@DaggerGenerated` exclusion) and one verify rule at **minValue = 90** over the combined scope. 90 ≥ both stated minimums, so it satisfies Domain ≥ 90 and is strictly stronger than Application ≥ 80; `infrastructure.*` (entity/dao/converters/repository/gateway) carries no % threshold and is exercised by the Robolectric integration tests. (2) **Test architecture:** each module that needs real Room in its tests declares a **test-only `@Database`** in its test source set with `kspTest(room.compiler)` (`TestConfirmationDatabase` in `:adherence-tracking`; `ReceiverTestConfirmationDatabase` in `:notifications`) — the `:app` aggregate is unreachable from a library test. The `:notifications` e2e test adds a **test-only** `testImplementation(project(":adherence-tracking"))` (the production graph stays clean; Konsist excludes `/test/` paths). The receiver exposes three `@VisibleForTesting` seams (`gatewayProvider`, `confirmTimeoutMillis`, `onOutcomeForTest`) so the test injects a real-Room / failing / slow gateway and observes the async outcome; the production default of `gatewayProvider` is `EntryPointAccessors`.

#### Consequences

`koverVerify` passes with `:adherence-tracking` measured ~100 % on the gated scope (verified). The single-gate collapse is a faithful, surfaced reading of the two-threshold spec under ADR-086, not a silent descope. The test-only cross-feature dependency is intentional and required by AC-004's end-to-end intent. The receiver's static `@VisibleForTesting` seams are reset in the test's `@After` to avoid cross-test leakage. Status Proposed.

---

### ADR-102: Demo-vs-full-F-006 gap — T-010 persists only the `Confirmation` (no `Dose.status` co-update, no `DoseRepository`, `UnknownDose` deferred); first at-rest persistence of `Confidential` data accepts ADR-043 plain-text Room

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/06 § F-006`; `_context/04 § Architecture Risks`; `_context/13 § THR-010 / § 9 Q1`; ADR-043; supersedes nothing

#### Context

`_context/06 § F-006` is the full confirm flow: typed parse (1) → dispatch (2) → BR-005/BR-011 checks (3) → `Confirmation` write **and** `Dose.status` co-update in one `withTransaction` (4) → defensive `SchedulerPort.cancel` (5) → dismiss (6) → emit `DoseConfirmed` (7). M-002 has no persisted `Dose` (no `Medication`, no Dose materialization), so the full transaction cannot be realized yet. T-010 is also the project's **first at-rest persistence of `Confidential`-class data** (the `Confirmation` row in plain-text SQLite — the THR-009 / THR-010 surface).

#### Decision

T-010 realizes F-006 steps 1–2, the `Confirmation`-write half of step 4, step 6, and step 7. **Deferred to M-004 / M-005:** the `Dose.status` co-update (step 4 second half) and the cross-module `:adherence-tracking → :scheduling` `DoseRepository` dependency (file 04 § Architecture Risks); the BR-011 24-hour-window check (needs `Dose.scheduledAt`; vacuously satisfied at on-time confirm); the defensive `SchedulerPort.cancel(doseId)` (step 5); and `Result.Err.UnknownDose` Dose-existence resolution (no `Dose` table to resolve against). `RoomConfirmationRepository` still wraps the single write in `withTransaction { }` so the co-update drops in later without restructuring. **At-rest encryption (AC-008a):** by reference to the already-ratified **ADR-043** (file 13 § 9 Q1, Option A — plain-text Room), SQLCipher / at-rest encryption is intentionally **not** added at this first persistence; the THR-010 residual stays an accepted `medium`. No new code — an audit-trail note that the question was consciously considered at first persistence.

#### Consequences

The demo writes a `Confirmation` and dismisses the notification but does not flip any `Dose.status` (there is none). The full Dose-aggregate same-transaction co-update, the defensive cancel, and `UnknownDose` are routed to M-004 / M-005 when real Doses exist. The plain-text-Room decision is unchanged from ADR-043; the file-13 § 9 (ii) revisit triggers remain the standard escalation path. Status Proposed.

---

### ADR-101: ADR-049 rule (ii) wired — sensitive `data class` redacted `toString()` Konsist rule; ADR-087 open question resolved (scope = feature `.domain` + `:core::event` data classes; non-PII `:core` types exempt); `Result.Err.UnexpectedNotificationPayload` added

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); resolves **ADR-087**; ADR-081 (code-surface-dependent rule wiring); ADR-049 (the two redaction rules); ADR-031; `_context/13 §§ Observability / Information Disclosure`

#### Context

ADR-049 specifies two redaction rules; rule (i) (no dynamic `throw` messages) was wired at T-008 (ADR-081 / `NoDynamicThrowMessageRule`). Rule (ii) (sensitive `data class` redacted `toString()`) was deferred to "the first sensitive Domain `data class`", and ADR-087 left open whether rule (ii)'s literal "(:domain, :core) data class" scope would over-match non-PII `:core` data classes (`Result.Ok`, `ReminderFired`). T-010 introduces `Confirmation` (first `Confidential`-class Domain `data class`) and `:core::event::DoseConfirmed` (first `:core` event carrying a Confidential field), forcing the rule to be wired and the scope question to be settled.

#### Decision

Wire rule (ii) as a new `:core` Konsist rule `DataClassRedactedToStringRule` (+ negative test) — existing rule (i) file untouched. **Resolve ADR-087:** scope rule (ii) to `data class`es that carry `Confidential`-class data per file 13 — i.e. **feature-module `.domain` data classes** + **`:core::event` data classes** — and **exempt non-PII shared-kernel types** via an allow-list: `Result.Ok` / `Result.Err.*` (in `io.nemopill.core.result`, out of scope automatically) and `ReminderFired` (in `:core::event`, explicitly allow-listed — carries only `DoseId` + `Instant` + a non-Confidential variant). Every in-scope data class must override `toString()` (the auto-generated data-class `toString()` dumps all fields). `Confirmation` and `DoseConfirmed` get a redacted `toString()` (`"…(REDACTED)"`); a runtime test pins the field-free content. Also add `Result.Err.UnexpectedNotificationPayload` (a field-free `data object`) to the `:core` `Result.Err` catalog for the receiver's typed-parse rejection (file 06 § F-006); being field-free it is non-PII and out of rule (ii) scope.

#### Consequences

`arch-conformance` (stage 5) now enforces redaction on Confidential-bearing data classes; future sensitive Domain entities are covered by the same package-scoped rule. Rule (i) stays green (all new `throw`/log messages are static). The exempt allow-list is the single place to record a deliberately-non-PII `:core::event` type. Status Proposed; supersedes the open portion of ADR-087.

---

### ADR-100: `:core::event::DoseConfirmed` added to the `DomainEvent` hierarchy — carries Confidential `status`, redacted `toString()`, dedupe key `(confirmationId, confirmedAt)`, no cross-module consumer in MVP

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/02 § Domain Events`; `_context/06 § F-006 step 7 / line 35`; ADR-089 (event bus); ADR-101 (rule (ii))

#### Context

`_context/02 § Domain Events` lists `DoseConfirmed` as an additive member of the `:core::event` `DomainEvent` hierarchy (seeded by `ReminderFired` at T-009, ADR-089). F-006 step 7 emits it on a successful confirm. It must reference `ConfirmationId` / `ConfirmationStatus` / `ConfirmationSource`, which therefore must be visible from `:core` (one of the forces behind the ADR-097 vocabulary relocation).

#### Decision

Add `data class DoseConfirmed(doseId, confirmationId, status, confirmedAt, source) : DomainEvent` in `:core::event`. `ConfirmDoseFromReminderUseCase` publishes it on a successful `Confirmation` write. It carries Confidential `status`, so it overrides `toString()` to a redacted form (ADR-101). **Dedupe key `(confirmationId, confirmedAt)`** per file 02 (UC-007 correction re-emits with the same `confirmationId`). **No cross-module consumer in T-010** (file 06 line 35) — the on-screen Adherence counter (M-002 item 5, T-011) is the first subscriber.

#### Consequences

The event hierarchy grows additively (no behavior change to existing subscribers). The redaction is enforced by ADR-101's rule. The first consumer lands in T-011. Status Proposed.

---

### ADR-099: `:adherence-tracking` `Confirmation` aggregate + `RoomConfirmationRepository` — 1:1 unique index on `doseId` with INSERT-OR-REPLACE idempotency, `withTransaction`, base-`RoomDatabase` injection; first `:adherence-tracking` production code

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/04 § ConfirmationRepository / § Local persistence`; `_context/06 § Idempotency rule / § Ordering rule / § Retry rule`; `_context/02 row Confirmation` / `BR-005`; ADR-043 (plain-text Room)

#### Context

T-010 stands up the first `:adherence-tracking` production code: the `Confirmation` Domain entity, the `ConfirmationRepository` port + `RoomConfirmationRepository` adapter, `ConfirmDoseFromReminderUseCase` (UC-004), and the Room `@Entity` / `@Dao` / `@TypeConverter`s. The repository must be 1:1-unique and idempotent on `doseId` (file 06 § Idempotency rule — safe against double-tap) and must use `withTransaction` (file 06 § Ordering rule), yet cannot reference the `:app`-resident `NemoPillDatabase`.

#### Decision

`ConfirmationEntity` declares a **unique index on `doseId`**; `ConfirmationDao.upsert` uses `@Insert(onConflict = REPLACE)` so a second confirm for the same Dose replaces the row (idempotent on `doseId`). `RoomConfirmationRepository` wraps the write in `db.withTransaction { }` and is constructor-injected with the **base `androidx.room.RoomDatabase`** (provided from the `NemoPillDatabase` singleton in `:app` DI) plus the `ConfirmationDao` — so the feature module never imports the `:app` aggregate. `Instant` ↔ epoch-millis and both enums ↔ `name` go through `ConfirmationConverters`. `ConfirmDoseFromReminderUseCase` constructs the `Confirmation` (`source = NOTIFICATION_ACTION`, `confirmedAt = ClockPort.now()`, random `ConfirmationId`), persists, and on `Result.Ok` publishes `DoseConfirmed`; adapter-boundary exceptions map to `Result.Err.Unexpected` (static message — ADR-049 rule (i) green), `CancellationException` rethrown. **First at-rest persistence of `Confidential` data** is plain-text Room per the ratified ADR-043 (AC-008a; THR-010 residual `medium` accepted).

#### Consequences

The `confirmation` table (PK `confirmationId`, unique index on `doseId`) is the project's first persisted aggregate; its committed schema is ADR-098's `1.json`. Idempotency is enforced at the storage layer, not in the use case. The base-`RoomDatabase` injection keeps `:adherence-tracking` independent of `:app`. Status Proposed.

---

### ADR-098: `NemoPillDatabase` single-instance Room `@Database` in `:app` (root package, version 1, `exportSchema = true`); Room codegen via KSP at the `@Database` site only — `annotationProcessor` → KSP switch with minimal placement

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/04 § Architecture Risks`; `_context/06 § Section 1 / Section 2`; ADR-083 / ADR-091 (KSP minimal placement); `_context/08 § AC-005`

#### Context

T-010 stands up the project's first Room database. File 04 makes the single-`NemoPillDatabase` design load-bearing (the cross-module Dose-aggregate transaction), and only `:app` may depend on all feature modules whose DAOs it aggregates, so the `@Database` must live in `:app`. The `:adherence-tracking` build still declared `annotationProcessor(room.compiler)` — wrong for this KSP project — and the Room schema-export location had to be set. AC-005 names the committed schema path exactly: `app/schemas/io.nemopill.app.NemoPillDatabase/1.json`.

#### Decision

`NemoPillDatabase` (`@Database(entities = [ConfirmationEntity::class], version = 1, exportSchema = true)`, `@TypeConverters(ConfirmationConverters::class)`) lives in `:app` in the **`io.nemopill.app` root package** (not a `persistence` sub-package) so Room's FQN-derived schema directory is exactly `app/schemas/io.nemopill.app.NemoPillDatabase/` per AC-005 (resolving the packet's internal path inconsistency in favor of the acceptance criterion). Room codegen runs via **KSP at the `@Database` site only**: `:app` gets `ksp(room.compiler)` + the `room.schemaLocation` KSP arg; `:adherence-tracking` keeps only `room-runtime`/`room-ktx` (its `@Dao`/`@Entity` are processed at the `:app` `@Database` compilation unit — the standard multi-module Room model), with `kspTest(room.compiler)` for its test-only `@Database`. The stale `annotationProcessor(room.compiler)` is removed (not merely switched). No `fallbackToDestructiveMigration` outside test builds (file 06 § Section 2). `1.json` is committed.

#### Consequences

The empirically-confirmed minimal KSP placement (Room compiler in `:app` + per-test-source `kspTest`, none in feature `main`) refines ADR-083 / ADR-091 for the Room case. Future entities (`Medication` / `DoseSchedule` / `Dose`) are added with a `Migration(from, to)` + a new committed schema JSON. The committed `1.json` declares the `confirmation` table with the unique `doseId` index. Status Proposed.

---

### ADR-097: Receiver→use-case seam — a neutral `:core` `NotificationConfirmationGateway` port bound in `:app` (option C); shared confirmation vocabulary (`ConfirmationId` / `ConfirmationStatus` / `ConfirmationSource`) relocated to `:core`

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-010 (M-002); `_context/08_active_task_packet.md § Architect Decision Required`; `_context/04 § Gradle dependency graph (line 170)`; `_context/06 § F-006`; `_context/13 § "PendingIntent → notification-action receiver"`; supersedes nothing; relates ADR-089 (`DomainEventPublisher` in `:core`)

#### Context

`_context/04 § Gradle dependency graph` forbids feature modules from depending on each other ("`:app` is the only module allowed to depend on the four feature modules"), yet `_context/06 § F-006` step 2 and `_context/13`'s "PendingIntent → notification-action receiver" row place `ConfirmFromNotificationReceiver` in `:notifications` and have it directly "invoke `ConfirmDoseFromReminderUseCase` in `:adherence-tracking::application`". A `:notifications` class cannot reference an `:adherence-tracking` type without an illegal cross-feature Gradle dependency. The T-010 packet surfaced this as the second load-bearing module seam (the first — `:scheduling` → `:notifications` via the `:core` event bus — was proven by T-009) and mandated resolving it as an ADR before wiring the receiver. A secondary constraint: `:core::event::DoseConfirmed` (added in T-010) references `ConfirmationId` / `ConfirmationStatus` / `ConfirmationSource`, and `:notifications` needs `ConfirmationStatus` for its typed IPC parse — so those newtypes cannot live in `:adherence-tracking::domain` (that would force `:core` to depend on a feature, and `:notifications` to import `:adherence-tracking`).

#### Decision

Adopt the packet's recommended **option (C)**: define a neutral `:core` port `io.nemopill.core.confirm.NotificationConfirmationGateway { suspend fun confirm(doseId, status): Result<Unit, Result.Err> }`. `:notifications`' receiver depends only on this `:core` port (it already depends on `:core`) and obtains it via `EntryPointAccessors`; `:adherence-tracking` provides the implementation (`AdherenceConfirmationGateway`) that delegates to `ConfirmDoseFromReminderUseCase`; `:app::di::AdherenceModule` binds port → impl. Relocate the shared confirmation vocabulary to `:core` — `ConfirmationId` to `:core::id` (symmetry with `DoseId`, resolving the packet's "Developer picks" question) and `ConfirmationStatus` / `ConfirmationSource` to a new `:core::confirm` package (genuinely shared-kernel: the IPC `status` enum and the `:core` event both need them). `:adherence-tracking::domain::Confirmation` re-uses the `:core` types.

#### Consequences

Both feature modules depend only on `:core`; neither imports the other (the strict file-04 graph holds, Konsist `NoCrossFeatureDomainImportRule` stays green). The **command** semantics of F-006 are preserved ("invokes the use case") rather than pushing a command through the past-tense `DomainEventPublisher`; precedented by `DomainEventPublisher` itself living in `:core`. Security: the seam does not widen the IPC-ingress trust boundary — the typed `ConfirmationStatus` parse still happens in the receiver before the port is called, and the receiver stays `exported="false"`. Deviates from the packet's literal placement of `ConfirmationStatus` / `ConfirmationSource` / `ConfirmationId` in `:adherence-tracking::domain` (the packet explicitly anticipated this and delegated the choice). Status Proposed pending gatekeeper ratification.

---

### ADR-096: `:notifications` owns its notification string resources and Kover application-layer threshold; channel name/description + demo copy live in `:notifications/res`, not `:app/res`

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § In Scope / Likely Change Surface`; `_context/11 § Notification channel display names`; ADR-086

#### Context

The T-009 packet's "Likely Change Surface" literally directs the `reminder_on_time` channel name/description and demo notification copy into `app/src/main/res/values/strings.xml`. But `NotificationChannels` and `ReminderNotificationBuilder` live in `:notifications`, which `:app` depends on (not the reverse). A library module cannot read the `:app` `R` class, and the on-demand notification build (triggered by a bus event, not by `:app`) cannot have strings passed down at construction time. Placing the strings in `:app/res` would therefore require `:notifications` to import `:app` — breaking the load-bearing `:notifications`-must-not-import-`:app` boundary this slice exists to prove. File 11 requires "no inline literals; sourced from string resources" but does not pin the owning module; file 04 says `:notifications` owns the notification surface.

#### Decision

Author all notification strings (channel display name + description, on-time title/body templates, "Take"/"Skip" labels, hardcoded non-PII demo placeholders) in `notifications/src/main/res/values/strings.xml`, referenced via `io.nemopill.notifications.R`. EN only in T-009 (ES is M-006). Also activate the `:notifications` Kover gate using the ADR-086 report-level-filter form: `io.nemopill.notifications.application.*` ≥ 80% line (`PresentReminderUseCase`, `ReminderFiredListener`); domain is vacuous; infrastructure/root carry no threshold.

#### Consequences

Honors file 11's "no inline literals" intent and the module graph simultaneously; deviates from the packet's literal file path. Future `:notifications` copy (BR-010 variants, ES) lands in this module's resources. Coverage gate measured at 100% line in the gated scope at T-009 close. Surfaced for gatekeeper ratification. Status Proposed.

---

### ADR-095: `ConfirmFromNotificationReceiver` is a synchronous logging-and-dismiss stub in T-009; typed parse → Confirmation write (and its `goAsync()`) deferred to T-010

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § In Scope / Explicitly Out Of Scope`; `_context/06 § Section 2 / § Timeout rule`; `_context/13 § THR-002`

#### Context

M-002 item (4) — the inline-action confirm path that writes a Room `Confirmation` via `ConfirmDoseFromReminderUseCase` — is explicitly out of T-009 scope (no Room in M-002-early). T-009 only needs the receiver to exist (manifest-declared, `exported="false"`) and dismiss the notification on tap, so the action PendingIntents have a real target. The packet suggested `goAsync()` + a bounded coroutine scope per file 06 § Timeout rule.

#### Decision

Implement `onReceive` as a synchronous stub: validate the action string at the entry point, parse the `doseId`/`status` extras enough to log one static, non-PII line keyed on the bounded status, and dismiss the source notification via `NotificationManagerCompat.cancel(notificationIdFor(doseId))`. Do **not** introduce `goAsync()` here: the stub does no async or suspending work, so a `goAsync()`/`finish()` wrapper would be pointless machinery. The `goAsync()` + bounded-scope pattern is instead demonstrated in T-009 on the publish side (`ReminderAlarmReceiver`, which genuinely awaits a suspend `publish`), and lands in `ConfirmFromNotificationReceiver` in T-010 alongside the suspend `ConfirmDoseFromReminderUseCase` dispatch that needs it.

#### Consequences

Tapping "Take"/"Skip" logs and dismisses but writes nothing in T-009. T-010 replaces the stub with the typed parse → use-case dispatch (with `Result.Err.UnexpectedNotificationPayload` / `UnknownDose`) and the `goAsync()` scope. Status Proposed.

---

### ADR-094: On-time Reminder inline actions — two `FLAG_IMMUTABLE` broadcast `PendingIntent`s ("Take"=TAKEN / "Skip"=SKIPPED) to an `exported="false"` receiver; content-tap resolves the launcher Intent at runtime

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § In Scope`; `_context/11 § Notification (inline action)`; `_context/06 § Section 2`; ADR-023

#### Context

The on-time Reminder must expose the two inline actions the product is built around, and every `PendingIntent` must use `FLAG_IMMUTABLE` (ADR-023; exercised by the `PendingIntentFlagImmutableRule` Konsist check). The content-tap must open the app, but `:notifications` may not import `:app`'s `MainActivity`.

#### Decision

`ConfirmActionPendingIntentFactory` builds two `PendingIntent.getBroadcast(...)` actions with `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`, targeting `ConfirmFromNotificationReceiver`, carrying action `io.nemopill.notifications.ACTION_CONFIRM_FROM_NOTIFICATION` and extras `{ doseId, status }` with `status ∈ {TAKEN, SKIPPED}` (aggregate ID only — no Patient data). Request codes are stable per `(doseId, action)` so the two intents never collide. The content-tap uses `PendingIntent.getActivity(...)` (also `FLAG_IMMUTABLE`) over `packageManager.getLaunchIntentForPackage(packageName)`, mirroring the T-008 `AlarmManagerSchedulerAdapter` launcher-resolution pattern to avoid a `:notifications → :app` import.

#### Consequences

Three `FLAG_IMMUTABLE` PendingIntents per notification, all asserted by the Robolectric integration test (immutable, broadcast, action, extras, target receiver). Lock-screen residual (THR-008) is the accepted product affordance, not a regression. Status Proposed.

---

### ADR-093: `NotificationManagerCompat` adapter + `reminder_on_time` channel created at `IMPORTANCE_HIGH`

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/04 § NotificationPort`; `_context/06 § Section 1 / Section 2`; `_context/11 § BR-010 channel-importance`; BR-004

#### Context

`NotificationPort` needs its first adapter, and the `reminder_on_time` channel (ID pinned in file 06) must be created with an importance posture. File 11 § BR-010 channel-importance deliberately diverges from M3's quieter notification-style default because on-time Reminders are time-critical (BR-004).

#### Decision

`NotificationManagerNotificationAdapter` implements `NotificationPort` via `NotificationManagerCompat`, posting under a stable per-Dose notification ID (dedupe key = `doseId`, file 06 § Idempotency rule). `NotificationChannels.ensureReminderOnTimeChannel` creates the channel via `NotificationChannelCompat.Builder` at `IMPORTANCE_HIGH`, with name/description from string resources (ADR-096). The post is guarded by `areNotificationsEnabled()` (the `Result.Err.NotificationsPermissionRevoked` runtime-permission UX is deferred to M-004; demo relies on a manual grant). `@SuppressLint("MissingPermission")` documents the guarded deferral.

#### Consequences

Only `reminder_on_time` is created in T-009 (other BR-010 channels are M-004). The channel ID is the durable contract; importance is a creation-time default the Patient may override. Status Proposed.

---

### ADR-092: Dagger 2.51.1 cannot read Kotlin 2.1.0 metadata on the members-injection (`@Inject` field) validation path; avoid field injection (use `EntryPointAccessors`); durable fix is a Hilt version bump routed forward

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § Risks`; ADR-083; `_context/04 § DI`

#### Context

T-009 introduced the project's first `@Inject lateinit var` field-injection site (`NemoPillApplication.reminderFiredListener`). `:app:hiltJavaCompile{Debug,Release}` failed with `IllegalStateException: Unable to read Kotlin metadata due to unsupported metadata version` from `dagger.internal.codegen.kotlin.KotlinMetadata.metadataOf`, reached via `InjectValidator.validateField → isMissingSyntheticPropertyForAnnotations`. Dagger/Hilt 2.51.1's bundled Kotlin-metadata reader does not support Kotlin 2.1.0's metadata version, and field injection is the trigger. T-008 worked because it used only constructor injection and `@AndroidEntryPoint`/`@HiltViewModel`, which do not exercise that path.

#### Decision

Avoid `@Inject` field injection entirely for now. `NemoPillApplication` retrieves `ReminderFiredListener` from the SingletonComponent via `EntryPointAccessors.fromApplication(this, AppStartupEntryPoint::class.java)` (a provision `@EntryPoint`, not members injection). The durable fix — bumping Hilt to a release whose metadata reader supports Kotlin 2.1.0 — is deliberately **not** folded into T-009 (a version-catalog change with broad blast radius against the "all 8 CI stages green" gate); it is routed to a follow-up dependency-maintenance task.

#### Consequences

No `@Inject` fields anywhere until the Hilt bump lands; constructor injection and provision entry points are the sanctioned patterns. The same `EntryPointAccessors` pattern is used for the `ReminderAlarmReceiver` (ADR-091). Re-evaluate at the Hilt bump. Status Proposed.

---

### ADR-091: Broadcast-receiver Hilt injection via `EntryPointAccessors` + `@EntryPoint`; `:scheduling` gains the Hilt Gradle plugin + KSP (refines ADR-083) and excludes Dagger-generated classes from its Kover gate

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § In Scope / Risks`; ADR-083; ADR-086

#### Context

A manifest `BroadcastReceiver` is OS-instantiated and cannot be constructor-injected. `@AndroidEntryPoint` field injection is unusable here: its injection hook calls `super.onReceive(...)`, which targets the abstract `BroadcastReceiver.onReceive` and so does not compile from Kotlin source (the Hilt superclass rewrite happens post-compile). T-008's ADR-083 kept the Hilt Gradle plugin + KSP out of `:scheduling` (its `@Inject`-constructor adapters were aggregated into `:app`'s graph).

#### Decision

`ReminderAlarmReceiver` declares a nested `@EntryPoint @InstallIn(SingletonComponent::class) ReminderAlarmEntryPoint` exposing `DomainEventPublisher` and `ClockPort`, and retrieves them in `onReceive` via `EntryPointAccessors.fromApplication(context.applicationContext, ...)`. Processing an `@EntryPoint` requires Hilt codegen in `:scheduling`, so the module now applies the Hilt Gradle plugin + `ksp(hilt.compiler)` and adds `kotlinx-coroutines-android` for the bounded `goAsync()` publish scope. This refines ADR-083 for the receiver-injection case. A side effect: Dagger now generates `ScheduleDemoReminderUseCase_Factory` in `io.nemopill.scheduling.application`, which dropped the Kover application-layer gate to 38.7%; the gate now excludes `@dagger.internal.DaggerGenerated` classes (a report-level filter consistent with ADR-086), restoring it to 100% on hand-written code.

#### Consequences

`:scheduling` is now a Hilt-processing module. The same `EntryPointAccessors` pattern serves `NemoPillApplication` (ADR-092). The Kover generated-code exclusion is a general improvement (generated factories never belong in coverage) and may be applied to other modules if/when they gain KSP. Status Proposed.

---

### ADR-090: Demo `ReminderAlarmReceiver` publishes `ReminderFired(ON_TIME)` unconditionally on the event bus via a bounded `goAsync()` scope; the precondition-checked `FireReminderUseCase` (file 06 § F-005 steps 2–3) is deferred to M-004

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/08_active_task_packet.md § In Scope / Out Of Scope`; `_context/06 § F-005 / § Timeout rule`; BR-010

#### Context

T-009 turns the T-008 no-op alarm receiver into the cross-Bounded-Context seam. The real F-005 fire path (read the `Dose` from Room, check `status == pending` and the BR-010 1-hour window) requires Room and a persisted `Dose`, both out of M-002-early scope.

#### Decision

`ReminderAlarmReceiver.onReceive` reads the `doseId` extra (the only entry-point parse — file 06 § AlarmManager PendingIntent boundary) and publishes `ReminderFired(DoseId, ClockPort.now(), ON_TIME)` onto the `:core` `DomainEventPublisher`, unconditionally, for the hardcoded demo Dose. The publish runs on a `goAsync()` + bounded coroutine scope under a 5s `withTimeout` (well under the OS ~10s receiver lifetime), with `PendingResult.finish()` in `finally` and static, non-PII log lines. `:scheduling` never imports `:notifications`; the seam is the bus.

#### Consequences

The walking-skeleton demo always fires `ON_TIME`. The real precondition-checked `FireReminderUseCase` (and the `LATE` variant) are M-004. The in-process bus's no-durability trade-off (file 04 § Architecture Risks) is accepted for the demo; if the cold-start delivery proves flaky on hardware, the Room-as-outbox path is the routed durable fix. Status Proposed.

---

### ADR-089: First `:core::event` production code — `InProcessEventBus` is a `MutableSharedFlow<DomainEvent>` (replay 8, DROP_OLDEST), bound as an `@Singleton`; `:core` takes `kotlinx-coroutines-core` as its one production dependency

- Date: 2026-06-07
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-009 (M-002); `_context/04 § DomainEventPublisher`; `_context/06 § Ordering rule / Idempotency rule`; `_context/02 § Domain Events`

#### Context

Cross-module communication is event-driven through `:core`'s `DomainEventPublisher` (file 04). T-009 is the first slice needing it. The cold-start race (the alarm-fired receiver publishes before the app's subscriber registers) requires subscriber-late catch-up.

#### Decision

Add `event/DomainEvent.kt` (sealed marker), `event/ReminderFired.kt` (`data class` of `DoseId` + `Instant` + `ReminderVariant{ON_TIME,LATE}` — no Patient data; only `ON_TIME` emitted in T-009), `event/DomainEventPublisher.kt` (`suspend fun publish` + `val events: Flow<DomainEvent>`), and `event/InProcessEventBus.kt` — a `MutableSharedFlow<DomainEvent>(replay = 8, extraBufferCapacity = 16, onBufferOverflow = DROP_OLDEST)`. `DROP_OLDEST` keeps `publish` non-suspending under back-pressure (a fired Reminder is never blocked by a slow subscriber); the replay buffer covers the cold-start subscriber-late case. `:core` gains `kotlinx-coroutines-core` as its sole production dependency — pure-JVM/multiplatform, so the "no Android imports in `:core`" build guarantee holds. The bus is bound as an `@Singleton` (`CoreEventModule`, `@Provides` since `:core` carries no Hilt).

#### Consequences

The `DomainEvent` hierarchy is seeded with `ReminderFired` only; the other file-02 members are additive-only with their owning features. No event durability across process death (accepted trade-off, file 04). `:core` Kover gate measured 100% line on `io.nemopill.core.*` (incl. `event.*`) at T-009 close. Status Proposed.

---

### ADR-088: ktlint `standard:function-naming` suppressed per-`@Composable` (PascalCase Compose convention); repo-wide `.editorconfig` exemption deferred to the Compose-proliferation milestone

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope — DemoScreen`; ADR-044 (ktlint)

#### Context

T-008 introduces the project's first Jetpack Compose function (`DemoScreen`). The ktlint `standard:function-naming` rule (bundled with ktlint-gradle 12.1.1) flags PascalCase function names and, in this version, does not auto-exempt `@Composable` functions. Compose UI functions conventionally use PascalCase, so the rule fires on every screen-level composable.

#### Decision

Suppress the rule locally with `@Suppress("ktlint:standard:function-naming")` plus a rationale comment on the `@Composable` function, rather than introducing a repo-wide `.editorconfig` now. Only one screen-level composable lands in T-008.

#### Consequences

Each future screen-level composable repeats the local suppression. When Compose screens proliferate (expected M-003 Medication CRUD), a single `.editorconfig` with `ktlint_function_naming_ignore_when_annotated_with = Composable` should replace the scattered suppressions — routed to that milestone. No effect on existing non-Compose files. Status Proposed pending the `.editorconfig` decision.

---

### ADR-087: ADR-049 routing at T-008 — rule (i) wired and exercised; rule (ii) deferred to the first sensitive Domain `data class`; `:core` `Result` scoping question surfaced for the rule-(ii) task

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope — ADR-049 routing`; ADR-081; ADR-049; ADR-031

#### Context

ADR-081 deferred ADR-049's two Konsist rules to the tasks first introducing their code surfaces. T-008 lands the first production `throw` — the `ScheduleDemoReminderUseCase` boundary's `CancellationException` rethrow — which triggers rule (i). `DoseId` is a `@JvmInline value class`, not a `data class`, so it does not trigger rule (ii). However, `Result.Ok` and `Result.Err.Unexpected` are `:core` `data class`es, and ADR-049 rule (ii)'s literal scope is "every Domain-layer (`:domain`, `:core`) `data class`".

#### Decision

(i) Wire rule (i) now as `core/src/test/kotlin/io/nemopill/core/konsist/NoDynamicThrowMessageRule.kt` (with a negative test). It forbids string templates / `String.format` / `StringBuilder.append` in a production `throw` message argument — a line-level text-scan heuristic consistent with `PendingIntentFlagImmutableRule` (Konsist 0.17.0 cannot reliably parse throw-argument expressions). It passes (the rethrow carries no message; the use case's unexpected-failure message is a static `const`). (ii) Defer rule (ii) to the first task introducing a Patient-data-bearing Domain `data class` — expected the M-002 Dose-materialization slice (the `Dose` entity) or, failing that, the M-003 Medication CRUD slice (`Medication` / `DoseSchedule` / `Confirmation`).

#### Consequences

Rule (i) is live and CI-enforced (arch-conformance stage). Rule (ii) remains unwired with a named target task. Surfaced (not silently resolved) for the rule-(ii)-wiring task: ADR-049 rule (ii)'s "(:domain, :core)" scope would also match `:core`'s `Result.Ok` / `Result.Err.Unexpected` `data class`es, which carry **no** Patient data. That task must decide whether to (a) require a redacted `toString()` on those non-PII shared-kernel types too, or (b) scope rule (ii) to Patient-data-bearing feature-module Domain entities and explicitly exempt `:core`'s `Result` family. Status Proposed.

---

### ADR-086: Kover 0.8.3 per-package coverage gates via report-level `filters` (per-rule filters forbidden in 0.8.x); resolves the ADR-078 deferral; `:scheduling` infra/presentation excluded from the module coverage report

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope — Activate Kover thresholds`; ADR-078; `_context/05 § Coverage thresholds`

#### Context

ADR-078 specified per-package Kover thresholds but its quoted DSL (`koverReport { verify { rule { ... } } }`) is the Kover 0.7.x form and could not be configured against empty source sets at T-007 (deferred — the actual `_source` build files carried no Kover config block). T-008 lands real code. Kover 0.8.x **forbids** per-rule `filters` (build error: "It is forbidden to override filters for a specific report, use custom report variants").

#### Decision

Configure each gated module with a report-level filter that scopes both the report and the verify gate: `kover { reports { filters { includes { classes("<pkg>.*") } } verify { rule("…") { bound { minValue = N } } } } }`. `:core` → `io.nemopill.core.*` ≥ 90 % line. `:scheduling` → `io.nemopill.scheduling.application.*` ≥ 80 % line. `:app` and the not-yet-implemented feature modules keep no percentage threshold. Gates run via `./gradlew koverVerify` (each module's own rule) and `koverHtmlReport`.

#### Consequences

Gates are real, non-vacuous, and met with margin (measured at T-008: `:core` 5/5 lines = 100 %; `:scheduling::application` 7/7 lines = 100 %). Because the filter is report-wide, `:scheduling`'s coverage *report* shows only the `application` package — `infrastructure.*` / `presentation.*` are excluded from the report entirely, not merely un-gated (they carry no `%` target per `_context/05` and are exercised by the Robolectric integration test). If future work needs to *report* infra/presentation coverage without *gating* it, migrate to Kover report variants. This resolves the ADR-078 deferral; ADR-078's per-module threshold intent stands, only its DSL form is superseded for 0.8.3. Status Proposed.

---

### ADR-085: `ReminderAlarmReceiver` relocated from `:app` to `:scheduling::infrastructure`

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope`; `_context/04 § :scheduling`; ADR-023

#### Context

file 04 places the alarm receiver in `:scheduling`. `AlarmManagerSchedulerAdapter` (in `:scheduling::infrastructure`) must reference the receiver class to construct its operation `PendingIntent`. Had the receiver stayed in `:app`, the adapter would need a `:scheduling → :app` dependency, which is illegal per the module graph (feature modules must not depend on `:app`).

#### Decision

Move the receiver to `io.nemopill.scheduling.infrastructure.ReminderAlarmReceiver`; delete the old `io.nemopill.app.reminder.ReminderAlarmReceiver`; repoint `:app`'s `AndroidManifest.xml` `<receiver>` to the new FQCN, keeping `android:exported="false"`. `onReceive` is a static-log no-op stub for T-008 (notification rendering is the next M-002 slice, item 3). `BootCompleteReceiver` — the one sanctioned `exported="true"` receiver — is untouched.

#### Consequences

The adapter references the receiver within its own module; no cross-module dependency violation. The receiver declaration stays in `:app`'s manifest (where `<application>` lives), addressed by FQCN; the merged manifest resolves the class through `:app`'s dependency on `:scheduling`. No new permissions; the receiver remains non-exported. Status Proposed.

---

### ADR-084: Demo scheduling-adapter design — `setAlarmClock`, idempotent request-code-by-`doseId`, `FLAG_IMMUTABLE` on every `PendingIntent`, runtime-resolved `showIntent`

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/04 § Exact-time scheduling / § SchedulerPort`; `_context/01` assumption (1); BR-004; ADR-023; ADR-031

#### Context

T-008's `AlarmManagerSchedulerAdapter` is the first real `SchedulerPort` implementation, retiring the Robolectric half of file 01 assumption (1) for the scheduling leg (M-002 Done-When item 1 + Robolectric half of item 2).

#### Decision

Use `AlarmManager.setAlarmClock(AlarmClockInfo(triggerAtMillis, showIntent), operation)` — the most reliable Doze bypass (file 04). The `operation` `PendingIntent` targets `ReminderAlarmReceiver` with a stable request code (`REQUEST_CODE_BASE + doseId.value.hashCode()`) and `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`, so re-scheduling the same `doseId` replaces rather than duplicates the alarm (BR-004 exactly-once; verified under `ShadowAlarmManager`). The optional `showIntent` is resolved at runtime from `packageManager.getLaunchIntentForPackage(packageName)` (wrapped immutable; `null` when unavailable, e.g. under the library test manifest) rather than referencing `:app`'s `MainActivity` — this preserves the `:scheduling`-must-not-import-`:app` boundary while still satisfying the "every `PendingIntent` is immutable" guardrail (ADR-023). `cancelAll()` cancels the single demo Dose (sufficient for the slice).

#### Consequences

Idempotency is the adapter's responsibility via the stable request code (BR-004). The `doseId` rides as an Intent extra — an aggregate ID only, ADR-031-compliant (no drug name / schedule / Confirmation history). Full multi-Dose cancellation, per-Dose request codes, and the boot re-arm path land with later M-002 slices. Status Proposed.

---

### ADR-083: KSP + Hilt Gradle plugin wiring; minimal placement — Hilt + KSP codegen in `:app` only; `:scheduling` carries the `hilt-android` library only

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope — First Hilt + KSP DI graph`; `_context/04 § Dependency injection`; ADR-075

#### Context

T-008 stands up the project's first Hilt `@Module` and DI graph. The Hilt *libraries* (`hilt-android` / `hilt-compiler`) were cataloged at T-006; the KSP and Hilt Gradle *plugins* were absent. The packet flagged uncertainty over whether `:scheduling` — whose `@Inject`-constructor adapter and use case are consumed by `:app`'s `@Module` — needs its own Hilt + KSP, and instructed the Developer to confirm the minimal placement empirically.

#### Decision

Add `ksp = "2.1.0-1.0.29"` (matched exactly to the catalog's Kotlin 2.1.0) and the `com.google.dagger.hilt.android` (v2.51.1, reusing the existing `hilt` version) plugin aliases to `libs.versions.toml [plugins]`; declare both `apply false` in the root build; apply both in `:app` with `ksp(libs.hilt.compiler)`. `:scheduling` gets `implementation(libs.hilt.android)` **only** — no Hilt/KSP plugin. Empirically confirmed at execution: Dagger generates the `@Inject`-constructor factories (`AlarmManagerSchedulerAdapter`, `ScheduleDemoReminderUseCase`) inside `:app` from `:scheduling`'s *compiled* `@Inject` constructors (binary dependency), so `:app`-only codegen is sufficient. `@HiltAndroidApp` on `NemoPillApplication`, `@AndroidEntryPoint` on `MainActivity`, `@HiltViewModel` on `DemoViewModel`; the ViewModel is obtained via the activity `viewModels()` delegate, so `hilt-navigation-compose` is not required.

#### Consequences

`:app:kspDebugKotlin` + `hiltAggregateDeps*` + `hiltJavaCompile*` run cleanly across debug/release/test variants; `:app:assembleDebug` succeeds with no missing-binding errors. KSP1 (default for the `2.1.0-1.0.x` line) is compatible with Hilt 2.51.1. If a feature module later needs its own `@Module` / entry point / `@HiltViewModel`, Hilt + KSP is added to that module at that time. Status Proposed.

---

### ADR-082: Coroutines (1.10.1) and Truth (1.4.4) added to the version catalog

- Date: 2026-06-05
- Status: Proposed
- Owners: Developer; Security; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-008 (M-002); `_context/08_active_task_packet.md § In Scope — catalog dependencies`; `_context/04 § Async/concurrency / § Testing toolchain`; `_context/05 § Dependency and supply chain`

#### Context

T-008's first Application use case is `suspend`; `DemoViewModel` drives it on `viewModelScope`; tests use `runTest` and Truth assertions per file 04 § Testing toolchain. None of coroutines or Truth were cataloged.

#### Decision

Add `coroutines = "1.10.1"` (pairs with Kotlin 2.1.0) with libraries `kotlinx-coroutines-core` / `-android` / `-test`, and `truth = "1.4.4"` with library `truth`. `:app` gets `kotlinx-coroutines-android` (viewModelScope); `:scheduling` gets `kotlinx-coroutines-test` + `truth` on its test classpath; `:core` gets `truth` (test). Coroutines is intentionally not added to `:scheduling` main — `suspend` and `CancellationException` come from `kotlin-stdlib`.

#### Consequences

Per `_context/05 § Dependency and supply chain`, this `libs.versions.toml` change is a Security-applicability / dependency-review event, recorded here with the one-line justification above. Verified against the merged manifest: none of these dependencies introduces a network or any new permission. Status Proposed.

---

### ADR-081: ADR-049 Konsist code-surface-dependent rules deferred to task that introduces the relevant code surface; infrastructure-only tasks with empty source sets do not wire code-surface rules

- Date: 2026-06-02
- Status: Accepted
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-007 (M-001 close); `_context/08_active_task_packet.md § In Scope — Assess ADR-049`; `_context/09_decision_log.md ADR-049`

#### Context

ADR-049 defines two Konsist rules deferred from T-006: (i) no `throw` statement in production code uses Kotlin string templates or `String.format` / `StringBuilder.append` in its message argument; (ii) every Domain-layer `data class` must override `toString()` to a redacted form. At T-007 execution, a search of all `src/main/kotlin/` files across six modules finds zero `throw` statements and zero `data class` declarations. The T-007 scope includes four files in `:app::src/main/kotlin/` (MainActivity, NemoPillApplication, BootCompleteReceiver, ReminderAlarmReceiver) — none contain a `throw` or a `data class`. All feature modules and `:core` have empty production source sets. Wiring either rule now means writing a Konsist predicate that vacuously passes (no production classes to inspect), adding zero correctness value while permanently tying the rule's first real exercise to an infrastructure-only task that cannot demonstrate it.

#### Decision

Adopt the convention: **infrastructure-only tasks whose production source sets contain no relevant code surface defer the corresponding Konsist rule to the task that first introduces that code surface.** Applied to ADR-049:

- Rule (i) (`throw`-message-string validation) — deferred to the first M-002 task that authors a `Result.Err.Unexpected` throw or any `throw` in production code. Expected: early M-002 use-case task.
- Rule (ii) (`data class` redacted `toString()`) — deferred to the first M-002 task that introduces a sensitive Domain `data class` (Medication, Dose, or Schedule aggregate). Expected: M-002 or M-003 domain entity task.

The target task for each rule must be named explicitly in that task's `_context/08_active_task_packet.md § In Scope` at PM-refresh time. If either rule is not wired by the task that introduces the relevant code surface, it becomes a blocker for QA's `Coverage Sufficient` verdict on that task.

#### Consequences

- ADR-049 rules remain unwired through T-007 close. The Konsist suite at M-001 close contains the six rules from T-006 plus zero new rules from T-007.
- The PM role records the target task for each ADR-049 rule in the T-008 task packet as a named in-scope item.
- Any future infrastructure-only task that defers a Konsist rule must record the target task explicitly in the handoff log — silent deferral without naming a target is not permitted by this convention.

---

### ADR-080: Roborazzi 1.7.0 selected as Compose snapshot test tool; `roborazzi-compose` BOM-aligned on `:app` test classpath; no snapshot captures at T-007 (import-resolution smoke-test only)

- Date: 2026-06-02
- Status: Accepted
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-007 (M-001 close); `_context/08_active_task_packet.md § In Scope — Wire Roborazzi`; `_context/12_environments_and_devops.md`; ADR-044

#### Context

ADR-044 named Roborazzi as the snapshot tool. T-007 wires it on the `:app` test classpath. The T-007 packet flagged the risk that `roborazzi-compose` depends on Compose UI and could cause resolution conflicts without a Compose BOM on the test classpath. The `:app` module already imports `implementation(platform(libs.compose.bom))` for its production source set but had no BOM import in `testImplementation`.

Version selected: **Roborazzi 1.7.0** (latest stable in the 1.7.x family as of 2026-06-02). Artifacts: `io.github.takahirom.roborazzi:roborazzi:1.7.0` and `io.github.takahirom.roborazzi:roborazzi-compose:1.7.0`.

#### Decision

Wire both `roborazzi` and `roborazzi-compose` on `:app` `testImplementation` at version 1.7.0. Add `testImplementation(platform(libs.compose.bom))` to `:app` `dependencies` alongside the roborazzi entries to align Compose transitive dependencies and avoid version-resolution conflicts. No snapshot is captured at T-007 — the `SmokeRoborazziTest` class confirms the `captureRoboImage` import resolves on the classpath (AC-004) and is deleted or replaced in M-003 when the first Compose screen is authored.

#### Consequences

- `libs.versions.toml` gains `roborazzi = "1.7.0"` under `[versions]` and two library entries `roborazzi` and `roborazzi-compose` under `[libraries]`.
- `:app::build.gradle.kts` gains `testImplementation(platform(libs.compose.bom))`, `testImplementation(libs.roborazzi)`, `testImplementation(libs.roborazzi.compose)`.
- `SmokeRoborazziTest.kt` is the AC-004 artifact; it is a compile-only reference to `::captureRoboImage` with no real Compose render.
- First real `captureRoboImage()` call, snapshot baseline commit, and 20-screen baseline in M-003 per `_context/05 § Snapshot baseline at 20`.

---

### ADR-079: Robolectric 4.14.1 as the Android unit-test runner; `includeAndroidResources = true` on all five Android modules; `:core` excluded (kotlin("jvm"))

- Date: 2026-06-02
- Status: Accepted
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-007 (M-001 close); `_context/08_active_task_packet.md § In Scope — Configure Robolectric`; ADR-044

#### Context

ADR-044 selected Robolectric as the Android unit-test runner. `robolectric = "4.14.1"` was already cataloged in `libs.versions.toml` at T-006. T-007 wires `testOptions.unitTests.isIncludeAndroidResources = true` in each Android module so the Robolectric runtime can resolve Android resources. T-006 had already added `testImplementation(libs.robolectric)` to `:medication-management`, `:scheduling`, `:notifications`, and `:adherence-tracking`; T-007 adds it to `:app`. `:core` uses `kotlin("jvm")` — no Android Gradle Plugin — so `RobolectricTestRunner` cannot resolve Android SDK classes there and `testOptions.unitTests` is not valid DSL.

#### Decision

- Apply `testOptions { unitTests { isIncludeAndroidResources = true } }` to `:medication-management`, `:scheduling`, `:notifications`, `:adherence-tracking`, and `:app`.
- Add `testImplementation(libs.robolectric)` to `:app` (already present in the four feature modules from T-006).
- Exclude `:core` from all Robolectric configuration.
- `SmokeRobolectricTest.kt` in `:app::src/test/kotlin/io/nemopill/app/` is the AC-003 artifact; it is deleted or replaced in M-002 when the first real Robolectric test lands.

#### Consequences

- The Robolectric SDK jar download on cold CI runners adds 30–60 s to the `unit-integration-ui-snapshot` stage on first run; `gradle/actions/setup-gradle` caching mitigates on subsequent runs.
- Tests annotated `@RunWith(RobolectricTestRunner::class)` can now be written in all five Android modules without additional dependency wiring.

---

### ADR-078: Kover 0.8.3 per-module threshold configuration; vacuous-pass convention for infrastructure-only tasks; no threshold on `:app`

- Date: 2026-06-02
- Status: Accepted
- Owners: Developer; Human gatekeeper (Isidro Rodriguez)
- Related milestone or task: T-007 (M-001 close); `_context/08_active_task_packet.md § In Scope — Wire Kover`; `_context/05_engineering_quality_security_and_compliance.md § Coverage thresholds`; ADR-044

#### Context

ADR-044 selected Kover as the coverage tool. `org.jetbrains.kotlinx.kover:0.8.3` was already cataloged in `libs.versions.toml` and declared `apply false` in the root `build.gradle.kts`. T-007 applies the plugin per-module and configures thresholds.

#### Decision

**Plugin application:** `alias(libs.plugins.kover)` added to each of the six module `build.gradle.kts` files. Root `build.gradle.kts` additionally applies the plugin with `apply(plugin = "org.jetbrains.kotlinx.kover")` and declares all six modules as `kover()` dependencies for multi-module aggregation. `./gradlew koverHtmlReport` generates the merged HTML report at `build/reports/kover/html/` (default Kover output path = `_source/build/reports/kover/html/`).

**Per-module thresholds (configured via `koverReport { verify { rule { ... } } }`):**

| Module | Package filter | Threshold |
|---|---|---|
| `:core` | `io.nemopill.core` | ≥ 90 % line |
| `:medication-management` | `io.nemopill.medicationmanagement.domain` | ≥ 90 % line |
| `:medication-management` | `io.nemopill.medicationmanagement.application` | ≥ 80 % line |
| `:scheduling` | `io.nemopill.scheduling.domain` | ≥ 90 % line |
| `:scheduling` | `io.nemopill.scheduling.application` | ≥ 80 % line |
| `:notifications` | `io.nemopill.notifications.domain` | ≥ 90 % line |
| `:notifications` | `io.nemopill.notifications.application` | ≥ 80 % line |
| `:adherence-tracking` | `io.nemopill.adherencetracking.domain` | ≥ 90 % line |
| `:adherence-tracking` | `io.nemopill.adherencetracking.application` | ≥ 80 % line |
| `:app` | — | No percentage threshold |

No threshold on `*.infrastructure.*` or `*.presentation.*` subpackages per `_context/05 § Engineering Quality`. No threshold on `:app` because its coverage is owned by integration and snapshot rows.

**Vacuous-pass convention:** all thresholds are vacuously satisfied at T-007 because all production source sets are empty. The first business code to land in M-002 will be the first real threshold measurement. The `koverVerify` task is expected to exit zero throughout T-007.

#### Consequences

- `./gradlew koverVerify` exits zero from root and per-module on a clean baseline.
- `./gradlew koverHtmlReport` produces the merged HTML report; the `coverage` CI stage uploads it as `kover-report` artifact (14-day retention), satisfying M-001 Done When item (4).
- Thresholds apply to named packages only; infrastructure and presentation layers are explicitly excluded from the coverage gate per `_context/05`.
- If Kover 0.8.x `koverReport` DSL proves incompatible with a future AGP upgrade, this ADR is the baseline; a superseding ADR records the migration path.

---

### ADR-077: Cross-environment role execution — Cowork hosts Architect / PM / Proposal-Mode turns; Claude Code hosts Apply-Mode Developer turns; `CLAUDE.md` remains the single environment-agnostic instruction surface and the file-based approval gate is authoritative in both

- Date: 2026-06-01
- Status: Proposed
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: No active task packet — operating-model decision recorded at the Human gatekeeper's request; `CLAUDE.md § Operating Modes`, `§ Multi-Agent Roles`, `§ Skills`; originates from the 2026-06-01 Cowork conversation on whether to migrate code-writing to Claude Code

#### Context

The project is currently driven from Claude Cowork. As M-001 implementation work scales up (T-006 delivered the six-module Gradle scaffold; T-007 is closing M-001), the Human gatekeeper asked whether the Developer role's code-writing should move to Claude Code instead of staying in Cowork. The two environments have different strengths: Cowork hosts the MCP connectors this project relies on for planning and traceability (Jira, Confluence, Slack, Figma, Trino/Athena) and is well-suited to document-centric Architect / PM turns and Proposal-Mode design; Claude Code offers a tighter edit → test → git loop better suited to the Apply-Mode Developer grind (multi-file edits, running the Gradle/Konsist/Robolectric suites, branch and PR operations). The risk in splitting environments is that the approval discipline could fragment if it depended on which tool was in use.

#### Decision

Role execution is split by environment, not migrated wholesale:

1. **Cowork** hosts Architect and Project-Manager turns and all Proposal-Mode work — `/start-working` contextualization sessions, `_context/08_active_task_packet.md` refreshes, milestone slicing, ADR drafting, and handoff-log maintenance. This is where the connectors and document workflows live.
2. **Claude Code**, opened at the repository root, hosts Apply-Mode Developer turns once a task packet is flipped to `Approved for apply` — file-heavy implementation in `_source/`, running the test suites, and git/PR operations.
3. **`CLAUDE.md` is the single, environment-agnostic instruction surface.** Both environments read the same `CLAUDE.md`, honor the same `_context/` read order, write to the same `_context/handoff_log.md`, and respect the same file-based gate: `Approved for apply` in `_context/08_active_task_packet.md` plus explicit human approval is the only authorization for `_source/` mutation, regardless of which tool is open. The gate is a property of the repo state, not the environment.

This decision codifies a routing convention; it does not change any role contract in `_context/14_agent_roles_and_handoffs.md` or any operating-mode rule in `CLAUDE.md`.

#### Alternatives Considered

- **Keep everything in Cowork** — viable and lowest-friction for small, few-file tasks; the cost of switching tools outweighs the tighter loop when implementation is light. Retained as the default for small Developer tasks; the Claude Code split is reserved for file-heavy or test-iteration-heavy work. Not chosen as the exclusive posture because the edit/test/git loop is slower here for large implementation slices.
- **Migrate the whole project to Claude Code** — rejected. It would strip the planning roles of the MCP connectors that supply traceability (Jira/Confluence/Slack links cited throughout the ADR set) and weaken the document-centric Architect / PM workflow that the framework depends on.
- **Make the approval gate environment-specific** (e.g., only Cowork may flip the gate) — rejected. The gate must be a single source of truth in repo state; tying it to an environment would create two divergent notions of "approved."

#### Consequences

- The edit → test → git feedback loop for large Apply-Mode slices tightens; Cowork retains the connector-backed planning surface.
- Claude Code must be opened at the repository root so it auto-loads `CLAUDE.md` and discovers `.claude/` (skills and commands); otherwise the framework rules and the `/start-working` accelerator will not be in effect.
- The split introduces a context-switch cost; the project accepts it only when implementation is file- or test-heavy, keeping small Developer tasks in Cowork.
- `_context/handoff_log.md` remains the cross-session, cross-environment continuity record; every role turn in either environment appends a handoff entry as before.
- `_context/14_agent_roles_and_handoffs.md` may be amended at a future PM/Architect turn to name the host environment per role for completeness; this ADR is the interim record.
- Status is `Proposed`; the Human gatekeeper can ratify to `Accepted`. No `_source/` change is implied, so this does not itself require an `Approved for apply` flip.

---

### ADR-076: CI unblock hot-fix — restore missing `gradle-wrapper.jar` (8.10.2) out-of-band of T-007 scope; adopt solo-dev branch protection (Option A: zero required reviews, retain all CI status checks)

- Date: 2026-06-01
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper, repo admin); Developer
- Related milestone or task: T-007 (M-001 close); PR #1 (`task-007-design` → `main`, https://github.com/irodriguez-io/NemoPill/pull/1); `_context/08_active_task_packet.md § AC-007(iii)`; `_context/12_environments_and_devops.md` (branch-protection policy)

#### Context

PR #1 is `BLOCKED` for two independent reasons surfaced during the T-007 push:

1. **CI red.** The required `1 · Setup` check fails at `gradle/actions/wrapper-validation@v3` with "Expected to find at least 1 Gradle Wrapper JARs but got only 0." Root cause confirmed by inspection: `_source/gradle/wrapper/gradle-wrapper.jar` was never committed and is absent locally — only `gradle-wrapper.properties` (pinning Gradle 8.10.2) and `libs.versions.toml` are tracked under `_source/gradle/`. It is not a `.gitignore` issue (`.gitignore` excludes only `.DS_Store` and `.claude/settings.local.json`). Checks 2–6 declare `needs: setup`, so they SKIP rather than run.
2. **Unmergeable review gate.** Classic branch protection on `main` sets `required_approving_review_count: 1` with `enforce_admins: true`. GitHub forbids a PR author approving their own PR, so for a solo developer with no second account the PR is structurally unmergeable, and `--admin` override is disabled by `enforce_admins`.

The missing JAR was a **known, deliberately-deferred gap**: `_context/08_active_task_packet.md` AC-007(iii) records it as an "onboarding note, no resolution needed in T-007," and the T-007 packet scopes the branch-protection edit as a Human-gatekeeper action at milestone close — not Developer-role work. Resolving either inside T-007 therefore falls outside the approved task scope, triggering the CLAUDE.md scope/stop-condition.

#### Decision

The Human gatekeeper (Isidro Rodriguez), who is also repo admin, **explicitly authorized an out-of-band hot-fix** to unblock M-001 close, accepting that this work runs ahead of / outside the T-007 planned scope. Two decisions:

1. **Restore the wrapper JAR now.** Add the official Gradle 8.10.2 `gradle-wrapper.jar` to `_source/gradle/wrapper/`, commit on `task-007-design`, and push. The source of truth is the official upstream JAR for tag `v8.10.2`; `wrapper-validation@v3` checksums it against the known-good registry, so any official 8.10.2 copy validates (the committed JAR's SHA-256 `2db75c40…448046` was cross-checked against Gradle's published `gradle-8.10.2-wrapper.jar.sha256` and matches exactly). This supersedes the AC-007(iii) "defer" note for the JAR specifically.

   **Second root cause found during the fix:** `_source/gradlew` line 100 was corrupted — `CLASSPATH="\$APP_HOME/..."` escaped the `$`, so `$APP_HOME` never expanded and the JVM received a literal, non-existent classpath, failing with `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain` at the "Prime Gradle cache" step *after* wrapper validation passed. Restored to the canonical unquoted `CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar`. Both the missing JAR and the launcher corruption were required to get `1 · Setup` green.
2. **Replace the network-dependent wrapper-validation action with a pinned-checksum check.** `gradle/actions/wrapper-validation@v3` failed on 3 of 4 runs with intermittent `connect ETIMEDOUT` to its Cloudflare-fronted checksum-registry endpoint (Gradle's own services reported 100% uptime, so the flakiness is runner→Cloudflare, not a Gradle outage). The `1 · Setup` step now verifies `_source/gradle/wrapper/gradle-wrapper.jar` against the pinned SHA-256 `2db75c40…448046` via `sha256sum -c` — offline, deterministic, and a stronger supply-chain guarantee than the action (it pins the exact expected wrapper version rather than accepting any known Gradle wrapper). Trade-off: the pinned hash must be updated whenever the Gradle version in `gradle-wrapper.properties` changes; a prominent comment in `ci.yml` documents this and points to https://gradle.org/release-checksums/. This is a deliberate change to a CI security control, made under the same gatekeeper-approved hot-fix.

3. **Adopt Option A branch protection (solo-dev posture).** Set `required_pull_request_reviews.required_approving_review_count` to `0` on `main` while retaining all required status checks (the current six; the two new T-007 checks `unit-integration-ui-snapshot` and `coverage` are added at T-007 close per the packet). The CI gate is the real safety net; the human-review gate is decorative for a single-author repo, especially with `dismiss_stale_reviews: true`. Options B (sock-puppet second account), C (`enforce_admins: false` bypass), and D (migrate classic protection → Ruleset with bypass actor) were rejected for solo-dev use; D remains the preferred path if a second collaborator ever joins.

#### Consequences

- `_source/gradle/wrapper/gradle-wrapper.jar` becomes a tracked binary; `./gradlew --version` must print `Gradle 8.10.2` cleanly before re-push, and CI `1 · Setup` (and downstream 2–6) should go green.
- `main` PRs merge as soon as CI is green, with no human approval required. The trade-off — losing the forced self-review-in-PR-view prompt — is accepted and mitigated by the multi-stage CI gate.
- The branch-protection change is applied by the admin via `gh api -X PATCH repos/irodriguez-io/NemoPill/branches/main/protection/required_pull_request_reviews -f required_approving_review_count=0` (token `repo` scope is sufficient). It must be performed on the gatekeeper's machine; it cannot be executed from the agent sandbox.
- `_context/12_environments_and_devops.md` should be updated to document the Option A branch-protection posture at T-007 close (Human-gatekeeper edit).
- This ADR is the audit record that the JAR restoration and protection change were a consciously-approved hot-fix executed out-of-sync with the T-007 task plan; the T-007 close handoff entry in `_context/handoff_log.md` should reference ADR-076 in place of the original AC-007(iii) "deferred" disposition.

---

### ADR-075: Konsist package path — `io.nemopill.core.konsist` adopted; `_context/08` typo noted

- Date: 2026-05-25
- Status: Accepted
- Owners: Developer; Isidro Rodriguez (Human gatekeeper)
- Related milestone or task: T-006; `_context/08_active_task_packet.md § Konsist Tests`; `_context/04_solution_architecture.md § Package Conventions`

#### Context

`_context/08_active_task_packet.md` specified the Konsist test package path as `com.nemopill.core.konsist` — using the `com.` prefix, which diverges from the project-wide `io.nemopill.*` namespace established in `_context/04_solution_architecture.md`. The two files disagreed; the CLAUDE.md stop-condition for inter-file conflicts was triggered.

#### Decision

Use `io.nemopill.core.konsist` for the Konsist test package, consistent with `_context/04`. The `com.nemopill` reference in `_context/08` is a transcription error introduced during PM contextualization. No behavior change — the package path affects only test class names and Gradle test filter patterns.

#### Consequences

- All six Konsist rule files are written to `io/nemopill/core/konsist/` under `_source/core/src/test/kotlin/`.
- The CI `arch-conformance` stage filters tests with `--tests "io.nemopill.core.konsist.*"`.
- `_context/08_active_task_packet.md` should be corrected to `io.nemopill.core.konsist` at next PM task refresh (T-007 or later).
- No ADR backfill required; the `io.nemopill.*` namespace was already settled in ADR-009 / `_context/04`.

---

### ADR-074: PendingIntent FLAG_IMMUTABLE rule uses text-scan heuristic at Konsist 0.17.0

- Date: 2026-05-25
- Status: Accepted
- Owners: Developer; Isidro Rodriguez (Human gatekeeper)
- Related milestone or task: T-006; ADR-023; `_context/05_engineering_quality_security_and_compliance.md § Security Guardrails`

#### Context

ADR-023 mandates that every `PendingIntent.getBroadcast / getActivity / getService / getForegroundService` call include `PendingIntent.FLAG_IMMUTABLE`. Konsist 0.17.0 exposes file text but its AST-level bitwise-expression analysis is not reliable enough to parse `flags or FLAG_IMMUTABLE` combinator patterns. A strict per-call AST inspection would require either upgrading Konsist (not yet available) or writing a custom PSI visitor (out of T-006 scope).

#### Decision

Implement a text co-occurrence heuristic: if a production file imports `android.app.PendingIntent` AND calls any factory method, the rule asserts that the string `FLAG_IMMUTABLE` appears anywhere in the file text. This catches the most dangerous omissions (files that never use the flag at all) while accepting the narrow false-negative of a file that uses FLAG_IMMUTABLE in one call but omits it in another. A TODO comment in `PendingIntentFlagImmutableRule.kt` records this gap.

#### Consequences

- Rule implementation is simpler and more readable.
- Narrow false-negative remains: multi-call files could hide a single non-compliant call. Accepted for M-001 scope; upgrade to per-call AST analysis deferred to a future Konsist version.
- Negative tests verify both the detection path (absent flag) and the pass path (flag present).

---

### ADR-073: T-006 Foundation implemented — Gradle scaffolding, Konsist suite, pre-commit hook, CI pipeline

- Date: 2026-05-25
- Status: Accepted
- Owners: Developer; Isidro Rodriguez (Human gatekeeper)
- Related milestone or task: T-006; M-001 Foundation (Planned); `_context/08_active_task_packet.md`; `_context/12_environments_and_devops.md`

#### Context

M-001 Foundation requires a compilable six-module Gradle project, architecture-conformance tests wired to CI, and branch-protection tooling before any feature code is written. T-006 is the first Apply Mode task that writes to `_source/`, `_build/`, and `.github/`.

#### Decision

All T-006 deliverables created in a single Apply Mode session:

- **`_source/`**: `settings.gradle.kts`, root `build.gradle.kts` (with `installGitHooks` task), `gradle.properties` (configuration cache, parallel builds), `gradle/libs.versions.toml` (version catalog), `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.10.2), `gradlew` / `gradlew.bat`.
- **Module build files**: `:app` (Android application, compileSdk=35, minSdk=26, targetSdk=35, Java 17), `:core` (kotlin-jvm, Konsist on testImplementation), `:medication-management`, `:scheduling`, `:notifications`, `:adherence-tracking` (Android library, same SDK targets).
- **Manifests**: `app/src/main/AndroidManifest.xml` — `allowBackup="false"`, `dataExtractionRules`, zero `<uses-permission android:name="android.permission.INTERNET"/>`. `app/src/main/res/xml/data_extraction_rules.xml` — excludes all domains from both `<cloud-backup>` and `<device-transfer>`. Minimal `AndroidManifest.xml` for each feature library module.
- **Konsist rules** (`_source/core/src/test/kotlin/io/nemopill/core/konsist/`): `PriorityOneInternetPermissionAllowListRule`, `NoNetworkImportsRule`, `DomainLayerNoAndroidRule`, `NoUpwardLayerDependencyRule`, `NoCrossFeatureDomainImportRule`, `PendingIntentFlagImmutableRule` — each with positive and negative test classes.
- **Pre-commit hook**: `_build/hooks/pre-commit` — gitleaks → framework validator → ktlintCheck in sequence.
- **CI**: `.github/workflows/ci.yml` — six jobs (setup, lint, secret-scan, framework-validate, arch-conformance, build). `.gitleaks.toml` — extends default ruleset with Android/Gradle-specific patterns; excludes `fixtures/` and `build/`.

#### Consequences

- M-001 Done-When items 1 (compilable modules), 2 (Konsist rules passing), 3 (pre-commit hook), 5 (gitleaks in CI), 6 (framework validate in CI) are satisfied.
- M-001 Done-When item 4 (≥ 80% unit/integration coverage) and item 7 (Robolectric green) are deferred to T-007 per task packet.
- `_source/` is now the Gradle project root; all CI `run` steps use `working-directory: _source`.
- The `installGitHooks` task is wired to the root `build` lifecycle; new contributors receive the hook automatically after their first `./gradlew build`.
- KSP annotation processing (Room, Hilt) is listed as `annotationProcessor` for now — switching to KSP plugin is deferred to the first feature module task that actually implements Room or Hilt (T-007 or later).

---

### ADR-072: M-000 Design Tail Closure milestone complete — files 12 / 11 / 09 / 10 / 13 contextualized, validator green, file-07 reconciled

- Date: 2026-05-21
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Project Manager
- Related milestone or task: M-000 close at T-005; `_context/07_delivery_plan_and_milestones.md § Milestone Register M-000`; `_context/13_threat_model_and_data_classification.md` (the final design-tail artifact); T-005 + M-000-close handoff entry

#### Context

M-000 (Design Tail Closure) was created at ADR-038 to formally track the design-tail file series (09, 10, 11, 12, 13) so the framework was fully contextualized before any M-001 implementation work began. Per `_context/07 § Milestone Register`, the eight Done When items enumerate: (1)-(5) per-file contextualization sessions for files 12 / 11 / 09 / 10 / 13; (6) validator exits zero at close; (7) per-file handoff entries appended; (8) M-000-close handoff entry appended. At T-004 close (2026-05-20T18:40:07Z), the M-000 progress meter stood at `4 / 5 done-when items satisfied` (files 12, 11, 09, 10 closed; 13 remaining). T-005 closes file 13.

#### Decision

M-000 is **complete** at T-005 close on 2026-05-21. All eight Done When items satisfied: (1) `_context/12_environments_and_devops.md` closed at T-001 (2026-05-17T22:33:21Z); (2) `_context/11_visual_language_and_design_system.md` closed at T-002 (2026-05-18); (3) `_context/09_decision_log.md` closed at T-003 (2026-05-20T00:44:41Z); (4) `_context/10_non_functional_requirements.md` closed at T-004 (2026-05-20T18:40:07Z); (5) `_context/13_threat_model_and_data_classification.md` closed at T-005 (2026-05-21); (6) `bash _framework/validate_framework.sh` exits zero post-T-005 close; (7) all per-file handoff entries appended to `_context/handoff_log.md` across T-001..T-005; (8) M-000-close handoff entry appended at T-005 close (this handoff entry).

Per ADR-039 "Touch file 07 once at M-000 close, not incrementally," the file-07 reconciliation is also part of this ADR's scope: (a) the four design-tail rows in `_context/07 § Dependency And Blocker Register` (covering files 09, 10, 11, 12, 13 — four rows because files 10 and 13 share a combined row) are marked **Resolved 2026-05-21 at T-005 close** in their Type cells; (b) the M-000 row of `§ Milestone Register` has its `Status` field flipped from `Active` to `Complete`.

Next milestone is **M-001 — Foundation And Quality Gates** per ADR-036; its `Prerequisites` cell in `_context/07 § Milestone Register` already names "`M-000` complete" so no edit there. T-006 (the next task packet) will be the first M-001 task slice; the next PM-refresh conversation begins M-001 reviewable scope.

#### Alternatives Considered

- **Defer M-000 close until T-005 + a separate close-evidence conversation** — rejected. The T-005 packet `AC-004` explicitly names T-005 close as the M-000 close in a single handoff entry; splitting close-evidence into a separate conversation would violate the "Touch file 07 once at M-000 close" discipline of ADR-039.
- **Bundle file 13 close + M-000 close into separate ADRs** — rejected. M-000 close is a single delivery event; recording it as a single ADR honors the post-T-003 file-09 amendment convention.

#### Consequences

- M-001 reviewable scope opens for the next PM-refresh conversation. T-006 packet refresh against the first M-001 task slice (e.g., six-module Gradle scaffolding + Konsist + Kover + pre-commit hook + framework validator merge-blocking wiring) is the next role's responsibility.
- The five-task design-tail sequence (T-001 → T-002 → T-003 → T-004 → T-005, locked at the 2026-05-17T14:23:24Z handoff per ADR-040) is **complete**. Future task IDs (T-006+) target M-001 implementation slices, not design-tail files.
- The Architect role's "stay in design phase, do not write to `_source/`" guard relaxes for M-001 tasks per CLAUDE.md's role contract — M-001 task packets will explicitly route Architect → Developer turns.
- The post-T-003 file-09 amendment convention (new ADRs land at Accepted directly; supersession ADRs land at Proposed) is now exercised across T-004 (3 new ADRs at Accepted) and T-005 (5 new ADRs at Accepted, no supersessions); the convention is durable.

### ADR-071: `THR-###` threat ID convention for `_context/13` STRIDE table — disambiguates from `T-###` task-packet IDs from ADR-039

- Date: 2026-05-21
- Status: Accepted
- Owners: Architect
- Related milestone or task: M-000 / T-005; `_context/13_threat_model_and_data_classification.md § STRIDE Threat Table`; T-005 + M-000-close handoff entry

#### Context

The framework template for file 13 (`_framework/13_threat_model_and_data_classification.md`) uses `T-001` etc. as the example Threat ID prefix in its `## STRIDE Threat Table` section. NemoPill's task-packet ID convention from ADR-039 also uses `T-###` (T-001 through T-005 are real task packets to date). Without disambiguation, any future grep for `T-005` would return both the T-005 task packet and any future STRIDE row coincidentally numbered 005.

#### Decision

Threat IDs in `_context/13_threat_model_and_data_classification.md` use the **`THR-###`** prefix. Sequential numbering from `THR-001`; current baseline is `THR-001` through `THR-016` (16 rows; `THR-007` Repudiation resolves to `Not applicable` per AC-002 but the row is preserved). Future threat rows added to file 13 continue the `THR-###` sequence.

The convention applies only to file 13. The `T-###` prefix continues to mean task-packet ID per ADR-039.

#### Alternatives Considered

- **Keep `T-###` per framework-template default and disambiguate by context** — rejected. Grep-pain at scale; the file-13 STRIDE table will grow as features add new attack surfaces, and confusing threat rows with task packets in handoff-log entries would be brittle.
- **Use a category-based prefix (`SPF-001` for Spoofing, `TMP-001` for Tampering, etc.)** — rejected. Category prefixes would require renumbering when a row changes category (rare but real), and the STRIDE category is already a column in the table; the prefix needn't duplicate it.

#### Consequences

- Any future file-13 STRIDE row uses `THR-###` and continues sequential numbering from the file-13 baseline.
- Abuse case IDs in `_context/13 § Abuse Cases` use the `A-###` prefix (current baseline `A-001` through `A-006`); this convention is established by file 13 §5 itself and does not need a separate ADR.
- The PM-refresh conversation that produces T-006 must avoid issuing a task-packet ID that clashes with the THR-### namespace (T-006 is fine; THR-006 and T-006 are different namespaces — grep discipline maintained).

### ADR-070: Patient-facing Terms of Service authoring scope and content requirements deferred to M-006 alongside Spanish Privacy Policy translation

- Date: 2026-05-21
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-005 (decision); M-006 (deferred for execution per ADR-061); `_context/13_threat_model_and_data_classification.md § Open Security Questions Q2`; T-005 + M-000-close handoff entry

#### Context

NemoPill does not currently have a Patient-facing Terms of Service document. The bilingual Privacy Policy at `legal/PRIVACY_POLICY.{en,es}.md` per ADR-025 / ADR-048 (with Spanish long-form translation deferred to M-006 per ADR-061) is the only published content contract. The file-13 threat-model analysis surfaced multiple guidance items that **must** reach the Patient via a Patient-facing legal artifact — and the Privacy Policy is the wrong surface for them (it describes how data is handled, not what the Patient must do). The Architect-role conversation that drafted file 13 surfaced these requirements during §1 (OS-trust assumption), §3 (Privacy Policy + ToS publishing boundary), §5 (abuse-case responses), and §9 Q2 (the dedicated open question).

#### Decision

NemoPill ships a Patient-facing Terms of Service document. Authoring is deferred to the same future task that completes the Spanish Privacy Policy translation per ADR-061 (currently planned for M-006 pre-launch). The ToS is drafted alongside the Privacy Policy translation; both ship as a coordinated bilingual legal artifact set in `legal/PRIVACY_POLICY.{en,es}.md` + `legal/TOS.{en,es}.md` (path convention to be confirmed by the future task; convention follows ADR-048's path pattern).

**Four mandatory content requirements** established by `_context/13 § Open Security Questions Q2`:

- **(a) OS-trust-as-hard-prerequisite acknowledgment** per `_context/13 § Scope And Assumptions` Trust Assumption (i) — Patient is informed that if the Android OS, lock screen, biometric subsystem, AlarmManager, Keystore, or Auto Backup `dataExtractionRules`-honoring subsystem is compromised, NemoPill cannot defend itself and accepts that residual; Patient is responsible for keeping device OS up to date.
- **(b) Pre-discard checklist** per `_context/13 § Abuse Cases A-002 Response` and ADR-069 Compensating control (i) — (1) Settings → "Delete all my data"; (2) uninstall NemoPill; (3) Android factory reset — mandatory pre-discard guidance.
- **(c) Lock-screen sensitive-content recommendation** per `_context/13 § Abuse Cases A-003 Response` — Android Settings → Notifications → "Show sensitive content on lock screen: Off" is the strongest mitigation against shoulder-surfing; NemoPill cannot enforce from app code.
- **(d) Biometric-gate enable + audit-enrolled-biometrics recommendations** per `_context/13 § Abuse Cases A-001 / A-004 / A-006` — Patient informed that (i) app-level `BiometricPrompt` gate is default off per ADR-019; enabling it is the single most effective in-product mitigation against TA-2 / TA-3; (ii) `BiometricPrompt` accepts any device-enrolled biometric by Android API contract — Patients in shared-device relationships should periodically audit OS-enrolled biometrics.

#### Alternatives Considered

- **Embed the four guidance items in the Privacy Policy** — rejected. Privacy Policy describes data processing (Article 13 / CCPA notice format), not Patient behavior obligations; mixing the two confuses the document genre and risks regulatory non-compliance (Patient-behavior text in a Privacy Policy isn't enforceable as a contract).
- **Surface the four items via UI tooltips or onboarding screens** — rejected. UI surfaces are too transient (tooltips dismiss; onboarding runs once and is forgotten); the items need a durable, referenceable artifact.
- **Surface via release notes per ADR-047** — rejected. Wrong audience (release notes target Patients reading what's new in this version, not Patients building a long-term security mental model).
- **Defer ToS authoring indefinitely** — rejected. The ratification of ADR-043 in ADR-069 is *conditional* on the pre-discard checklist landing in ToS as a compensating control; deferring ToS indefinitely would unmoor ADR-069's compensating-control architecture.

#### Consequences

- The M-006 task slice must include ToS authoring as a Done When item (alongside the existing Privacy Policy Spanish translation per ADR-061).
- Content drift between `_context/13 § Open Security Questions Q2` and the eventual `legal/TOS.{en,es}.md` is a Security-role guardrail per `_context/05 § Quality Gates` — the four mandatory content requirements must be cited in the ToS-authoring task packet's Acceptance Criteria.
- The four ToS content requirements are reproduced in `_context/13 § Open Security Questions Q2` for direct citation by the future task; this ADR is the durable record.
- A future regulatory bar change (Chile GDPR-style law replacement, Brazil LGPD re-entry) may add further ToS content requirements; this ADR is amended (not superseded) when that happens.

### ADR-069: ADR-043 (plain-text Room encryption) ratified at T-005 re-evaluation — compensating controls via ToS authoring (ADR-070) and three revisit triggers

- Date: 2026-05-21
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-005; `_context/13_threat_model_and_data_classification.md § Open Security Questions Q1`; supersedes nothing — ADR-043 unchanged; T-005 + M-000-close handoff entry

#### Context

ADR-043 (plain-text Room encryption decision queued for re-evaluation at T-005) explicitly deferred the ratify-or-supersede decision to T-005 against the file-13 adversary enumeration. `_context/13 § STRIDE Threat Table THR-010` scores the Information Disclosure surface of plain-text Room against TA-4 (forensic recovery off discarded hardware) at Likelihood `2` × Impact `4` = `medium` residual. Three outcomes were permitted by the T-005 packet: (a) ratify ADR-043 (no new ADR for the encryption decision itself); (b) supersede with SQLCipher full-database encryption (Proposed ADR); (c) partial column encryption (Proposed ADR).

#### Decision

**Option A — Ratify ADR-043** (plain-text Room kept). The ratification rests on four observations from the file-13 analysis (detailed in `_context/13 § Open Security Questions Q1`):

1. TA-4 Likelihood is genuinely `2` — modern Android factory-reset on stock devices reliably wipes app data; chip-off forensics is rare for medication-adjacent data (low resale value).
2. The cheapest residual reduction is on the Patient-action side (pre-discard checklist in ToS), not the cryptographic side.
3. Option B (SQLCipher) introduces new residuals around Android Keystore brittleness (Keystore key loss on factory reset is permanent) and TA-6 supply-chain enlargement (SQLCipher dependency expands the existing supply-chain surface from `THR-005` / `THR-012`).
4. Option C (partial column encryption) doesn't substantively reduce TA-4 — Adherence patterns are themselves disclosive even with medication names hidden.

**Three compensating controls** make this ratification durable:

- **(i) Pre-discard checklist as mandatory ToS content** — see ADR-070; the upcoming ToS must include (1) Settings → "Delete all my data"; (2) uninstall NemoPill; (3) Android factory reset — in that order, before discarding the device. This addresses the TA-4 root cause (discarded hardware retaining data) at far lower cost than SQLCipher integration.
- **(ii) Revisit triggers for encrypted-Room** — Option B becomes the default recommendation if any of the following triggers fires: (a) a Patient-data-richer feature lands (free-text notes, photo attachments, Patient-name capture, contact-info capture); (b) the regulatory bar rises (Chile's pending GDPR-style law replacement activates encryption-at-rest expectations, Brazil LGPD re-entry, CCPA / CPRA reasonable-security-measures interpretation tightens); (c) a post-launch incident in the TA-4 class occurs (Patient-reported data-recovery breach via GitHub Issues per ADR-061, security-researcher disclosure). When any trigger fires, a Proposed ADR superseding ADR-043 with Option B is the standard response.
- **(iii) `_context/13 § Scope And Assumptions` Trust Assumption (i) is load-bearing** for this decision — the OS-trust-as-hard-prerequisite assumption is acknowledged as load-bearing in `_context/13 §1` already; the ratification of ADR-043 is conditional on that assumption holding.

ADR-043 itself is **unchanged**. This ADR is the record of the T-005 re-evaluation event and the compensating-control architecture; ADR-043's `Status` remains `Accepted` and its text is not amended.

#### Alternatives Considered

- **Option B — Supersede ADR-043 with SQLCipher full-database encryption** — rejected per the four observations above. Trade-off summary: TA-4 residual drops `medium` → `low`, but new residuals are introduced around Android Keystore brittleness and supply-chain enlargement; cold-start performance hit must be measured against the `_context/10 § Performance Targets` ADR-066 p95 ≤ 2 s NFR before commitment.
- **Option C — Partial column encryption (medication-name only)** — rejected per observation 4 above. Adherence dosing patterns reveal medication class even when the name is hidden, so the marginal protection of column-level encryption doesn't justify the per-column complexity in `:medication-management`.
- **Defer the decision to a future ratification conversation** (Option γ from `_context/13 § Open Security Questions Q1` framing) — rejected because the file-13 analysis is sufficient to settle the decision now; deferring would consume an additional conversation cycle without producing additional analysis.

#### Consequences

- M-001 / M-003 schema work proceeds against plain-text Room per ADR-043; no SQLCipher dependency added to `gradle/libs.versions.toml`.
- The pre-discard checklist (compensating control (i)) is now a hard ToS content requirement per ADR-070 — the M-006 task slice must surface this.
- The three revisit triggers are durable; any future PM-refresh or Architect-role conversation that observes a trigger event must surface it for gatekeeper review.
- If the gatekeeper later reverses this ratification (e.g., a TA-4 class incident occurs), a Proposed ADR superseding ADR-043 with Option B lands at `Status: Proposed` per the post-T-003 supersession convention.

### ADR-068: Canonical device-locked threat envelope — six-actor adversary list (TA-1..TA-6) + four explicit out-of-scope adversary classes + OS-trust-as-hard-prerequisite trust assumption

- Date: 2026-05-21
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-005; `_context/13_threat_model_and_data_classification.md § Scope And Assumptions`; T-005 + M-000-close handoff entry

#### Context

Prior to T-005, the threat-model adversary list was diffuse across `_context/05 § Security Guardrails` (device-locked posture, optional biometric, no-Patient-data-in-failure-evidence, hard-delete-only, no-`HIPAA` posture), `_context/06 § Trust Boundaries And Validation Ownership`, and ~12 file-09 ADRs that mention specific adversaries by context (ADR-021 no-network attack surface; ADR-031 forensic-evidence concerns; ADR-022 deliberate-confirmation pattern). Without a canonical adversary list, any new security ADR or section addition risked re-deriving the adversary surface from scratch, leading to drift.

#### Decision

The canonical NemoPill adversary list — to be cited by future security-adjacent decisions — is the six-actor enumeration locked in `_context/13 § Scope And Assumptions § Threat actor profile`:

- **`TA-1`** — Opportunistic shoulder-surfer or nearby observer; Capability `1`
- **`TA-2`** — Trusted-circle adversary (family, roommate, caregiver, partner) with intermittent unsupervised access to an unlocked device; Capability `3`
- **`TA-3`** — Opportunistic thief of an unlocked device; Capability `2`
- **`TA-4`** — Forensic recovery off discarded / sold / donated hardware; Capability `3`
- **`TA-5`** — Co-installed malicious app on the same device; Capability `4`
- **`TA-6`** — Compromised software supply chain (malicious Gradle dependency, direct or transitive); Capability `5`

**Four explicit out-of-scope adversary classes** are also locked:

- Nation-state or law-enforcement-grade forensics against a *locked* device.
- Targeted attacker with physical custody, unlock, and unlimited time (border seizure, court-ordered custody-dispute access).
- Compromise of Google Play App Signing or Patient's Google account takeover.
- Malicious insider in the organizational sense (no organization, no employees in single-Patient framing).

**The OS-trust-as-hard-prerequisite trust assumption** is also locked: if the Android OS, lock-screen subsystem, `BiometricPrompt`, AlarmManager, Android Keystore, or Android Auto Backup honoring `dataExtractionRules` is compromised, NemoPill cannot defend itself and accepts that residual. This is the load-bearing assumption that underpins ADR-021 (no-`INTERNET`), ADR-019 (optional biometric), ADR-041 (no-cloud-backup + dataExtractionRules + Play App Signing), ADR-067 (on-device-only logging), and (via ADR-069's compensating-control structure) ADR-043 (plain-text Room).

#### Alternatives Considered

- **Treat each ADR's adversary mention independently** (status quo prior to T-005) — rejected. Cross-file drift risk; each new ADR would re-derive its adversary surface, leading to inconsistent citations.
- **Adopt an industry threat-model template** (MITRE ATT&CK Mobile, OWASP Mobile Top 10, NIST SP 800-30) — rejected. None of these maps cleanly onto single-Patient device-locked Android with no network surface; the cited categories (e.g., network-based, lateral movement, exfiltration over network) don't apply; forcing them in would dilute the actual NemoPill threat surface.
- **Enumerate per-feature adversary lists** — rejected. The adversary list is product-wide (a shoulder-surfer threatens any in-app feature, not feature-specific), so the canonical list lives at the product level.

#### Consequences

- Future security-adjacent decisions cite TA-1..TA-6 by ID rather than re-deriving the adversary set.
- Adding a new adversary class requires a Proposed ADR superseding ADR-068 (e.g., if a future feature introduces a network surface and a network attacker class becomes relevant, ADR-068 is superseded by a new ADR enumerating the expanded list).
- The four out-of-scope classes are explicit; future product decisions that would bring any of them into scope (e.g., adding multi-tenant support brings "malicious insider" into scope) require revisiting this ADR.
- The OS-trust assumption being load-bearing means any change to the OS-vendor relationship (e.g., distributing via F-Droid in addition to Play Store, which would re-evaluate the App Signing trust assumption) requires revisiting this ADR.

### ADR-067: On-device-only logging contract — Log.e/Log.w for Result.Err.Unexpected only + Play Console Android Vitals as the sole remote evidence stream

- Date: 2026-05-20
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-004; `_context/10_non_functional_requirements.md § Observability And Operability`; T-004 handoff entry

#### Context

`_context/05 § Observability Expectations` already committed to a minimal logging policy (Log.e for `Result.Err.Unexpected` only; no analytics SDK in MVP) and `_context/12 § Monitoring And On-Call` already committed to Google Play Console Android Vitals as the crash-reporting surface (with Konsist redaction rules forbidding Patient data in `throw` statements and Domain `data class` `toString()`). These commitments were captured in two different files at two different milestones; the file-10 NFR register restates them but the cumulative guarantee — "the only evidence streams NemoPill produces are on-device logcat (volatile, OS-managed retention) and Google Play Console Android Vitals (anonymized, Google-managed retention)" — has not been recorded as a single durable decision that future product surfaces (e.g., a hypothetical caregiver-share feature or post-MVP server-side adherence aggregation) must explicitly supersede.

#### Decision

NemoPill commits to **on-device-only logging plus Google Play Console Android Vitals as the sole remote evidence stream** as a single durable architectural property. Concretely: (a) `Log.e` / `Log.w` for the `Result.Err.Unexpected` branch only — never for expected business-rule failures, which are surfaced as Patient-facing UX copy via `Result.Err`; (b) tag convention `NemoPill.<module>.<class>`; (c) every log line is Patient-data-free per the file-12 Konsist redaction rules (no string-template `throw` messages; redacted `toString()` on every Domain `data class`); (d) Google Play Services collects and transmits stack-trace-plus-device-metadata Android Vitals payloads with no app-private state per the redaction rules; (e) Patients can opt out at the OS level via Google Play Services → Usage & diagnostics; (f) no third-party crash reporter (Crashlytics, Sentry, Bugsnag, Rollbar), no on-device crash log file, no analytics SDK, no remote logging vendor.

#### Alternatives Considered

- **Third-party crash reporter (Firebase Crashlytics or equivalent)** — rejected because every such reporter requires `android.permission.INTERNET`, contradicting `_context/05 § Data protection` sub-rule (b) and the Konsist `arch-conformance` no-network-imports rule. The cost of adding a vendor + revising the privacy posture exceeds the marginal benefit over Play Console Android Vitals for a Reminder-and-Confirmation app whose crash surface is small.
- **On-device crash log file with Patient-driven export** — considered and deferred per `_context/12 § Monitoring And On-Call`'s "Revisit condition" bullet. Adds an export affordance that contradicts `_context/05 § Data protection`'s no-export rule. Re-evaluated if Android Vitals turns out to miss a meaningful class of failures.
- **Verbose logging across all use cases** — rejected because (i) it inflates logcat noise and reduces signal-to-noise during debugging, (ii) it creates risk of accidentally including Patient data in log lines, (iii) Domain events are already observable via `_context/04 § DomainEventPublisher` for debug-time introspection without standing log emission.

#### Consequences

- Positive: single, defensible evidence-stream contract that future product surfaces must explicitly supersede via a fresh ADR; aligned with the no-`INTERNET` posture; minimal logcat noise; minimal cross-border data flow (only the Google-mediated Android Vitals path); zero project-paid observability cost.
- Negative: limited debugging information for genuinely surprising production failures; reliance on Google's continued operation of Play Console Android Vitals and its retention policy; no observability for devices without Google Play Services (Huawei devices in some markets, custom Android variants); no on-device crash log the Patient can share via support channels (which is intentional — `_context/12 § Monitoring And On-Call § On-call expectations` confirms no Patient-facing support channel in MVP).
- Follow-up: if a future product surface (caregiver feature, server-side feature, post-MVP analytics) requires a different evidence-stream model, the change requires a fresh ADR superseding this one *and* superseding `_context/05 § Data protection` sub-rule (b) and `_context/05 § Observability Expectations`.

### ADR-066: App cold-start NFR target — p95 ≤ 2 seconds on minSdk = 26-class hardware

- Date: 2026-05-20
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-004; `_context/10_non_functional_requirements.md § Performance Targets`; T-004 handoff entry

#### Context

The Patient opens NemoPill primarily during or after a Reminder to confirm a Dose. A slow cold-start delays the Confirmation interaction and erodes trust in the on-time experience that the entire architecture (file-04 `AlarmManager.setAlarmClock` choice, file-06 BR-010 1-hour late window, file-10 ±60-second firing latency) is calibrated to deliver. Prior `_context/` files did not pin a numeric cold-start target; the closest references are `_context/03 § UX Constraints And Rules § Responsiveness or device constraints` ("the lock-screen Reminder must be glanceable in under 2 seconds") and Google's Android Vitals threshold for "slow cold-start" (≥ 5 seconds). Without a project-locked numeric target, future milestones cannot evaluate whether a Compose-screen addition or a DI-graph change has regressed cold-start performance.

#### Decision

NemoPill commits to **app cold-start to today's Dose list visible at p95 ≤ 2 seconds on minSdk = 26-class hardware**, measured via `Activity.reportFullyDrawn()` and the Google Play Console Android Vitals "Startup time" panel post-launch. The 2-second target is comfortably inside Google's "good" band (< 5 seconds is "slow") and matches the file-03 "glanceable in under 2 seconds" framing that already applies to the lock-screen Reminder presentation. Validated at M-006 close per `_context/07 § M-006 Done When` item (6) and re-validated after each milestone whose scope adds Compose screens (M-003, M-005, M-006).

#### Alternatives Considered

- **Looser target (p95 ≤ 5 seconds)** — rejected because it sits at Google's Android Vitals "slow" threshold; landing the project floor at the Google-flagged "your app is slow" line gives no headroom for milestone additions.
- **Tighter target (p95 ≤ 1 second)** — rejected because Kotlin + Compose + Hilt + Room cold-start on minSdk = 26-class hardware regularly lands in the 1–2 second band at typical DI-graph sizes; pinning the target below the band would force premature optimization that competes with file-05 § TDD-workflow's tests-first discipline.
- **No numeric target (qualitative "the app should feel fast")** — rejected because the file-10 NFR register explicitly forbids adjective-only targets per `_context/10 § Performance Targets`'s framework-template guidance ("Prefer numeric, observable targets over adjectives").

#### Consequences

- Positive: explicit threshold the M-006 walkthrough and post-launch Android Vitals monitoring can evaluate against; gives a fail-fast signal if a future DI / Compose / Room change regresses cold-start; consistent with file-03's "glanceable" framing.
- Negative: target may need re-validation if Android 16 / minSdk uplift changes the device-class baseline; minSdk = 26 hardware is at the older end of Google's "currently-supported Android" range and the target is intentionally generous for that hardware (newer devices will be faster).
- Follow-up: M-001 foundation milestone wires the `Activity.reportFullyDrawn()` instrumentation so milestone-close walkthroughs have a measurement surface. If post-launch Android Vitals shows p95 trending above 2 seconds, this ADR is revisited rather than the threshold silently relaxed.

### ADR-065: Reminder firing latency NFR target — ±60 seconds p95 vs. Dose.scheduledAt, bounded by AlarmManager.setAlarmClock semantics

- Date: 2026-05-20
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-004; `_context/10_non_functional_requirements.md § Performance Targets`; T-004 handoff entry

#### Context

`_context/04 § Technology And Runtime Decisions § Exact-time scheduling` chose `AlarmManager.setAlarmClock` over `WorkManager` periodic / expedited work precisely because sub-minute precision is required ("`WorkManager`'s expedited / periodic work is too imprecise for a 09:00:00 Dose"). `_context/06 § BR-010` and `_context/01 § Resolved Product Decisions Q1` pinned the 1-hour late-Reminder window as the deadline by which `pending` Doses must transition to `missed`. Between the sub-minute precision floor of `setAlarmClock` and the 1-hour BR-010 deadline lies an unpinned envelope: how close to `Dose.scheduledAt` must the Reminder fire on the happy path? Without a project-locked threshold, the M-002 and M-004 manual Doze tests have no numeric pass/fail criterion; the file-03 AC-002 ("within a tolerance acceptable for typical morning, midday, and evening Dose times") is suggestive but not testable.

#### Decision

NemoPill commits to **Reminder firing latency ±60 seconds of `Dose.scheduledAt`, p95, when the device is awake or in Doze and the alarm was previously registered via `AlarmManager.setAlarmClock`**. The threshold matches `AlarmManager.setAlarmClock`'s documented OS-imposed best-effort exactness — the API contract that file-04 chose. Measured at the `Instant` at which `notifications::ReminderFiredListener` observes the `ReminderFired` domain event; validated under Robolectric integration tests for the deterministic path and on real hardware in the M-002 walking-skeleton + M-004 manual Doze test per `_context/07 § M-002` / `§ M-004`. Late firings beyond +60 seconds but within +1 hour are governed by `_context/06 § BR-010`'s late-Reminder window, not by this threshold.

#### Alternatives Considered

- **Tighter target (±30 seconds p95)** — rejected because it overpromises against `setAlarmClock`'s documented typical drift under Doze. The M-004 hardware Doze test would likely fail this threshold unless the OS happens to deliver alarms faster than its own contract, which is not a reliable basis for an NFR.
- **Looser target (±120 seconds p95)** — rejected because closely-spaced TimesOfDay (e.g., an 08:00 Dose and an 08:05 Dose for two different Medications per `_context/03 § EC-002`) could fire 2 minutes apart and the Patient would perceive the misalignment.
- **No latency target, only the BR-010 1-hour late-window deadline** — rejected because the file-10 NFR register requires numeric observable thresholds per the framework template's `## How To Fill This File` guidance and per the T-004 packet's `AC-002` rule, and because milestone walk-throughs need a pass/fail criterion finer than BR-010's 1-hour deadline.

#### Consequences

- Positive: gives the M-002 / M-004 manual Doze tests a concrete pass/fail threshold; explicitly defers to `AlarmManager.setAlarmClock`'s OS contract rather than overpromising against it; preserves BR-010's 1-hour window as the late-firing budget independent of the on-time precision target.
- Negative: a single OS-released update that tightens Doze-throttling behavior could push p95 above the ±60-second threshold and force an ADR-revisit (this is acknowledged as a known platform-behavior risk in `_context/07 § Dependency And Blocker Register`'s Android-OS-background-execution row).
- Follow-up: the M-004 manual Doze test is the empirical confirmation point for this threshold on the developer's hardware; the post-launch Google Play Console Android Vitals dashboard does not directly measure Reminder firing latency, so post-launch validation relies on Patient-reported issues plus the M-004 / fix-forward re-test discipline per `_context/12 § Operational Runbooks § Fix-forward release after a Production bug`.

### ADR-064: T-003 inline-authorization caveat — one-time compression of PM-refresh + gatekeeper-flip + Architect-execution into one conversation

- Date: 2026-05-20
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect (inline PM + gatekeeper + Architect compression)
- Related milestone or task: M-000 / T-003; `_context/08_active_task_packet.md` Task Status `Inline-authorization caveat`; T-003 handoff entry

#### Context

T-001 and T-002 followed a three-conversation sequence — Project Manager refreshes `_context/08` to the next T-### at `Ready for proposal`, Human gatekeeper flips `Task status` to `Approved for apply`, then a fresh `/start-working` conversation runs Architect execution. For T-003 the Human gatekeeper opted to compress the three roles into one conversation to avoid spinning up two preparatory sessions for a primarily mechanical backfill task.

#### Decision

Authorize Apply Mode for T-003 inline in the same `/start-working` conversation in which the PM-role packet refresh occurs, on the explicit caveat that this compression is a one-time convenience and does not modify the documented three-conversation norm. The compression is recorded in the T-003 packet's Task Status table as an `Inline-authorization caveat` row and in this ADR.

#### Alternatives Considered

- Strict three-conversation sequence (T-001 / T-002 precedent) — rejected because the backfill is mechanical synthesis from already-locked decisions, with no novel substance for which a separate preparation conversation would meaningfully improve quality.
- Permanent shift to single-conversation flow for all future Architect-role tasks — rejected because the role separation between PM (slicing) and Architect (execution) is itself a governance asset that the three-conversation pattern enforces by construction; collapsing it permanently would dissolve the gate even for tasks where the gate matters.

#### Consequences

- Positive: avoids two preparatory conversations for a synthesis task; gatekeeper review burden concentrated in one session.
- Negative: the gatekeeper sees the packet draft and the file-09 execution in the same context, reducing the "fresh read" advantage of a separate review conversation. Acceptable for T-003 only because the backfilled ADRs are all decisions the gatekeeper has already ratified.
- Follow-up: T-004 (file 10) and T-005 (file 13) explicitly revert to the standard three-conversation pattern; this is restated in the T-003 close handoff entry to prevent precedent drift.

### ADR-063: Decision Log consolidation policy — ~40–60 ADRs from ~151 surfaced candidates, compact 4-field skeleton, newest-at-top

- Date: 2026-05-20
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-003; `_context/09_decision_log.md` (entire file); T-003 framing answers

#### Context

Prior `/start-working` sessions surfaced 151 "ADR — ..." candidates across ten handoff entries (largest single batches: T-001 file-12 at ~43 candidates, T-002 file-11 at ~49 candidates). Rendering each candidate as a standalone ADR would produce a ~30K-word file that the 3–4 hrs/week gatekeeper review bandwidth cannot absorb; rendering only the major decisions would lose traceability for fine-grained sub-decisions that downstream milestones must respect.

#### Decision

Consolidate the 151 candidates into roughly 40–60 ADRs by grouping closely-related sub-decisions under a single ADR with bulleted sub-decisions in the Decision field. Each ADR uses the framework's 4-section skeleton (Context / Decision / Alternatives Considered / Consequences) rendered as 1–3 sentences or 2–4 bullets per field, target ~120–200 words per ADR. ADRs are ordered newest-at-top in a single chronological sequence with zero-padded three-digit IDs (`ADR-001` upward) assigned oldest-to-newest. All backfilled ADRs are `Status: Accepted` since every decision was already locked at section-acceptance time during prior sessions. The T-003 consolidation actually landed at **64 ADRs** — four slightly over the upper bound — judged acceptable because each of the four over-bound merges would have conflated decisions the downstream milestones must reference distinctly.

#### Alternatives Considered

- One ADR per candidate (~151 ADRs) — rejected: review burden exceeds bandwidth; many sub-decisions are trivially small and would clutter the file.
- Major decisions only (~20–30 ADRs) — rejected: loses traceability for sub-decisions M-001 onward will need to reference (e.g., individual Konsist rules, individual token families).
- Grouped by `_context/` file rather than chronological — rejected: harder to interpret as a project decision history; the framework template's "newest at top" rule is the standard ADR convention.

#### Consequences

- Positive: file is ~58KB / ~64 ADRs, navigable in one read; cross-references to originating `_context/` file and handoff timestamp preserve the deep reasoning chain; future amendments slot into the same skeleton.
- Negative: consolidation drift risk — a sub-decision inside a bulleted Decision field could be over-stated or under-stated; mitigation is the per-ADR cross-reference list so a reader can chase the source if a sub-bullet's load-bearing role is ambiguous.

### ADR-062: Design-system surface scoping — minimal customization, 3 nav destinations, brand-owner gatekeeping

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 11 Theming And Customization, § Section 2 Applicability, § Section 8 Components (NavigationBar row); 2026-05-19T00:43:53Z handoff

#### Context

A design system can grow unbounded customization surfaces (in-app theme picker, font-size override, color customization, secondary navigation tiers) that each expand the snapshot matrix and erode the novice-Patient utility-app posture from file 03. NemoPill's MVP needs to lock the customization perimeter and the navigation taxonomy explicitly so downstream milestones don't drift wider.

#### Decision

Customizable surfaces minimal at two rows: biometric/passcode app lock (Patient-configurable per ADR-019) and per-channel notification deep-links (Settings → Notifications screen is a 4-row NemoPill-authored surface firing `Intent.ACTION_CHANNEL_NOTIFICATION_SETTINGS` per channel ID, reading `NotificationManager.getNotificationChannel(channelId).importance` on resume). No in-app theme picker; theme follows Android `Configuration.uiMode`. No font-size override; OS-level font scaling honored. No color customization. Three primary nav destinations (Today / Adherence / Medications); Settings reached via gear icon in the Today-screen `TopAppBar` rather than a 4th destination. A Brand-owner gatekeeping rule governs durable design-system changes: token name/value changes, semantic role re-mappings, Material version flips, component contract revisions, notification channel display-name changes, BR-010 copy changes, hard-delete dialog copy changes, deliberate-confirmation interaction pattern changes, and English Privacy Policy substance changes all require a task packet plus `Approved for apply` flip plus an ADR amendment to file 09. Inline-prose changes that don't affect any cross-file contract do not require an ADR.

#### Alternatives Considered

- In-app theme picker / font-size override — rejected: expands snapshot matrix and conflicts with the novice-Patient simplicity posture; OS-level settings are single source of truth.
- 4 nav destinations including Settings — rejected: a 4-destination bottom bar in a 3-screen MVP feels padded; the gear icon convention is M3-standard for Settings access.

#### Consequences

- Positive: locked customization perimeter; snapshot baseline stays at 20 captures (ADR-017); novice-Patient cognitive load minimized.
- Negative: future Patients who would benefit from in-app font scaling beyond OS levels have no escape hatch within MVP; promoting font-size override is itself an ADR amendment with Section-3 (Design Tokens) downstream rework.

### ADR-061: English Privacy Policy substance authored in file 11; Spanish long-form translation deferred to M-006; GitHub Issues as sole contact path

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002 (authoring), M-006 (Spanish translation); `_context/11` § Section 10 Content And Voice; `_context/05` § Compliance CCPA / CPRA + LatAm rows; `_context/06` § Data Ownership uninstall ≡ deletion; `_context/12` § Section 6 Privacy Policy path; 2026-05-19T00:43:53Z handoff

#### Context

ADR-024 (CCPA / CPRA + LatAm Applicable) and ADR-025 (bilingual Privacy Policy) commit to a substantive policy text, not a stub. The file-05 Compliance rows depend on the policy being substantively correct, not merely present. Authoring the policy substance requires plain-language disclosure of the factual claims already pinned in files 05 and 06, while staying inside the Architect role's competence (no legal craft). The Spanish long-form translation is heavy and is gated on the M-006 "Spanish translation capacity" row of `_context/07`.

#### Decision

Author ~800 words of English Privacy Policy substance into `_context/11` § Section 10 across 12 H1 sections: About this policy / What information NemoPill collects / What information NemoPill does NOT collect / How your data is stored / How your data is transmitted / How your data is shared / Your rights and choices / Data retention / Children's privacy / Security / Your rights under specific data protection laws (CCPA / CPRA + LatAm national data-protection laws sections) / Changes to this policy / Contact. The policy is plain-language disclosure of the facts pinned in `_context/05` (no data collected, no data shared, no data sold) and `_context/06` (uninstall ≡ total deletion, no transmission off-device, no Patient data in failure evidence). The Spanish long-form translation is deferred to M-006 close per `_context/07` Dependency And Blocker Register's "Spanish translation capacity" row; until M-006 lands, Spanish-locale Patients see the English policy via the defensive fallback in `_context/11` § Section 8's Settings → Privacy Policy screen. Sole contact path is `https://github.com/izirodriguez/NemoPill/issues` with a public-surface caveat: the policy explicitly warns that GitHub Issues are public and Patients should not include personal medical information in them.

#### Alternatives Considered

- Stub Privacy Policy referencing external generator — rejected: doesn't satisfy CCPA / LatAm substantive requirements; file 05 § Compliance evidence row depends on the policy being authored.
- Email contact path — rejected for MVP: solo-dev open-source project doesn't have a monitored privacy@ address; GitHub Issues is the file-05 Evidence repo-as-audit-trail surface already. Logged as Section 12 planned-hardening item in `_context/11`.
- Both EN+ES drafted in T-002 — rejected: Spanish long-form drafting capacity is the file-07 blocker; short bilingual strings are in T-002 scope, the ~800-word long-form is M-006.

#### Consequences

- Positive: CCPA / CPRA + LatAm Compliance rows substantively satisfied at T-002 close; APK ships with EN policy bundled at M-006 launch.
- Negative: Spanish-locale Patients between M-006 close and Spanish translation completion see English text; the defensive fallback is explicit and language-tagged. Future native-speaker secondary review of the Spanish translation is a logged planned-hardening item.

### ADR-060: Notification copy — 4 channel display names and 4 BR-010 copy variants × EN+ES; NotificationFactory resolves template placeholders

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002 (copy authoring), M-004 (wiring); `_context/11` § Section 10 BR-010 notification copy and channel display names; `_context/06` § Section 2 channel ID contracts; `_context/02` BR-010; 2026-05-19T00:43:53Z handoff

#### Context

ADR-005's BR-010 distinguishes four notification states (on-time, late within 1-hour window, silent-missed, batched-summary) routed onto four channel IDs from `_context/06` § Section 2 (`reminder_on_time`, `reminder_late`, `reminder_missed`, `reminder_summary`). Patients see both the channel display names (on the Settings → Apps → NemoPill → Notifications system surface) and the notification copy itself. Both need authoring in both English and Spanish before M-004 can wire the `:notifications` module.

#### Decision

Author 4 notification channel display names bilingually in `_context/11`: `Dose reminders` / `Late reminders` / `Missed dose notes` / `Reminder summaries` (EN) and `Recordatorios de dosis` / `Recordatorios tardíos` / `Notas de dosis no tomadas` / `Resúmenes de recordatorios` (ES). Author 4 BR-010 notification copy variants × EN + ES with `{medication-name}` / `{dose}` / `{time}` / `{n}` template placeholders resolved at notification-build time by the `:notifications` module's `NotificationFactory` using Android `<plurals>` and locale-aware time formatting. Inline-action verb pair locked at "Take" / "Skip" (EN) and "Tomar" / "Saltar" (ES) on the `reminder_on_time` and `reminder_late` channels only; `reminder_missed` is silent per BR-010 (no inline actions), `reminder_summary` is tap-to-open the today's-Dose list (no inline actions).

#### Alternatives Considered

- Generic notification copy per channel without template placeholders — rejected: BR-010's actionability requires the Patient to see the specific medication, dose, and time without opening the app.
- Inline actions on all four channels — rejected: BR-010's silent-missed posture is unobtrusive by design; adding inline actions would make missed-Dose notifications feel as urgent as on-time ones.

#### Consequences

- Positive: M-004 wiring consumes pinned copy without re-authoring; Patient sees consistent bilingual chrome across the notification stack and the system Settings surface.
- Negative: any future BR-010 copy change requires a Brand-owner gatekeeping ADR amendment (per ADR-062); silent erosion blocked by construction.

### ADR-059: Konsist rule bundle `:core::konsist/TokenLiteralRules.kt` consolidating 4 literal-token rules

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002 (authoring), M-001 (implementation); `_context/11` § Sections 6, 7, 9, 10; `_context/05` § Architecture conformance test row (ADR-017); 2026-05-19T00:43:53Z handoff

#### Context

The design system's value depends on developers using named tokens rather than literal values. Without static enforcement, a literal `15.dp` slips in next to `space.4 = 16.dp` and the token system erodes. Konsist (per ADR-044) is already the architecture-conformance tool; literal-token rules are a natural extension of the existing rule set.

#### Decision

Author a Konsist rule file `:core::konsist/TokenLiteralRules.kt` consolidating four literal-token rules: (i) no literal `dp` value matching a named `space.*` step (Section 6); (ii) no literal `dp` size matching a named `iconSize.*` token (Section 7); (iii) no literal `Int` millisecond duration matching a `motion.*` duration (Section 9); (iv) no literal `String` parameter in `Text()` composables — the parameter must be `stringResource(R.string.x)` or `pluralStringResource(R.plurals.x, count)` (Section 10). Bundled into a single rule file so the M-001 arch-conformance suite has a single import surface. Evasion edge cases (e.g., `15.dp + 1.dp` or `200 + 0`) are acknowledged in `_context/11` § Section 12; the rules are detective controls against ordinary drift, not adversarial bypass.

#### Alternatives Considered

- Code-review checklist instead of static rules — rejected: the file-05 § Engineering Quality discipline pattern is to encode rules as test failures rather than checklist items.
- One Konsist rule file per token family — rejected: 4 small files is more import overhead than one bundle for downstream feature modules.

#### Consequences

- Positive: design-token drift caught at CI time, not at design review; M-001 lands the rule file once and all future feature modules inherit enforcement.
- Negative: a determined developer can evade the literal-detection regex; the planned-hardening item in Section 12 logs the evasion class as something to revisit if it materializes.

### ADR-058: Content and voice — direct + warm tone, sentence case, three-part error pattern, Spanish formality register `usted`, Konsist asserts no literal String in Text()

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 10 Content And Voice; `_context/06` § `Result.Err` catalog (ADR-029); `_context/03` novice-Patient persona; 2026-05-19T00:43:53Z handoff

#### Context

Without a locked content style, copy will drift across screens, error messages, and notifications. File 03's novice-Patient persona requires a consistent, warm, unambiguous voice; the closed `Result.Err` catalog from ADR-029 requires every error string to map onto a typed Err variant; the Latin American distribution scope requires a Spanish formality register decision.

#### Decision

Lock the NemoPill content voice as direct and warm. Conventions: sentence case throughout (no title case, no all-caps); oxford comma; em-dash with no surrounding spaces; numerals for all numbers (no "five doses" — "5 doses"); straight quotes only (curly quotes drift across rendering passes and break snapshot determinism). Error message pattern is three-part: "What happened. Why. What to do next." mapped to the closed `Result.Err` catalog so every error string is anchored to a typed Err variant. Spanish formality register locked at `usted` (formal) for the Latin American distribution scope; verbs conjugated `usted`-form throughout, including microcopy ("Tome", "Su medicación", "¿Está seguro?"). Konsist asserts that every `Text(...)` composable receives `stringResource()` or `pluralStringResource()`, never a literal `String`, so localization-readiness is enforced at architecture-conformance time (part of the rule bundle in ADR-059).

#### Alternatives Considered

- Tone "professional / clinical" — rejected: file 03's novice-Patient + low-anxiety posture argues for warmth.
- Spanish `tú` (informal) — rejected: Latin American medical-context convention favors `usted` for respectful default across distribution scope; switching later would re-baseline every Spanish snapshot.
- Generic error messages — rejected: three-part pattern gives Patients explicit guidance; file-03 environment constraints (one-handed, glanceable) reward unambiguous text.

#### Consequences

- Positive: copy stays coherent across screens, notifications, error toasts, and the Privacy Policy; Spanish translations follow a single register.
- Negative: future native-speaker review may surface phrasing edits; surfaced as Section 12 planned-hardening item.

### ADR-057: Motion design — restrained / functional principle; 3 duration tokens + 3 named easings; `LocalReducedMotion` honors Android system reduced-motion including float duration scaling

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 9 Motion And Interaction; `_context/03` TalkBack-compatible environment constraint; 2026-05-19T00:43:53Z handoff

#### Context

Motion in a medication-reminder app is utility, not delight. Excessive motion erodes the file-03 glanceable / battery-tolerant posture, breaks deterministic snapshots, and harms motion-sensitive Patients. Android's accessibility framework exposes both an on/off reduced-motion signal and a continuous `animator_duration_scale` (system-wide animation speed multiplier) that Patients use for motion-sickness mitigation.

#### Decision

Restrained / functional motion principle. Three motion duration tokens in `:core::theme/Motion.kt`: `motion.fast = 100ms`, `motion.standard = 200ms`, `motion.long = 400ms`. Three named easings: `easing.standard`, `easing.enter`, `easing.exit`. A `LocalReducedMotion` CompositionLocal reads both `LocalAccessibilityManager.current.shouldEnableAnimations()` and `Settings.Global.getFloat("animator_duration_scale", 1.0f)`, exposing `shouldAnimate: Boolean` and `scale: Float`. Every motion duration is multiplied by `scale`; when `shouldAnimate == false` durations collapse to 0ms while end state is preserved. The Section-4 snapshot baseline (ADR-017) is captured with default scale `1.0` and `shouldAnimate = true`; reduced-motion behavior is verified by a separate non-baseline Compose UI test that exercises `scale = 0.0` and asserts end-state matches the animated equivalent at `t = ∞`.

#### Alternatives Considered

- Honor only the boolean reduced-motion signal — rejected: misses motion-sickness Patients who use the system-wide scale slider rather than the boolean toggle.
- Disable motion entirely in MVP — rejected: motion supplies subtle feedback (selection state, drawer entry) that aids the novice persona.

#### Consequences

- Positive: accessibility-respectful by construction; snapshot determinism preserved with default scale baseline.
- Negative: every animated composable must read `LocalReducedMotion`; a developer who hard-codes `tween(200)` without consulting the CompositionLocal escapes the discipline — Section 12 planned-hardening logs a future Konsist rule for the `tween(duration)` literal-detection.

### ADR-056: Five NemoPill-specialized components with Konsist-enforced anti-softening

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002 (specification), M-006 (implementation); `_context/11` § Section 8 Components; `_context/05` § Data Protection deletion UX anti-softening commitment (ADR-022); `_context/06` § F-014 (ADR-030); `_context/12` § Section 5 defensive Room-migration wrapper (ADR-045); 2026-05-19T00:43:53Z handoff

#### Context

The MVP screen set requires several components whose behavior is load-bearing on prior security and data-protection commitments. Without specification at the design-system layer, these components risk inconsistent implementation across milestones, including silent softening of the hard-delete deliberate-confirmation pattern that file 05 § Data Protection explicitly pre-committed against.

#### Decision

Specify five NemoPill-specialized components in `_context/11` § Section 8:

- **Hard-delete confirmation dialog** — hero icon `Icons.Outlined.DeleteForever` at `iconSize.hero` in `color.danger` + `text.h2` heading "Delete all data?" + `text.body` body + unchecked `Checkbox` "I understand this is irreversible" + horizontal button row (`secondary` "Cancel" + `destructive` "Delete all data" disabled until checkbox checked) + non-dismissible by tap-outside or back-button. Konsist asserts the destructive `Button` in this dialog is always paired with a `Checkbox`-gated `enabled` state. Softening requires an ADR amendment with Security-role review.
- **Biometric-gate Settings group** — Settings entry plus inactivity-timeout allow-list (1 / 5 (default) / 15 / 60 minutes / Never re-prompt while app is open).
- **Settings → Privacy Policy screen** — sticky TOC chips + `LazyColumn` Markdown body with `Modifier.widthIn(max = 600.dp)` at Expanded breakpoint; locale resolution via `LocalConfiguration.current.locales` with Spanish-first / English-fallback per ADR-048.
- **Defensive Room-migration failure screen** — full-screen, non-dismissible, mounted before the navigation tree per ADR-045's defensive wrapper; renders "Update required — see Play Store for the fix" with deep-link to Play Store listing.
- **Notification with inline actions** — composition for the four BR-010 notification copy variants per ADR-060.

#### Alternatives Considered

- Typed-phrase confirmation for hard delete (e.g., "type DELETE to confirm") — rejected: novice-Patient + one-handed environment makes typing on-keyboard-while-confirming worse than the checkbox-gated pattern.
- Press-and-hold confirmation — rejected: TalkBack interaction model doesn't expose press-and-hold reliably.

#### Consequences

- Positive: anti-softening enforced as architecture-conformance test; data-protection commitment carries forward from spec to code without erosion.
- Negative: every future addition or modification to these components is an ADR amendment gate; intentional friction.

### ADR-055: Layout and iconography — no explicit column grid, M3 window-size classes, 48×48dp touch-target minimum, Material Symbols + 4 icon-size tokens + closed universal-symbol allowlist

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 6 Spacing And Layout, § Section 7 Iconography And Imagery; `_context/03` responsive scope (ADR-007) and TalkBack constraint; 2026-05-19T00:43:53Z handoff

#### Context

The 4 MVP screens are list-and-detail patterns, not dashboards. A column grid adds wrapper overhead without payoff; an open iconography vocabulary invites silent erosion of file-03's novice-Patient accessibility commitment.

#### Decision

Layout: no explicit column grid; edge-padded layouts only (a 3-row breakpoint table + 10-row layout-primitive table + 3 edge-padding exceptions are pinned in `_context/11` § Section 6). Material 3 window-size classes (Compact / Medium / Expanded) for responsive switching, integrated with `currentWindowAdaptiveInfo()` and `SupportingPaneScaffold`. Core Compose / M3 layout primitives only — no NemoPill-authored wrapper primitives (custom layouts allowed inside feature modules but not promoted to `:core::theme/`). 48×48dp touch-target minimum for every interactive element (WCAG 2.5.5 AAA + Android Material standard).

Iconography: Material Symbols Outlined (default) + Filled (selected-state pair); 4 icon-size tokens in `:core::theme/IconSize.kt` (`iconSize.inline = 20dp`, `iconSize.default = 24dp`, `iconSize.large = 32dp`, `iconSize.hero = 48dp`) — separate from the spacing scale (different dimensional domain). Universal-symbol icon-alone allowlist closed at 4 entries (`arrow_back`, `close`, `more_vert`, `add`); additions require ADR amendment. Minimal flat-style illustrations for empty states only; no photography, 3D, generative, or animated imagery at MVP. `Theme.illustration.fill` and `Theme.illustration.line` segregated Compose namespace for Brand-4 (`#C4E2F5`, per ADR-053) — gives Brand-4 a code handle without promoting it to a Section-3 token. Heading + body + action + optional-illustration empty-state pattern with 4 canonical empty states authored bilingually.

#### Alternatives Considered

- 12-column grid like Material 2 baseline — rejected: list-and-detail patterns don't benefit from grid alignment.
- 44×44pt iOS-aligned touch target — rejected: Android Material standard is 48dp and 4dp matters at the AA / AAA threshold.
- Open icon vocabulary — rejected: closed allowlist prevents drift; the 4 universal symbols are Patient-recognizable across novice fluency.

#### Consequences

- Positive: small, predictable layout vocabulary; novice Patient sees consistent icon language; APK weight minimal for Material Symbols (tree-shaken).
- Negative: adding an icon-alone usage requires an ADR amendment; intentional friction to preserve the accessibility posture.

### ADR-054: Typography system — Roboto + Roboto Mono (M3 default), 8 named styles + 3 weights + tabular numerals globally, body-text measure constraint at tablet / foldable-unfolded, 1dp underline for hyperlinks, italic discouraged

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 5 Typography; `_context/03` low-light glanceable environment + novice-Patient persona; 2026-05-19T00:43:53Z handoff

#### Context

Typography choice affects APK weight, accessibility (especially for low-vision Patients), and snapshot stability. Atkinson Hyperlegible was considered for its low-vision optimization but costs ~250KB APK weight + maintenance overhead. M3-canonical typography integrates cleanly with the ADR-052 Material 3 theming foundation.

#### Decision

Roboto + Roboto Mono (M3 default) over Atkinson Hyperlegible — zero APK-weight cost, predictable rendering, M3-canonical. Future low-vision accessibility re-evaluation logged as Section 12 planned-hardening item. 8 NemoPill named typography styles mapped to M3 baseline (`text.display`, `text.h1`, `text.h2`, `text.h3`, `text.body`, `text.body.small`, `text.caption`, `text.code`) — enough hierarchy for the 4 MVP screens, fewer than M3's 13 styles to maintain. 3 typography weights: 400 (default), 500 (selected-state labels only — reserved exclusively for selected-state signal), 700 (headings + primary button label). Other Roboto weights (100, 300, 900) not loaded and rejected at Konsist. Tabular numerals (`fontFeatureSettings = "tnum"`) globally applied across all 8 styles for column alignment in today's-Dose time column and Adherence-history percentage column. Phone breakpoint imposes no body-text measure constraint; tablet / foldable-unfolded breakpoints impose `Modifier.widthIn(max = 600.dp)` on body-text containers (WCAG 2.4.8 reading-comfort optimal range 50–75 characters). Hyperlinks render as 1dp underline at text baseline + `color.accent` (WCAG 1.4.1: information not conveyed by color alone). Italic discouraged — weight 700 via `text.h3` is the emphasis mechanism; Spanish strings render in upright Roboto.

#### Alternatives Considered

- Atkinson Hyperlegible for low-vision optimization — rejected at MVP for APK weight + maintenance cost; logged as planned-hardening for future re-evaluation.
- M3's 13-style scale verbatim — rejected: 5 of the 13 styles never used in the 4 MVP screens; smaller scale reduces token surface.

#### Consequences

- Positive: zero APK-weight cost; deterministic snapshots (no font-fallback rendering drift); WCAG 1.4.1 + 2.4.8 satisfied.
- Negative: low-vision Patients who would benefit from Atkinson Hyperlegible see Roboto for now; surfaced as a planned re-evaluation.

### ADR-053: Brand palette mapping — `#2C5EAD` / `#1591DC` / `#4BB8FA` / `#C4E2F5` distributed across `color.accent`, `color.info`, and `Theme.illustration.*`; `color.border` accepts WCAG 1.4.11 failure with documented compensating control

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 3 Design Tokens, § Section 4 Color System (brand-palette amendment in-session); 2026-05-19T00:43:53Z handoff

#### Context

The brand palette `#2C5EAD` / `#1591DC` / `#4BB8FA` / `#C4E2F5` was provided mid-T-002 with the instruction "use your judgement to push back on these colors". Contrast analysis revealed that Brand-2 `#1591DC` fails WCAG AA normal text on `color.surface` light (3.5:1) and Brand-3 `#4BB8FA` fails everything in light theme (2.2:1); only Brand-1 `#2C5EAD` maps directly to `color.accent` light. Brand-4 `#C4E2F5` fails WCAG 1.4.11 entirely on light surface (1.33:1). Separately, the `color.border` choices (light `#D4D4D8` at 1.6:1 and dark `#3F3F46` at 1.4:1) fail the WCAG 1.4.11 ≥ 3:1 floor.

#### Decision

Map the brand palette as follows: `#2C5EAD` → `color.accent` light; `#4BB8FA` → `color.accent` dark; `#1591DC` → `color.info` dark; `color.info` light retained as brand-adjacent `#0277BD` (~200° hue vs. brand ~210–220° range) because no canonical brand-palette value satisfies AA normal text on `color.surface` light; `#C4E2F5` reserved for Section-7 Iconography via the segregated `Theme.illustration.fill` Compose namespace (ADR-055). Accept `color.border`'s WCAG 1.4.11 failure with an explicit compensating control: no element relies on `color.border` alone to convey meaning — form-field inputs paired with labels + `color.accent` focus rings; list-row dividers paired with `space.3` spacing; card borders paired with `elevation.1` shadow. Recorded as the first row of `_context/11` § Section 12 § Accepted gaps with reopen-trigger "future component design requiring standalone border meaning".

#### Alternatives Considered

- Map all 4 brand colors directly to semantic tokens — rejected: 3 of the 4 fail WCAG AA in at least one theme; would force the design system to ship with knowing accessibility regressions.
- Darken `color.border` to pass WCAG 1.4.11 — rejected: visually heavier than the design intent and not required given the compensating control.

#### Consequences

- Positive: brand identity present in `color.accent` (most-visible role) + `color.info` + illustrations; accessibility floor preserved for text-bearing surfaces.
- Negative: `#1591DC` and `#4BB8FA` are restricted to dark-theme contexts; Brand-4 is gated behind the illustration namespace; the `color.border` compensating control must be honored by every future component. Future component design requiring standalone border meaning triggers an ADR amendment to bump `color.border` to passing values.

### ADR-052: Theming foundation — Material 3 as underlying token vocabulary, NemoPill-canonical semantic naming, light + dark only, OS-driven theme switching, WCAG 2.1 AA floor with AAA for primary text

- Date: 2026-05-18
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-002; `_context/11` § Section 2 Applicability, § Section 3 Design Tokens, § Section 4 Color System, § Section 11 Theming And Customization; 2026-05-19T00:43:53Z handoff

#### Context

The Material-version choice (M3 vs. M2) affects every Compose component's default tokens, the snapshot baseline matrix from ADR-017, and the entire downstream M-001 implementation surface. The choice was deliberately deferred to file 11 by file 04 because it is a pure design-system decision with no file-04 architectural dependency. The MVP also needs a locked theming model (light / dark / high-contrast scope) and a contrast floor against the file-03 low-light / glanceable environment constraint.

#### Decision

Material 3 as the canonical underlying token vocabulary; `androidx.compose.material3` aligns with `targetSdk = 35` from ADR-041 and provides battle-tested adaptive layouts (`SupportingPaneScaffold`, `WindowSizeClass`). NemoPill-canonical color naming layered on top (`color.surface`, `color.surface.muted`, `color.text.primary`, `color.text.secondary`, `color.border`, `color.accent`, `color.success`, `color.warning`, `color.danger`, `color.info`) — M3-internal naming is an implementation detail mappable at the theme layer. Hex color values with optional 8-digit alpha suffix; native Compose `Color(0xFF1A1A1A)` constructor consumption. Light + dark only theming; no high-contrast variant (Section-3 values exceed AA across the board with AAA for primary text; OS-level high-contrast amplifier handles low-vision Patients implicitly; a third theme would push the snapshot baseline from 20 to 32+). Theme switching follows Android `Configuration.uiMode`; no in-app theme picker. WCAG 2.1 AA contrast floor; AAA achieved for `color.text.primary` and `color.text.secondary` in both themes. Switching to Material 2 in any later milestone requires an ADR amendment with explicit consequences-on-snapshot-baseline section.

#### Alternatives Considered

- Material 2 — rejected: M3 ships in `androidx.compose.material3`, aligns with the `targetSdk = 35` baseline, and provides the adaptive-layout primitives that ADR-007's foldable scope demands.
- OKLCH or RGB color values — rejected: hex `Color(0xFFRRGGBB[AA])` is the Compose-native constructor and familiar to Android developers.
- Adding a high-contrast theme — rejected: snapshot matrix cost exceeds benefit when AA floor is met and AAA achieved for primary text; reopen-trigger "OS-level high-contrast amplifier insufficient for low-vision Patient feedback".

#### Consequences

- Positive: M3-canonical adaptive layouts + token system inherited cleanly; snapshot baseline stays at 20 captures; theme switching has zero in-app UI cost.
- Negative: Material 2 is foreclosed without an ADR amendment; M3 token internals leak through if NemoPill-canonical naming is bypassed at any layer (mitigated by Konsist rules in ADR-059).

### ADR-051: Four operational runbooks in `_build/runbooks/`; standard runbook structure; M-004 Doze test task-packet-scoped not standing

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-004 (Doze test), M-006 pre-launch (runbook authoring); `_context/12` § Section 9 Operational Runbooks; 2026-05-17T22:33:21Z handoff

#### Context

Solo-dev operations need a small, named set of repeatable procedures captured in version-controlled Markdown so the Future Architect / Developer / Human-as-on-call can execute them without reconstructing the steps from scratch. Without a canonical runbook list, ad-hoc procedure docs proliferate in commit messages.

#### Decision

Four operational runbooks registered in `_build/runbooks/`: (1) `cut-play-release.md` (Cut a Play Console release); (2) `fix-forward-release.md` (Fix-forward release procedure); (3) `upload-key-recovery.md` (Upload signing key recovery); (4) `failed-room-migration.md` (Failed Room migration response). Authoring schedule: runbooks 1, 2, 4 at M-006 pre-launch; runbook 3 on first need (per the in-session Q9.1b interpretation). Standard runbook structure recommended: `## When to use` / `## Prerequisites` / `## Procedure` / `## Verification` / `## Rollback / abort` / `## Open-loop questions for the gatekeeper`. Not Konsist-enforced; procedural discipline only. The M-004 manual Doze test procedure is task-packet-scoped (lives inside the M-004 task packet), not a standing runbook in MVP; promoted to a standing runbook only if Fix-forward releases re-exercise it.

#### Alternatives Considered

- Single combined operations document — rejected: discoverability suffers; per-procedure files are easier to update.
- Runbooks as inline `_context/12` content — rejected: file 12 is a contextual / decisional surface, not an operational one; runbooks belong in `_build/`.

#### Consequences

- Positive: small, named procedure set with stable filenames; gatekeeper can find the right runbook by name during an incident.
- Negative: runbook 3 (`upload-key-recovery.md`) authoring deferred until first need means the procedure is reconstructed under stress if the trigger materializes; trade-off acknowledged.

### ADR-050: Backup, recovery, and DR posture — no Patient data backups by design; no standalone DR document; three DR-like scenarios with in-section recovery paths; upload signing key offline-encrypted external medium discipline

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001; `_context/12` § Section 8 Backup, Recovery, And Disaster Plan; `_context/05` § Data Protection (b); `_context/06` § Data Ownership uninstall ≡ deletion (ADR-034); 2026-05-17T22:33:21Z handoff

#### Context

The framework template's Section 8 vocabulary assumes server-data backups, which contradicts the file-05 + file-06 + ADR-066 commitment to no Patient data leaving the device. The on-device-only posture means the only meaningful recovery story is around project assets (upload signing key, repo, Play Console state), not Patient data.

#### Decision

No Patient data backups by design — composed from ADR-021 (no INTERNET), ADR-066 (allowBackup="false"), and ADR-034 (uninstall ≡ total deletion). No standalone DR plan document in MVP. Only three DR-like scenarios are enumerated with in-section recovery paths: (i) upload-key loss — recovery via offline-encrypted external medium (the upload key is local-only at `~/.android/nemopill-upload-keystore.jks` per ADR-041; an offline encrypted backup on an external medium that is disconnected from the internet whenever NemoPill-related data resides on it is the sole recovery path); (ii) repo-host outage — repository is the audit trail per ADR-026; recovery is "wait for GitHub" or "switch git remote" if outage is extended; (iii) Play Console account compromise — `_build/play-store-state.md` (ADR-047) preserves the mirror; recovery is Google Play Console account recovery + key rotation if the upload key was exposed.

#### Alternatives Considered

- Standalone `_build/dr-plan.md` document — rejected: the only meaningful DR surface is the three scenarios above, each fully captured in `_context/12` § Section 8; a separate document would duplicate without adding.
- Cloud-stored upload key backup — rejected: violates the file-05 evidence integrity posture; cloud-stored signing keys are a common exfiltration vector.

#### Consequences

- Positive: minimal recovery surface; no false sense of safety from a DR document that doesn't actually cover Patient data.
- Negative: if the upload key offline-encrypted external medium fails physically, the project is forced to rotate signing keys through Play App Signing (ADR-041 inherits the safety net); manual procedure documented in runbook 3 (ADR-051).

### ADR-049: Google Play Console Android Vitals as sole crash reporter; two new Konsist rules (no string templates in throw messages; data class toString() redaction); no custom dashboards; P0 / P1 / P2 response targets as norms not SLAs

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security (per ADR-031 alignment)
- Related milestone or task: M-000 / T-001; `_context/12` § Section 7 Monitoring And On-Call; `_context/05` § Data Protection no-INTERNET (ADR-021); `_context/06` § Error Handling no-Patient-data-in-failure-evidence (ADR-031); 2026-05-17T22:33:21Z handoff

#### Context

Crash reporting is a known conflict surface: most vendors (Crashlytics, Sentry) require `android.permission.INTERNET` which contradicts ADR-021. Google Play Console "Android Vitals" runs in the separate Play Services process and does not require the app to declare INTERNET. The ADR-031 "no Patient data in failure evidence" hard rule must be statically enforceable before any crash reporting lands.

#### Decision

Google Play Console "Android Vitals" as the sole crash-reporting tool — Crashes & ANRs surface only; honors the no-INTERNET rule (Play Services runs in a separate process) and provides aggregate metrics + stack traces without per-Patient identifiers. Two new Konsist rules added to the Section-4 arch-conformance suite (file 12): (i) no `throw` statement uses Kotlin string templates or `String.format` / `StringBuilder.append` in its message argument — prevents Patient data from leaking into exception messages; (ii) every Domain-layer (`:domain`, `:core`) `data class` overrides `toString()` to a redacted form. No custom dashboards in MVP; the Play Console "Android Vitals → Crashes & ANRs" page is the sole dashboard, linked from `_build/play-store-state.md` at M-006 pre-launch. On-call response-time targets — P0 same-day / P1 work-week / P2 next normal release — are operating norms for the solo gatekeeper, explicitly not SLAs to external stakeholders. No escalation chain.

#### Alternatives Considered

- Crashlytics — rejected: requires INTERNET permission; would invalidate the file-05 architectural rule.
- On-device crash log file — rejected: requires Patient-action to share with developer; Android Vitals is cleaner for the solo-dev model. Reopen-trigger logged for "Android Vitals missing a meaningful slice of Production crashes" (Section 10 planned-hardening row).

#### Consequences

- Positive: zero conflict with no-INTERNET architecture; aggregate metrics suffice for the solo-dev model; Konsist rules close the Patient-data-in-evidence channel statically.
- Negative: per-Patient debugging requires the Patient to reproduce on their own device; trade-off accepted given the on-device-only posture.

### ADR-048: Privacy Policy path / format / loading convention — `legal/PRIVACY_POLICY.{en,es}.md` (Markdown) repo source-of-truth, APK-bundled `res/raw{,-es}/privacy.md`, build-time `copyPrivacyPolicyAssets` Gradle task enforces sync

- Date: 2026-05-17
- Status: Accepted (refines ADR-025)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-006 (authoring + Spanish translation); `_context/12` § Section 6 Privacy Policy; `_context/05` § Evidence + § Compliance CCPA + LatAm rows; ADR-025; 2026-05-17T22:33:21Z handoff

#### Context

ADR-025 committed to a bilingual Privacy Policy but didn't pin path / format / loading convention. The policy needs to be (i) the repo source-of-truth for evidence purposes; (ii) reachable from a public URL for Play Console submission; (iii) viewable inside the app without requiring INTERNET (ADR-021). Three usage paths force a sync discipline so they don't drift.

#### Decision

Privacy Policy at `legal/PRIVACY_POLICY.en.md` and `legal/PRIVACY_POLICY.es.md` (Markdown) as the repo source-of-truth. Three usage paths: (i) repo source-of-truth in `legal/`; (ii) GitHub Pages renders the same files at a public URL for Play Console "Privacy Policy" link; (iii) APK-bundled `res/raw/privacy.md` and `res/raw-es/privacy.md` (loaded from APK, not network — preserves the no-INTERNET posture per ADR-021). Build-time `copyPrivacyPolicyAssets` Gradle task enforces sync between `legal/` and `res/raw{,-es}/` so the three paths cannot drift. Language fallback in the Settings → Privacy Policy screen is Spanish-first / English-fallback per `LocalConfiguration.current.locales` (ADR-056).

#### Alternatives Considered

- Network-fetched Privacy Policy — rejected: requires INTERNET; violates ADR-021.
- Hardcoded policy text inside Kotlin source — rejected: legal content needs version control and Markdown rendering, not hardcoded constants.

#### Consequences

- Positive: three paths stay in sync via Gradle task; APK ships with policy bundled, no network dependency.
- Negative: any policy edit must pass the Gradle sync task before commit; the Gradle task itself is M-001 / M-006 implementation work.

### ADR-047: Release notes triad — GitHub Releases + `v<versionName>` tag + CHANGELOG.md (Keep A Changelog) + Play Console "What's new" (bilingual, 500-char cap each); `_build/play-store-state.md` version-controlled Play Console state mirror

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-006 (first production release); `_context/12` § Section 6 Release Process; 2026-05-17T22:33:21Z handoff

#### Context

Release notes need to land on three surfaces (GitHub Releases for the audit trail, CHANGELOG.md for the in-repo human-readable history, Play Console "What's new" for the Patient-facing surface) without drift. Play Console state itself (country list, listing text, screenshots, staged-rollout policy, versionCode ledger) lives in a Google-managed UI that has no native version control.

#### Decision

Three release-notes surfaces: (i) GitHub Releases tied to the `v<versionName>` annotated git tag; (ii) `CHANGELOG.md` in Keep A Changelog format at the repo root; (iii) Play Console "What's new" entry, bilingual (EN + ES), 500-character cap per language per Play Console limits. `_build/play-store-state.md` introduced at M-006 pre-launch as a version-controlled mirror of Play Console state. Content structure pinned in `_context/12` § Section 6: country list, listing text, screenshots manifest, "What's new" history, staged-rollout policy statement (always "100% immutable" per ADR-046), versionCode ledger.

#### Alternatives Considered

- GitHub Releases only — rejected: CHANGELOG.md is the in-repo human-readable surface; Play Console requires its own "What's new" text.
- Skip `_build/play-store-state.md` — rejected: Play Console state is otherwise unversioned; the mirror is the only way to detect Play-side drift.

#### Consequences

- Positive: release-notes drift caught by triple-surface convention; Play Console state has a version-controlled twin.
- Negative: every release authors release notes three times (in three surfaces); manual discipline required.

### ADR-046: Release model — manual Play Console promote, immutable versionCode, no staged rollout, fix-forward sole recovery, per-milestone Internal-Testing releases, pre-OOO 24-hour rule, no formal freeze windows, no rollback rehearsal

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001; `_context/12` § Section 5 Continuous Delivery And Deployment, § Section 6 Release Process, § Section 10 DevOps Risks And Trade-Offs accepted-gap rows; 2026-05-17T22:33:21Z handoff

#### Context

A staged rollout (10% → 50% → 100%) is the canonical safety net for Android Play releases — it gives a halt-rollout option when Production reveals a regression Internal Testing missed. The Human gatekeeper opted for the simpler release flow despite my strong recommendation for staged rollout. This is a deliberate trade-off acknowledged as the most-important accepted-gap row in `_context/12` § Section 10.

#### Decision

Manual promote via Play Console UI (no CI-automated Play upload in MVP). Two-step manual sequence: trigger `release.yml`, then upload + promote in Play Console. Each release is immutable by `versionCode`; no staged rollout in MVP — every release is 100% Production immediately on promote. Fix-forward is the sole recovery path; halt-rollout safety net unavailable. Per-milestone Internal-Testing releases for MVP; production-track release at M-006 only. Post-MVP cadence: release-when-worth-releasing. No formal freeze windows; gatekeeper availability is the implicit freeze. Pre-OOO 24-hour rule: never promote to Production within 24 hours before a known upcoming OOO window. No rollback rehearsal cadence in MVP; revisit if a fix-forward release happens in anger. Reopen-trigger for staged rollout: "a P0 fix-forward in anger that would have benefited from halt-rollout."

#### Alternatives Considered

- Staged rollout 10 / 50 / 100 — recommended by Architect, rejected by Human gatekeeper in-session; complexity not warranted for solo-dev MVP without active Patient cohort.
- CI-automated Play upload — rejected: manual gate is the file-05 § Evidence + Security review hook for the *Data safety* declaration; automation would either skip the hook or re-implement it.

#### Consequences

- Positive: simplest possible release flow; gatekeeper has full visibility on every promote.
- Negative: a P0 regression at 100% Production has no halt-rollout escape valve; fix-forward must ship within hours and may itself ship a worse regression under stress. Trade-off acknowledged and explicitly logged as the canonical Section-10 accepted-gap row.

### ADR-045: Room migration discipline — forward-only with explicit Migration objects, no `fallbackToDestructiveMigration`, defensive `try/catch` wrapping, Konsist asserts schema JSON + Migration object presence

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-006 (first migration); `_context/12` § Section 5 Database migration strategy; `_context/04` Single Room database (ADR-015); 2026-05-17T22:33:21Z handoff

#### Context

Room offers `fallbackToDestructiveMigration` which silently drops all tables on schema-version mismatch. For NemoPill, this would silently delete the Patient's medication history — a data-protection failure mode worse than crashing. Forward-only migrations with explicit Migration objects are the only safe choice given the no-export feature (ADR-022, ADR-032).

#### Decision

Forward-only Room migrations with explicit `Migration` objects per schema-version increment. `fallbackToDestructiveMigration` is *not* used; failed migrations crash rather than drop Patient data. Defensive `try/catch` wrapper required around every Room `Migration` body — failure surfaces a one-screen "Update required — see Play Store for the fix" message via the Defensive Room-migration failure screen (ADR-056) rather than a raw crash dialog. Konsist asserts (i) `app/schemas/<version>.json` files are committed for every schema version and (ii) a `Migration` object exists for every consecutive schema-version pair. Failed Room migration response is documented in runbook 4 (ADR-051).

#### Alternatives Considered

- `fallbackToDestructiveMigration` — rejected: silently destroys Patient data on schema mismatch; worse than crashing.
- No defensive wrapping — rejected: a raw crash dialog gives Patients no guidance; the "Update required" screen is recoverable via Play Store.

#### Consequences

- Positive: Patient data preserved through schema changes; failed migrations are recoverable rather than catastrophic; Konsist enforces the discipline statically.
- Negative: every schema change is an explicit Migration object that must be authored, tested, and reviewed; intentional friction proportional to the data-protection stakes.

### ADR-044: CI / tooling stack — GitHub Actions, trunk-based branching, versioned `_build/hooks/pre-commit` shell + Gradle installer, gitleaks, Konsist over ArchUnit, Kover over JaCoCo, Roborazzi over Paparazzi, framework validator wired as merge-blocking + pre-commit

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-001 (CI implementation); `_context/12` § Section 4 Continuous Integration, § Section 3 Configuration And Secrets; 2026-05-17T22:33:21Z handoff

#### Context

The file-05 quality program (ADR-017) requires a CI tool, a coverage tool, an architecture-conformance tool, a snapshot tool, a secret scanner, and a pre-commit hook surface — all of which were deferred from file 05 to file 12 in T-001. Each has stable open-source options; picking deliberately and recording the rationale prevents future second-guessing.

#### Decision

GitHub Actions as CI tool — two workflow files: `.github/workflows/ci.yml` (merge-blocking, eight stages) and `.github/workflows/release.yml` (manual-promote via `workflow_dispatch`). Trunk-based branch strategy with short-lived feature branches; no GitFlow; feature branches deleted after merge; tags mark released versionCodes. Versioned `_build/hooks/pre-commit` shell script + `git config core.hooksPath` Gradle installer over Lefthook / Husky — zero external dependencies vs. Go binary or Node runtime. Pre-commit hook fast-path checks: `gitleaks protect --staged` + framework validator + `ktlintCheck` scoped to staged files. Full Lint / Konsist / Kover / Robolectric / `assembleDebug` run only in CI to keep the pre-commit budget at ~5 seconds. gitleaks as secret-scanner vendor, wired into both pre-commit hook and CI stage `secret-scan` (`gitleaks/gitleaks-action@v2`); config at `.gitleaks.toml` extending default detectors with `.jks` / `.keystore` / `.p12` / `KEYSTORE_PASSWORD=` / `KEY_ALIAS=` / `KEY_PASSWORD=` patterns. Konsist over ArchUnit for architecture-conformance — Kotlin-native, KSP-based, idiomatic for the file-05 rule set. Kover over JaCoCo for coverage — JetBrains-native, cleaner multi-module Kotlin/Android configuration, supports per-module thresholds via `koverVerify`. Roborazzi over Paparazzi for snapshot testing — reuses Robolectric harness; aligns with ADR-017's all-tests-under-Robolectric decision. Framework validator (`bash _framework/validate_framework.sh`) wired as a merge-blocking CI stage and as a pre-commit hook entry. No test-result cache in MVP (deliberate exclusion); only Gradle dependency cache + Gradle build cache + Gradle configuration cache.

#### Alternatives Considered

- ArchUnit / JaCoCo / Paparazzi — rejected: each has equivalent capability but worse fit for the Kotlin-first, Robolectric-everywhere stack.
- Husky or Lefthook for pre-commit — rejected: external runtime dependency; shell + Gradle installer is zero-dependency.
- GitFlow — rejected: solo-dev + trunk-based is the file-07 calendar-constraint-friendly model.

#### Consequences

- Positive: stable open-source stack with documented rationale per choice; pre-commit budget tight, CI is the heavy stage.
- Negative: any future switch (e.g., to ArchUnit) is an ADR amendment touching every module's test config; intentional friction.

### ADR-043: Plain-text Room encryption decision queued for re-evaluation at T-005 (file 13 threat model)

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-001 (deferral surfaced), M-000 / T-005 (re-evaluation); `_context/12` § Section 3 Configuration And Secrets, § Section 10 § Planned hardening; `_context/05` § Data Protection sub-rule (a); 2026-05-17T22:33:21Z handoff

#### Context

The Human gatekeeper raised an in-session question about Patient-data encryption during T-001 Section 3 drafting. File 05 § Data Protection sub-rule (a) decided plain-text Room storage for MVP, calibrated against a device-locked threat model. The right venue to revisit this is file 13 (threat model), which captures *adversaries and assumptions*. Silently amending file 05 mid-T-001 would have bypassed Security-role review.

#### Decision

Surface the plain-text Room encryption question as an explicit Section-10 planned-hardening item in `_context/12` rooted to T-005. T-005 must explicitly re-evaluate the device-locked threat model the file-05 § Data Protection sub-rule (a) decision is calibrated against, with concrete adversary scenarios on the table: full file-system access to unlocked device; forensic recovery off discarded hardware; pre-Section-3 cloud-backup exfiltration window (now closed by ADR-066). If the threat-model exercise reveals adversaries the current decision does not cover, the follow-up is a `_context/05` revision packet — not a unilateral file-13 edit.

#### Alternatives Considered

- Silently amend file 05 to add Room encryption — rejected: bypasses the file-05 § Engineering Quality review discipline.
- Defer the question to a post-MVP milestone — rejected: T-005 is the canonical adversary-modeling surface and is within M-000; deferring further risks losing the question.

#### Consequences

- Positive: question carried forward to the correct venue; gatekeeper's concern is recorded, not lost.
- Negative: plain-text Room remains in MVP through M-006 unless T-005 changes the call; trade-off accepted because the device-locked threat model is the current calibration anchor.

### ADR-042: Background-work tuning — WorkManager `15.minutes` + `LINEAR` backoff + `30.seconds` delay + `Constraints.NONE`; `BroadcastReceiver.goAsync()` 8-second cap; `USE_EXACT_ALARM` preferred over `SCHEDULE_EXACT_ALARM`

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001, M-004 (Doze test re-validation); `_context/12` § Section 3 Configuration And Secrets, § Section 5 Database migration strategy adjacent; `_context/04` Dual-strategy background work (ADR-012); `_context/06` § Reliability Timeout layers; 2026-05-17T22:33:21Z handoff

#### Context

ADR-012 pinned the dual-strategy background-work choice (AlarmManager + WorkManager) but left specific tuning to file 12. The WorkManager horizon-extension job and pending→missed sweep need a repeat interval, backoff policy, and constraint set. `BroadcastReceiver.goAsync()` has a 10-second OS hard kill that needs a safety margin. Android 13+ introduced `USE_EXACT_ALARM` (non-revocable) and `SCHEDULE_EXACT_ALARM` (revocable) with different Play Console justification requirements.

#### Decision

WorkManager first-pass values: `repeatInterval = 15.minutes`, `BackoffPolicy.LINEAR`, `backoffDelay = 30.seconds`, `Constraints.NONE`. Re-validated at M-004 manual Doze test on real hardware. `BroadcastReceiver.goAsync()` internal cap at 8 seconds (2-second safety margin under the 10-second hard kill), enforced via `withTimeout(8_000)`. `USE_EXACT_ALARM` (API 33+, non-revocable, Play Console use-case justification) preferred for Reminder firing; `SCHEDULE_EXACT_ALARM` (API 31–32, revocable) as fallback. Both declared in manifest with per-API resolution; `android:maxSdkVersion="32"` on `SCHEDULE_EXACT_ALARM`.

#### Alternatives Considered

- Shorter WorkManager repeat interval (e.g., 5 minutes) — rejected: WorkManager minimum periodic-work interval is 15 minutes; shorter would not be honored.
- `Constraints.requiresBatteryNotLow()` — rejected at MVP: too easy for the constraint to defer the sweep past the BR-010 1-hour window; revisit at M-004 if hardware Doze testing shows otherwise.
- `goAsync()` cap at 9 seconds — rejected: tighter safety margin than 2 seconds risks the OS hard kill if the system is under load.

#### Consequences

- Positive: tuning anchored to file-06 § Reliability timeout layers; M-004 has a clear re-validation target.
- Negative: 15-minute WorkManager interval means the pending→missed sweep latency is up to 15 minutes; BR-010's 1-hour late window is the upper bound that this tuning must stay inside.

### ADR-041: Android delivery hardening — minSdk=26, targetSdk=35, compileSdk=35; `android:allowBackup="false"` + Android 12+ `dataExtractionRules` excluding `cloud-backup` + `device-transfer`; Google Play App Signing enrollment with upload key on dev machine only

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: M-000 / T-001, M-001 (manifest implementation); `_context/12` § Section 2 Infrastructure, § Section 3 Configuration And Secrets, § Section 6 Release Process; `_context/05` § Data Protection sub-rule (b); 2026-05-17T22:33:21Z handoff

#### Context

The Android SDK targets affect the file-06 permission ladder (e.g., `BOOT_COMPLETED` Android-7+ unlock requirement, Android-8+ background-receiver restrictions, Android-12+ `FLAG_IMMUTABLE` requirement, Android-13+ exact-alarm permission flavors). Android's default `android:allowBackup` is `true` — meaning the Room database would be silently exfiltrated to Google Cloud via Play Services, bypassing ADR-021's no-INTERNET commitment (Play Services has its own network permission set, separate from the NemoPill app process). This was an in-session discovery during T-001 Section 3 drafting that closed a real gap in file 05.

#### Decision

Android SDK targets: `minSdk = 26` (Android 8 Oreo) — lowest defensible floor for the file-06 permission ladder; `targetSdk = 35` (Android 15) — latest stable API keeping Play submission within the one-year rule; `compileSdk = 35`. Manifest: `android:allowBackup="false"` + Android 12+ `<dataExtractionRules>` excluding `<cloud-backup>` and `<device-transfer>`. Google Play App Signing enrollment for upload-key management — upload key on developer machine only at `~/.android/nemopill-upload-keystore.jks`; manual AAB upload to Play Console for MVP; no CI mirror of the upload key. A future file 05 revision should consider whether `allowBackup` discipline belongs in file 05 § Data Protection sub-rule (b) as a first-class manifest invariant rather than implicit in file 12.

#### Alternatives Considered

- `minSdk = 21` (broader device reach) — rejected: too many of the file-06 permission/behavior rules diverge below API 26.
- Leave `android:allowBackup` unset (Android default true) — rejected when surfaced: silently violates ADR-021; the no-INTERNET architectural rule extends through manifest invariants.
- CI-mirrored upload key — rejected: upload key on CI is a known exfiltration vector; manual upload is the secure default for MVP.

#### Consequences

- Positive: real gap in file 05 closed (`allowBackup` discipline); SDK targets aligned with the file-06 permission ladder; signing key kept off CI.
- Negative: `minSdk = 26` excludes some older devices in the LatAm distribution scope; trade-off accepted because the permission-ladder simplification outweighs the device-reach cost.

### ADR-040: Design tail sequence — T-001=file 12, T-002=file 11, T-003=file 09, T-004=file 10, T-005=file 13

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 / T-001 (sequence locked at packet authoring), T-001 / T-002 / T-003 / T-004 / T-005 (execution); `_context/07` Dependency And Blocker Register file-09 mitigation column; `_context/08_active_task_packet.md` T-001/T-002/T-003 packets; 2026-05-17T14:23:24Z handoff

#### Context

The first option chosen by the Human at T-001 packet authoring ("Close design tail 09 → 13") conflicted with `_context/07` Dependency And Blocker Register's mitigation column for file 09, which says "Schedule a `/start-working` session for file 09 after file 12 and file 11 are contextualized (so file 09 inherits a stable decision surface)". Surfaced per CLAUDE.md Stop Conditions; Human reselected the file-07-honoring sequence.

#### Decision

Design tail tasks executed in this exact sequence: T-001 = `_context/12_environments_and_devops.md`; T-002 = `_context/11_visual_language_and_design_system.md`; T-003 = `_context/09_decision_log.md`; T-004 = `_context/10_non_functional_requirements.md`; T-005 = `_context/13_threat_model_and_data_classification.md`. The file 09 ADR backfill (T-003) waits until file 12 and file 11 are contextualized so the largest two ADR candidate batches are pinned before consolidation. Future T-### sessions inheriting this state must NOT re-litigate sequencing or the upstream T-001-locked / T-002-locked decisions unless the trigger condition for the decision has materialized.

#### Alternatives Considered

- Close design tail 09 → 13 — rejected on Stop-Condition surfacing: conflicts with file-07 mitigation.
- T-003 = file 09 first (back-loading the design ADRs) — rejected: file 09 would be incomplete on T-001 and T-002 close; require post-hoc amendment.

#### Consequences

- Positive: file 09 backfill at T-003 has a stable decision surface to consolidate from; ADR candidates from T-001 and T-002 land in their own handoff entries and are then transposed into file 09 cleanly.
- Negative: design tail closure extends across five conversations rather than e.g., two; trade-off accepted because the file-09 backfill quality depends on the prior tasks closing.

### ADR-039: Task-packet conventions — sequential T-### IDs, `## Tests To Add First` resolves to `Not applicable` for Architect-role packets, `Approved for apply` flip authorizes Architect `/start-working` sessions, file-07 Dependency And Blocker Register reconciled once at M-000 close, mid-session handoff entry discipline, backslash-escape convention for placeholder marker references

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Project Manager; Architect
- Related milestone or task: M-000 / T-001 (conventions locked at packet authoring); `_context/08_active_task_packet.md` T-001 packet; 2026-05-17T14:23:24Z handoff

#### Context

Six operational conventions were established at T-001 packet authoring time to make the design-tail task sequence executable without re-litigating mechanics at each task. Each addresses a specific procedural gap CLAUDE.md left implicit.

#### Decision

Six task-packet conventions:

- **Sequential T-### IDs** — matches the existing project numeric-ID conventions (`BR-###`, `F-###`, `J-###`, `UC-###`, `AC-###`, `EC-###`). Milestone-scoped IDs (`M-001-T-01`) and semantic IDs (`T-DESIGN-09`) considered and rejected.
- **`## Tests To Add First` resolves to `Not applicable` for Architect-role documentation task packets.** The framework template's tests-first vocabulary (`unit, integration, contract, or UI test`) presupposes a code deliverable, which Architect-role tasks do not have; the section-by-section drafting protocol in CLAUDE.md is the operational analogue. Verification surface for Architect-role tasks lives in `## Acceptance Criteria` instead, using executable grep / validator commands plus enumerated cross-file consistency hooks.
- **`Approved for apply` flip authorizes Architect-role `/start-working` sessions** even though the Architect doesn't write to `_source/`. The flip is operational authorization (run the session now) rather than mutation authorization (Architect's mutation surface to `_context/` is allowed by the role contract regardless of Task status). Preserves gating-workflow symmetry across role types.
- **Touch file 07's Dependency And Blocker Register once at M-000 close**, not five times incrementally per design-tail file. When T-005 closes, the M-000-close handoff entry reconciles all design-tail rows in file 07's Register as resolved.
- **Mid-session handoff entry discipline for partial-implementation recovery.** If a future Architect-role `/start-working` session approaches context exhaustion before all sections are drafted (typically after locking section 6 of 10 in a long file), the session must append a mid-session handoff entry listing drafted-and-locked / drafted-but-not-yet-locked / unstarted sections.
- **Backslash-escape convention for documentation references to framework placeholder markers** in `_context/`. Inside `_context/` files, any prose reference to the literal `\{\{REQUIRED:\}\}` / `\{\{OPTIONAL:\}\}` / `\{\{EXAMPLE:\}\}` syntax must escape the braces with backslashes so the validator's regex doesn't false-fire. Markdown renders the escaped form identically.

#### Alternatives Considered

- Milestone-scoped task IDs — rejected for uniformity with existing project ID conventions.
- Treat `## Tests To Add First` as required for all task packets — rejected: presupposes code deliverable.
- Edit file 07 incrementally per design-tail file — rejected: five edits is the noisy / drift-prone path.

#### Consequences

- Positive: design-tail task sequence runs without re-litigation; gatekeeper has a single consistent control surface.
- Negative: the conventions are not in CLAUDE.md itself, so a future agent reading only CLAUDE.md may miss them; T-003 records them here in file 09 as the durable surface.

### ADR-038: `M-000 — Design Tail Closure` milestone added to `_context/07`, reversing the prior "design tail is implicit, not milestone-tracked" decision

- Date: 2026-05-17
- Status: Accepted (supersedes the design-tail-is-implicit framing in the 2026-05-17T00:22:16Z handoff)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: M-000 (milestone defined by this ADR); `_context/07` Milestone Register row `M-000`; `_context/08_active_task_packet.md` (every T-### packet inherits `Milestone ID: M-000`); 2026-05-17T14:23:24Z handoff

#### Context

The original file-07 decision was to treat design-tail contextualization (files 09, 10, 11, 12, 13) as implicit M-001 prerequisites tracked via handoff log only. T-001 packet authoring revealed this leaves every design-tail task packet without a stable `Milestone ID`, creating retrospective ambiguity in the handoff log and in task-packet history.

#### Decision

Add `M-000 — Design Tail Closure` as a row in `_context/07` Milestone Register with five "Done When" items (one per design-tail file: 09, 10, 11, 12, 13 contextualized; validator clean). The M-001 Prerequisites cell becomes `M-000 complete.` rather than the prior verbose "design tail closed (implicit gate per Delivery Summary)..." prose. Every T-### task packet inherits `Milestone ID: M-000`. The "design tail is implicit" rationale (Milestone Register is product-outcome-oriented; design-system tokens / CI choices aren't product outcomes) is acknowledged and traded off against ID stability.

#### Alternatives Considered

- Keep design tail implicit, scope task packets without a `Milestone ID` row — rejected: violates CLAUDE.md's "Milestones in 07 must be the only source of milestone IDs referenced by 08".
- Promote each design-tail file to its own milestone — rejected: 5 milestones for design tail vs. 6 milestones for product; bloats the Register without payoff.

#### Consequences

- Positive: every T-### packet has a stable Milestone ID; M-000 progress meter (`N / 5 done-when items satisfied`) gives the gatekeeper a single-glance status.
- Negative: Milestone Register no longer purely product-outcome-oriented; minor concession to operational clarity.

### ADR-037: `Guardrail Pass` verdict term for Security review; Human cannot flip `Task status` to `Approved for apply` while QA `Coverage Insufficient` or Security in blocking state; milestone-close handoff entry must affirmatively record "no new ADRs this milestone" when applicable

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); QA; Security
- Related milestone or task: `_context/07` Review And Approval Rules, Milestone-Level Definition Of Done; 2026-05-17T00:22:16Z handoff

#### Context

ADR-017's two-pass TDD discipline uses QA's `Coverage Sufficient` / `Coverage Insufficient` verdict to gate `Approved for apply`. Security's pre-Apply-Mode review needs a parallel verdict vocabulary so task packets have a single canonical phrase to record Security's verdict. CLAUDE.md does not explicitly state that Human cannot flip while QA is `Coverage Insufficient`; without pinning this, a future Human-gatekeeper turn might read the rule as a soft suggestion. Milestone-close handoff entries that simply omit an ADR-candidates section leave readers uncertain whether decisions were made or not recorded.

#### Decision

`Guardrail Pass` verdict term introduced for Security proposal review; mirror of QA's `Coverage Sufficient`. Blocking verdict from Security parallels QA's `Coverage Insufficient`. Human cannot flip `Task status` to `Approved for apply` while QA's verdict is `Coverage Insufficient` or Security is in a blocking guardrail state (when Security is activated). Milestone-close handoff entries must affirmatively state "no new ADRs this milestone" rather than omit the topic, preventing silent drift.

#### Alternatives Considered

- Single verdict (e.g., `Approved`) for both QA and Security — rejected: blurs role distinction and conflates coverage with security.
- Allow Human override of QA / Security verdicts at any time — rejected: defeats the purpose of two-pass review.

#### Consequences

- Positive: review discipline rules are explicit and audit-friendly; milestone-close entries unambiguous about ADR activity.
- Negative: extra vocabulary to maintain; Security verdict adds a step when activated (no cost when Security folds into Architect per file-05 § Compliance Activation).

### ADR-036: Six milestones M-001 through M-006 — vertical walking-skeleton sequencing, explicit M-001 Foundation, in-place reliability, feature-complete MVP exit with no dogfood gate

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/07` Milestone Register, Milestone-Level Definition Of Done; 2026-05-17T00:22:16Z handoff

#### Context

The MVP needs a sequencing shape that retires the largest design risk first (file-01 assumption (1): Android scheduled-alarm reliability under Doze) while preserving the Bounded Context boundaries from `_context/01` and the architectural seams from `_context/04`. The MVP exit criterion needs to be operationally achievable without depending on data NemoPill cannot yet collect (the file-01 ≥ 80% adherence success metric requires a deployed Patient cohort).

#### Decision

Six milestones: M-001 Foundation; M-002 Walking Skeleton; M-003 Medication Management; M-004 Scheduling & Reminders with Reliability; M-005 Adherence Tracking; M-006 Cross-Cutting Presentation & MVP Close. Vertical walking-skeleton sequencing — M-002 retires the integration risk from file-01 assumption (1) before any feature milestone consumes resources building on top of it. Explicit M-001 Foundation milestone first (not folded into the first feature milestone) so file-05's coverage thresholds and architecture-conformance tests exist from day one and the two-pass review machinery has the test infrastructure it needs. In-place reliability: BR-010 (late-Reminder window), BR-011 (retroactive Confirmation window), BR-012 (timezone change), boot survival, and pending→missed sweep live inside the feature milestones that need them (not deferred to a late consolidation milestone) — preserves the file-04 `:scheduling` / `:notifications` seam. MVP exit criterion: feature-complete only (all 12 BRs implemented, all 21 file-03 BDD scenarios green, all 8 ACs satisfied, all 15 file-06 F-flows tested, file-05 quality gates green, bilingual Privacy Policy authored). No dogfood-period gate. Per-milestone Internal-Testing release; production-track release at M-006 only.

#### Alternatives Considered

- Bounded-context-horizontal sequencing — rejected: integration risk hides at the back; file-01 assumption (1) would not be retired until late.
- Journey-by-journey sequencing — rejected: milestone scope is wide; file-04 boundaries blur.
- Folding Foundation into M-002 — rejected: would mean the first proposal review has no test infrastructure to render a real `Coverage Sufficient` verdict against.
- MVP exit with dogfood gate — rejected during interview: conflates feature-complete with adherence-metric proof; would block MVP close on data NemoPill cannot yet collect.

#### Consequences

- Positive: largest design risk retired by M-002; reliability is integral, not appended; MVP exit is operationally achievable.
- Negative: M-006 carries the largest cross-cutting Presentation surface; intentional concentration to keep feature milestones thin.

### ADR-035: Operating model — NemoPill framework roles (Project Manager, Developer, QA, Security, Compliance, Legal) filled by Claude Code agents; Human Isidro is gatekeeper and the only role authorized to flip `Task status` to `Approved for apply`; 3–4 hrs/week is human review-and-approval bandwidth, NOT implementation throughput

- Date: 2026-05-17
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper)
- Related milestone or task: `_context/01` Delivery context refinement; `_context/07` Delivery Summary calendar-constraints + Dependency And Blocker Register staffing row; auto-memory entry `project_nemopill_operating_model.md`; 2026-05-17T00:22:16Z handoff

#### Context

The "solo dev" framing from `_context/01` § Delivery context left an ambiguity about the relationship between the Human's time budget and project throughput. Calibrating milestone sequencing or scope to "solo developer with 3–4 hrs/week" produces wildly different results depending on whether the 3–4 hrs is treated as coding time or review time.

#### Decision

The framework roles — Project Manager, Developer, QA, Security, Compliance, Legal — are filled by Claude Code agents operating under CLAUDE.md's per-role contracts. The Human user Isidro Rodriguez is the gatekeeper: the only role authorized to flip `Task status: Approved for apply`, and the subjective-quality reviewer for the final product. The 3–4 hrs/week constraint is human review-and-approval bandwidth, NOT implementation throughput. Throughput is bounded by agent context budget per session, not by human calendar time. This reframing changed the wording of `_context/07` Delivery Summary's calendar-constraints bullet and the Dependency And Blocker Register's staffing row, and was saved to auto-memory so future sessions don't re-make the "developer availability" mistake.

#### Alternatives Considered

- Treat solo-dev as literal: Human writes code, agents only review — rejected: contradicts the file-07 calendar reality and would make the milestone schedule unachievable.
- Hybrid: Human writes some code, agents write some code — rejected: muddles the role contract; review-only is the cleaner gate.

#### Consequences

- Positive: milestone scope can be calibrated against agent capacity (per-session context budget) rather than human-coding hours; the file-07 schedule becomes meaningful.
- Negative: every Apply Mode flip is a real review burden on the Human; the 3–4 hrs/week budget concentrates on that surface. Mitigated by section-by-section drafting protocols (CLAUDE.md) and Architect-Plus-Gatekeeper-in-session patterns (T-001, T-002, ADR-064).

### ADR-034: Uninstall ≡ total deletion — load-bearing equivalence for the data-protection commitment

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/06` § Data Ownership And Retention; `_context/05` § Data Protection; 2026-05-15T20:45:09Z handoff

#### Context

Android's app-uninstall semantics drop the app-private storage (Room DB + WorkManager DB), and pending AlarmManager registrations are canceled by the OS at package removal. The in-app hard-delete (ADR-022) reaches the same data-residue state. This equivalence is what makes the file-05 data-protection commitment durable: Patients have two paths to total deletion (uninstall and in-app hard-delete), both producing zero residue.

#### Decision

Pin "uninstall ≡ total deletion" as a load-bearing equivalence in `_context/06` § Data Ownership. The equivalence is composed from: ADR-021 (no INTERNET) + ADR-066 (`android:allowBackup="false"`) + ADR-022 (hard delete reaches zero residue) + Android's package-removal semantics. A future Google-Backup-by-default integration would break this equivalence and require an ADR. A future caregiver-account feature that synchronizes to cloud would also break it.

#### Alternatives Considered

- Treat hard-delete as the only complete-deletion path — rejected: misses Patients who uninstall instead of hard-deleting; the equivalence makes both paths credible.

#### Consequences

- Positive: data-protection commitment doesn't depend on Patient learning the in-app hard-delete; uninstall is a natural Patient action.
- Negative: any future integration that touches cloud backup (Google Drive, iCloud-equivalent, vendor analytics) breaks the equivalence and needs ADR review; intentional gate.

### ADR-033: 4-class sensitivity taxonomy (Sensitive personal data / App configuration / Trigger metadata / None) + per-discipline contract versioning rules (5 distinct disciplines)

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/06` § Trust Boundaries And Validation Ownership, § Interface And Contract Inventory; 2026-05-15T20:45:09Z handoff

#### Context

File 06 introduces 9 trust-boundary rows and 11 contract-inventory rows. Without an explicit sensitivity taxonomy, every row would have to re-invent the same vocabulary. Without a per-contract versioning rule, the safety mechanism is unclear (additive? class-name-stable? schema-version+Migration?).

#### Decision

4-class sensitivity taxonomy: **Sensitive personal data** (medication name, dose, schedule, adherence history); **App configuration** (notification channel importance, biometric inactivity timeout, theme preference); **Trigger metadata** (alarm IDs, worker class names, notification channel IDs — no Patient content); **None** (e.g., the clock / timezone boundary's adversarial-clock-manipulation accepted risk). Per-contract versioning rules: (i) *additive-only* for in-process domain events and `PendingIntent` extras; (ii) *class-name-stable* for WorkManager Workers; (iii) *strict version integer + Migration class* for Room schema; (iv) *ID-stable* for notification channels (renaming loses Patient customizations); (v) *git-tag* for the Privacy Policy; (vi) *per-Play-submission* for the Data safety declaration.

#### Alternatives Considered

- Single uniform versioning rule (e.g., semver) — rejected: each surface has different safety mechanisms; SemVer's three-component model doesn't apply uniformly.
- 2-class sensitivity taxonomy (sensitive vs. not) — rejected: loses the distinction between App configuration (Patient-customizable) and Trigger metadata (developer-controlled).

#### Consequences

- Positive: file 06's 9 boundary rows + 11 contract rows have a consistent vocabulary; future contracts inherit the taxonomy.
- Negative: 4 classes + 6 versioning rules is a non-trivial vocabulary; Konsist cannot enforce it, so discipline is procedural.

### ADR-032: Symmetric no-import + no-export at four architecture layers; on-device-only posture bidirectionally enforced

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/06` § Data Ownership; `_context/05` § Data Protection no-export rule (ADR-022); `_context/01` assumption (4); 2026-05-15T20:45:09Z handoff

#### Context

ADR-022 commits to no-export feature in MVP. File 01 assumption (4) records that the Patient manually configures Medications (no prescription import). Without making these symmetric and architecturally enforced, a future "let's add data sharing" conversation could expand the data-sharing perimeter in one direction (e.g., import only) without confronting the other.

#### Decision

No import feature + no export feature in MVP, symmetric, enforced at four architecture layers: (i) no `android.permission.INTERNET` (ADR-021) — no network surface to import-from or export-to; (ii) no network-stack imports (`java.net.*`, `okhttp3.*`, `retrofit2.*`) Konsist-enforced; (iii) no file-system-export affordance — no `ACTION_SEND` or file-write intent surface; (iv) no file-system-import affordance — no `ACTION_OPEN_DOCUMENT` or file-read intent surface. A future "let's add data sharing" conversation has to confront every layer, not just one.

#### Alternatives Considered

- Allow export but not import (or vice versa) — rejected: asymmetric perimeter creates pressure to add the other direction.

#### Consequences

- Positive: on-device-only posture is bidirectionally hard-locked; the perimeter is unambiguous.
- Negative: Patients cannot move medications between devices without re-entering them; trade-off acknowledged.

### ADR-031: No Patient data in failure evidence — hard rule (log lines and crash reports name failures by `Result.Err` sub-type and aggregate ID only, never by drug name, dose schedule, or Confirmation history); Konsist-enforced via ADR-049's two new rules

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/06` § Error Handling And Recovery; `_context/05` § Data Protection no-transmission (ADR-021); `_context/12` Konsist rules (ADR-049); 2026-05-15T20:45:09Z handoff

#### Context

ADR-021 commits to no transmission off-device. But ADR-018's observability (log `Result.Err.Unexpected`) and the deferred-but-acknowledged crash reporter both produce evidence streams that could leak Patient data if implemented naively. The safety property must be locked in *before* the deferred crash-reporter (file 12) lands.

#### Decision

No Patient data in failure evidence — hard rule, not a preference. Log lines and (forthcoming) crash reports name failures by `Result.Err` sealed sub-type and by aggregate ID (`MedicationId`, `DoseId`, `ConfirmationId`), never by drug name, dose schedule, or Confirmation history. Konsist-enforced at T-001 via the two new rules in ADR-049: (i) no `throw` statement uses Kotlin string templates / `String.format` / `StringBuilder.append` in its message argument; (ii) every Domain-layer `data class` overrides `toString()` to a redacted form.

#### Alternatives Considered

- Per-developer discipline (no static enforcement) — rejected: one slip leaks Patient data into Android Vitals; static rules close the channel.

#### Consequences

- Positive: Patient data cannot leak into observability streams; crash reports are safe to aggregate.
- Negative: per-Patient debugging requires the Patient to reproduce on their own device (ADR-049); trade-off accepted.

### ADR-030: `SchedulerPort.cancelAll()` extended from tests-only to F-014 hard-delete production path

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/06` § F-014 (hard-delete flow); `_context/04` SchedulerPort framing; ADR-022; 2026-05-15T20:45:09Z handoff

#### Context

File 04 § SchedulerPort framed `cancelAll()` as "intended for `Application.onDestroy`-style resets in tests". The F-014 hard-delete flow (ADR-022) needs to cancel every pending AlarmManager registration before dropping all Room tables, or else the Patient could receive Reminders for Medications that no longer exist. `cancelAll()` is the obvious mechanism but its production use crosses the original test-only scope.

#### Decision

Extend `SchedulerPort.cancelAll()` from tests-only to the F-014 hard-delete production path. File 06 § Reliability § Idempotency now makes `cancelAll()` safe against empty state, and the hard-delete flow relies on it in production. This is a small spec drift between files 04 and 06 — it lives in file 06 because that's where destructive-flow reliability rules belong; file 04 may benefit from a small clarifying note when next revisited.

#### Alternatives Considered

- Author a new `SchedulerPort.cancelAllForHardDelete()` method — rejected: same semantics, different name; would create a parallel method to maintain.

#### Consequences

- Positive: F-014 has clean Reminder-cancellation semantics; idempotency-against-empty-state is the right contract anyway.
- Negative: file 04's "tests-only" framing is now stale; logged for a future file-04 clarifying revision.

### ADR-029: `Result.Err` sealed sub-type catalog closed for MVP at 7 sub-types (ValidationFailed, RetroactiveWindowExpired, ExactAlarmPermissionRevoked, NotificationsPermissionRevoked, UnknownDose, UnexpectedNotificationPayload, Unexpected)

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/06` § Interface And Contract Inventory; `_context/04` two-tier error policy (ADR-014); 2026-05-15T20:45:09Z handoff

#### Context

ADR-014 pinned the two-tier error policy with sealed `Result<T, E>` but didn't enumerate the Err sub-types. Without enumeration, every adapter can introduce its own Err variant ad-hoc, breaking exhaustive `when` patterns in Presentation and Content And Voice (ADR-058) error strings drift.

#### Decision

`Result.Err` sealed sub-type catalog closed for MVP at 7 sub-types: `ValidationFailed` (BR-002 / BR-003); `RetroactiveWindowExpired` (BR-011); `ExactAlarmPermissionRevoked` (F-001 / F-011); `NotificationsPermissionRevoked` (F-005); `UnknownDose` (F-006 lookup); `UnexpectedNotificationPayload` (F-006 defensive parse); `Unexpected` (catch-all for converted exceptions). New `Result.Err` types after MVP require an ADR amendment.

#### Alternatives Considered

- Open catalog (any module can add Err variants) — rejected: defeats exhaustive `when` benefits and breaks Content And Voice's three-part error pattern (ADR-058).

#### Consequences

- Positive: exhaustive `when` in Presentation; one-to-one mapping from Err to user-facing error string; closed-set discipline.
- Negative: adding a new Err variant is an ADR amendment; intentional friction.

### ADR-028: Build-time / submission-time content contracts (Privacy Policy, Play Data safety declaration) inventoried in `_context/06` § Interface And Contract Inventory alongside runtime contracts

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/06` § Interface And Contract Inventory; `_context/05` § Compliance Activation + Evidence; 2026-05-15T20:45:09Z handoff

#### Context

File 06's contract inventory primarily focuses on runtime contracts (domain events, PendingIntent extras, WorkManager Workers, Room schema, notification channels). But the Privacy Policy and the Play Data safety declaration are also contracts — durable commitments to Patients and regulators that must not drift silently. Without inventorying them alongside runtime contracts, Security has no single drift-detection surface.

#### Decision

Inventory build-time / submission-time content contracts in `_context/06` § Interface And Contract Inventory alongside runtime contracts. The Privacy Policy contract rows (git-tag versioning per ADR-033) and the Play Data safety declaration (per-Play-submission versioning) appear in the same table as `MedicationCreated`, `Room schema`, `reminder_on_time` channel, etc. Gives Security a single reference for drift detection per the file-05 Compliance Activation Rule.

#### Alternatives Considered

- Inventory content contracts in a separate file-05 table — rejected: cross-references would multiply; the file-06 boundary inventory is the natural home for "things that must not drift silently".

#### Consequences

- Positive: Security has one drift-detection surface across runtime and content contracts.
- Negative: file 06's inventory is wider than its title suggests; minor structural concession.

### ADR-027: Android OS subsystems treated as integrations — 9 Integrations Register rows + 11 contract rows + 15 flow narratives (F-001 through F-015) as canonical QA citation handles

- Date: 2026-05-15
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/06` § Integrations Register, § Interface And Contract Inventory, § Data Flow Narratives; 2026-05-15T20:45:09Z handoff

#### Context

NemoPill has no SaaS or backend integrations — but it has a rich set of Android OS subsystems (AlarmManager, WorkManager, NotificationManager, BroadcastReceiver, BiometricPrompt, Clock, Timezone, Room, Play Store) that QA and Security need to validate against. Framing the OS as "internal infrastructure" leaves these surfaces without the integration-template rigor (criticality, trust column, contract versioning).

#### Decision

Treat Android OS subsystems as integrations (not internal infrastructure) in `_context/06` § Integrations Register and the cross-boundary Data Flow Narratives. 9 integrations rows (AlarmManager, WorkManager, NotificationManager, OS broadcast surface, system clock, system timezone, Android Keystore / BiometricPrompt, Room / SQLite, Google Play Store). 11 contract-inventory rows (3 domain-event producers + 2 PendingIntent payloads + 1 OS broadcast + 1 WorkManager Worker + 1 Room schema + 1 notification channel + 2 build-time content contracts per ADR-028). 15 flow narratives F-001 through F-015 (intentionally splitting BR-010 into 5 separate flows: F-005 on-time fire, F-008 sweep, F-009 late-Reminder presentation, F-010 batched-summary, F-011 boot-survival re-registration). 15 flow IDs are the canonical citation handle for QA test coverage — `_context/05` § Integration tests and § End-to-end tests map directly to F-### IDs.

#### Alternatives Considered

- Keep OS subsystems as internal infrastructure — rejected: loses integration-template rigor; QA / Security would lack a per-OS-surface validation handle.

#### Consequences

- Positive: file 06 gives QA / Security a single concrete artifact to validate BR-004 / BR-010 / BR-012 OS-crossing paths against; flow IDs eliminate "the boot path" vs. "F-011" ambiguity in test-plan vocabulary.
- Negative: file 06's vocabulary diverges slightly from the framework template (which assumes SaaS / backend integrations); the OS-as-integrations framing is recorded in the file preamble for future readers.

### ADR-026: Evidence + audit trail entirely in the GitHub repo `https://github.com/izirodriguez/NemoPill`; no external evidence locker; 5-year regulator-prudent retention tail beyond distribution end

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security; Compliance (folded into Security per ADR-024)
- Related milestone or task: `_context/05` § Evidence And Audit; 2026-05-13T22:53:12Z handoff

#### Context

Solo-dev project with the CCPA + LatAm compliance posture from ADR-024 needs an evidence locker. External evidence vendors (compliance SaaS) add cost and another network surface; the GitHub repo itself is the durable record for `_context/`, ADRs, handoff entries, task packets, Privacy Policy version history, Play Console submission snapshots, and dependency-review commit messages.

#### Decision

Evidence + audit trail entirely in the GitHub repo at `https://github.com/izirodriguez/NemoPill`. Seven artifact classes: (i) `_context/` files; (ii) ADRs in `_context/09_decision_log.md`; (iii) handoff entries in `_context/handoff_log.md`; (iv) `_context/08_active_task_packet.md` git history; (v) Privacy Policy version history in `legal/`; (vi) Play Console submission snapshots in `_build/play-store-state.md`; (vii) dependency-review commit messages. Indefinite retention for in-repo artifacts. Repository accessibility "as long as NemoPill is distributed" plus a 5-year regulator-prudent retention tail (longest plausible statute-of-limitations / inspection window across in-scope jurisdictions; conservative default that file 12 can revise if a specific jurisdiction's longest applicable tail differs).

#### Alternatives Considered

- External evidence locker (compliance SaaS vendor) — rejected: cost + network surface; the file-05 distinguishes operationally between Patient data (zero retention) and project audit data (indefinite + 5-year tail), and the repo is the natural home for the latter.

#### Consequences

- Positive: zero-cost evidence trail; git history gives temporal ordering; no vendor lock-in.
- Negative: GitHub outage = audit trail unavailable; trade-off acknowledged in the file-12 § Section 8 DR scenario (ADR-050).

### ADR-025: Bilingual (English + Spanish) Privacy Policy serves both CCPA and the Latin American national data-protection laws row; authored at file 11 (substance) and file 12 (path / format / loading per ADR-048)

- Date: 2026-05-13
- Status: Accepted (substance later authored at T-002 per ADR-061; path/format refined at T-001 per ADR-048)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Compliance (folded into Security)
- Related milestone or task: `_context/05` § Compliance CCPA / CPRA + LatAm rows; 2026-05-13T22:53:12Z handoff

#### Context

ADR-024's compliance posture requires a Privacy Policy that satisfies both CCPA / CPRA (US California Patients) and the consolidated Latin American national data-protection laws row. A single bilingual artifact is the simplest path; separate documents per jurisdiction would multiply maintenance burden.

#### Decision

Single bilingual (English + Spanish) Privacy Policy serves both CCPA / CPRA and the LatAm national data-protection laws row. Deferred for content authoring to file 11 (substance) and file 12 (path / format / loading). Final wording lives in `legal/PRIVACY_POLICY.en.md` and `legal/PRIVACY_POLICY.es.md` per the file-05 Evidence trail; file 11 owns the content authoring decisions.

#### Alternatives Considered

- Separate per-jurisdiction Privacy Policy documents — rejected: substantively the same disclosure (no data collected, no data shared, no data sold) across all in-scope jurisdictions.

#### Consequences

- Positive: one artifact, two languages, multiple jurisdictions satisfied.
- Negative: any policy edit must be authored bilingually; Spanish long-form translation is gated on the M-006 Spanish-translation-capacity row.

### ADR-024: Play distribution = US + Latin America excluding Brazil; CCPA / CPRA Applicable; GDPR / UK-DPA Not applicable; Latin American national data-protection laws Applicable as a single combined row

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Compliance (folded into Security per file-05 Compliance Activation Rule)
- Related milestone or task: `_context/05` § Compliance Applicability Matrix, § Distribution; `_context/12` § Section 6 Distribution scope (ADR-046); 2026-05-13T22:53:12Z handoff

#### Context

Compliance scope is downstream of distribution scope. Google Play does not support sub-national geo-restriction, so any US distribution implicitly includes California and triggers CCPA / CPRA. EU / EEA / UK distribution would trigger GDPR / UK-DPA with the Article 20 portability requirement that contradicts ADR-022's no-export commitment. Brazil distribution would trigger LGPD with its own specific requirements that diverge from the consolidated LatAm row.

#### Decision

Play distribution locked to US + Latin America excluding Brazil. CCPA / CPRA `Applicable` because California is included by virtue of US distribution. GDPR / UK-DPA `Not applicable` because no EU / EEA / UK distribution; the no-export rule from ADR-022 stands. If distribution ever expands to the EU, Article 20 portability would force a revision of the no-export rule (ADR amendment required). Latin American national data-protection laws (Mexico LFPDPPP, Argentina PDPL, Chile Ley 19.628, Colombia Ley 1581, Peru Ley 29733, Uruguay Ley 18.331, etc.) `Applicable` as a single combined row, satisfied substantively by the no-transmission + hard-delete architecture plus a bilingual Privacy Policy (ADR-025). Brazil LGPD excluded by the no-Brazil distribution decision. Compliance role folded into Security review per the file-05 Compliance Activation Rule when only the always-applicable rows are present.

#### Alternatives Considered

- Worldwide distribution — rejected: GDPR / LGPD compliance burden exceeds MVP scope.
- US only — rejected: LatAm distribution is part of the product's reach decision in `_context/01`.

#### Consequences

- Positive: compliance scope is well-defined and operationally satisfied by the architecture; CCPA satisfaction is cheap (no data to honor a request against).
- Negative: any future distribution expansion to EU or Brazil is a multi-file revision; intentional gate.

### ADR-023: All `PendingIntent` instances use `FLAG_IMMUTABLE` on Android 12+, enforced by Konsist as an architecture-conformance assertion (not a code-review checklist item)

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/05` § Security Guardrails OS permissions and IPC; `_context/06` § Trust Boundaries reinforcement (ADR-027); 2026-05-13T22:53:12Z handoff

#### Context

Intent-redirection is a real CVE class on Android; `PendingIntent` instances without `FLAG_IMMUTABLE` can be hijacked by malicious apps on Android 12+. The discipline must be pattern-matchable and enforced statically, not left to code-review checklists.

#### Decision

All `PendingIntent` instances use `FLAG_IMMUTABLE` (or `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT` when mutability of extras is required but PendingIntent identity is not) on Android 12+. Konsist asserts that every `PendingIntent.getActivity` / `getBroadcast` / `getService` call site includes `FLAG_IMMUTABLE` in its flags argument. The rule is reinforced at the Trust Boundaries layer in `_context/06` as the architectural guardrail against intent-redirection at notification inline-action and AlarmManager `PendingIntent` boundaries.

#### Alternatives Considered

- Code-review checklist enforcement — rejected: one slip past review is one CVE.

#### Consequences

- Positive: intent-redirection class statically prevented; pattern-matchable rule belongs in tests, not in review checklists.
- Negative: rare cases where mutable PendingIntent is legitimately needed require an explicit Konsist suppression with reviewer comment; intentional friction.

### ADR-022: Patient-controlled irreversible hard delete with explicit deliberate-confirmation UX; no export feature in MVP (the deletion UX cannot be silently softened later)

- Date: 2026-05-13
- Status: Accepted (deliberate-confirmation pattern later locked to checkbox-gated per ADR-056)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/05` § Data Protection sub-rule (b); `_context/06` § F-014 (ADR-030); `_context/11` § Section 8 hard-delete dialog (ADR-056); 2026-05-13T22:53:12Z handoff

#### Context

Hard data deletion needs to be (i) Patient-initiated (no automatic deletion), (ii) irreversible (no recovery from external backup, by design — ADR-021 + ADR-066 ensure no off-device residue), and (iii) deliberate (the Patient must consciously confirm the irreversibility). Export is the natural escape valve that softens the hard-delete promise; if exports exist, Patients have a partial-recovery path, which changes the data-protection commitment.

#### Decision

Patient-controlled irreversible hard delete via a Settings action that drops every Room table. Deletion is no-recovery, no-backup, no-undo. Patient is shown a confirmation dialog stating this plainly with an explicit deliberate-confirmation interaction (typed phrase or two-step tap — pattern choice later locked to checkbox-gated at file 11 per ADR-056). No export feature in MVP — deliberate omission, not oversight. Adding export requires revising `_context/01` scope, a Security-role review, and a new file-05 row. The deletion UX cannot be silently softened later — file-05 § Data Protection pre-commits this and file 11's hard-delete dialog component (ADR-056) operationalizes it as a Konsist-asserted invariant.

#### Alternatives Considered

- Soft-delete (mark as deleted but keep rows) — rejected: defeats the data-protection commitment.
- Single-tap "OK" confirmation — rejected: not deliberate enough; novice Patient might tap by accident.

#### Consequences

- Positive: data-protection commitment is unambiguous; deletion UX gated against silent erosion.
- Negative: Patients who want to wipe-and-restart cannot export-then-reimport; trade-off accepted as part of the on-device-only posture.

### ADR-021: No `android.permission.INTERNET`, enforced by Konsist (no `java.net.*`, `okhttp3.*`, `retrofit2.*`, or analogous network APIs imported anywhere); load-bearing rule for ADR-024 and ADR-049

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/05` § Data Protection sub-rule (a); `_context/06` § Trust Boundaries; `_context/12` § Section 7 Android Vitals (ADR-049); ADR-024 (CCPA satisfaction depends on this); 2026-05-13T22:53:12Z handoff

#### Context

The privacy commitment from `_context/01` (no data collected, no data shared, no data sold) must be architecturally enforced, not just promised. Declaring `android.permission.INTERNET` in the manifest is the single point at which the privacy commitment can be broken — without that permission, the OS prevents any direct network access. Konsist-enforcement closes the indirect channels (a library that internally opens connections without the permission would crash, but better to fail at build than at runtime).

#### Decision

The app declares no `android.permission.INTERNET` in `AndroidManifest.xml`, verified by Konsist tests asserting (i) the permission is absent, and (ii) no class anywhere imports `java.net.*`, `okhttp3.*`, `retrofit2.*`, or analogous network APIs. This is the load-bearing rule for the file-05 § Compliance CCPA + LatAm rows (ADR-024), for the Play Data safety declaration ("no data collected, no data shared, no data sold"), and for the file-12 § Section 7 Android Vitals choice (ADR-049 — Play Services runs in a separate process).

#### Alternatives Considered

- INTERNET permission + manual discipline — rejected: one slip exfiltrates data.
- Network-only-for-crash-reporter — rejected: defeats the whole posture; crash reporting routed to Android Vitals via Play Services (ADR-049).

#### Consequences

- Positive: privacy commitment is architecturally enforced; CCPA satisfaction is trivial (no data to honor a request against); ADR-049's no-INTERNET-conflicting crash reporter inherits this safety.
- Negative: any future feature that wants a network surface must confront this rule and amend it explicitly; intentional friction.

### ADR-020: Defense-in-depth input validation — Domain owns canonical BR enforcement; Presentation duplicates format / range checks for synchronous UX feedback; Room re-validates preconditions on resurrection

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/05` § Input Validation; `_context/06` § Trust Boundaries Domain layer rules; 2026-05-13T22:53:12Z handoff

#### Context

The functional-programming purity of Domain (ADR-013) requires Domain value-object constructors to enforce BRs. But Presentation must give the Patient synchronous UX feedback (red field outline, inline error text) without round-tripping through `Result.Err`. Re-validating preconditions when reading from Room ("trust nothing, even your own database") guards against corrupted-row defects.

#### Decision

Defense-in-depth input validation: (i) Domain owns the canonical BR enforcement in value-object constructors; the Domain layer is the source of truth. (ii) Presentation duplicates format / range checks for synchronous UX feedback (e.g., "name cannot be empty" shown inline as the Patient types). Duplication is intentional. (iii) Room persistence boundary re-validates preconditions on resurrection — value-object constructors are the ultimate guard against corrupted-row defects.

#### Alternatives Considered

- Single-layer validation (Domain only) — rejected: forces a round-trip through Result.Err for synchronous UX feedback.
- Trust Room rows (no re-validation on read) — rejected: corrupted-row defects (from migration bugs, manual sqlite tampering) would propagate to runtime.

#### Consequences

- Positive: synchronous UX without sacrificing Domain authority; defense against corrupted-row defects.
- Negative: validation logic appears in two layers (Domain + Presentation); developers must keep them in sync — file-05 mitigates with rule-anchoring (every Presentation check cites the BR it mirrors).

### ADR-019: App-level biometric / PIN gate is Patient-configurable, default off; OS lock screen is the primary auth boundary; biometric inactivity timeout allow-list locked to 1 / 5 (default) / 15 / 60 minutes / Never re-prompt while app is open (per ADR-056)

- Date: 2026-05-13
- Status: Accepted (inactivity timeout values locked at file 11 per ADR-056)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/05` § Authentication; `_context/11` § Section 8 Biometric-gate Settings group (ADR-056); 2026-05-13T22:53:12Z handoff

#### Context

The device-locked threat model (calibrated against shoulder-surfers + casual access) makes the OS lock screen the primary auth boundary. Adding a forced app-level biometric gate would frustrate the majority of Patients who already keep their devices locked, while still defending the minority case (shared device, unattended unlocked device). Patient-configurable default-off threads the trade-off correctly.

#### Decision

App-level auth is optional and Patient-configurable. OS lock screen is the primary auth boundary; `androidx.biometric.BiometricPrompt` is layered on top as an optional gate with a Patient-configurable inactivity timeout. Default off. Inactivity timeout allow-list locked at 1 / 5 (default) / 15 / 60 minutes / Never re-prompt while app is open (file 11 § Section 8 Biometric-gate Settings group per ADR-056). Settings UX surfaces the toggle plus the timeout selector.

#### Alternatives Considered

- Always-on app-level gate — rejected: defeats the file-03 one-handed-friendly + glanceable posture for the majority.
- No app-level gate at all — rejected: closes off the defense-in-depth surface for shared-device Patients.

#### Consequences

- Positive: novice Patients aren't friction-blocked; security-aware Patients have an optional layer.
- Negative: default-off means the safety net only protects Patients who discover the toggle; mitigated by Settings discoverability.

### ADR-018: Observability is intentionally minimal — log only `Result.Err.Unexpected`; metrics / tracing / alerting `Not applicable`; no analytics SDK in MVP; crash reporting deferred to file 12 (later resolved to Android Vitals per ADR-049)

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; Security
- Related milestone or task: `_context/05` § Observability; `_context/12` § Section 7 (ADR-049 resolves crash reporting); 2026-05-13T22:53:12Z handoff

#### Context

Client-only app with no service surface and a sensitive-data privacy commitment has limited need for observability. Standard observability vocabulary (metrics, tracing, alerting) presupposes a service that someone is on-call for; NemoPill has neither. Adding any of these primarily creates a privacy exfiltration surface without operational payoff. An analytics SDK is the most common silent-data-leak vector and is explicitly excluded.

#### Decision

Observability intentionally minimal. Log only `Result.Err.Unexpected` (the catch-all for converted exceptions); Domain events observable through `DomainEventPublisher` bus, not separately logged. Metrics / Tracing / Alerting all `Not applicable`. Crash reporting deferred to file 12 but framed as acknowledged-not-committed because adding a crash reporter implies network transmission which conflicts with ADR-021's no-INTERNET rule (later resolved to Android Vitals per ADR-049). No analytics SDK in MVP — restated even though file 01 implies it, because file 05 is the file Security reads.

#### Alternatives Considered

- Add APM / metrics — rejected: implies network surface.
- Add analytics SDK for Patient-behavior insights — rejected: contradicts the privacy commitment.

#### Consequences

- Positive: no exfiltration surface; minimal vendor dependencies.
- Negative: low operational visibility into Production behavior; Android Vitals fills the gap at ADR-049.

### ADR-017: Engineering quality program — two-pass TDD discipline, coverage thresholds (`:domain`/`:core` ≥ 90% / `:application` ≥ 80%), all tests under Robolectric (no `:src/androidTest/`), snapshot baseline of 20, architecture conformance as a test row, quality gates split by phase with role-based ownership

- Date: 2026-05-13
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect; QA
- Related milestone or task: `_context/05` § Engineering Quality, § Test Portfolio, § Coverage, § Quality Gates; `_context/12` § Section 4 (CI implementation per ADR-044); 2026-05-13T22:53:12Z handoff

#### Context

The file-05 quality program needs to be operationally executable for a solo gatekeeper with the role-architecture from ADR-035 (Claude Code agents fill Developer/QA/Security; Human only flips Approved-for-apply). The program must encode review discipline as structural rules, not as personal habits.

#### Decision

Two-pass TDD discipline: Developer proposes Gherkin/BDD scenarios in `_context/08` referencing file-03 BDD IDs and file-02 BR rules; QA renders `Coverage Sufficient` / `Coverage Insufficient` verdict before the Human sets `Approved for apply`. QA cannot self-approve; Developer cannot bypass QA. Adapter code follows pragmatic tests-first (sketch first, then Robolectric tests); pure Domain and Application code follows strict tests-first. Coverage thresholds: `:domain`/`:core` ≥ 90% line; `:application` ≥ 80%; no `%` target on `:infrastructure` or `:presentation` (covered by integration / E2E / snapshot rows). Tool: Kover (per ADR-044). All test types run under Robolectric for MVP; `:src/androidTest/` is empty; Compose UI Test uses `createComposeRule` under Robolectric's JUnit 4 runner. Snapshot baseline 4 screens × 2 form-factor breakpoints × 2 themes = 16 + 4 notification snapshots = 20; tool Roborazzi (per ADR-044). Architecture conformance is a real test row (sharing tool with Domain-purity gate — Konsist per ADR-044): no `android.*` in `:domain`, no upward `:infrastructure` → `:presentation` dep, no cross-feature `:domain`/`:application` imports. Quality Gates split by phase: type check + lint + fast test suite at pre-commit (Git hook); slow test suite + coverage + debug build + framework validator at pre-merge; QA test-coverage review at pre-Apply-Mode; Security guardrail review (conditional) and `_context/` consistency check at task close. Role-based ownership per Q3-A: Developer owns automated gates; QA owns the test-coverage review; Security owns the guardrail review.

#### Alternatives Considered

- Connected-device instrumented tests in `:src/androidTest/` — rejected for MVP for reproducibility and speed; deferred to a later milestone.
- Single uniform coverage threshold — rejected: Domain warrants 90% (pure, exhaustively testable); Infrastructure isn't line-coverable in a meaningful way.

#### Consequences

- Positive: gatekeeper has structural review discipline rather than personal habits; quality gates are role-anchored.
- Negative: many gates produce many CI-time costs; ADR-044's pre-commit fast-path mitigates by deferring slow checks to CI.

### ADR-016: Cross-cutting Presentation screens (today's-Dose list, Adherence history) live in `:app::presentation/` — non-feature-owned views

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Repository Structure; 2026-05-09T20:12:34Z handoff

#### Context

The today's-Dose list and the Adherence history view both join data from multiple Bounded Contexts (Medication Management + Scheduling-Reminders + Adherence Tracking). Placing them in any single feature module would force that module to import another's Domain or Application — violating ADR-011's "feature modules never import each other's Domain or Application packages" rule.

#### Decision

Cross-cutting Presentation screens (today's-Dose list, Adherence history) live in `:app::presentation/`, not in any feature module. They are explicitly named as non-feature-owned views in `_context/04`. They consume use cases from multiple feature modules via the `:app::di` DI graph.

#### Alternatives Considered

- Place today's-Dose list in `:scheduling` — rejected: `:scheduling` would have to import Adherence Tracking's `Confirmation` to render the today-Dose row's "taken" indicator.
- Create a new `:cross-cutting-presentation` module — rejected: only two screens; module overhead exceeds benefit.

#### Consequences

- Positive: feature-module purity preserved; cross-cutting screens have a natural home.
- Negative: `:app` is now a Presentation module as well as a shell; documented as the convention going forward.

### ADR-015: Single Room `NemoPillDatabase` instance for the whole app; `Instant` stored as UTC epoch millis via Room `TypeConverter`; `ZoneId` (IANA TZ ID) stored as IANA string via Room `TypeConverter`; `ClockPort` and `TimezonePort` as shared-kernel ports

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Persistence-mapping resolution; `_context/02` Q3 resolution; ADR-006 (Q1/Q2/Q3 deferral from file 02); ADR-045 (migration discipline); 2026-05-09T20:12:34Z handoff

#### Context

ADR-006 deferred the persistence-layer mapping for `Instant` and `ZoneId` (IANA TZ ID) to file 04. The Dose-aggregate atomic transaction in `room.withTransaction { }` spans `:scheduling` (Dose status) and `:adherence-tracking` (Confirmation), so a single Room database is required to preserve the cross-module atomicity. ClockPort and TimezonePort need a home that both `:scheduling` and Domain can depend on without violating ADR-008's Domain-purity rule.

#### Decision

Single `NemoPillDatabase` Room instance for the whole app, declared in `:app::di::AppModule`, with all four feature DAOs registered on it. `Instant` stored as UTC epoch millis via Room `TypeConverter`. `ZoneId` (IANA TZ ID) stored as the IANA string via Room `TypeConverter`. `ClockPort` and `TimezonePort` declared in `:core` as shared-kernel ports; default adapters `SystemClockAdapter` / `SystemTimezoneAdapter` provided in `:core`. Domain functions take `now: Instant` as a parameter — making BR-008 / BR-009 / BR-010 / BR-011 / BR-012 deterministically testable without device-clock manipulation.

#### Alternatives Considered

- Per-feature Room databases — rejected: would break the Dose-aggregate cross-module transaction.
- ClockPort in `:scheduling` — rejected: Adherence Tracking also needs the clock; `:core` is the right home.
- ISO-8601 string storage for Instant — rejected: epoch millis is the more compact and lossless representation for storage.

#### Consequences

- Positive: Dose-aggregate transactions atomic across modules; Domain is deterministically testable.
- Negative: a single database file means a corrupted Room file affects all features; mitigated by ADR-045's defensive migration discipline.

### ADR-014: Two-tier error policy with custom sealed `Result<T, E>` in `:core` (rejecting `kotlin.Result`); five functional-programming rules anchoring Domain purity, immutability, side-effect isolation, two-tier error, and shared-utility admission

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Error policy + Functional Programming Rules; ADR-029 (catalog of Err sub-types); 2026-05-09T20:12:34Z handoff

#### Context

Domain and Application layers need a typed error channel so file-02's BR violations are exhaustively patternable in `when` expressions. `kotlin.Result` is `Throwable`-based and therefore exception-flavored — it doesn't give the same exhaustiveness guarantees as a custom sealed Result. Domain purity also requires explicit rules about side-effect isolation and shared-utility promotion.

#### Decision

Two-tier error policy with custom sealed `Result<T, E>` in `:core`. Expected business failures (every BR has a Result variant per ADR-029) use `Result.Err.<Variant>`. Exceptions only for unexpected / programmer-error situations; converted at the Application boundary to `Result.Err.Unexpected`. `kotlin.Result` rejected. Five functional-programming rules: (1) **Domain purity** — every dependency including clock and timezone passed in as parameter; verification rule is "Domain compiles with only `kotlin-stdlib` + `junit` + `truth` on its classpath." (2) **Immutability** — `data class` with `val` only; state changes return new instances; immutable collections in Domain. (3) **Side-effect isolation** — all I/O at Application layer; Domain functions are non-suspending by default. (4) **Two-tier error policy** (as above). (5) **Shared-utility admission** — two-of-three rule (a utility may be promoted to `:core` only when used in at least two of the three Bounded Contexts); no `util` package; helpers private / internal to feature modules unless promoted.

#### Alternatives Considered

- `kotlin.Result` — rejected: exception-flavored; doesn't give exhaustive sealed-variant patterning.
- Throw exceptions for BR violations — rejected: BR violations are expected business failures, not exceptional conditions.

#### Consequences

- Positive: BR-violation patterning is exhaustive at compile time; Domain purity is verifiable.
- Negative: every adapter / use case must explicitly wrap exceptions to convert them to `Result.Err.Unexpected`; intentional friction at the Application boundary.

### ADR-013: Hilt (KSP) for DI over Koin — compile-time graph verification preferred over runtime-only resolution for a solo dev

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Technology and Runtime Decisions; 2026-05-09T20:12:34Z handoff

#### Context

DI choice for an Android Kotlin project is typically Hilt vs. Koin. Hilt offers compile-time graph verification (errors surface at build); Koin offers runtime DI with a smaller learning curve but errors surface only at runtime. Solo dev with no second pair of eyes benefits from compile-time guarantees.

#### Decision

Hilt with KSP for DI. Compile-time graph verification preferred over runtime-only resolution. KSP over KAPT for build-speed reasons.

#### Alternatives Considered

- Koin — rejected: runtime errors are harder to catch without code review by a second person.
- Manual DI (constructor injection without a container) — rejected at MVP for the ViewModel + Application-scoped graph complexity.

#### Consequences

- Positive: DI graph errors surface at build time, not at first Patient interaction.
- Negative: Hilt adds annotation processing overhead and increased build time compared to manual DI; trade-off accepted for the safety net.

### ADR-012: Dual-strategy background work — AlarmManager (`setExactAndAllowWhileIdle` / `setAlarmClock`) for exact-time Reminder firing + WorkManager for the daily horizon-extension job and the `pending → missed` sweep fallback

- Date: 2026-05-09
- Status: Accepted (specific tuning later locked at ADR-042)
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Technology and Runtime Decisions; `_context/01` assumption (1) (Doze reliability); ADR-042 (tuning); 2026-05-09T20:12:34Z handoff

#### Context

The architectural tiebreaker is Reminder reliability under Android background-execution restrictions (Doze, App Standby, battery optimization, reboot). WorkManager's exactness is insufficient for sub-minute precision (BR-004 requires on-time Reminders within an operationally tight window). AlarmManager's `setExactAndAllowWhileIdle` / `setAlarmClock` provide the exactness but are unsuitable for periodic / deferred work where WorkManager's durability is the right fit.

#### Decision

Dual-strategy: `AlarmManager.setExactAndAllowWhileIdle` (or `setAlarmClock` when the Patient experience benefits from the lock-screen alarm chrome) for exact-time Reminder firing; `WorkManager` for the daily horizon-extension job (BR-009) and the `pending → missed` sweep fallback (BR-010 silent-missed). Specific tuning (repeat interval, backoff, constraints) deferred to file 12 (later locked at ADR-042).

#### Alternatives Considered

- WorkManager only — rejected: insufficient exactness for BR-004.
- AlarmManager only — rejected: not the right fit for daily periodic work.

#### Consequences

- Positive: each tool plays to its strengths; reliability under Doze is the architectural tiebreaker honored.
- Negative: two background-work mechanisms to maintain; mitigated by the file-06 § F-flow narratives (ADR-027) that document each one's specific role.

### ADR-011: Cross-module communication is event-driven only via in-process `DomainEventPublisher` in `:core`; eight ports total; one repository per aggregate root with `room.withTransaction { }` atomicity

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Interface And Adapter Strategy, Module Or Component Map; 2026-05-09T20:12:34Z handoff

#### Context

The 6-module Gradle layout (ADR-009) needs a communication discipline that preserves module boundaries. Direct cross-feature imports would dissolve the Bounded Context separation. Repositories need to be aggregate-aligned (DDD-correct) to enforce file-02 invariants (BR-001 atomicity for Schedule replacement; BR-007 for archive).

#### Decision

Cross-module communication is event-driven only. Feature modules never import each other's Domain or Application packages; they emit and subscribe to domain events on `DomainEventPublisher` (in `:core`). MVP transport is in-process (`MutableSharedFlow`), no message bus, no Room-as-outbox. Eight ports (interfaces) total: `MedicationRepository`, `DoseRepository`, `ConfirmationRepository`, `SchedulerPort`, `NotificationPort`, `ClockPort`, `TimezonePort`, `DomainEventPublisher`. Use cases (interactors) are not ports themselves — they are the Application layer's public API consumed directly by Presentation. `MedicationRepository` is one repository per aggregate root (DDD-correct): `Medication` and its `DoseSchedule`s are fronted by a single repository (file-02 `Medication aggregate`); BR-001 / BR-007 atomicity runs inside `room.withTransaction { }`.

#### Alternatives Considered

- Separate `MedicationRepository` and `DoseScheduleRepository` ports — rejected during interview (DDD-incorrect for the Medication aggregate).
- Message bus / outbox table — rejected at MVP: in-process Flow is sufficient and avoids the operational overhead.

#### Consequences

- Positive: Bounded Context boundaries preserved at compile time; aggregate invariants atomically enforced.
- Negative: a future cross-process or cross-device split (e.g., adding a caregiver companion app) would require a real message bus / outbox; intentional MVP simplification.

### ADR-010: Scheduling-Reminders Bounded Context split into `:scheduling` (lifecycle / clock arithmetic) and `:notifications` (user-facing presentation)

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Module Or Component Map; 2026-05-09T20:12:34Z handoff

#### Context

The Scheduling-Reminders Bounded Context from `_context/01` includes both the clock-arithmetic side (deciding *that* a Reminder must fire — on-time vs. late vs. missed per BR-010) and the user-facing presentation side (deciding *what the Patient sees* — distinct text variants, silent-missed copy, batched-summary). Lumping these into a single Gradle module would conflate clock-arithmetic logic with copy-and-content authoring.

#### Decision

Split the Scheduling-Reminders Bounded Context across two Gradle modules: `:scheduling` for the lifecycle / clock-arithmetic side, `:notifications` for the user-facing presentation side. The boundary between them is Reminder *firing*: scheduling decides *that* a Reminder must fire; notifications decides *what the Patient sees*. This was a user amendment during the file-04 interview and is respected by file 06's integration map and file 11's content-and-tone guidance.

#### Alternatives Considered

- Single `:scheduling-reminders` module — rejected: would mix clock-arithmetic tests with snapshot tests for notification copy; module boundaries should reflect testing seams.

#### Consequences

- Positive: clean seam between clock arithmetic and content authoring; BR-010 silent-missed and batched-summary copy live in `:notifications`; clock decisions live in `:scheduling`.
- Negative: one Bounded Context split across two Gradle modules requires explicit handling in `_context/06` § F-flows (multi-step flows traverse both modules).

### ADR-009: Six Gradle modules with `:core` as pure-Kotlin shared kernel (feature-first + shell + shared kernel) — `:app` + `:core` + `:medication-management` + `:scheduling` + `:notifications` + `:adherence-tracking`

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Module Or Component Map, Repository Structure; 2026-05-09T20:12:34Z handoff

#### Context

The 3 Bounded Contexts from ADR-002 (Medication Management, Scheduling-Reminders, Adherence Tracking) need to map to Gradle modules that enforce the boundaries at compile time. A single `:app` module with package-only separation was the user's initial preference; the Repository Structure step revealed dependency-cycle issues that argue for a separate shared-kernel module.

#### Decision

Six Gradle modules: `:app` (shell + cross-cutting Presentation per ADR-016); `:core` (kotlin("jvm") Gradle module — pure Kotlin, zero Android dependency enforced by build; hosts `Result<T, E>`, ID newtypes, `ClockPort` / `TimezonePort` interfaces and their `SystemClockAdapter` / `SystemTimezoneAdapter` defaults, `DomainEvent` / `DomainEventPublisher` / `InProcessEventBus` triad); `:medication-management`; `:scheduling`; `:notifications`; `:adherence-tracking`. `:core` is a kotlin("jvm") module specifically so Domain-layer purity is guaranteed at build time. Initial user preference was to collapse `:core` into `:app`; reverted during the interview after the dependency-cycle issue surfaced.

#### Alternatives Considered

- Single `:app` Gradle module with package-only separation — rejected at user's explicit choice after dependency-cycle issue surfaced.
- Bounded-context-only modules (no `:core`, no `:app` shell) — rejected: `:core` is required for shared kernel; `:app` is required for shell + cross-cutting Presentation.

#### Consequences

- Positive: compile-time enforcement of Bounded Context boundaries; pure-Kotlin shared kernel guarantees Domain-layer purity at build time (a JVM library cannot import `android.*`).
- Negative: 6 modules is more Gradle ceremony than 1 module; trade-off accepted for the discipline.

### ADR-008: Clean Architecture with Compose + ViewModel + StateFlow; Reminder reliability under Android background-execution restrictions is the architectural tiebreaker

- Date: 2026-05-09
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/04` Architecture Style, Technology and Runtime Decisions; 2026-05-09T20:12:34Z handoff

#### Context

NemoPill needs an architectural style that (i) keeps the Domain layer pure and testable, (ii) makes the Presentation layer composable for the file-03 responsive scope, and (iii) handles OS callbacks (BroadcastReceiver, WorkManager.Worker, notification-action handlers) cleanly. The presentation pattern (MVVM vs. MVI) is deliberately left unspecified at the layer level; Compose + ViewModel + StateFlow is the concrete implementation pattern.

#### Decision

Clean Architecture with four layers: Presentation / Application / Domain / Infrastructure. Presentation owns OS callbacks (BroadcastReceiver, WorkManager.Worker, notification-action handlers) — they are entry points, same as a screen tap. Domain is pure-Kotlin only; clock and timezone are *ports*, never read directly in Domain. Implementation pattern: Compose + ViewModel + StateFlow. Architectural tiebreaker: Reminder reliability under Android background-execution restrictions (Doze, App Standby, battery optimization, reboot). When two design choices conflict, this is the tiebreaker.

#### Alternatives Considered

- Hexagonal Architecture — rejected: very similar to Clean for this scale; Clean's familiar layer vocabulary is the more common choice for Android.
- MVVM-specific architecture (not Clean Architecture) — rejected: would conflate presentation pattern with layer architecture; keeping them separate lets the presentation pattern evolve without touching layer boundaries.

#### Consequences

- Positive: Domain testability is structural; the architectural tiebreaker is named and applicable to future trade-offs.
- Negative: 4-layer overhead is more ceremony than a 2-layer (UI + data) shape; trade-off accepted for testability.

### ADR-007: UX scope and contract — two experience channels (Android app UI + Android system notifications), single novice Patient persona with five environment constraints, fully responsive scope across phone / tablet / foldables, seven Key Journeys (J-001..J-007), nine Use Cases (UC-001..UC-009), 21 BDD scenarios as the QA contract

- Date: 2026-05-08
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/03` Experience Channels, Personas, Key Journeys, Use Cases, BDD Scenarios, Edge Cases; 2026-05-08T21:36:25Z handoff

#### Context

The UX surface needs to be bounded in channels (otherwise scope drift adds Wear OS / widgets / voice etc.), persona-grounded (otherwise feature choices drift toward power-user defaults), responsive-bounded (otherwise the Compose adaptive-layout surface is undefined), and testably-contracted (otherwise QA has no anchor).

#### Decision

Two experience channels: (i) Android app UI; (ii) Android system notifications (lock-screen + notification-shade Reminders with inline action buttons). No Wear OS, widget, email, SMS, or voice. Single human persona: Patient, novice smartphone fluency. Five environment constraints to honor everywhere: offline-first, screen-reader (TalkBack) compatible, low-light / glanceable, one-handed friendly, battery / Doze-tolerant. Responsiveness scope: fully responsive across phone, tablet, and foldables (explicit user override of the initial proposal). Seven Key Journeys (J-001..J-007): J-001 primary onboarding, J-002 steady-state daily loop, J-003 Schedule replacement (BR-001/BR-006), J-004 Archive Medication (BR-007), J-005 retroactive Confirmation (BR-011), J-006 late-Reminder / device-off recovery (BR-010), J-007 timezone change (BR-012). Nine Use Cases UC-001..UC-009. 21 BDD scenarios — the testable backbone, each mapping to a BR, aggregate invariant, or Adherence calculation. 8 Acceptance Criteria AC-001..AC-008. 7 Edge Cases EC-001..EC-007 (including DST fall-back EC-005 and concurrent Confirmation taps EC-006 — the strongest argument for transactional Dose ↔ Confirmation write path).

#### Alternatives Considered

- Multiple personas (Patient + caregiver) — rejected: scope-out per ADR-004.
- Phone-only responsive scope — rejected at user override: tablet and foldables in scope.
- No formal BDD scenarios — rejected: the 21 scenarios are the QA contract.

#### Consequences

- Positive: UX surface bounded; QA has a 21-scenario contract; Edge Cases preserved in the test suite.
- Negative: foldable scope widens the Compose adaptive-layout surface; mitigated by file 11's M3 window-size class strategy (ADR-052).

### ADR-006: Late-Reminder Window of 1 hour (BR-010), Retroactive Confirmation Window of 24 hours (BR-011), Device-timezone-anchored Dose Schedules with regenerate-on-change (BR-012, `DeviceTimezoneChanged` domain event, `Dose.generatedInTimezone` attribute)

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/02` BR-010 / BR-011 / BR-012; Q1 / Q2 / Q3 resolution turn; 2026-05-06T22:05:22Z handoff

#### Context

File 01 and file 02 surfaced three open product questions that needed resolution before subsequent files could be authored: (Q1) Reminder-misfire / device-off behavior; (Q2) retroactive Dose logging policy; (Q3) `TimeOfDay` semantics across timezones and DST. Each affects multiple downstream surfaces (Confirmation source, Dose lifecycle, domain events, error handling, notification copy).

#### Decision

Q1 — Late-Reminder Window of G = 1 hour. Within window, fire late Reminder with distinct text at next OS opportunity. Beyond window, transition to `missed` with a single silent low-priority notification. Multiple late / missed Doses queued during a device-off period are batched into one summary at wake. Encoded as BR-010. Q2 — Retroactive Dose logging allowed within R = 24 hours of `Dose.scheduledAt`. Confirmation gets `source = retroactive`, `confirmedAt = time of tap` (no manual time entry in MVP). Retroactive `taken` Confirmations count toward Adherence identically to on-time Confirmations. Encoded as BR-011. Q3 — Local wall-clock follows device timezone. `Dose.scheduledAt` stored as UTC `Instant`; new `Dose.generatedInTimezone` (IANA TZ ID) added. On `ACTION_TIMEZONE_CHANGED`, future-pending Doses are deleted and regenerated in the new timezone (transactional). DST: spring-forward shifts to first valid time after the gap; fall-back fires once at the first occurrence. Encoded as BR-012 + `DeviceTimezoneChanged` domain event.

#### Alternatives Considered

- Q1: longer late-Reminder window (e.g., 4 hours) — rejected: risks collision with next typical Dose.
- Q2: manual time entry for retroactive Confirmations — rejected: novice-Patient persona + complexity of validating user-entered past times; defer to post-MVP.
- Q3: store `scheduledAt` as local wall-clock — rejected: ambiguous across DST and timezone changes.

#### Consequences

- Positive: three time-related ambiguities settled; downstream files (03 / 04 / 06) inherit a stable set of time-handling rules.
- Negative: travel-day drug-spacing bends slightly (Patient's "morning meds in my morning" is preserved at the cost of inter-Dose interval drift); accepted for MVP.

### ADR-005: Business rules BR-001 through BR-009 and 7 domain events as in-process contracts; Dose generation horizon 3 days capped at endDate (BR-009); archive deletes future-pending Doses (BR-007); no MVP re-activation of archived Medication; Adherence binding definition via BR-008

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/02` Business Rules section, Domain Events section, Dose Lifecycle; 2026-05-06T21:01:23Z handoff

#### Context

The domain needs explicit business rules anchoring the success metric (ADR-003), the Confirmation cardinality (ADR-004), and the Dose lifecycle. Without explicit BRs, every downstream file would have to re-infer them; without explicit domain events, cross-context communication has no contract.

#### Decision

Nine business rules BR-001 through BR-009 (BR-010 / BR-011 / BR-012 added at the Q1/Q2/Q3 resolution per ADR-006). BR-008 is the binding definition of Adherence tying to file-01's primary success metric. BR-009 sets the Dose generation horizon at 3 days, capped at `Dose Schedule.endDate`, with a daily horizon-extension job. BR-007 sets archive behavior: future-pending Doses are deleted (no `cancelled` status added); past Doses are preserved. Re-activation of an archived Medication is not allowed in MVP (Option Y) — to resume a discontinued Medication, the Patient creates a new Medication. 7 domain events declared: `MedicationCreated`, `DoseScheduleReplaced`, `MedicationArchived`, `DoseMaterialized`, `ReminderFired`, `DoseConfirmed`, `DoseMissed`. Logical cross-context contracts; transport is in-process for MVP (per ADR-011 — no message bus).

#### Alternatives Considered

- Add `cancelled` status to Dose for archive behavior — rejected: complicates state lifecycle without payoff; deletion is cleaner.
- Allow re-activation of archived Medication — rejected at user's choice (Option Y): novice Patient may forget the archive happened; new Medication is the cleaner mental model.
- Different Dose generation horizon (1 day or 7 days) — rejected: 3 days balances battery cost (Doze re-wakes) and crash recovery (boot-survival re-registration).

#### Consequences

- Positive: business rules are explicit, BR-anchored vocabulary across all downstream files.
- Negative: BR-007 archive deletion is asymmetric (future deleted, past preserved); deliberate trade-off recorded.

### ADR-004: Confirmation cardinality — strict 1:1 mutable with in-place correction (Option A); no audit trail in MVP

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/02` Confirmation entity, BR set, Dose lifecycle invariants; 2026-05-06T21:01:23Z handoff

#### Context

Each Dose has at most one Confirmation (taken or skipped). When the Patient corrects a Confirmation (UC-007), there are two options: (A) strict 1:1 mutable — the Confirmation row is overwritten in place; (B) immutable with history — every correction appends a new row, preserving the audit trail. Audit trail is a maintenance and review burden in MVP without an obvious payoff for the novice Patient.

#### Decision

Option A — strict 1:1 mutable Confirmation. Correction overwrites the Confirmation in place. No audit trail in MVP. Drives the Dose ↔ Confirmation invariant: at any time, a Dose has zero or one Confirmation.

#### Alternatives Considered

- Option B — immutable with history — rejected: audit trail adds storage + UI complexity without payoff for the novice persona.

#### Consequences

- Positive: simpler domain model; Adherence calculation is straightforward (latest Confirmation wins).
- Negative: no historical trail of corrections; if Patient corrects then corrects again, only the latest state is preserved. Accepted as MVP simplification; can revisit if a regulatory or feature requirement surfaces.

### ADR-003: Domain model — 4 entities (Medication, Dose Schedule, Dose, Confirmation), 6 value objects, 2 aggregates (Medication aggregate + Dose aggregate); Patient rejected as MVP entity; Frequency as tagged union (daily / every-n-hours / weekly-on-days / every-n-days)

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/02` Domain Model section, Aggregates, Value Objects; 2026-05-06T21:01:23Z handoff

#### Context

The domain model needs to be concrete enough that downstream Architecture and Use Cases can reference it precisely. The choice of aggregates (which entities form transactional boundaries) directly affects ADR-011's atomicity rules. Modeling Patient as an entity would invite multi-user feature creep contradicting ADR-002's scope exclusions.

#### Decision

4 entities: Medication, Dose Schedule, Dose, Confirmation. Patient considered and explicitly rejected as an MVP entity; multi-user remains out of scope. 6 value objects: Strength, Form, TimeOfDay, DateRange, Frequency (tagged union: `daily` / `every-n-hours` / `weekly-on-days` / `every-n-days`), DoseQuantity. 2 aggregates: Medication aggregate (root: Medication; includes Dose Schedule); Dose aggregate (root: Dose; includes Confirmation). `Dose Schedule` is the canonical UL term (matches `_context/01`); `Schedule` was considered and rejected to preserve the Ubiquitous Language.

#### Alternatives Considered

- Patient as a first-class entity — rejected: invites multi-user feature creep.
- Frequency as a polymorphic class hierarchy instead of tagged union — rejected: tagged union is more Kotlin-idiomatic and pattern-matchable.
- Single aggregate (Medication + Dose Schedule + Dose + Confirmation all under Medication root) — rejected: would force the Dose ↔ Confirmation transaction to span the whole aggregate, complicating concurrency.

#### Consequences

- Positive: clean DDD model; aggregates align with transactional boundaries; UL stays canonical.
- Negative: 2 aggregates means cross-aggregate consistency requires domain events (ADR-005) rather than transactional atomicity; intentional MVP discipline.

### ADR-002: NemoPill project framing — identity (NemoPill Medication Reminder), three Bounded Contexts (Medication Management / Scheduling-Reminders / Adherence Tracking), Ubiquitous Language fixed (Patient, Medication, Dose, Dose Schedule, Reminder, Confirmation, Adherence, Scheduler), primary success metric (Reminder-to-Confirmation rate ≥ 80% over rolling 30-day window), solo-developed Android client with hard scope exclusions (no multi-user/caregiver, no HIPAA-covered handling, no drug-interaction/allergy warnings)

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: `_context/01` Product Vision and Scope (Identity, Bounded Contexts, Ubiquitous Language, Success Metric, Delivery context, Out of Scope); 2026-05-06T12:45:19Z handoff

#### Context

The Software Design Framework requires file 01 to lock the project's identity, vocabulary, and hard scope boundaries before any architecture or design work begins. Without this, every downstream file has to re-litigate naming, scope, and success criteria — losing the Ubiquitous Language anchor that DDD uses to keep code, conversation, and documentation aligned.

#### Decision

- **Project identity:** NemoPill Medication Reminder.
- **Three Bounded Contexts:** Medication Management, Scheduling & Reminders, Adherence Tracking. (Scheduling-Reminders later split into `:scheduling` + `:notifications` Gradle modules per ADR-010.)
- **Ubiquitous Language fixed:** Patient, Medication, Dose, Dose Schedule, Reminder, Confirmation, Adherence (plus Scheduler as a system actor). Any term used in downstream files must match this list exactly; no synonyms.
- **Primary success metric:** Reminder-to-Confirmation rate ≥ 80% over a rolling 30-day window.
- **Secondary success metric:** framework completeness (design phase) + per-milestone code-quality review (coding phase).
- **Delivery context:** solo-developed Android application — no cloud, no multi-user, no caregiver features. (The operating-model clarification at ADR-035 later refined "solo-developed" to mean "Claude Code agents fill the roles; Human is gatekeeper".)
- **Hard scope exclusions:** multi-user / caregiver accounts; HIPAA-covered handling; drug-interaction or allergy warnings.
- **Key assumption to validate early:** Android `AlarmManager` / `WorkManager` reliability under Doze mode (file-01 assumption (1); retired at M-002 per ADR-036).

#### Alternatives Considered

- Different project name — rejected: NemoPill is the chosen identity.
- Different Bounded Context split (e.g., merging Adherence into Scheduling) — rejected: Adherence has its own success-metric anchor and warrants its own context.
- Multi-user / caregiver in scope — rejected as hard exclusion for MVP scope discipline.

#### Consequences

- Positive: every downstream file inherits a stable identity, vocabulary, Bounded Context map, success metric, and scope perimeter; UL violations are auditable.
- Negative: 80% adherence success metric requires deployed Patient data NemoPill cannot yet collect; MVP exit gates on feature-completeness (ADR-036) rather than metric proof.

### ADR-001: Software Design Framework adoption — CLAUDE.md as the sole instruction surface; multi-role workflow (Architect → Project Manager → Developer → QA → Security) with human Approved-for-apply gate separating planning from implementation; `_framework/` (read-only templates) + `_context/` (contextualized project state) + `_source/` (code) + `_build/` (artifacts) folder convention; per-file `/start-working` sessions enforcing One File Per Conversation

- Date: 2026-05-06
- Status: Accepted
- Owners: Isidro Rodriguez (Human gatekeeper); Architect
- Related milestone or task: project bootstrap; `CLAUDE.md` (instruction surface); `_framework/`, `_context/`, `_source/`, `_build/` directory conventions; 2026-05-06T12:45:19Z handoff "Process change applied this session"

#### Context

NemoPill is a solo-built project where the Human gatekeeper has 3–4 hrs/week of review-and-approval bandwidth (per ADR-035). Without an explicit instruction surface and multi-role workflow, work would drift between planning and implementation without a structural gate — exactly the failure mode the Software Design Framework is designed to prevent.

#### Decision

Adopt the Software Design Framework as the project's operating discipline. Components: (i) `CLAUDE.md` as the sole instruction surface for Claude agents working in this project — encodes the multi-role workflow (Architect → Project Manager → Developer → QA → Security), the human-approval gate (`Task status: Approved for apply`) that separates planning from implementation, and the per-file session protocol; (ii) folder convention: `_framework/` holds the unfilled placeholder files (read-only template content), `_context/` holds the contextualized project-specific copies (source of truth for planning and implementation), `_source/` holds the actual code (only Developer role writes here, only in Apply Mode), `_build/` holds build artifacts; (iii) per-file `/start-working` sessions — file 09 framework's "Contextualization Sessions: One File Per Conversation" rule enforces per-file scope, prior-state detection at session start, and clean stop after each file; (iv) the section-by-section drafting override (added at T-002 / T-001 timeframe) refines the per-file protocol to one `##` section at a time rather than one placeholder at a time.

#### Alternatives Considered

- Ad-hoc planning + coding without a framework — rejected: review bandwidth is too scarce to absorb the drift; the Approved-for-apply gate is the load-bearing scarcity-management mechanism.
- A different framework (e.g., RUP, SAFe) — rejected: SDF's per-file granularity and section-by-section discipline match the conversation-based Claude Code interaction model.

#### Consequences

- Positive: every downstream decision lives in a known surface (`_context/` files 01–13); planning artifacts and code artifacts are cleanly separated; Apply Mode is gated by an explicit Human flip.
- Negative: the framework itself is operational overhead; mitigated by the per-file session protocol that keeps each conversation tightly scoped.

## Copyable ADR Skeleton

Copy and edit this block when a new decision is made:

```md
### ADR-<ID>: <concise decision title>

- Date: <YYYY-MM-DD>
- Status: <Proposed | Accepted | Superseded | Rejected>
- Owners: <names, roles, or team>
- Related milestone or task: <optional reference>

#### Context

<What problem, constraint, or trade-off forced this decision?>

#### Decision

<What was decided?>

#### Alternatives Considered

- <Alternative 1 and why it was not chosen>
- <Alternative 2 and why it was not chosen>

#### Consequences

- Positive: <expected benefit or simplification>
- Negative / trade-off: <cost, limitation, or new risk introduced>
- Follow-up actions: <optional next steps>
```
