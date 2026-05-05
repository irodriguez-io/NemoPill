# Software Design Framework

This framework helps software teams — including non-engineers — prepare the right context for Claude agents to execute work task by task and milestone by milestone. It is designed for projects that want Clean Architecture boundaries, a functional-programming bias, Domain-Driven Design for product language and user experience, test-first implementation, and an explicit human approval gate before code is applied.

The framework runs in a multi-role workflow: **Architect → Project Manager → Developer → QA → Security**. Each role is a specialized Claude agent that reads from a shared `_context/` folder, performs a bounded turn, and writes a structured handoff entry before yielding to the next role. The human is the final approver between Proposal Mode and Apply Mode.

## Folder Convention

When you start a new project, the framework expects this layout at the repository root:

```
project-root/
  _framework/   # the unfilled template files (this pack)
  _context/     # contextualized, project-specific copies — written during /start-working
  _source/      # actual code, written by the Developer agent in Apply Mode
  _build/       # build artifacts, CI/CD config, generated output
  CLAUDE.md     # the sole agent instruction surface (root level)
  README.md     # this file
```

`_framework/` is read-only template content. The Architect agent reads from it during the `/start-working` interview and writes contextualized output to `_context/`. The Developer agent only ever reads from `_context/` and only ever writes code to `_source/`. This separation keeps the templates pristine and makes the framework reusable across projects.

## What Is In This Pack

Required files:

- `CLAUDE.md`: the sole instruction surface for Claude agents. Lives at the repository root.
- `_framework/01_product_vision_and_scope.md`: business problem, users, goals, non-goals, success measures, ubiquitous language, bounded contexts.
- `_framework/02_domain_model_and_business_rules.md`: domain objects, business rules, invariants, policies, events.
- `_framework/03_user_experience_and_use_cases.md`: personas, journeys, use cases, BDD scenarios, UX constraints, user-visible acceptance criteria.
- `_framework/04_solution_architecture.md`: Clean Architecture boundaries, module responsibilities, interfaces and adapters, runtime choices, repository structure.
- `_framework/05_engineering_quality_security_and_compliance.md`: TDD workflow, test portfolio, quality gates, observability, security rules, optional compliance requirements.
- `_framework/06_integrations_data_flow_and_boundaries.md`: integrations, contracts, trust boundaries, data flows, validation ownership, failure handling.
- `_framework/07_delivery_plan_and_milestones.md`: roadmap slices, milestone sequencing, blockers, review cadence, milestone-level definition of done.
- `_framework/08_active_task_packet.md`: the one active task the agent is allowed to plan or implement.
- `_framework/09_decision_log.md`: ADR log for durable engineering decisions.

Optional but recommended files:

- `_framework/10_non_functional_requirements.md`: performance, availability, scalability, accessibility, observability targets.
- `_framework/11_visual_language_and_design_system.md`: tokens, typography, color, spacing, component library, motion, brand. Use `Not applicable` for headless services.
- `_framework/12_environments_and_devops.md`: environments, secrets, CI, deploy, rollback, infrastructure as code.
- `_framework/13_threat_model_and_data_classification.md`: STRIDE table, abuse cases, data classes, retention. Skip for purely public, non-sensitive data.
- `_framework/14_agent_roles_and_handoffs.md`: explicit role contracts and handoff log format.
- `_framework/15_glossary.md`: standalone glossary once the Ubiquitous Language table outgrows file 01.

Tooling:

- `_framework/validate_framework.sh`: executable handoff gate. Verifies `_context/` is fully populated and CLAUDE.md is consistent.
- `_framework/agent-harnessing.md`: background reading on the harnessing philosophy behind this framework.

## Placeholder Contract

Every template file uses three placeholder kinds:

- `{{REQUIRED: ...}}` must be replaced before the document is used for a real task.
- `{{OPTIONAL: ...}}` should be replaced with project-specific content or the phrase `Not applicable`.
- `{{EXAMPLE: ...}}` shows the expected level of detail and should be removed after the team writes the real content.

The `validate_framework.sh` script enforces that no placeholders remain in `_context/` before agent handoff.

## Workflow For A New Project

This is the end-to-end flow for someone starting a brand-new project from zero.

### 1. Bootstrap The Repo

Drop this entire pack into a `_framework/` subfolder of an empty repository. Place `CLAUDE.md` and `README.md` at the repository root. Create empty `_context/`, `_source/`, and `_build/` folders.

### 2. Run `/start-working`

Open the project in Claude Code and invoke the `/start-working` command. The Software Architect agent will:

1. Read `_framework/01_product_vision_and_scope.md`.
2. Count placeholders and announce progress as `placeholder N of M`.
3. Ask one question per placeholder, in order.
4. Use your answers to fill the placeholders and write the contextualized file to `_context/01_product_vision_and_scope.md`.
5. Confirm the result with you.
6. Move to file `02`, repeat. Continue through all framework files in order, including any optional 10-15 files that apply to your project.

