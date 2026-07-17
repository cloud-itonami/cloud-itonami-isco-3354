(ns govlicensing.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [govlicensing.actor :as actor]
            [govlicensing.advisor :as advisor]
            [govlicensing.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-office! st {:office-id "office-1" :name "City Business Licensing Office"
                                :max-supply-cost 500})
    (store/register-application! st {:application-id "APP-1" :office-id "office-1"
                                     :applicant-name "Aya Tanaka" :license-type "food-service"})
    st))

(deftest commits-a-registered-application-log-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:office-id "office-1" :op :log-application-record :stake :low
                 :application-id "APP-1" :status :intake}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "office-1"))))))

(deftest holds-an-over-cost-supply-order-until-escalated
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:office-id "office-1" :op :coordinate-supply-order :stake :low
                 :item "case-file cabinets" :cost 5000}
        interrupted (actor/run-request! graph request {} "thread-2")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "office-1")))
    (let [resumed (actor/approve! graph "thread-2")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "office-1")))))))

(deftest interrupts-then-approves-flag-licensing-review-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:office-id "office-1" :op :flag-licensing-review :application-id "APP-1"
                 :concern-category :new-application :stake :low}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "office-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "office-1")))))))

(deftest holds-a-finalization-language-proposal-regardless-of-nominal-op
  (testing "even though the advisor filed the proposal under a legitimate,
            allowlisted op (:log-application-record), a rationale that
            describes actually taking a licensing-decision action is a
            hard, permanent block through the full graph -- it never
            reaches :request-approval, it goes straight to :hold"
    (let [st (fresh-store)
          rogue-advisor (reify advisor/Advisor
                          (-advise [_ _store _request]
                            {:op :log-application-record :effect :propose
                             :application-id "APP-1" :confidence 0.99 :stake :low
                             :rationale "let's issue the license to this applicant"}))
          graph (actor/build-graph {:store st :advisor rogue-advisor})
          request {:office-id "office-1" :op :log-application-record :stake :low :application-id "APP-1"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "office-1"))))))

(deftest holds-an-op-outside-the-closed-allowlist
  (testing "an op that is not a member of the closed allowlist (here, a
            direct license issuance) is a hard, permanent block -- it
            never reaches :request-approval either, because no such op
            exists in the allowlist in the first place"
    (let [st (fresh-store)
          rogue-advisor (reify advisor/Advisor
                          (-advise [_ _store _request]
                            {:op :issue-license :effect :propose
                             :application-id "APP-1" :confidence 0.99 :stake :low
                             :rationale "proceeding with license issuance"}))
          graph (actor/build-graph {:store st :advisor rogue-advisor})
          request {:office-id "office-1" :op :issue-license :stake :low :application-id "APP-1"}
          result (actor/run-request! graph request {} "thread-5")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "office-1"))))))
