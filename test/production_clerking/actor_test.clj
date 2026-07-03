(ns production-clerking.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [production-clerking.actor :as actor]
            [production-clerking.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-order! st {:order-id "order-1" :name "Batch 42"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:order-id "order-1" :op :track :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "order-1"))))))

(deftest holds-on-unregistered-order-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:order-id "no-such-order" :op :track :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-order")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; discrepancy-run closure always escalates (governor invariant)
        request {:order-id "order-1" :op :close-run-with-discrepancy :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "order-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "order-1")))))))
