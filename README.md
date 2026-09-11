# cloud-itonami-isco-3354

Open Occupation Blueprint for **ISCO-08 3354**: Government Licensing Officials.

This repository designs a forkable OSS business for an independent government
licensing coordination practice: a documentation/logistics-coordination robot
handles license-application intake, case-record filing, review-appointment
scheduling and office-supply coordination under a governor-gated actor, so a
licensing office keeps its own administrative coordination records instead of
renting a closed case-management SaaS.

**This actor has NO licensing/permitting-decision authority whatsoever.** It
never issues, denies, renews or revokes a license or permit, and no such
operation exists anywhere in its op-allowlist — that authority is
structurally absent from the codebase, not merely gated behind escalation.
See "Robotics premise" and "No licensing-decision authority" below.

**Maturity: `:implemented`.** `src/govlicensing/` implements the
`LicensingCoordinationActor` as a `langgraph.graph/state-graph`
(`govlicensing.actor`) wired to a `Licensing Coordination Advisor`
(`govlicensing.advisor`) and an independent `LicensingCoordinationGovernor`
(`govlicensing.governor`), following the itonami actor pattern
(ADR-2607011000 / ADR-2607121000): `:intake -> :advise -> :govern -> :decide
-+-> :commit (:ok?) +-> :request-approval (:escalate?, human-in-the-loop
interrupt) +-> :hold (:hard?)`. 18 tests / 68 assertions green
(`kbb -M:test`).

HARD invariants (always hold, never overridable): office provenance (the
requesting office must be independently verified/registered before any
action), no-actuation (`:effect` must be `:propose`), a closed op-allowlist
(`:log-application-record`, `:schedule-review-appointment`,
`:flag-licensing-review`, `:coordinate-supply-order` — nothing else, ever;
**no op that issues, denies, renews or revokes a license or permit exists in
this allowlist at all**), a registered license-application case-file basis
belonging to the requesting office for any proposal that cites one, and a
finalization-language check: **any proposal that describes actually issuing,
granting, denying, rejecting, revoking, suspending or renewing a license or
permit is a hard, permanent block, regardless of which `:op` it is nominally
filed under.** This actor never has authority to decide a licensing matter
itself — `:flag-licensing-review` only ever surfaces an application for a
human licensing official to decide.

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-licensing-review` (every single time — never auto-commit-eligible)
and a `:coordinate-supply-order` whose cost exceeds the office's registered
`:max-supply-cost` ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical/administrative domain work**. Here a documentation/
logistics-coordination robot performs license-application intake, case-
record filing, review-appointment scheduling and office-supply coordination
under an actor that proposes actions and an independent **Licensing
Coordination Governor** that gates them. The governor never dispatches
hardware itself and never lets any action touch actual licensing/permitting
authority; `:high`/`:safety-critical` actions (such as flagging an
application for licensing review or an over-ceiling supply order) require
human sign-off, and no action may ever issue, deny, renew or revoke a
license or permit.

## No licensing-decision authority

Government Licensing Officials issue, renew, deny and revoke licenses and
permits — decisions that determine whether a person or business may legally
operate. **This actor is a documentation/logistics-coordination robot ONLY.**
It has no op, anywhere in its allowlist, that resembles issuing, denying,
renewing or revoking a license. These are structurally absent from the
closed op-allowlist (see `govlicensing.governor`'s `allowed-ops`) — not
merely gated behind escalation. Any observation that suggests a licensing
decision is needed is surfaced ONLY via the always-escalate
`:flag-licensing-review` op, which a human licensing official reviews and
decides. See `govlicensing.governor`'s `finalization-language?` check for
the defense-in-depth block that holds any proposal describing an actual
licensing decision, regardless of which `:op` it is nominally filed under.

## Core Contract

```text
office roster + license-application case files + supply catalog
        |
        v
Licensing Coordination Advisor -> Licensing Coordination Governor -> log/schedule/order, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, issue,
deny, renew or revoke a license or permit, or suppress an operating record.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3354`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
