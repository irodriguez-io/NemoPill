# T-008 — Claude Code Execution Prompt

**Milestone:** M-002 (Walking Skeleton — End-to-End Vertical Slice)
**Task:** T-008 (scheduling leg — Schedule the Demo Reminder)
**Mode:** Apply Mode — requires `_context/08_active_task_packet.md` `Task status: Approved for apply`
**Reviews cleared:** QA `Coverage Sufficient` · Security `Guardrail Pass`

> Flip `Task status` to `Approved for apply` in `_context/08_active_task_packet.md`
> before running this prompt. The prompt assumes the flip is already done and aborts
> if it is not.

---

## Prompt (paste into Claude Code at the repository root)

```text
Open the repository root and follow CLAUDE.md.

Read _context/01 through _context/09 in order, plus optional files 10–13. Treat
_context/08_active_task_packet.md (Task T-008, Milestone M-002) as the ONLY active
task contract.

Confirm before doing anything else: _context/08 Task status reads "Approved for apply".
If it reads anything else, STOP and tell me — do not write to _source/.

Operate in Apply Mode for T-008 (the status authorizes it). Execute exactly the
packet's In-Scope; do not touch anything in its Explicitly-Out-Of-Scope list.
Respect io.nemopill.* (ADR-075) and the Clean-Architecture boundaries: no Android
imports in :core; :scheduling must not import :app or any other feature module; I/O
only in the Application layer via ports; Domain stays pure and non-suspending.

Tests-first discipline. Write these before finalizing production code:
  - ScheduleDemoReminderUseCaseTest (fake SchedulerPort + FakeClock; asserts
    now + 10 min offset, single idempotent registration per BR-004, Result.Ok shape)
  - AlarmManagerSchedulerAdapterRobolectricTest (ShadowAlarmManager; asserts trigger
    time, FLAG_IMMUTABLE operation PendingIntent, receiver target)
  - ResultTest (so :core meets its ≥90% threshold)
  - FakeClock fixture

Security guardrails — these are mandatory, all test/Konsist-backed:
  - FLAG_IMMUTABLE on EVERY PendingIntent (operation AND showIntent),
    | FLAG_UPDATE_CURRENT for the idempotent replace. The PendingIntentFlagImmutableRule
    Konsist rule must pass.
  - ReminderAlarmReceiver is exported="false" in the manifest. Do not touch
    BootCompletedReceiver (the only sanctioned exported="true" exception).
  - Zero new <uses-permission> entries. The merged manifest must add no permissions.
  - No INTERNET / no network imports anywhere. No Room, no persistence, no Patient data.
  - No Patient data in any Result.Err message or log line (ADR-031). The
    ReminderAlarmReceiver.onReceive no-op stub logs no dynamic content — static
    logging only.

Coverage / CI notes:
  - Activate the deferred Kover per-package rules now that real code lands (resolves
    ADR-078): :core ≥ 90% on io.nemopill.core.*, :scheduling::application ≥ 80%. No
    threshold on infrastructure/presentation. The new code must MEET these bars.
  - The unit-integration-ui-snapshot CI stage must stay GREEN WITHOUT a new Roborazzi
    snapshot baseline — snapshot baselines are deferred to M-003. Keep the optional
    Compose render test light (or defer it if it threatens slice size); do NOT
    introduce a snapshot baseline in T-008.
  - All eight CI stages must pass, including arch-conformance (Konsist) with the
    FLAG_IMMUTABLE rule exercised by the new adapter, and coverage.

ADR-049 routing (ADR-081): if a production `throw` lands (e.g. the use-case boundary
catch → Result.Err.Unexpected, or a Domain precondition), wire rule (i) now
(no string-template / String.format / StringBuilder.append in a throw message
argument). Rule (ii) (Domain data class toString() redaction) does NOT trigger here —
DoseId is a value class, not a data class — so name its real target task (first
sensitive Domain data class, expected M-002 Dose-materialization or M-003 Medication
CRUD) in the close handoff. Record the verdict either way.

Run locally before pushing the CI probe PR:
  ./gradlew :core:test :scheduling:test :app:test koverVerify koverHtmlReport
  ./gradlew :app:assembleDebug

At close:
  - Append the T-008 ADRs to _context/09_decision_log.md (newest-first at top of
    ## Current State, 4-field skeleton): KSP+Hilt Gradle plugin wiring and minimal
    placement; coroutines + Truth catalog additions; Kover 0.8.3 per-package DSL;
    demo scheduling-adapter design; ReminderAlarmReceiver relocation to
    :scheduling::infrastructure; ADR-049 verdict if durable.
  - Append the T-008-close handoff entry to _context/handoff_log.md per AC-009
    (AC-001…AC-008 evidence, ADR-049 verdict + named target task, which M-002 Done
    When items this slice satisfied vs. open for T-009+, next-role routing to PM for
    the T-009 slice, and the CLAUDE.md one-file-per-conversation closing line).
  - Do NOT edit Task status, and do NOT touch _context files 01–07 or 10–13,
    _framework/, CLAUDE.md, or existing Konsist rules (except adding the ADR-049
    rule(s) only if a throw lands).

Surface any conflict or discovered constraint in the handoff as an ADR at
Status: Proposed — do not silently resolve it. If a constraint forces descoping an
acceptance criterion, descope via a mid-session handoff entry and route it through a
PM refresh; never silently drop an AC.
```

---

## Acceptance criteria recap (from the packet)

- AC-001 — `./gradlew :core:test :scheduling:test :app:test` exits zero.
- AC-002 — use-case unit test passes (offset math, idempotent single registration, `Result.Ok`).
- AC-003 — Robolectric adapter test passes (trigger time, `FLAG_IMMUTABLE`, receiver target).
- AC-004 — Hilt graph compiles; `:app:assembleDebug` succeeds.
- AC-005 — KSP + Hilt Gradle plugins wired; codegen runs.
- AC-006 — Kover thresholds active and met (resolves ADR-078).
- AC-007 — all eight CI stages green; no new permissions in merged manifest.
- AC-008 — ADR-049 routed; ADRs appended.
- AC-009 — T-008-close handoff entry appended.
