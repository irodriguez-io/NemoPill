# Agent Harnessing

An agent harness is the project-local operating system for AI-assisted development.
It gives coding agents enough context to explore, plan, implement, verify, and
review work while keeping humans in control of publication, destructive actions,
scope changes, and product direction.

The goal is not to make an agent autonomous in every sense. The goal is to make
agent work reproducible, reviewable, and bounded so the same project can be
handled consistently across tasks, sessions, and contributors.

## Core Outcomes

A good harness should produce these outcomes:

- agents know what the project is, what it is not, and which boundaries matter
- durable decisions live in documentation instead of transient chat history
- recurring workflows are captured as skills with clear trigger conditions
- sub-agents are used for bounded, parallel work instead of vague delegation
- verification paths are explicit before code changes are treated as done
- humans retain control over pushes, merges, destructive git operations, broad
  architecture changes, dependency changes, and external publication

## Documentation Stack

Use layered documentation. Each layer should answer a different class of question.

### `CLAUDE.md`

`CLAUDE.md` is the harness contract. It should be short enough to read before
work starts and strong enough to prevent predictable mistakes.

Include:

- project purpose and explicit non-goals
- current implemented surface area and planned direction
- architecture boundaries and ownership rules
- non-negotiable invariants
- docs and skills to read first
- standard commands for checks, tests, builds, and local development
- human approval checkpoints
- task-specific done criteria
- working style expectations for exploration, implementation, review, and handoff

Template:

```md
# <Project> Agent Harness

<Project> uses AI coding agents as a local, human-gated development harness.

Agents may plan, explore, implement, verify, and review local changes. Agents
must not push branches, merge pull requests, delete branches, publish releases,
or perform destructive actions without explicit human instruction.

## Product Scope

- Current modules: <module-list>
- Planned modules: <planned-module-list>
- Explicit non-goals: <non-goal-list>

## Architecture Boundaries

- <layer-1>: <responsibility>
- <layer-2>: <responsibility>
- <layer-3>: <responsibility>

## Non-Negotiables

- Preserve <critical-invariant>.
- Keep <business-rule> in <owning-layer>.
- Do not move <future-concern> into <current-module>.

## Standard Commands

- `<command>`
- `<command>`

## Human Checkpoints

Require human approval before:

- adding or changing dependencies
- broad refactors
- environment or configuration contract changes
- destructive git actions
- publication actions
```

### `docs/*.md`

Project docs hold durable knowledge that should not be repeated in every prompt.
Split them by decision type so an agent can load only the relevant context.

Useful baseline docs:

- `docs/overview.md`: purpose, audience, modules, workflows, and product rules
- `docs/architecture.md`: layers, request flow, ownership, interfaces, and data flow
- `docs/configuration.md`: environment variables, external systems, mappings, and
  operational contracts
- `docs/frontend-and-ux.md`: route patterns, interaction model, visual system, and
  client-side state rules
- `docs/testing-and-operations.md`: standard commands, test coverage, reporting,
  exports, release notes, troubleshooting, and extension guidance

The docs should explain why the system is shaped the way it is, not just where
files are. Agents need decision context more than inventories.

### Task-Specific Done Criteria

Vague tasks become safer when the harness defines what "done" means for each
kind of change.

Examples:

- Mapper or external-field changes: validate fetch shape, mapping logic,
  configuration, and tests together.
- Domain or metric changes: keep rules in the owning layer, update view models
  only where needed, and run focused unit or integration tests.
- UI or route changes: preserve expected state flow, verify route behavior, and
  check visual output when layout changed.
- Report or export changes: keep preview and export semantics aligned, and check
  print or file-generation assumptions.
- New module seeding: decide the owning product area first, create a minimal
  route/model boundary, and avoid bolting future concepts onto the wrong current
  module.

## Skills

Skills are reusable, scoped operating procedures. They are best for workflows
that repeat often enough to deserve their own rules but are too specific for the
global harness contract.

Create one skill per recurring mode of work, such as:

- product model and naming
- UI and route design
- design system changes
- debugging loop
- reporting and export behavior
- code review
- framework-specific implementation guidance

Each skill should include:

- when to use it
- what to read first
- important project-specific risks
- expected workflow
- validation expectations
- what not to do
- completion standard

