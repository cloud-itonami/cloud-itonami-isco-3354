# Operator Guide

## First Deployment

1. Register the licensing office and its independently verified identity,
   including the office's supply-cost ceiling.
2. Register each license-application case file as it is opened by human
   intake staff.
3. Run synthetic operating cases (intake logging, appointment scheduling,
   supply orders, licensing-review flags).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions
   (every `:flag-licensing-review`, every over-ceiling supply order).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- registered office and application case-file roster, kept current
- safety-critical escalation path for licensing-review flags and
  over-ceiling orders
- provenance for all operating records
- human review for every licensing-review flag and every over-ceiling
  supply order
- audit export for all gated actions
- **no code path, configuration or override may allow this actor to
  issue, deny, renew or revoke a license or permit** — this must remain
  structurally impossible, not merely disabled by configuration

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks (in particular any
licensing-review flag) escalate to a human licensing official, and that no
automated path can ever issue, deny, renew or revoke a license or permit.
