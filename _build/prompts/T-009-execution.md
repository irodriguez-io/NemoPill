# T-009 Execution Prompt (Claude Code, Developer role)

Use this prompt in **Claude Code opened at the repository root** (ADR-077), in a fresh
conversation, **only after** `_context/08_active_task_packet.md` shows
`Task status: Approved for apply` and **T-008 is merged to `main`**.

---

## Prompt

```text
Open the repository root and follow CLAUDE.md. You are the Developer role.

Read, in order, before writing anything:
- CLAUDE.md
- _context/01 through _context/09
- _context/10, _context/11, _context/13 (NFRs, design system, threat model)
- the most recent _context/handoff_log.md entries (T-008 execution + T-009 PM refresh + QA/Security review)

Operating mode:
- _context/08_active_task_packet.md (T-009, M-002) is the ONLY active task contract.
- Confirm at session start that Task status is `Approved for apply`. If it is anything
  else, STOP and tell me — do not write to _source/.
- Confirm T-008 is merged to `main` (T-009 builds on T-008's :core Result/DoseId/ClockPort,
  :scheduling ReminderAlarmReceiver/SchedulingConstants/AlarmManagerSchedulerAdapter, the
  KSP+Hilt graph, and the Kover ADR-086 DSL). If T-008 is NOT merged, STOP and surface it
  per the packet's Task Status note — do not proceed.

Scope discipline:
- Implement exactly what T-009 § In Scope specifies; touch nothing in § Explicitly Out Of Scope.
- This is M-002 Done-When item (3) only — render the on-time notification and EXPOSE the
  two inline actions. The action handler (ConfirmFromNotificationReceiver) is a
  logging-and-dismiss STUB; the typed parse → ConfirmDoseFromReminderUseCase → Room
  Confirmation write is T-010. No Room, no :adherence-tracking code, no FireReminderUseCase
  preconditions (those are M-004).
- Package namespace io.nemopill.* everywhere (ADR-075). No com.nemopill.
- Honor Clean-Architecture module boundaries: :core has zero Android imports (the
  InProcessEventBus is a coroutines SharedFlow, not an Android type); :scheduling must NOT
  import :notifications and vice-versa — the cross-context seam is the :core event bus;
  :notifications may not import :app or any other feature module. Domain stays pure and
  non-suspending; all I/O lives in the Application layer via ports.

Tests first (write these before finalizing production code):
- :core   InProcessEventBusTest                              (AC-002: publish→receive, late-subscriber replay, ReminderFired shape)
- :notifications  ReminderFiredListenerTest + FakeNotificationPort
                                                             (AC-003: single idempotent presentation on doseId, Result.Err.Unexpected on adapter failure)
- :notifications  NotificationManagerNotificationAdapterRobolectricTest
                                                             (AC-004: channel, posted notification, two "Take"/"Skip" actions, both FLAG_IMMUTABLE PendingIntents + action/extras/target)

Acceptance gates (run locally before pushing the CI probe PR):
- ./gradlew :core:test :scheduling:test :notifications:test :app:test          (AC-001)
- ./gradlew :app:assembleDebug                                                 (AC-005)
- ./gradlew koverVerify koverHtmlReport                                        (AC-006)
- ./gradlew lint ktlintCheck  and  :core:test --tests "io.nemopill.core.konsist.*"
  (AC-007 local equivalents: arch-conformance incl. PendingIntentFlagImmutableRule exercised
   by the three new PendingIntents, NoDynamicThrowMessageRule still green, no-INTERNET /
   no-network-import and exported="false" assertions still green)
- bash _framework/validate_framework.sh exits zero

Security guardrails you must keep green (Security rendered Guardrail Pass on these — do not regress):
- FLAG_IMMUTABLE on every constructed PendingIntent (both "Take"/"Skip" actions AND the
  content-tap intent).
- ConfirmFromNotificationReceiver declared exported="false" in AndroidManifest.xml.
- No new <uses-permission> entries (POST_NOTIFICATIONS already present); no INTERNET; no
  java.net.* / okhttp / retrofit imports anywhere.
- No Patient data in any notification content, Result.Err, or log line — use the hardcoded
  non-PII demo placeholders; static throw/log messages only.
- reminder_on_time channel display name + description and the notification copy come from
  strings.xml resources, not inline literals (sentence case per _context/11; "Take" / "Skip"
  labels per _context/11 § Notification (inline action)).

At close (do not skip):
- Append the new ADRs to _context/09_decision_log.md (newest-first at top of ## Current State,
  4-field skeleton, Status: Proposed): InProcessEventBus design (MutableSharedFlow + replay-buffer
  size + @Singleton binding + DomainEvent sealed hierarchy seeded with ReminderFired); demo
  ReminderAlarmReceiver→bus wiring + the demo-vs-full-F-005 gap; NotificationManagerCompat adapter +
  reminder_on_time channel importance posture (confirm against _context/11 § BR-010 channel-importance;
  on-time is time-critical → IMPORTANCE_HIGH); the two-button FLAG_IMMUTABLE factory; the
  ConfirmFromNotificationReceiver stub-now/dispatch-in-T-010 decision; the Hilt-in-:notifications
  placement + broadcast-receiver injection pattern (confirm @AndroidEntryPoint vs EntryPointAccessors
  empirically, mirror T-008's ADR-083 finding); the :notifications Kover thresholds.
- Reaffirm ADR-049: rule (i) green (static throw/log messages only in new code); rule (ii) still
  deferred to the first sensitive Domain data class (name the target task again) and reaffirm the
  ADR-087 open question now also implicates ReminderFired.
- Append the T-009-close handoff entry to _context/handoff_log.md per AC-009: AC-001…AC-008 evidence
  refs; the demo-vs-full-F-005 gap and the deferred Result.Err.NotificationsPermissionRevoked UX;
  which M-002 Done-When items this slice satisfied (3) and which remain open (2-hardware, 4, 5, 6);
  next-role routing to a PM refresh for the T-010 slice (item 4); and the CLAUDE.md
  one-file-per-conversation closing line.
- Do NOT edit Task status (leave it for the PM refresh to advance to T-010). Do NOT push branches,
  open/merge PRs, or perform destructive git ops without my explicit instruction.

Surface conflicts in the handoff — do not silently resolve them. Substantive workarounds land as
ADRs pending my ratification. If a discovered constraint forces descoping (e.g., the cold-start
replay race can't be made reliable under the in-process bus alone), descope that single item via a
mid-session handoff entry and route it to a PM refresh — do not silently drop an acceptance criterion.
```

---

## Pre-flight checklist (before you paste the prompt)

- [ ] `_context/08_active_task_packet.md` → `Task status: Approved for apply`
- [ ] T-008 merged to `main` (branch `Task-8-execution` pushed, CI probe PR green, merged)
- [ ] Fresh Claude Code conversation, opened at the repo root
- [ ] You are available to ratify ADRs / answer conflict surfaces during the run

## Watch-items carried from the QA/Security review

- T-009's hard dependency is **T-008 on `main`** — the Developer is instructed to halt if it isn't.
- Receiver layer-placement doc tension (file 05 says parse "at the entry point in `:presentation`";
  files 06/13 place the receiver in `:notifications::infrastructure`) is immaterial in T-009 (stub
  only) — it matters for the **T-010** packet when the real typed parse lands.
- The stub's `Log.i`/`Log.w` is demo-only scaffolding; it must use static, non-PII messages and is
  replaced by the T-010 dispatch — note this in the close handoff.
