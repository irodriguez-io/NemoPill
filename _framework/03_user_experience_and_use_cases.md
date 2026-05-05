# User Experience And Use Cases

## How To Fill This File

- If the product has no visual UI, reinterpret UX as operator, API consumer, or workflow experience.
- Keep the scenarios directly traceable to the goals in `01_product_vision_and_scope.md`.
- Write behaviors the QA role can test without guessing intent.

## Experience Overview

- Experience channels: `{{REQUIRED: web app, mobile app, internal tool, CLI, API, email workflow, partner portal, etc.}}`
- Primary user journey this phase must support: `{{REQUIRED: the most important end-to-end flow}}`
- UX success signal: `{{REQUIRED: what success looks like from the user perspective}}`

## Personas Or Actors

| Actor | Context | Goal | Skill Level | Accessibility Or Environment Notes |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: actor or persona}}` | `{{REQUIRED: where and why they use the product}}` | `{{REQUIRED: what they are trying to accomplish}}` | `{{OPTIONAL: novice, expert, mixed, external, internal, etc.}}` | `{{OPTIONAL: screen reader, offline risk, low bandwidth, shared device, etc.}}` |
| `{{OPTIONAL: secondary actor}}` | `{{OPTIONAL: context}}` | `{{OPTIONAL: goal}}` | `{{OPTIONAL: skill level}}` | `{{OPTIONAL: notes}}` |

## Key Journeys

| Journey ID | Actor | Trigger | Happy Path Summary | Failure Or Alternate Paths |
| --- | --- | --- | --- | --- |
| `J-{{REQUIRED: 001}}` | `{{REQUIRED: actor}}` | `{{REQUIRED: what starts the journey}}` | `{{REQUIRED: the main steps in plain language}}` | `{{OPTIONAL: errors, retries, abandonment, or manual follow-up}}` |
| `J-{{OPTIONAL: 002}}` | `{{OPTIONAL: actor}}` | `{{OPTIONAL: trigger}}` | `{{OPTIONAL: happy path}}` | `{{OPTIONAL: alternate paths}}` |

## Use Cases

| Use Case ID | Name | Primary Actor | Preconditions | Outcome |
| --- | --- | --- | --- | --- |
| `UC-{{REQUIRED: 001}}` | `{{REQUIRED: short verb-based name}}` | `{{REQUIRED: actor}}` | `{{REQUIRED: what must already be true}}` | `{{REQUIRED: successful end state}}` |
| `UC-{{OPTIONAL: 002}}` | `{{OPTIONAL: name}}` | `{{OPTIONAL: actor}}` | `{{OPTIONAL: preconditions}}` | `{{OPTIONAL: outcome}}` |

## BDD Scenarios

Use one scenario per observable behavior. Duplicate the pattern as needed.

### `{{REQUIRED: scenario title}}`
- Given `{{REQUIRED: the starting state}}`
- When `{{REQUIRED: the action or event}}`
- Then `{{REQUIRED: the expected result}}`
- And `{{OPTIONAL: a second observable outcome}}`

## UX Constraints And Rules

- Navigation or workflow rules: `{{OPTIONAL: steps that must remain explicit or guarded}}`
- Content or copy rules: `{{OPTIONAL: wording constraints, confirmation language, localization notes}}`
- Responsiveness or device constraints: `{{OPTIONAL: device classes, screen sizes, offline behavior}}`
- Accessibility expectations: `{{REQUIRED: keyboard support, contrast, semantic markup, screen reader needs, error messaging, or "Not applicable"}}`
- Design system or visual language rules: `{{OPTIONAL: tokens, component library, brand rules, or "Not applicable"}}`

## User-Visible Acceptance Criteria

- `{{REQUIRED: user-visible acceptance criterion 1}}`
- `{{REQUIRED: user-visible acceptance criterion 2}}`
- `{{OPTIONAL: additional acceptance criterion}}`

## Edge Cases To Preserve In Tests

- `{{OPTIONAL: empty state, duplicate submission, expired token, partial failure, permission edge case, etc.}}`
- `{{OPTIONAL: another edge case}}`
