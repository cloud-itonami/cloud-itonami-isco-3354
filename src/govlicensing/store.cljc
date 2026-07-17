(ns govlicensing.store
  "SSoT for the ISCO-08 3354 independent government licensing coordination
  practice actor (itonami actor pattern, ADR-2607011000 / ADR-2607121000 /
  CLAUDE.md Actors section; README's 'Robotics premise' -- a documentation/
  logistics-coordination robot performs license-application intake, case-
  record filing, review-appointment scheduling and office-supply
  coordination under this advisor/governor pair, which never dispatches
  hardware itself and never lets a proposal issue, deny, renew or revoke a
  license or permit). Modeled on cloud-itonami-isco-3341's
  officesupervision.store.

  Domain:

    office        — a registered government licensing office/agency
                     (:office-id, :name, :max-supply-cost). The
                     `:max-supply-cost` field is the registered ceiling a
                     proposed :coordinate-supply-order's :cost is compared
                     against -- an order above it always escalates to human
                     sign-off rather than auto-committing.
    application    — a registered license-application case file owned by
                     this office ({:application-id :office-id
                     :applicant-name :license-type}). A case file must be
                     independently verified/registered (by a human intake
                     process, outside this actor) before any of this
                     actor's proposals may cite it -- this actor never
                     opens a case ex nihilo, it only coordinates around an
                     already-opened one.
    record         — a committed operating record (a logged application
                     entry, a scheduled review appointment, a flagged
                     review, or a supply order) — written ONLY via
                     commit-record!.
    ledger         — append-only audit trail, commit or hold.

  NOTE: nothing in this namespace, this actor's op-allowlist
  (`govlicensing.governor`), or any other namespace in this repository
  ever issues, denies, renews or revokes a license or permit -- that
  authority is structurally absent from this codebase, not merely gated."
  )

(defprotocol Store
  (office [s office-id])
  (application [s application-id])
  (records-of [s office-id])
  (ledger [s])
  (register-office! [s o])
  (register-application! [s a])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (office [_ office-id] (get-in @a [:offices office-id]))
  (application [_ application-id] (get-in @a [:applications application-id]))
  (records-of [_ office-id] (filter #(= office-id (:office-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-office! [s o]
    (swap! a assoc-in [:offices (:office-id o)] o) s)
  (register-application! [s app]
    (swap! a assoc-in [:applications (:application-id app)] app) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:offices {} :applications {}
                                    :records [] :ledger []}
                                   seed)))))
