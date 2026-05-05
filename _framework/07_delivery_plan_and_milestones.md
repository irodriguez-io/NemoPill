# Delivery Plan And Milestones

## How To Fill This File

- Break work into milestone slices that can be reviewed, demonstrated, and approved in order.
- Keep milestones outcome-oriented rather than team-activity-oriented.
- Use stable IDs so `08_active_task_packet.md` can point to the current milestone and task unambiguously.

## Delivery Summary

- Current delivery phase: `{{REQUIRED: discovery, MVP, beta, hardening, migration, rollout, etc.}}`
- Release cadence: `{{OPTIONAL: weekly, per milestone, continuous delivery, manual release train, etc.}}`
- Demo or review cadence: `{{OPTIONAL: when the team reviews proposal output and completed changes}}`
- Known calendar constraints: `{{OPTIONAL: launch dates, freeze windows, audit deadlines, staffing constraints}}`

## Milestone Register

| Milestone ID | Name | Goal | Prerequisites | Done When | Status |
| --- | --- | --- | --- | --- | --- |
| `M-{{REQUIRED: 001}}` | `{{REQUIRED: milestone name}}` | `{{REQUIRED: the outcome this milestone must deliver}}` | `{{OPTIONAL: dependencies or prior milestones}}` | `{{REQUIRED: objective completion criteria}}` | `{{REQUIRED: Planned, Active, Blocked, or Complete}}` |
| `M-{{OPTIONAL: 002}}` | `{{OPTIONAL: milestone name}}` | `{{OPTIONAL: goal}}` | `{{OPTIONAL: prerequisites}}` | `{{OPTIONAL: done when}}` | `{{OPTIONAL: status}}` |

## Dependency And Blocker Register

| Item | Type | Impact | Owner | Mitigation |
| --- | --- | --- | --- | --- |
| `{{OPTIONAL: dependency or blocker}}` | `{{OPTIONAL: internal dependency, vendor dependency, approval, environment, staffing, legal, compliance, etc.}}` | `{{OPTIONAL: what it blocks or threatens}}` | `{{OPTIONAL: owner}}` | `{{OPTIONAL: mitigation or next action}}` |

## Milestone-Level Definition Of Done

- The milestone goal is demonstrably met: `{{REQUIRED: how the team proves it}}`
- The relevant tests and quality gates pass: `{{REQUIRED: which evidence is required}}`
- User-facing or operational behavior is reviewed: `{{OPTIONAL: demo, sign-off, QA pass, accessibility check, rollout review, etc.}}`
- Documentation updates are complete: `{{REQUIRED: which documents must be updated before closing the milestone}}`

## Task Slicing Rules

- One task packet must describe exactly one task that can be reviewed in a single pass.
- A task must belong to one milestone only.
- A task should produce one coherent user-visible, domain-visible, or operational outcome.
- If a task needs more than one approval cycle, split it into smaller tasks before giving it to the agent.

## Review And Approval Rules

- Proposal review owner: `{{REQUIRED: who reviews the agent's proposal before code is applied}}`
- Apply approval owner: `{{REQUIRED: who may authorize implementation}}`
- Completion sign-off owner: `{{OPTIONAL: who confirms the task or milestone is done}}`