Template:

```md
---
name: <project>-<skill-name>
description: Use when <trigger-condition>. Focus on <primary-outcome>.
---

# <Skill Name>

Use this skill when <task-type>.

## Read First

- `docs/<relevant-doc>.md`
- `<relevant-source-path>`

## Workflow

1. Establish the current behavior.
2. Identify the owning layer or module.
3. Make the smallest change that satisfies the task.
4. Run the smallest validation that proves the behavior.
5. Summarize changed behavior and remaining risk.

## Risks To Check

- <risk-1>
- <risk-2>
- <risk-3>

## Completion Standard

- <done-signal-1>
- <done-signal-2>
```

Skills should be narrow. A good skill makes one kind of work easier to do
correctly; it should not become a second project manual.

## Sub-Agents

Sub-agents are bounded helpers for parallel or specialized work. They work best
when their responsibility is concrete, their output is small, and their write
scope is clear.

Useful roles:

- Explorer: read-only mapping of affected files, execution paths, schemas, and
  nearby tests.
- Test planner: identifies the smallest validation plan and likely coverage gaps.
- Worker: implements a scoped change in an assigned file or module area.
- Reviewer: checks correctness, regressions, missing tests, and boundary
  violations after implementation.
- Product or architecture guard: checks module ownership, naming, route semantics,
  view-model shape, and future-facing boundaries.

Use sub-agents when they can reduce blind spots or run in parallel without
blocking the immediate next step. Do not delegate vague ownership like "fix the
app." Delegate bounded work like "map the route and use-case files touched by
`<feature>`" or "review `<changed-file>` for regressions against
`docs/architecture.md`."

When assigning implementation work, include:

- exact responsibility
- allowed write scope
- relevant docs or skills
- expected validation
- instruction not to revert unrelated user changes

Example assignment:

```text
Implement <behavior> in <module/path>. Own only <file-or-directory-scope>.
Follow `CLAUDE.md` and the <relevant-skill> skill. Do not revert edits outside
your scope. Run <targeted-command> or explain why it could not run. Return the
changed paths and validation result.
```

## Human-Gated Workflow

The harness should make human checkpoints explicit. Agents can do a lot of local
work, but humans should own decisions that publish, destroy, or materially change
the project contract.

Require human approval before:

- dependency additions or major dependency upgrades
- broad refactors or architecture rewrites
- environment variable, secret, deployment, or configuration contract changes
- destructive filesystem or git actions
- branch pushes, pull request creation, merges, releases, or branch deletion
- external service changes with lasting side effects

For normal local development, the expected flow is:

1. Explore the existing implementation and relevant docs.
2. State the intended scope when the work is non-trivial.
3. Implement the smallest defensible change.
4. Run targeted validation first.
5. Run broader validation when the change spans layers or user-facing behavior.
6. Review the diff for correctness and accidental scope creep.
7. Hand off the local diff, validation results, and residual risks to the human.

## Reproducible Setup Checklist

Use this checklist when adding an agent harness to another project.

1. Create `CLAUDE.md`.
   - Define project purpose, non-goals, architecture boundaries, invariants,
     standard commands, human checkpoints, and task-specific done criteria.
2. Split durable knowledge into focused docs.
   - Start with overview, architecture, configuration, frontend/UX if relevant,
     testing, and operations.
3. Add local skills for repeated workflows.
   - Prefer narrow skills with clear triggers and completion standards.
4. Define sub-agent roles.
   - Name what each role owns, when to use it, and what output it should return.
5. Establish validation commands.
   - Document targeted checks and broader checks. Make it clear when each is
     appropriate.
6. Define publication boundaries.
   - Keep pushes, merges, releases, branch deletion, and destructive operations
     human-owned unless the human explicitly delegates a specific action.
7. Keep the harness current.
   - Update docs and skills when architecture, workflows, commands, or product
     boundaries change.

## Quality Bar

A harness is working when a new capable agent can enter the repo, read the
harness docs, choose the right skill, use sub-agents deliberately, make a scoped
change, validate it, and hand off a reviewable local diff without needing hidden
project knowledge from chat history.

The best harnesses are not large. They are explicit about the few decisions that
matter most.
