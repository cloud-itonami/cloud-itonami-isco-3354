(ns govlicensing.governor
  "LicensingCoordinationGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating every
  documentation/logistics-coordination action an advisor may propose. The
  governor never dispatches hardware itself and never lets a proposal
  issue, deny, renew or revoke a license or permit -- that authority stays
  with a human licensing official, permanently and unconditionally, and no
  op resembling it exists anywhere in this actor's allowlist. Modeled on
  cloud-itonami-isco-3341's officesupervision.governor. Task twist: a
  proposal that cites a case file must cite a REGISTERED license-
  application belonging to the requesting office, a supply order's cost is
  an arithmetic ceiling against the office's registered
  `:max-supply-cost`, and the op itself must be a member of a CLOSED
  allowlist — every op outside it (in particular anything that issues,
  denies, renews or revokes a license or permit) is a hard, permanent
  block.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. office provenance     — the requesting office must be
                                independently verified/registered before
                                any action.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never commits an operating record itself;
                                it only gates what the advisor may
                                propose).
    3. closed op-allowlist    — `:op` must be one of
                                #{:log-application-record
                                  :schedule-review-appointment
                                  :flag-licensing-review
                                  :coordinate-supply-order}. Nothing that
                                issues, denies, renews or revokes a
                                license or permit is EVER a member of this
                                set -- such an op is structurally absent
                                from the allowlist and unconditionally
                                :hard? if it somehow appears.
    4. application basis      — a `:log-application-record`,
                                `:schedule-review-appointment` or
                                `:flag-licensing-review` proposal must
                                cite a REGISTERED license-application case
                                file belonging to this office.
    5. finalization-language  — a proposal whose rationale describes
                                actually TAKING a licensing-decision
                                action (\"issue the license\", \"deny the
                                license application\", \"revoke the
                                license\", etc. — the ACTION phrase, not
                                the bare topic noun \"license\"/\"permit\")
                                is a hard, permanent block regardless of
                                which `:op` it is filed under. This is
                                defense-in-depth on top of invariant 3 and
                                is deliberately phrased as the
                                finalize-ACTION, not the bare noun, so it
                                does not self-trip on a
                                `:flag-licensing-review` proposal's own
                                default rationale merely disclaiming that
                                it never issues/denies/revokes a license or
                                permit — see `govlicensing.advisor`'s
                                default rationale and this namespace's
                                `never-self-trips-on-default-mock-advisor-
                                proposals` test.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/:safety-critical
  regardless of confidence):
    6. :op :flag-licensing-review (surfacing an application for licensing
                                review is never auto-commit-eligible; a
                                human licensing official always reviews
                                it).
    7. supply-order cost ceiling (a `:coordinate-supply-order` whose
                                :cost exceeds the office's registered
                                :max-supply-cost always requires human
                                sign-off, rather than being refused
                                outright — it may be a legitimate one-off
                                purchase).
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [govlicensing.store :as store]))

(def confidence-floor 0.6)

(def ^:private allowed-ops
  #{:log-application-record :schedule-review-appointment :flag-licensing-review
    :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-licensing-review})

(def ^:private application-basis-ops
  #{:log-application-record :schedule-review-appointment :flag-licensing-review})

;; Phrased as the finalize-ACTION (article + verb + object), never the bare
;; topic noun "license"/"permit" alone, so a legitimate proposal that
;; merely MENTIONS "license" or "permit" (e.g. :flag-licensing-review's own
;; default rationale, which explicitly disclaims issuing, denying, renewing
;; or revoking one) can never self-trip this check.
(def ^:private finalization-phrases
  ["issue the license" "issue a license" "issue license"
   "grant the license" "grant a license"
   "approve the license application" "approve the license" "approve the permit"
   "deny the license application" "deny a license application"
   "deny the application" "deny the permit application"
   "reject the license application" "reject the application"
   "reject the permit application"
   "revoke the license" "revoke a license" "revoke the permit"
   "suspend the license" "suspend the permit"
   "renew the license" "renew a license" "renew the permit"
   "finalize the license" "finalize a license"
   "finalize the licensing decision" "finalize the permit"])

(defn- finalization-language?
  "True when `proposal`'s rationale describes actually TAKING a
  licensing-decision action (issuing, denying, renewing or revoking a
  license or permit), never merely mentioning the topic."
  [proposal]
  (let [text (str/lower (str (:rationale proposal)))]
    (boolean (some #(str/includes? text %) finalization-phrases))))

(defn- hard-violations [{:keys [request proposal]} office-record basis-record]
  (let [{:keys [op]} proposal
        allowed? (contains? allowed-ops op)
        needs-application? (contains? application-basis-ops op)]
    (cond-> []
      (nil? office-record)
      (conj {:rule :no-office :detail "未登録 office"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は直接実行しない）"})

      (not allowed?)
      (conj {:rule :op-not-allowed
             :detail (str "op " (pr-str op) " は closed allowlist に含まれない"
                          "（免許・許可証の発行・拒否・更新・取消の最終決定は常に人間の"
                          "licensing official の専管事項であり、このアクターの allowlist"
                          "に含めない）")})

      (and allowed? needs-application? (nil? basis-record))
      (conj {:rule :unknown-application :detail "未登録 application への提案は不可"})

      (and allowed? needs-application? basis-record
           (not= (:office-id basis-record) (:office-id request)))
      (conj {:rule :application-wrong-office :detail "application が別 office のもの"})

      (finalization-language? proposal)
      (conj {:rule :finalization-language-blocked
             :detail (str "免許・許可証の発行・拒否・更新・取消を実行する提案は、"
                          "op の種類によらず常に恒久的にブロックされる"
                          "（人間の licensing official の専管事項であり、上書き不可）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `govlicensing.store/Store`. Pure — never mutates the store,
  never commits an operating record itself."
  [request context proposal store]
  (let [office-record (store/office store (:office-id request))
        op (:op proposal)
        needs-application? (contains? application-basis-ops op)
        basis-record (when needs-application?
                       (some->> (:application-id proposal) (store/application store)))
        hard (hard-violations {:request request :proposal proposal}
                              office-record basis-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops op)
        over-cost? (and (= :coordinate-supply-order op)
                        office-record
                        (number? (:cost proposal))
                        (> (:cost proposal) (:max-supply-cost office-record)))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-cost?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-cost?))}))
