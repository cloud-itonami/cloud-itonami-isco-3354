# Contributing

`cloud-itonami-isco-3354` accepts contributions to the OSS actor, policy
tests, documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
```

Keep changes small and include tests for policy, audit, store or
disclosure behavior.

## Rules

- Do not commit real applicant data, credentials or licensing documents.
- Keep production writes and disclosures behind Licensing Coordination
  Governor.
- Never add an op to the closed allowlist that issues, denies, renews or
  revokes a license or permit — this occupation's actor must never gain
  licensing/permitting-decision authority, structurally or otherwise.
- Treat this occupation's workflows as high-risk: add tests for
  permission, purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
