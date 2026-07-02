# cloud-itonami-isco-4322

Open Occupation Blueprint for **ISCO-08 4322**: Production Clerks.

This repository designs a forkable OSS business for an independent production clerk: a production-tracking robot performs job-ticket scanning and work-in-progress counting under a governor-gated actor, so the practice keeps its own tracking and reconciliation records instead of renting a closed production-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a production-tracking robot performs physical job-ticket scanning and work-in-progress counting under an actor that proposes
actions and an independent **Production Clerking Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
closing a production run with an unresolved discrepancy, or overriding a scheduling conflict affecting safety staffing) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production order + schedule + tracking protocol
        |
        v
Production Advisor -> Production Clerking Governor -> track/reconcile, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4322`). Required capabilities:

- :robotics
- :forms
- :telemetry
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
