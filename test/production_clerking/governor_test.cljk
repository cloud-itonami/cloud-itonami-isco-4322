(ns production-clerking.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [production-clerking.store :as store]
            [production-clerking.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-order! st {:order-id "order-1" :name "Batch 42"})
    st))

(deftest ok-on-clean-track
  (let [st (fresh-store)
        proposal {:op :track :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:order-id "order-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-order
  (let [st (fresh-store)
        proposal {:op :track :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:order-id "no-such-order"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-order (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :track :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:order-id "order-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-discrepancy-run-closure
  (let [st (fresh-store)
        proposal {:op :close-run-with-discrepancy :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:order-id "order-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-scheduling-conflict-override
  (let [st (fresh-store)
        proposal {:op :override-scheduling-conflict :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:order-id "order-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :track :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:order-id "order-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:order-id "order-1" :op :reconcile})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "order-1"))))
    (is (= 1 (count (store/ledger st))))))
