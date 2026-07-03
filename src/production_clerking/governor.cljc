(ns production-clerking.governor
  "ProductionClerkingGovernor — the independent safety/traceability
  layer for the ISCO-08 4322 independent production-clerking actor.
  Wired as its own `:govern` node in `production-clerking.actor`'s
  StateGraph, downstream of `:advise` — the Advisor has no notion of
  order provenance or discrepancy-closure/staffing-override risk, so
  this MUST be a separate system able to reject a proposal (itonami
  actor pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. order provenance  — the request's order must be registered.
    2. no-actuation        — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: closing a production run with an
  unresolved discrepancy and overriding a scheduling conflict
  affecting safety staffing always require human sign-off):
    3. :op :close-run-with-discrepancy.
    4. :op :override-scheduling-conflict.
    5. low confidence (< `confidence-floor`)."
  (:require [production-clerking.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:close-run-with-discrepancy :override-scheduling-conflict})

(defn- hard-violations [{:keys [proposal]} order-record]
  (cond-> []
    (nil? order-record)
    (conj {:rule :no-order :detail "未登録 order"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `production-clerking.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [order-record (store/order store (:order-id request))
        hard (hard-violations {:proposal proposal} order-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
