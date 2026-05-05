# Threat Model And Data Classification

## How To Fill This File

- This file gives the Security agent something concrete to review against. Without it, security review becomes vibes.
- Capture realistic threats, not exhaustive ones. Focus on what would actually hurt this product.
- Keep this file aligned with `05_engineering_quality_security_and_compliance.md` and `06_integrations_data_flow_and_boundaries.md`.
- If the product handles only public, non-sensitive data and has no auth surface, write `Not applicable` with one sentence of justification.

## Scope And Assumptions

- Systems in scope: `{{REQUIRED: which services, surfaces, and integrations are covered}}`
- Systems explicitly out of scope: `{{REQUIRED: what is intentionally excluded and why}}`
- Trust assumptions: `{{REQUIRED: which actors, networks, and dependencies are trusted; which are not}}`
- Threat actor profile: `{{REQUIRED: e.g., opportunistic external attacker, malicious insider, compromised dependency, abusive end user}}`

## Data Classification

| Data Class | Examples | Storage Location | Access Control | Retention | Deletion Trigger |
| --- | --- | --- | --- | --- | --- |
| `Public` | `{{OPTIONAL: examples}}` | `{{OPTIONAL: where it lives}}` | `{{OPTIONAL: who can access}}` | `{{OPTIONAL: retention}}` | `{{OPTIONAL: trigger}}` |
| `Internal` | `{{OPTIONAL: examples}}` | `{{OPTIONAL: location}}` | `{{OPTIONAL: access}}` | `{{OPTIONAL: retention}}` | `{{OPTIONAL: trigger}}` |
| `Confidential` | `{{REQUIRED: examples — e.g., business analytics, internal IP}}` | `{{REQUIRED: location}}` | `{{REQUIRED: access}}` | `{{REQUIRED: retention}}` | `{{REQUIRED: trigger}}` |
| `Restricted / Regulated` | `{{REQUIRED: examples — e.g., PII, PHI, payment, credentials}}` | `{{REQUIRED: location}}` | `{{REQUIRED: access}}` | `{{REQUIRED: retention}}` | `{{REQUIRED: trigger}}` |

## Trust Boundaries

| Boundary | What Crosses | Validation Owner | Authentication Mechanism | Notes |
| --- | --- | --- | --- | --- |
| `{{REQUIRED: e.g., public API ingress}}` | `{{REQUIRED: data and identities crossing in}}` | `{{REQUIRED: which layer validates}}` | `{{REQUIRED: how the caller is authenticated}}` | `{{OPTIONAL: rate limit, anti-replay, signature}}` |
| `{{OPTIONAL: another boundary}}` | `{{OPTIONAL: data}}` | `{{OPTIONAL: validator}}` | `{{OPTIONAL: auth}}` | `{{OPTIONAL: notes}}` |

## STRIDE Threat Table

Add or remove rows as needed. Score likelihood and impact on a 1-5 scale.

| Threat ID | Category | Asset Or Flow | Description | Likelihood | Impact | Existing Mitigation | Residual Risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `T-{{REQUIRED: 001}}` | `Spoofing` | `{{REQUIRED: asset}}` | `{{REQUIRED: how the threat manifests}}` | `{{REQUIRED: 1-5}}` | `{{REQUIRED: 1-5}}` | `{{REQUIRED: control or "none yet"}}` | `{{REQUIRED: low / medium / high}}` |
| `T-{{OPTIONAL: 002}}` | `Tampering` | `{{OPTIONAL: asset}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: mitigation}}` | `{{OPTIONAL: residual risk}}` |
| `T-{{OPTIONAL: 003}}` | `Repudiation` | `{{OPTIONAL: asset}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: mitigation}}` | `{{OPTIONAL: residual risk}}` |
| `T-{{OPTIONAL: 004}}` | `Information Disclosure` | `{{OPTIONAL: asset}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: mitigation}}` | `{{OPTIONAL: residual risk}}` |
| `T-{{OPTIONAL: 005}}` | `Denial Of Service` | `{{OPTIONAL: asset}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: mitigation}}` | `{{OPTIONAL: residual risk}}` |
| `T-{{OPTIONAL: 006}}` | `Elevation Of Privilege` | `{{OPTIONAL: asset}}` | `{{OPTIONAL: description}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: 1-5}}` | `{{OPTIONAL: mitigation}}` | `{{OPTIONAL: residual risk}}` |

## Abuse Cases

| Abuse Case ID | Actor | Goal | Attack Path | Detection Signal | Response |
| --- | --- | --- | --- | --- | --- |
| `A-{{OPTIONAL: 001}}` | `{{OPTIONAL: attacker profile}}` | `{{OPTIONAL: what they want}}` | `{{OPTIONAL: steps}}` | `{{OPTIONAL: how we notice}}` | `{{OPTIONAL: containment and recovery}}` |
| `A-{{OPTIONAL: 002}}` | `{{OPTIONAL: actor}}` | `{{OPTIONAL: goal}}` | `{{OPTIONAL: path}}` | `{{OPTIONAL: detection}}` | `{{OPTIONAL: response}}` |

## Identity And Access Model

- Identity provider: `{{REQUIRED: who issues identities and how}}`
- Authentication factors required: `{{REQUIRED: e.g., password + MFA, SSO, passkey, mTLS, API key}}`
- Authorization model: `{{REQUIRED: RBAC, ABAC, scopes, custom; with role inventory or pointer to it}}`
- Session and token policy: `{{REQUIRED: lifetimes, refresh, revocation, binding}}`
- Privileged access procedure: `{{OPTIONAL: how break-glass and elevated access are granted, logged, and reviewed}}`

## Logging And Audit Requirements

- Security-relevant events that must be logged: `{{REQUIRED: auth attempts, role changes, data exports, admin actions, etc.}}`
- Log retention: `{{REQUIRED: duration and storage location}}`
- Tamper-evidence: `{{OPTIONAL: append-only storage, hashing, separate account}}`
- Audit access procedure: `{{OPTIONAL: who can read security logs and how}}`

## Privacy And Regulatory Mapping

| Requirement | Source | How This Product Satisfies It | Owner |
| --- | --- | --- | --- |
| `{{OPTIONAL: e.g., right to deletion}}` | `{{OPTIONAL: GDPR Article 17}}` | `{{OPTIONAL: deletion job, soft delete, etc.}}` | `{{OPTIONAL: owner}}` |
| `{{OPTIONAL: another requirement}}` | `{{OPTIONAL: source}}` | `{{OPTIONAL: implementation}}` | `{{OPTIONAL: owner}}` |

## Open Security Questions

- `{{OPTIONAL: unresolved security question that may affect design or scope}}`
- `{{OPTIONAL: another unresolved question}}`
