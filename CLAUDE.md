# Software Design Framework — Claude Instructions

This repository uses `CLAUDE.md` as the sole instruction surface for Claude agents working in this project. It encodes the multi-role workflow (Architect → Project Manager → Developer → QA → Security) and the human-approval gate that separates planning from implementation.

## Folder Convention

This project follows the Software Design Framework folder layout:

- `_framework/` holds the unfilled placeholder files (01-09 plus the optional 10-15). Treat this folder as read-only template content; never edit files here during a project task.
- `_context/` holds the contextualized, project-specific copies of those files. **All planning and implementation must use `_context/` as the source of truth.** If `_context/` is empty, the project has not been started — invoke the `/start-working` skill before doing anything else.
- `_source/` holds the actual code. Only the Developer role writes here, and only in Apply Mode.
- `_build/` holds build artifacts, generated output, CI/CD config, and tooling.

If a file referenced below does not yet exist in `_context/`, stop and tell the user that the framework has not been fully contextualized.

## Read Order

Before planning or coding, read the project context in this order from `_context/`:

1. `01_product_vision_and_scope.md`
2. `02_domain_model_and_business_rules.md`
3. `03_user_experience_and_use_cases.md`
4. `04_solution_architecture.md`
5. `05_engineering_quality_security_and_compliance.md`
6. `06_integrations_data_flow_and_boundaries.md`
7. `07_delivery_plan_and_milestones.md`
8. `08_active_task_packet.md`
9. `09_decision_log.md`

If optional files exist, read them after `09` and before starting work:

- `10_non_functional_requirements.md`
- `11_visual_language_and_design_system.md`
- `12_environments_and_devops.md`
- `13_threat_model_and_data_classification.md`
- `14_agent_roles_and_handoffs.md`
- `15_glossary.md`

If `_context/14_agent_roles_and_handoffs.md` exists, it is the authoritative role contract and overrides the Multi-Agent Roles section below.

## Operating Modes

**Proposal Mode is the default.** Analyze the context, design tests first, outline implementation second, and stop before mutating files in `_source/`.

**Apply Mode is allowed only when both are true:**

1. The human explicitly approves implementation in chat.
2. `_context/08_active_task_packet.md` sets `Task status` to `Approved for apply`.

`Approved for apply` is the only task status that authorizes Apply Mode. No other phrase substitutes.

## Core Rules

- Treat `_context/08_active_task_packet.md` as the only active task contract. One task at a time.
- Stay inside the current milestone defined in `_context/07_delivery_plan_and_milestones.md`.
- Do not implement anything marked out of scope in `_context/01_product_vision_and_scope.md` or `_context/08_active_task_packet.md`.
- Prefer pure functions, explicit data flow, and the Clean Architecture boundaries defined in `_context/04_solution_architecture.md`.
- Use the exact vocabulary defined in `_context/01_product_vision_and_scope.md` (Ubiquitous Language) and `_context/15_glossary.md` if present. Do not introduce synonyms.
- Update any affected `_context/` documents after approved implementation work changes the documented truth.
- Append an ADR to `_context/09_decision_log.md` when implementation introduces a durable architecture, policy, integration, security, or delivery decision.
- Append a structured handoff entry to `_context/handoff_log.md` at the end of every role turn.
- Never push branches, merge PRs, delete branches, publish releases, or perform destructive git operations without explicit human instruction.

## Stop Conditions

Stop and ask for clarification if any of the following is true:

- `_context/` is empty or any file in `01` through `09` is missing.
- Unresolved `{{REQUIRED:}}`, `{{OPTIONAL:}}`, or `{{EXAMPLE:}}` placeholders remain in any `_context/` file. Run `bash _framework/validate_framework.sh` to detect them.
- `_context/08_active_task_packet.md` is missing or does not describe exactly one active task.
- The requested work spans multiple milestones without explicit approval.
- The request conflicts with the documented architecture, security rules, compliance obligations, or scope boundaries.
- Multiple framework files disagree about a fact. Surface the conflict; do not silently pick one.

## Response Structure

In Proposal Mode work, structure the response with:

- `Current Milestone`
- `Active Task`
- `Scope And Boundaries`
- `Tests First`
- `Planned Changes`
- `Verification`
- `Risks / Blockers`
- `Waiting For Approval`