**Architect interview prompt (for reference):**

```text
You are a senior software architect. You will guide me to write context files
for a software project I want to build. The framework files live in _framework/.
You will read them in order, ask me questions to contextualize each placeholder,
and write the contextualized output to _context/.

For each placeholder, announce "we are working on placeholder N of M", ask the
question for that placeholder, and use my answer to fill it. Once a file is
fully contextualized, save it to _context/ and move to the next file.

If I am uncertain about an answer, suggest a reasonable default tied to the
project context I have already given you, and mark it for review.
```

### 3. Plan Milestones And Tasks

After `_context/` is fully populated, the **Project Manager** agent reads `_context/07_delivery_plan_and_milestones.md` and slices the milestone register into a backlog of tasks. For the current milestone's first task, the PM populates `_context/08_active_task_packet.md` with: task ID, scope, out-of-scope, acceptance criteria, tests to add first, likely change surface, and `Task status: Ready for proposal`.

### 4. Run Proposal Mode

The **Developer** agent reads `CLAUDE.md`, enters Proposal Mode by default, and produces a structured proposal: tests first, planned changes, risks, files affected. **It does not write code yet.** The proposal is reviewed by **QA**, by **Security** (when active), and finally by the human.

### 5. Approve And Apply

When the proposal is acceptable, the human updates `_context/08_active_task_packet.md` to set `Task status: Approved for apply` and tells the Developer agent to switch to Apply Mode. The Developer writes code to `_source/`, runs tests, and reports back with a structured summary.

### 6. Update Context And Repeat

If the implemented work changed any documented truth (architecture, contracts, scope, decisions), the relevant `_context/` files are updated and an ADR is appended to `_context/09_decision_log.md`. Each role appends a handoff entry to `_context/handoff_log.md`. The PM then refreshes `_context/08_active_task_packet.md` for the next task.

## Role Activation Rules

- **Architect** is always active for `/start-working` and any change to files `_context/01` through `_context/14`.
- **Project Manager** is always active when slicing milestones into tasks or refreshing `_context/08`.
- **Developer** is active only after PM and the human approve a task and the Architect has confirmed `_context/` is consistent.
- **QA** is always active for proposal review and post-apply verification.
- **Security** is active when any standard in `_context/05` is `Planned` or `Applicable`, when the task touches authentication, authorization, secrets, or regulated data classes per `_context/13`, or when the task adds, removes, or changes an integration in `_context/06`.

The full role contract lives in `_framework/14_agent_roles_and_handoffs.md`.

## Readiness Checklist

Before the first agent handoff, verify:

- Every `{{REQUIRED:}}` placeholder in `_context/01` through `_context/09` (plus any optional 10-15 files) has been replaced with real content.
- Every `{{OPTIONAL:}}` placeholder has been replaced with real content or `Not applicable`.
- No `{{EXAMPLE:}}` placeholder remains.
- `_context/07` identifies milestone IDs, sequencing, and current blockers.
- `_context/08` contains exactly one active task and explicitly names what is out of scope.
- `_context/08` includes tests-to-add-first and a `Task status` value.
- `_context/05` marks every listed compliance standard as `Not applicable`, `Planned`, or `Applicable`.
- `CLAUDE.md` exists at the repository root.

Run the validator:

```sh
bash _framework/validate_framework.sh
```

The script fails if any required file is missing, if any placeholder remains, or if `CLAUDE.md` is inconsistent. Troubleshooting fallback:

```sh
rg -n '{{(REQUIRED|OPTIONAL|EXAMPLE):' _context/0[1-9]*.md _context/1[0-5]*.md
```

## How To Customize

- Replace every placeholder with concrete, project-specific language.
- Keep wording stable across files. Terms in `01`, `02`, `03`, and `15` should match exactly.
- Prefer precise, testable statements over aspirational text. If the agent cannot verify it, rewrite it.
- Keep each milestone and task small enough to review in one pass.
- If a section does not apply, write `Not applicable` and explain why in one sentence.
- Remove example text once you have supplied the real project content.

## Optional Repo Skills

Skills are reusable, scoped operating procedures. Place them under `.claude/skills/`:

- `start-working`: drives the Architect interview that turns `_framework/` into `_context/`.
- `framework-readiness-check`: runs and interprets `validate_framework.sh`.
- `proposal-review`: reviews a proposed task implementation against `_context/08` and the framework docs.

Skills are accelerators, not required context. Add them when a workflow repeats often enough to justify codifying it.

## Where To Go Next

If you are starting a new project: jump to "Workflow For A New Project" above and run `/start-working`.

If you are integrating this framework into an existing project: fill the context files retroactively from your current code, starting with `01` (vision) and `04` (architecture), then capture historical decisions as ADRs in `09`.
