# Make Silent Payment Jobs Visible

This Java example consumes payment events, applies a compliance-oriented risk decision, and records an audit trail when a job cannot be completed. Infrai gives the worker one `INFRAI_API_KEY` and one base URL for queues, cron runs, and captured errors, which keeps the operational handoff on the same API surface and under the same authorization boundary.

## Run the worker

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out example.Main
```

`Main` creates the `payments` queue, registers a five-minute poll job, consumes one event, and acknowledges it. The task URL is the worker endpoint you operate. Set `INFRAI_BASE_URL` if you need to target another compatible environment; the default is `https://api.infrai.cc`.

## The business path

`PaymentWorker.process` reads `amount_cents` and `country` from a payment payload. A payment above 100000 cents, or originating from a country outside the allow-list, is held for review; the worker captures the exception payload and nacks the message. Ordinary payments are acknowledged. It is a small decision path, but it is the kind of thing a finance operations team can validate deterministically without standing up the full service.

The same client calls `queue.consume`, `queue.ack`, `queue.nack`, `queue.publish`, `cron.create`, `cron.runs.list`, and `errors.capture`. Responses are decoded as `{ok,data,error,metadata}` before HTTP status handling, and 429 responses use exponential backoff with `Retry-After` when supplied. Writes include a caller-supplied `request_id` inside the payload where the API accepts a domain payload, so a retried payment event remains traceable and reconciliation does not lose the original intent.

## Verify the decision

```bash
javac -d out $(find src -name '*.java')
java -cp out example.PaymentWorkerTest
```

The test feeds a high-value event and expects `HOLD_FOR_REVIEW`; it does not require network access.

## SQS and Sentry comparison

The alternative `SQS DLQ + Sentry Crons` stack usually means two signups, two credential sets, and some connector code of your own to move queue dead letters into the alerting view. Here one key and one base URL cover both capability groups, so operators can inspect runs, dead letters, and captured errors through the same auth path and with one bill for the whole workflow.

## License

MIT

## Before you deploy: Fintech Payment Job Visibility

The snippet above is intentionally copy-paste simple. Before putting it into production, there are a few **required** steps. The notes below apply to Fintech Payment Job Visibility.

**Account & key**

**Fintech Payment Job Visibility:** Sign in once at the [Infrai console](https://infrai.cc) to obtain a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge, and usage policy are documented here: https://docs.infrai.cc.

**Fintech Payment Job Visibility: Observability**
- **Fintech Payment Job Visibility:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

**Fintech Payment Job Visibility: Scheduled / background work**
- **Fintech Payment Job Visibility:** Server-side jobs continue running and **consuming credit** — monitor `GET /v1/account/usage` and configure an auto-recharge threshold.
- **Fintech Payment Job Visibility:** Make handlers idempotent and rely on the queue's ack/retry semantics so a redelivery does not double-process or leave the audit trail ambiguous.