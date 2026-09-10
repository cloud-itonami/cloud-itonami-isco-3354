(ns govlicensing.advisor
  "Licensing Coordination Advisor — the advisor named in this repository's
  README, proposing a documentation/logistics-coordination operation (log a
  license-application record, schedule a review appointment, flag an
  application for human licensing-official review, or coordinate a supply
  order) from an office roster, a license-application case-file board and a
  supply catalog. Swappable mock/llm; the advisor ONLY proposes --
  `govlicensing.governor` checks office/application basis and the
  supply-cost ceiling independently and always escalates flagged licensing
  reviews and over-ceiling supply orders. The advisor can never propose to
  issue, deny, renew or revoke a license or permit: those ops are not
  members of the closed op-allowlist below -- they are structurally absent,
  not merely gated -- and the governor's finalization-language check
  hard-blocks any proposal whose rationale describes actually taking such a
  licensing-decision action regardless of which `:op` it is filed under.
  Modeled on cloud-itonami-isco-3341's advisor.

  A proposal: {:op :log-application-record|:schedule-review-appointment|
               :flag-licensing-review|:coordinate-supply-order
               :effect :propose :office-id str :application-id str
               :status kw :appointment-time str :concern-category kw
               :cost number :item str :stake kw :confidence n
               :rationale str, plus op-specific fields}

  NOTE: `:flag-licensing-review` only ever surfaces an application for
  human licensing-official review -- it is never a vehicle for the advisor
  itself to conclude that a license or permit should be issued, denied,
  renewed or revoked. Its default rationale below is deliberately phrased
  to describe the flagging action, not to narrate taking a licensing
  decision -- see `govlicensing.governor`'s finalization-language check and
  the self-trip regression test in `govlicensing.governor-test` for why the
  phrasing matters."
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op {:keys [office-id application-id status
                                  concern-category cost item] :as _request}]
  (case op
    :log-application-record
    (str "proposed logging a " (name (or status :intake)) " record on application "
         application-id " for office " office-id)

    :schedule-review-appointment
    (str "proposed a licensing-review/inspection appointment scheduling"
         " proposal for application " application-id " at office " office-id)

    ;; Deliberately phrased with plural/third-person verb forms ("issues",
    ;; "denies", "revokes") and never combined with an article immediately
    ;; before "license"/"permit" (no "issue the license" / "deny a license
    ;; application" / "revoke the license" substrings anywhere), so the
    ;; governor's finalization-language check -- which matches the
    ;; base-verb finalize-ACTION phrase, not this disclaiming sentence --
    ;; can never self-trip on this default rationale.
    :flag-licensing-review
    (str "surfaces application " application-id " (" (name (or concern-category :new-application))
         ") for human licensing-official review; flagging only -- this op"
         " never issues, denies, renews or revokes any license or permit,"
         " and it never makes a licensing decision itself")

    :coordinate-supply-order
    (str "proposed a supply order (" item ", cost " cost ") for office " office-id)

    (str "proposed " (name op) " for office " office-id)))

(defn- infer [_store {:keys [op stake office-id application-id status
                              appointment-time review-date concern-category
                              cost item] :as request}]
  {:op op
   :effect :propose
   :office-id office-id
   :application-id application-id
   :status status
   :appointment-time appointment-time
   :review-date review-date
   :concern-category concern-category
   :cost cost
   :item item
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op request)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a government-licensing-office documentation and logistics
   coordination advisor. Given a request, propose an :op from the closed
   allowlist (:log-application-record, :schedule-review-appointment,
   :flag-licensing-review, :coordinate-supply-order only), the relevant
   office-id/application-id, an honest :confidence and a :stake. You have
   NO licensing or permitting-decision authority: never propose an op that
   issues, denies, renews or revokes a license or permit -- no such op
   exists in your allowlist, and no phrasing that describes actually
   taking such a decision is ever acceptable in a rationale, regardless of
   which :op it is filed under. Those decisions are always made by a human
   licensing official, outside this actor entirely.
   :flag-licensing-review only surfaces an application for human review and
   always requires human sign-off regardless of confidence; a supply order
   above the office's registered cost ceiling always requires human
   sign-off too.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
