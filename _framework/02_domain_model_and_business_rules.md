# Domain Model And Business Rules

## How To Fill This File

- Use the exact domain language defined in `01_product_vision_and_scope.md`.
- Capture business truth here before you discuss persistence or framework details.
- If aggregates are not relevant, write `Not applicable` instead of leaving the section blank.

## Entities

| Entity | Identity | Description | Key Attributes | Lifecycle Notes |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: entity name}}` | `{{REQUIRED: what uniquely identifies it}}` | `{{REQUIRED: why it exists in the domain}}` | `{{REQUIRED: the attributes that matter to business logic}}` | `{{OPTIONAL: created, updated, archived, approved, expired, etc.}}` |
| `{{OPTIONAL: another entity}}` | `{{OPTIONAL: identity}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: key attributes}}` | `{{OPTIONAL: lifecycle notes}}` |

## Value Objects

| Value Object | Meaning | Fields | Validation Rules |
| --- | --- | --- | --- |
| `{{OPTIONAL: value object name}}` | `{{OPTIONAL: why it exists}}` | `{{OPTIONAL: component values}}` | `{{OPTIONAL: construction or formatting rules}}` |

## Aggregates And Consistency Boundaries

| Aggregate | Root | What Must Stay Consistent Together | Notes |
| --- | --- | --- | --- |
| `{{OPTIONAL: aggregate name or Not applicable}}` | `{{OPTIONAL: aggregate root}}` | `{{OPTIONAL: invariants that must hold in one transaction or decision boundary}}` | `{{OPTIONAL: why this boundary exists}}` |

## Business Rules

| Rule ID | Rule | Reason | Enforced In | Failure Behavior |
| --- | --- | --- | --- | --- |
| `BR-{{REQUIRED: 001}}` | `{{REQUIRED: the rule stated in business language}}` | `{{REQUIRED: why the rule exists}}` | `{{REQUIRED: domain, application, UI, integration, or shared boundary}}` | `{{REQUIRED: what happens when the rule is violated}}` |
| `BR-{{OPTIONAL: 002}}` | `{{OPTIONAL: another rule}}` | `{{OPTIONAL: reason}}` | `{{OPTIONAL: enforcement point}}` | `{{OPTIONAL: failure behavior}}` |

## Invariants

- `{{REQUIRED: statement that must always remain true for valid domain state}}`
- `{{OPTIONAL: another invariant}}`

## Policies, Decisions, And Calculations

| Policy Or Calculation | Inputs | Output | Deterministic? | Notes |
| --- | --- | --- | --- | --- |
| `{{OPTIONAL: policy or calculation name}}` | `{{OPTIONAL: domain inputs}}` | `{{OPTIONAL: resulting decision or value}}` | `{{OPTIONAL: yes or no}}` | `{{OPTIONAL: rules or edge cases worth naming}}` |

## State Transitions

| Object | From State | Trigger | To State | Guard Conditions |
| --- | --- | --- | --- | --- |
| `{{OPTIONAL: object name}}` | `{{OPTIONAL: starting state}}` | `{{OPTIONAL: event, decision, or user action}}` | `{{OPTIONAL: resulting state}}` | `{{OPTIONAL: what must be true before transition}}` |

## Domain Events

| Event | Trigger | Payload Summary | Consumers | Idempotency Notes |
| --- | --- | --- | --- | --- |
| `{{OPTIONAL: event name}}` | `{{OPTIONAL: what causes the event}}` | `{{OPTIONAL: important fields only}}` | `{{OPTIONAL: systems or contexts that react}}` | `{{OPTIONAL: replay and duplication handling}}` |

## Glossary Alignment Checks

- Terms that must exactly match `01_product_vision_and_scope.md`: `{{REQUIRED: list the critical terms or write "See ubiquitous language table"}}`
- Terms that must appear in user stories in `03_user_experience_and_use_cases.md`: `{{OPTIONAL: terms that must be preserved in UX and acceptance criteria}}`
