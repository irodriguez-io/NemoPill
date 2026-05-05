# Integrations, Data Flow, And Boundaries

## How To Fill This File

- Document every external dependency and every important data handoff.
- Focus on where data crosses boundaries, who validates it, and how failures are handled.
- Keep this file synchronized with the architecture and compliance rules when contracts change.

## Integrations Register

| System Or Dependency | Purpose | Trigger | Data Exchanged | Authentication / Trust | Criticality |
| --- | --- | --- | --- | --- | --- |
| `{{OPTIONAL: external system, internal shared service, or vendor}}` | `{{OPTIONAL: why it exists}}` | `{{OPTIONAL: what causes the interaction}}` | `{{OPTIONAL: important payload or record types}}` | `{{OPTIONAL: auth method, network trust, or manual process}}` | `{{OPTIONAL: low, medium, high, mission critical}}` |
| `{{OPTIONAL: another integration}}` | `{{OPTIONAL: purpose}}` | `{{OPTIONAL: trigger}}` | `{{OPTIONAL: data exchanged}}` | `{{OPTIONAL: authentication / trust}}` | `{{OPTIONAL: criticality}}` |

## Interface And Contract Inventory

| Contract | Producer | Consumer | Shape Or Schema | Versioning Rule | Backward Compatibility Notes |
| --- | --- | --- | --- | --- | --- |
| `{{OPTIONAL: API endpoint, event, file format, import/export, message, webhook, UI contract}}` | `{{OPTIONAL: producer}}` | `{{OPTIONAL: consumer}}` | `{{OPTIONAL: where the contract is defined}}` | `{{OPTIONAL: semantic version, date version, additive-only, etc.}}` | `{{OPTIONAL: notes}}` |

## Data Flow Narratives

Describe each important workflow that crosses a boundary.

| Flow ID | Trigger | Input Source | Processing Summary | Output Or Side Effect | Failure Handling |
| --- | --- | --- | --- | --- | --- |
| `F-{{OPTIONAL: 001}}` | `{{OPTIONAL: event or action}}` | `{{OPTIONAL: user, system, schedule, external message}}` | `{{OPTIONAL: what happens in order}}` | `{{OPTIONAL: stored data, response, event, notification, file, audit record}}` | `{{OPTIONAL: retries, compensating action, manual review, user error, dead letter queue, etc.}}` |

## Trust Boundaries And Validation Ownership

| Boundary | Data Entering The System | Validation Owner | Sensitive Data Class | Notes |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: API boundary, UI form boundary, import pipeline, webhook boundary, etc.}}` | `{{REQUIRED: what data enters here}}` | `{{REQUIRED: which layer or component validates it}}` | `{{OPTIONAL: public, internal, confidential, regulated, restricted}}` | `{{OPTIONAL: sanitization, schema validation, anti-replay, signature verification, etc.}}` |
| `{{OPTIONAL: another boundary}}` | `{{OPTIONAL: data entering}}` | `{{OPTIONAL: validation owner}}` | `{{OPTIONAL: sensitive class}}` | `{{OPTIONAL: notes}}` |

## Reliability Rules

- Timeout rule: `{{OPTIONAL: maximum wait time by integration or flow}}`
- Retry rule: `{{OPTIONAL: which calls may retry and with what limits}}`
- Idempotency rule: `{{OPTIONAL: how duplicate requests or messages are handled}}`
- Ordering rule: `{{OPTIONAL: whether events or updates must be processed in order}}`
- Partial failure rule: `{{OPTIONAL: what happens when downstream systems fail after upstream success}}`

## Error Handling And Recovery

- User-facing failure behavior: `{{OPTIONAL: what the user or caller sees when something fails}}`
- Operator-facing recovery steps: `{{OPTIONAL: dashboards, queues, replay, manual intervention, escalation}}`
- Audit or evidence requirements: `{{OPTIONAL: what must be recorded when failures or retries occur}}`

## Data Ownership And Retention

- Source of truth by major record type: `{{OPTIONAL: which system owns which data}}`
- Retention or deletion rules: `{{OPTIONAL: business, legal, or compliance retention expectations}}`
- Export or portability needs: `{{OPTIONAL: if users or partner systems must extract data}}`
