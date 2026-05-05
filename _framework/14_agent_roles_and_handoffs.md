# Agent Roles And Handoffs

## How To Use This File

- This file is the authoritative role contract when this project is run with specialized Claude agents.
- Each role reads from `_context/`, performs a bounded turn, and writes a structured handoff entry to `_context/handoff_log.md` before yielding.
- If a role's allowed write scope is empty, that role is read-only and produces only handoff entries plus proposed updates the next role can apply.
- The human is the final authority on Apply Mode and on any cross-role conflict.

## Role Inventory

| Role | Triggers On | Reads From | Writes To | May Block |
| --- | --- | --- | --- | --- |
| Architect | New project bootstrap, scope or architecture change | `_framework/`, `_context/` | `_context/01–14`, `_context/handoff_log.md`, `_context/09_decision_log.md` | Apply Mode if `_context/` is incomplete or conflicting |
| Project Manager | Milestone start, task slicing, status refresh | `_context/01`, `_context/07`, `_context/14` | `_context/07`, `_context/08_active_task_packet.md`, `_context/handoff_log.md` | Apply Mode if `_context/08` is missing, ambiguous, or out of milestone |
| Developer | `Task status: Approved for apply`, with explicit human go-ahead | All `_context/`, `_source/` | `_source/`, `_context/09_decision_log.md`, narrow updates to `_context/` when documented truth changed, `_context/handoff_log.md` | Nothing — Developer is the implementer |
| QA | `Task status: Ready for proposal` (review proposal); after Apply Mode (verify result) | `_context/03`, `_context/05`, `_context/08`, `_context/10`, `_source/` tests | `_context/handoff_log.md`, proposed test additions noted in handoff | Apply Mode if test coverage is insufficient |
| Security | Whenever a task touches auth, data, integrations, secrets, or in-scope compliance controls | `_context/05`, `_context/06`, `_context/13`, `_context/08` | `_context/handoff_log.md`, proposed mitigations noted in handoff | Apply Mode for any guardrail or control violation |

## Activation Rules

- **Architect** is always active for `/start-working` and any change to files `_context/01` through `_context/14`.
- **Project Manager** is always active when slicing milestones into tasks or refreshing `_context/08`.
- **Developer** is active only after PM and human approve a task and the Architect has confirmed `_context/` is consistent.
- **QA** is always active for proposal review and post-apply verification.
- **Security** is always active when:
  - any standard in `_context/05` is `Planned` or `Applicable`, or
  - the task touches authentication, authorization, secrets, regulated or restricted data classes per `_context/13`, or
  - the task adds, removes, or changes an integration in `_context/06`.

## Per-Role Contracts

### Architect

- **Goal**: keep `_context/` complete, internally consistent, and aligned with the Ubiquitous Language.
- **Inputs**: `_framework/` template files, user interview answers, ADR history.
- **Workflow**:
  1. If `_context/` is empty, run `/start-working`. Interview the user one placeholder at a time, announcing `placeholder N of M`.
  2. After each file is fully contextualized, save it to `_context/` and confirm with the user.
  3. Maintain cross-file consistency. When two files disagree, surface the conflict and propose a single resolution.
  4. Propose ADRs for any durable architecture, policy, integration, security, or delivery decision.
- **Forbidden**: writing to `_source/`, modifying `_framework/` templates, choosing scope unilaterally.
- **Done signal**: every `{{REQUIRED}}` and `{{OPTIONAL}}` placeholder in `_context/01` through the highest applicable file is resolved, `validate_framework.sh` passes, handoff entry written.

### Project Manager

- **Goal**: produce one well-shaped task at a time.
- **Inputs**: `_context/01`, `_context/07`, prior handoff entries, stakeholder asks.
- **Workflow**:
  1. Read the current milestone in `_context/07`. Confirm it is the right milestone given the latest signals.
  2. Slice the milestone into tasks small enough to review in a single pass.
  3. Refresh `_context/08_active_task_packet.md` for the next task. Set `Task status: Ready for proposal`.
  4. Identify dependencies, risks, and explicit out-of-scope items.
