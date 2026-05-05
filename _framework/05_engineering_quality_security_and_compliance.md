# Engineering Quality, Security, And Compliance

## How To Fill This File

- Define the minimum engineering bar the coding agent must satisfy on every task.
- Keep the compliance matrix accurate. If a standard is not relevant, say so explicitly.
- The Compliance role defined in `14_agent_roles_and_handoffs.md` is inactive unless this file marks at least one standard as `Applicable` or `Planned`.

## TDD Workflow

1. `{{REQUIRED: how the team wants tests proposed before code is written}}`
2. `{{REQUIRED: how tests are reviewed before implementation begins}}`
3. `{{REQUIRED: what verification must pass before the task can be considered complete}}`

## Test Portfolio

| Test Type | Purpose | Required For This Project? | Tooling | Notes |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: unit tests}}` | `{{REQUIRED: what unit tests must protect}}` | `{{REQUIRED: yes or no}}` | `{{OPTIONAL: framework or approach}}` | `{{OPTIONAL: coverage expectations}}` |
| `{{OPTIONAL: integration tests}}` | `{{OPTIONAL: purpose}}` | `{{OPTIONAL: yes or no}}` | `{{OPTIONAL: tooling}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: end-to-end tests}}` | `{{OPTIONAL: purpose}}` | `{{OPTIONAL: yes or no}}` | `{{OPTIONAL: tooling}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: contract or schema tests}}` | `{{OPTIONAL: purpose}}` | `{{OPTIONAL: yes or no}}` | `{{OPTIONAL: tooling}}` | `{{OPTIONAL: notes}}` |

## Quality Gates

| Gate | When It Runs | Pass Condition | Owner |
| --- | --- | --- | --- |
| `{{REQUIRED: type check, lint, test suite, build, review checklist, etc.}}` | `{{REQUIRED: local, pull request, pre-merge, deploy pipeline, nightly, etc.}}` | `{{REQUIRED: what must be true}}` | `{{OPTIONAL: engineer, reviewer, CI, release manager, etc.}}` |
| `{{OPTIONAL: additional gate}}` | `{{OPTIONAL: when}}` | `{{OPTIONAL: pass condition}}` | `{{OPTIONAL: owner}}` |

## Observability Expectations

- Required logs or events: `{{OPTIONAL: what operations, state changes, or failures must be observable}}`
- Metrics or service indicators: `{{OPTIONAL: latency, throughput, error rate, queue depth, task completion, business KPI, etc.}}`
- Tracing or correlation rules: `{{OPTIONAL: request IDs, job IDs, correlation IDs, or "Not applicable"}}`
- Alerting expectations: `{{OPTIONAL: what conditions should trigger operational attention}}`

## Security Guardrails

| Area | Rule | Enforcement Point |
| --- | --- | --- |
| `{{REQUIRED: authentication and authorization}}` | `{{REQUIRED: how access is verified and restricted}}` | `{{REQUIRED: where this is enforced}}` |
| `{{REQUIRED: input validation}}` | `{{REQUIRED: how inputs are validated or sanitized}}` | `{{REQUIRED: boundary where validation happens}}` |
| `{{REQUIRED: secret management}}` | `{{REQUIRED: where secrets live and how they are injected}}` | `{{OPTIONAL: deployment/runtime mechanism}}` |
| `{{OPTIONAL: data protection}}` | `{{OPTIONAL: encryption, masking, retention, audit, or deletion rules}}` | `{{OPTIONAL: enforcement point}}` |
| `{{OPTIONAL: dependency and supply chain}}` | `{{OPTIONAL: vulnerability scanning, pinning, provenance, review rules}}` | `{{OPTIONAL: enforcement point}}` |

## Compliance Applicability Matrix

Mark each row as `Not applicable`, `Planned`, or `Applicable`. Add or remove rows to fit the project.

| Standard Or Framework | Status | Scope Notes | Required Controls Or Evidence | Owner |
| --- | --- | --- | --- | --- |
| `HIPAA` | `Not applicable` | `{{OPTIONAL: what regulated data or workflow makes this relevant}}` | `{{OPTIONAL: audit trail, access controls, retention, BAAs, etc.}}` | `{{OPTIONAL: team or person}}` |
| `PCI DSS` | `Not applicable` | `{{OPTIONAL: cardholder data scope or payment delegation notes}}` | `{{OPTIONAL: segmentation, logging, scanning, vendor handling, etc.}}` | `{{OPTIONAL: owner}}` |
| `NIST 800-53 / NIST CSF` | `Not applicable` | `{{OPTIONAL: contractual or organizational reason}}` | `{{OPTIONAL: applicable control families or evidence}}` | `{{OPTIONAL: owner}}` |
| `{{OPTIONAL: GDPR, SOC 2, ISO 27001, FedRAMP, or another required standard}}` | `{{OPTIONAL: status}}` | `{{OPTIONAL: scope}}` | `{{OPTIONAL: required controls or evidence}}` | `{{OPTIONAL: owner}}` |

## Compliance Activation Rule

- If every listed standard is `Not applicable`, the Compliance Expert remains inactive for normal delivery work.
- If any listed standard is `Planned` or `Applicable`, the Compliance Expert must review tasks that touch in-scope data, controls, evidence, integrations, or operational procedures.

## Evidence And Audit Trail

- Required artifacts to preserve: `{{OPTIONAL: ADRs, test reports, approval records, deployment logs, control evidence, screenshots, tickets, etc.}}`
- Retention expectation: `{{OPTIONAL: how long evidence must be retained or where it is stored}}`
