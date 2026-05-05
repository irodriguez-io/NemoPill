# Non-Functional Requirements

## How To Fill This File

- Capture the qualities the system must exhibit independent of any specific feature.
- Prefer numeric, observable targets over adjectives. If the agent cannot verify it, rewrite it.
- Keep targets aligned with the goals in `01_product_vision_and_scope.md` and the architecture in `04_solution_architecture.md`.
- If a category does not apply, write `Not applicable` and explain why in one sentence.

## Performance Targets

| Concern | Target | Measurement Point | Notes |
| --- | --- | --- | --- |
| `{{REQUIRED: latency for primary user action}}` | `{{REQUIRED: e.g., p95 < 300 ms}}` | `{{REQUIRED: where this is measured}}` | `{{OPTIONAL: load assumption, percentile, region}}` |
| `{{OPTIONAL: throughput target}}` | `{{OPTIONAL: e.g., 500 req/s sustained}}` | `{{OPTIONAL: ingress, queue, worker, etc.}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: cold-start or first-paint target}}` | `{{OPTIONAL: target}}` | `{{OPTIONAL: measurement point}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: batch or background job duration}}` | `{{OPTIONAL: target}}` | `{{OPTIONAL: measurement point}}` | `{{OPTIONAL: notes}}` |

## Availability And Reliability

- Service level objective: `{{REQUIRED: e.g., 99.9% monthly availability for the primary user flow}}`
- Acceptable error budget: `{{OPTIONAL: e.g., 43 minutes downtime per month}}`
- Recovery time objective (RTO): `{{REQUIRED: maximum tolerable downtime}}`
- Recovery point objective (RPO): `{{REQUIRED: maximum tolerable data loss}}`
- Degraded-mode behavior: `{{OPTIONAL: what continues to work when a dependency fails}}`

## Scalability And Capacity

| Dimension | Current Expectation | One-Year Target | Notes |
| --- | --- | --- | --- |
| `{{REQUIRED: concurrent users}}` | `{{REQUIRED: number}}` | `{{OPTIONAL: number}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: data volume}}` | `{{OPTIONAL: size}}` | `{{OPTIONAL: size}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: requests per second}}` | `{{OPTIONAL: rate}}` | `{{OPTIONAL: rate}}` | `{{OPTIONAL: notes}}` |
| `{{OPTIONAL: storage growth rate}}` | `{{OPTIONAL: rate}}` | `{{OPTIONAL: rate}}` | `{{OPTIONAL: notes}}` |

- Scaling strategy: `{{OPTIONAL: vertical, horizontal, sharded, multi-region, autoscaling rules, etc.}}`

## Accessibility

- Conformance target: `{{REQUIRED: e.g., WCAG 2.2 AA, Section 508, or "Not applicable"}}`
- Required input modalities: `{{OPTIONAL: keyboard-only, screen reader, voice, touch}}`
- Required visual contrast and sizing rules: `{{OPTIONAL: contrast ratios, focus indicators, target sizes}}`
- Internationalization scope: `{{OPTIONAL: languages, locales, RTL support, currency, date format}}`
- Verification approach: `{{OPTIONAL: automated tooling, manual audit, assistive-tech testing}}`

## Observability And Operability

- Logging baseline: `{{REQUIRED: structured fields, retention, sampling}}`
- Required metrics: `{{REQUIRED: golden signals — latency, traffic, errors, saturation — plus business KPIs}}`
- Tracing baseline: `{{OPTIONAL: distributed tracing tool, propagation rules, span coverage}}`
- Dashboards required at launch: `{{OPTIONAL: list}}`
- Alerting thresholds: `{{OPTIONAL: which conditions page humans, which open tickets, which only inform}}`
- Health check contract: `{{OPTIONAL: liveness, readiness, deep health probes}}`

## Security NFRs

- Authentication strength: `{{REQUIRED: MFA, SSO, passkeys, API keys, mTLS, etc.}}`
- Session and token lifetimes: `{{OPTIONAL: max idle, absolute, refresh policy}}`
- Encryption in transit: `{{REQUIRED: e.g., TLS 1.3 minimum}}`
- Encryption at rest: `{{REQUIRED: scope and algorithm}}`
- Vulnerability response SLA: `{{OPTIONAL: critical / high / medium response and patch windows}}`

## Privacy NFRs

- Data minimization rule: `{{OPTIONAL: how the team avoids collecting unnecessary data}}`
- User-facing data rights: `{{OPTIONAL: access, correction, deletion, portability, consent}}`
- Cross-border transfer rules: `{{OPTIONAL: residency, transfer mechanisms}}`

## Maintainability And Evolvability

- Code quality bar: `{{OPTIONAL: complexity limits, doc requirements, public-API stability rules}}`
- Test coverage target: `{{OPTIONAL: line, branch, mutation coverage targets — only if meaningful}}`
- Refactoring policy: `{{OPTIONAL: when broad refactors are allowed and who approves them}}`
- Dependency policy: `{{OPTIONAL: how new dependencies are added, reviewed, and pinned}}`

## Cost And Resource Constraints

- Monthly infrastructure budget envelope: `{{OPTIONAL: target or ceiling}}`
- Cost-per-transaction or cost-per-user expectation: `{{OPTIONAL: target}}`
- Resource limits per service: `{{OPTIONAL: CPU, memory, storage caps}}`

## Verification Plan

| NFR Area | Verification Method | Cadence | Owner |
| --- | --- | --- | --- |
| `{{REQUIRED: e.g., latency p95}}` | `{{REQUIRED: load test, synthetic monitor, real user metric}}` | `{{REQUIRED: per release, weekly, continuous}}` | `{{OPTIONAL: owner}}` |
| `{{OPTIONAL: another NFR}}` | `{{OPTIONAL: method}}` | `{{OPTIONAL: cadence}}` | `{{OPTIONAL: owner}}` |
