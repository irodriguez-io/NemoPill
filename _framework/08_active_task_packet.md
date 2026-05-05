# Active Task Packet

## How To Use This File

- This file must contain exactly one active task.
- Update it before every handoff to a coding agent.
- The coding agent must treat this file as the only active task contract for planning and implementation.

## Task Status

| Field | Value |
| --- | --- |
| Milestone ID | `{{REQUIRED: milestone id from 07_delivery_plan_and_milestones.md}}` |
| Task ID | `{{REQUIRED: stable task id}}` |
| Task title | `{{REQUIRED: short verb-based task title}}` |
| Requested by | `{{OPTIONAL: product owner, tech lead, stakeholder, ticket id, or issue link}}` |
| Task status | `{{REQUIRED: Draft, Ready for proposal, Approved for apply, In progress, Blocked, Implemented, or Rejected}}` |
| Date last updated | `{{REQUIRED: YYYY-MM-DD}}` |

- `Approved for apply` is the only task status that authorizes Apply Mode after explicit human approval.

## Task Objective

- Problem to solve now: `{{REQUIRED: what needs to change in this task}}`
- Why now: `{{REQUIRED: why this task matters in the current milestone}}`
- Expected user-visible or system-visible outcome: `{{REQUIRED: what should be observably different after completion}}`

## In Scope

- `{{REQUIRED: the change this task must deliver}}`
- `{{REQUIRED: another explicit part of scope if needed}}`
- `{{OPTIONAL: supporting change that is still part of this task}}`

## Explicitly Out Of Scope

- `{{REQUIRED: related feature or refactor that must not be started in this task}}`
- `{{REQUIRED: another tempting but disallowed expansion}}`
- `{{OPTIONAL: follow-up work reserved for another milestone or task}}`

## Acceptance Criteria

- `{{REQUIRED: acceptance criterion 1}}`
- `{{REQUIRED: acceptance criterion 2}}`
- `{{OPTIONAL: additional acceptance criterion}}`

## Tests To Add First

- `{{REQUIRED: unit, integration, contract, or UI test the agent must design before implementation}}`
- `{{REQUIRED: another test or scenario that proves the task is done}}`
- `{{OPTIONAL: failure-path or regression test}}`

## Likely Change Surface

- Files or modules likely affected: `{{REQUIRED: list the likely files, packages, modules, or bounded contexts}}`
- Files or modules that must remain untouched: `{{REQUIRED: list protected areas to prevent scope creep}}`
- Contracts or schemas that may change: `{{OPTIONAL: APIs, events, DB schema, file formats, UI contracts, or "Not applicable"}}`

## Dependencies, Risks, And Blockers

- Dependencies: `{{OPTIONAL: other tasks, approvals, environments, data, or integrations this task depends on}}`
- Risks: `{{OPTIONAL: what might break, what is uncertain, or what must be watched carefully}}`
- Blockers: `{{OPTIONAL: anything preventing progress right now}}`

## Rollback Or Recovery Notes

- `{{OPTIONAL: how to back out the change or safely disable it if implementation fails}}`
- `{{OPTIONAL: manual recovery steps if partial implementation lands}}`

## Handoff Rules For The Agent

- Proposal Mode is the default.
- The agent must not mutate files unless the human explicitly approves implementation and the `Task status` field says `Approved for apply`.
- The agent must design tests first, implementation second, and stop after presenting the proposal.
- If the task packet conflicts with another document, the agent must surface the conflict and ask for clarification.
