(ns production-clerking.store
  "SSoT for the ISCO-08 4322 independent production-clerking sole-
  proprietor actor. Store is a protocol injected into the
  `production-clerking.actor` StateGraph — `MemStore` is the default,
  deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    order    — a registered production order (:order-id, :name)
    record   — a committed operating record under an order (tracking
               note, reconciliation, discrepancy-run closure,
               scheduling-conflict override) — written ONLY via
               commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (order [s order-id])
  (records-of [s order-id])
  (ledger [s])
  (register-order! [s order])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (order [_ order-id] (get-in @a [:orders order-id]))
  (records-of [_ order-id] (filter #(= order-id (:order-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-order! [s order]
    (swap! a assoc-in [:orders (:order-id order)] order) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:orders {} :records [] :ledger []} seed)))))