After approved Apply Mode work, structure the response with:

- `Implemented Changes`
- `Tests And Verification`
- `Document Updates`
- `Remaining Risks`
- `Stopped`

## Multi-Agent Roles

Each role reads from `_context/`, performs a bounded turn, and writes a structured handoff entry to `_context/handoff_log.md` before yielding. The full contract lives in `_context/14_agent_roles_and_handoffs.md`. Summary:

- **Architect**: runs `/start-working`, owns the `_framework/` → `_context/` translation, may propose ADRs, may not write to `_source/`.
- **Project Manager**: refreshes `_context/08_active_task_packet.md` and slices `_context/07` milestones into tasks. May not advance `Task status` to `Approved for apply`.
- **Developer**: implements approved tasks in `_source/`, runs tests, updates `_context/` if documented truth changed. Tests first, smallest defensible change second.
- **QA**: validates against `_context/03` BDD scenarios, `_context/05` test portfolio, and `_context/10` NFR verifications. Can block Apply Mode if coverage is insufficient.
- **Security**: validates against `_context/05` security guardrails and (when present) `_context/13` threat model and `_context/06` integration trust boundaries. Can block Apply Mode for guardrail violations.

## Compliance Activation

Per `_context/05_engineering_quality_security_and_compliance.md`:

- If every listed standard is `Not applicable`, the Compliance role is folded into Security review and not separately activated.
- If any listed standard is `Planned` or `Applicable`, Security must explicitly cite the relevant control or evidence requirement when reviewing tasks that touch in-scope data, controls, evidence, integrations, or operational procedures.

## Skills

Repo-scoped skills under `.claude/skills/` are accelerators, not required context. Recommended skills for this framework:

- `start-working`: interviews the user, contextualizes `_framework/` files into `_context/` one placeholder at a time.
- `framework-readiness-check`: runs and interprets `_framework/validate_framework.sh`.
- `proposal-review`: reviews a proposed implementation against the active task packet and `_context/`.

If a skill exists for the current task type, prefer it over ad-hoc workflow.

## Contextualization Sessions: One File Per Conversation

This rule overrides any contrary default in the `start-working` skill.

`/start-working` is intended to be invoked **once per framework file**. Do not attempt to contextualize multiple framework files in a single conversation — context length and attention quality degrade across long interviews, and the framework is explicitly designed for resumption across sessions via `_context/handoff_log.md`.

At the start of every `/start-working` session, before doing anything else:

1. List the contents of `_context/`.
2. For each file already present in `_context/`, treat it as **DONE** — do not re-interview the user about it.
3. Run `bash _framework/validate_framework.sh` from the repository root. The validator output identifies which files still contain unresolved `{{REQUIRED:}}`, `{{OPTIONAL:}}`, or `{{EXAMPLE:}}` markers.
4. The "next file to work on" is the lowest-numbered framework file that is either (a) not yet present in `_context/`, or (b) present but contains unresolved placeholders.
5. Read `_context/handoff_log.md` if it exists, to learn what prior sessions decided. **Do not re-litigate those decisions** — they are settled.
6. Read every file already present in `_context/` (in order 01 through whichever is the latest) so you carry the established Ubiquitous Language, Bounded Contexts, and prior decisions forward into the new file.
7. Announce to the user which file you intend to work on and a one- or two-line summary of what is already done. Confirm before beginning the interview.

When the file is complete:

- Write it to `_context/<file>`.
- Append a structured handoff entry to `_context/handoff_log.md`.
- Tell the user the session is complete and ask them to **start a new conversation with `/start-working`** for the next file.
- **Do not proceed to the next file in the same conversation**, even if the user requests it. Politely decline and explain that the per-file split is intentional for context-quality reasons. If the user explicitly insists in writing after that warning, you may continue, but warn that quality may degrade.

## Suggested Handoff Prompt

```text
Open the repository root and follow CLAUDE.md.
Read _context/01 through _context/09 in order, plus any optional 10-15 files that exist.
Operate in Proposal Mode unless I explicitly approve Apply Mode.
Use _context/08_active_task_packet.md as the only active task contract.
Design tests first, implementation second, and stop for review before applying code.
Append a handoff entry to _context/handoff_log.md before yielding.
```
