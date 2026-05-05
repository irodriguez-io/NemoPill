# Environments And DevOps

## How To Fill This File

- Describe how code moves from a developer's machine to production, and how it can be safely backed out.
- Keep secrets, environment variables, and infrastructure references precise enough that a Developer agent can wire a new service without guessing.
- Update this file whenever an environment, pipeline, or deployment contract changes.

## Environments

| Environment | Purpose | URL Or Endpoint | Audience | Data Class |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: local}}` | `{{REQUIRED: developer iteration}}` | `{{OPTIONAL: localhost convention}}` | `{{OPTIONAL: engineers}}` | `{{REQUIRED: synthetic / scrubbed / "no real data"}}` |
| `{{REQUIRED: dev or integration}}` | `{{REQUIRED: continuous merges, smoke tests}}` | `{{OPTIONAL: URL}}` | `{{OPTIONAL: engineers, QA}}` | `{{OPTIONAL: data class}}` |
| `{{OPTIONAL: staging or pre-prod}}` | `{{OPTIONAL: release candidate, UAT, perf}}` | `{{OPTIONAL: URL}}` | `{{OPTIONAL: stakeholders}}` | `{{OPTIONAL: data class}}` |
| `{{REQUIRED: production}}` | `{{REQUIRED: live user traffic}}` | `{{REQUIRED: URL}}` | `{{REQUIRED: end users}}` | `{{REQUIRED: real data}}` |

## Infrastructure

- Hosting provider: `{{REQUIRED: e.g., AWS, GCP, Azure, Vercel, Fly, on-prem, hybrid}}`
- Compute model: `{{REQUIRED: containers, serverless functions, VMs, edge workers, managed PaaS}}`
- Datastores in use: `{{REQUIRED: each database, cache, queue, object store with version}}`
- Network topology: `{{OPTIONAL: VPCs, subnets, ingress, egress, private connectivity}}`
- Infrastructure as code tool: `{{REQUIRED: Terraform, Pulumi, CDK, OpenTofu, Helm, "Not applicable", etc.}}`
- IaC repository location: `{{OPTIONAL: where IaC lives if separate from this repo}}`

## Configuration And Secrets

| Concern | Mechanism | Where Defined | Notes |
| --- | --- | --- | --- |
| `{{REQUIRED: environment variables}}` | `{{REQUIRED: how they are loaded}}` | `{{REQUIRED: file or platform service}}` | `{{OPTIONAL: naming convention, prefix rules}}` |
| `{{REQUIRED: secrets}}` | `{{REQUIRED: secret manager or vault}}` | `{{REQUIRED: location and access control}}` | `{{REQUIRED: rotation policy and owner}}` |
| `{{OPTIONAL: feature flags}}` | `{{OPTIONAL: flag service or library}}` | `{{OPTIONAL: where flag definitions live}}` | `{{OPTIONAL: naming, default-off rule, lifecycle}}` |
| `{{OPTIONAL: per-environment overrides}}` | `{{OPTIONAL: how overrides are applied}}` | `{{OPTIONAL: location}}` | `{{OPTIONAL: notes}}` |

- Secret rotation cadence: `{{REQUIRED: how often and by whom}}`
- Secret-in-code rule: `{{REQUIRED: e.g., never commit secrets; pre-commit scan must enforce this}}`

## Continuous Integration

- CI tool: `{{REQUIRED: GitHub Actions, GitLab CI, CircleCI, Buildkite, Jenkins, etc.}}`
- Pipeline definition location: `{{REQUIRED: path to CI config}}`
- Required CI stages: `{{REQUIRED: e.g., install, lint, type-check, unit, integration, build, security scan}}`
- Required passing checks before merge: `{{REQUIRED: list each blocking check}}`
- Average CI duration target: `{{OPTIONAL: target wall-clock for the merge-blocking pipeline}}`
- Caching strategy: `{{OPTIONAL: dependency cache, build cache, test result cache}}`

## Continuous Delivery And Deployment

- Branch strategy: `{{REQUIRED: e.g., trunk-based, GitFlow, release branches}}`
- Merge contract: `{{REQUIRED: PR approvals required, review owners, required status checks}}`
- Deployment trigger: `{{REQUIRED: auto on merge to main, manual promote, scheduled, tagged release}}`
- Deployment topology: `{{REQUIRED: rolling, blue/green, canary, immutable, in-place}}`
- Database migration strategy: `{{REQUIRED: forward-only, expand-contract, downtime window, "Not applicable"}}`
- Rollback mechanism: `{{REQUIRED: how to revert in <X minutes}}`
- Rollback rehearsal cadence: `{{OPTIONAL: how often a rollback is practiced}}`

## Release Process

- Release cadence: `{{REQUIRED: continuous, weekly, per milestone, etc.}}`
- Release approver: `{{REQUIRED: who authorizes a production deploy}}`
- Freeze windows: `{{OPTIONAL: dates or conditions when releases are paused}}`
- Release notes location: `{{OPTIONAL: changelog file, GitHub release, internal portal}}`
- Communication template: `{{OPTIONAL: where the release announcement is drafted and sent}}`

## Monitoring And On-Call

- Primary monitoring tool: `{{OPTIONAL: e.g., Datadog, Grafana, CloudWatch, Honeycomb}}`
- Error tracking tool: `{{OPTIONAL: e.g., Sentry, Rollbar}}`
- Dashboards required at launch: `{{OPTIONAL: list with links}}`
- On-call rotation tool: `{{OPTIONAL: e.g., PagerDuty, Opsgenie, "Not applicable"}}`
- On-call expectations: `{{OPTIONAL: hours, response time, escalation chain}}`

## Backup, Recovery, And Disaster Plan

- Backup scope: `{{OPTIONAL: which datastores are backed up}}`
- Backup cadence: `{{OPTIONAL: frequency and retention}}`
- Restore drill cadence: `{{OPTIONAL: how often restore is tested}}`
- Disaster recovery plan reference: `{{OPTIONAL: link to runbook}}`

## Operational Runbooks

| Scenario | Runbook Location | Owner |
| --- | --- | --- |
| `{{OPTIONAL: failed deploy rollback}}` | `{{OPTIONAL: path}}` | `{{OPTIONAL: owner}}` |
| `{{OPTIONAL: database failover}}` | `{{OPTIONAL: path}}` | `{{OPTIONAL: owner}}` |
| `{{OPTIONAL: incident triage}}` | `{{OPTIONAL: path}}` | `{{OPTIONAL: owner}}` |

## DevOps Risks And Trade-Offs

- `{{OPTIONAL: known operational gaps the team has accepted for now}}`
- `{{OPTIONAL: planned hardening work and target milestone}}`
