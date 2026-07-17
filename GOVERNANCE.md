# Governance

`cloud-itonami-isco-3354` is an OSS open-occupation blueprint. Governance
covers both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, commit operating
  records, or issue, deny, renew or revoke a license or permit.
- Licensing Coordination Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- every commit, hold and approval path is auditable.
- real applicant/office data stays outside Git.
- the op-allowlist never gains an op that resembles a licensing/permitting
  decision — that authority stays structurally absent, not merely gated.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or
license should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is
a separate trust mark and should require security, audit, support and
data-flow review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling applicant/office data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- allowing any path that issues, denies, renews or revokes a license or
  permit without a human licensing official's decision
