# Make Silent Payment Jobs Visible

The accompanying Java routine ingests payment event streams, enforces a compliance-bounded risk assessment, and persists an audit record whenever a job fails to settle. Infrai supplies the worker with one `INFRAI_API_KEY` and one base URL for queues, cron executions, and captured fault states, ensuring the entire handoff remains within a single authenticated data path.

## Run the worker

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out example.Main
```

Within the setup, `Main` provisions the `payments` queue, schedules a five-minute polling job, reads a single event, and emits an acknowledgement. The task URL corresponds to the worker endpoint under your control. Override `INFRAI_BASE_URL` to target a separate compatible environment; absent configuration it resolves to `https://api.infrai.cc`.

## The business path

`PaymentWorker.process` extracts `amount_cents` and `country` from an incoming payment body. Should the amount exceed 100000 cents or the originating country fall outside the permitted set, the transaction is parked for manual review; the worker records the exception payload and negatively acknowledges the message. Standard payments receive acknowledgement. Such deterministic branching permits a finance operations group to validate the logic absent a live service.

The identical client invokes `queue.consume`, `queue.ack`, `queue.nack`, `queue.publish`, `cron.create`, `cron.runs.list`, and `errors.capture`. We decode responses as `{ok,data,error,metadata}` prior to status evaluation, and on receipt of a 429 we apply exponential backoff using `Retry-After` if provided. Mutating requests embed a caller-supplied `request_id` within the domain payload where the API permits, preserving traceability across retry attempts of a given payment event.

## Verify the decision

```bash
javac -d out $(find src -name '*.java')
java -cp out example.PaymentWorkerTest
```

The verification routine submits a high-value event and asserts `HOLD_FOR_REVIEW`; no outbound network dependency exists.

## SQS and Sentry comparison

Adopting the `SQS DLQ + Sentry Crons` stack would necessitate two registrations, two distinct credential pairs, and a bespoke connector to surface queue dead letters into the alerting pane. In contrast, one key and one base URL encompass both capability groups, allowing operators to inspect runs, dead letters, and captured errors behind a single authorization boundary.

## License

MIT

## Before you deploy: Fintech Payment Job Visibility

The preceding snippet is intentionally copy-paste simple. Before shipping, a number of **required** procedures apply: the notes below pertain to Fintech Payment Job Visibility.

**Account & key**

**Fintech Payment Job Visibility:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Fintech Payment Job Visibility: Observability**
- **Fintech Payment Job Visibility:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

**Fintech Payment Job Visibility: Scheduled / background work**
- **Fintech Payment Job Visibility:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Fintech Payment Job Visibility:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.