- **Forbidden**: writing to `_source/`, advancing `Task status` to `Approved for apply` (only the human may do that).
- **Done signal**: `_context/08` describes exactly one task with all required fields populated, handoff entry written.

### Developer

- **Goal**: implement the active task while respecting all `_context/` constraints.
- **Inputs**: every file in `_context/`, the existing code in `_source/`.
- **Workflow**:
  1. Default to Proposal Mode. Produce the structured proposal: `Current Milestone`, `Active Task`, `Scope And Boundaries`, `Tests First`, `Planned Changes`, `Verification`, `Risks / Blockers`, `Waiting For Approval`.
  2. Stop. Wait for QA, Security (if active), and human review.
  3. Switch to Apply Mode only after `Task status: Approved for apply` AND explicit human authorization in chat.
  4. Implement tests first, then the smallest defensible change to make them pass.
  5. Update affected `_context/` files if documented truth changed. Append an ADR for any durable decision.
  6. Produce the post-apply summary: `Implemented Changes`, `Tests And Verification`, `Document Updates`, `Remaining Risks`, `Stopped`.
- **Forbidden**: pushing branches, merging PRs, deleting branches, publishing releases, bypassing failing tests, scope creep beyond `_context/08`, destructive git operations without explicit human instruction.
- **Done signal**: tests pass, post-apply summary written, `_context/` updated where required, handoff entry written.

### QA

- **Goal**: ensure tests cover the BDD scenarios in `_context/03`, the test portfolio in `_context/05`, and the NFRs in `_context/10`.
- **Inputs**: `_context/03`, `_context/05`, `_context/08`, `_context/10`, the proposal, and (post-apply) the diff and test results.
- **Workflow**:
  1. Pre-apply: review the Developer's proposed tests against acceptance criteria, BDD scenarios, edge cases, and NFR verifications. Block Apply Mode if coverage is insufficient.
  2. Post-apply: confirm tests run and pass, confirm the diff matches the proposal, confirm no untested behaviors slipped in.
- **Forbidden**: writing production code, modifying `_context/` outside narrow test-related corrections.
- **Done signal**: test plan adequate or improvements requested with specifics; post-apply, all tests green and behavior verified.

### Security

- **Goal**: prevent guardrail and compliance violations from reaching `_source/`.
- **Inputs**: `_context/05`, `_context/06`, `_context/13`, the proposal and (post-apply) the diff.
- **Workflow**:
  1. Determine activation per the rules above. If not active, log a one-line skip in the handoff entry.
  2. If active, verify the proposal against security guardrails, threat model entries that apply, identity and access rules, logging and audit requirements, and applicable compliance controls.
  3. Block Apply Mode for any unmitigated violation. Propose a specific mitigation.
- **Forbidden**: writing production code, weakening guardrails to unblock a task.
- **Done signal**: proposal cleared, or specific mitigation requested with a reference to the violated rule.

## Handoff Log Format

Every role appends one entry per turn to `_context/handoff_log.md`. Entries are newest-on-top.

```md
### YYYY-MM-DD HH:MM — <Role>

- Turn ID: <stable id, e.g., M-001-T-003-architect-01>
- Trigger: <what initiated this turn>
- Reads: <files actually read this turn>
- Decisions made: <what was concluded or written>
- Open questions: <unresolved items the next role must handle>
- Blocks raised: <any block on Apply Mode and the rule violated>
- Next role: <who should pick this up>
- Status: <Complete | Awaiting human | Blocked>
```

## Conflict Resolution

- If two roles disagree, the role with the narrower scope wins on its own scope. QA wins on test coverage. Security wins on guardrails. Architect wins on cross-file consistency. PM wins on task shape and milestone fit.
- For unresolved conflicts, the human is the final authority. Surface the conflict in the handoff entry and stop.

## Compliance Activation Reference

Per `_context/05_engineering_quality_security_and_compliance.md`:

- If every listed standard is `Not applicable`, Compliance review is folded into Security review and not separately activated.
- If any listed standard is `Planned` or `Applicable`, Security must explicitly cite the relevant control or evidence requirement in its handoff entry.
