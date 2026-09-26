# RabbitMQ Explained (and How This Project Uses It)

This doc has two parts: **Part 1** explains RabbitMQ from scratch, assuming you've never used a
message queue before. **Part 2** describes exactly how PolyPilot's `ai.signals` queue is wired up
today, and what you can change about it.

---

## Part 1 — RabbitMQ from zero

### The problem it solves

Normally, when service A wants service B to do something, A calls B directly over HTTP and waits
for a response. That's simple, but it has a downside: A is now *coupled in time* to B. If B is
slow, A waits. If B is down, A's call fails. If B needs to process 500 things and each takes 5
seconds, whoever is waiting on B waits 2500 seconds.

A **message queue** breaks that coupling. Instead of A calling B directly, A drops a message into
a queue and moves on immediately. B, whenever it's ready, picks messages off the queue and
processes them at its own pace. A doesn't wait, doesn't care if B is temporarily down, and doesn't
care how long B takes — it just wrote something to a queue.

RabbitMQ is a **message broker**: a standalone piece of infrastructure (a separate process/server)
whose only job is to hold these queues reliably, accept messages into them, and hand messages out
to whoever's listening. It's not built into either service — both A and B talk to RabbitMQ over the
network (in this repo, on the `polypilot-net` Docker network).

### The core building blocks

- **Message** — the actual payload (in this project, a JSON blob). It's just bytes plus a few
  metadata properties (content type, whether it should survive a restart, etc.).

- **Queue** — a named, ordered buffer of messages sitting in RabbitMQ, waiting to be consumed.
  Think of it like a literal physical line: messages go in one end, come out the other (roughly
  FIFO). A queue exists independently of whether anyone is currently publishing to it or consuming
  from it — it just holds messages.

- **Producer** — anything that publishes (sends) a message. In this project, `ai-agent` is the
  producer.

- **Consumer** — anything that subscribes to a queue and receives its messages. In this project,
  `orchestrator` is the consumer.

- **Exchange** — this is the part that surprises people coming from simpler systems. A producer
  **never publishes directly to a queue**. It always publishes to an **exchange**, and the exchange
  decides which queue(s), if any, the message gets routed into, based on rules called *bindings*.
  There are a few exchange types:
  - **Direct exchange** — routes a message to whichever queue is bound with a matching *routing
    key* (an exact string match). Good for "route this to exactly one specific place."
  - **Fanout exchange** — ignores routing keys entirely and copies the message to *every* queue
    bound to it. Good for broadcast ("tell everyone this happened").
  - **Topic exchange** — like direct, but the routing key can use wildcard patterns
    (`orders.*.created`). Good for pub/sub with categories.
  - **Default (nameless) exchange** — every RabbitMQ broker has one built-in direct exchange with
    the empty string `""` as its name. It has an implicit binding: any routing key you publish with
    is treated as a queue name, and if a queue with that exact name exists, the message goes
    straight there. This is the "just send it to this one queue" shortcut, and it's what this
    project uses (see Part 2).

- **Binding** — the rule connecting an exchange to a queue ("route messages with routing key X from
  exchange E into queue Q"). Exchanges and queues are declared independently; a binding is what
  wires them together.

- **Routing key** — a string the producer attaches to a message, which the exchange uses (per its
  type's rules) to decide where the message goes.

- **Durable vs. transient** — a queue (or exchange) declared as *durable* survives a RabbitMQ
  broker restart; *transient* ones vanish. Independently, a *message* can be marked *persistent*
  (survives a restart, written to disk) or *transient* (lost on restart, kept in memory only, a bit
  faster). For a durable queue to actually retain messages across a restart, the messages
  themselves also need to be published as persistent — durability of the queue alone isn't enough.

- **Acknowledgement (ack)** — when a consumer receives a message, RabbitMQ doesn't delete it from
  the queue immediately. It waits for the consumer to explicitly *acknowledge* successful
  processing. If the consumer crashes or disconnects before acking, RabbitMQ puts the message back
  on the queue for another consumer (or the same one, on reconnect) to try again. This is the core
  reliability guarantee: a message is not considered "done" until someone says so. (Spring AMQP,
  which `orchestrator` uses, handles acking automatically — it acks after your `@RabbitListener`
  method returns normally, and does *not* ack if it throws.)

- **Dead-letter queue (DLQ)** — a message can fail to process for reasons that will never resolve
  on their own (it's malformed, references data that doesn't exist, etc.). Retrying it forever
  would waste resources and could block the queue behind it. RabbitMQ lets you configure a queue
  with a *dead-letter exchange* — when a message on that queue is rejected (nacked) without being
  requeued, or expires, or the queue is too full, RabbitMQ automatically republishes it to that
  dead-letter exchange instead of discarding it. Typically that exchange routes into a separate
  "dead letter queue" that a human (or a separate, more careful process) can inspect later. This is
  how you avoid silently losing failed messages while also not looping on them forever.

### The mental model, end to end

```
Producer --publish(routing_key)--> Exchange --binding rules--> Queue --> Consumer --ack/nack-->
                                                                    \
                                                                     --(on failure)--> DLX --> DLQ
```

Nothing about this requires the producer and consumer to be online at the same time, written in
the same language, or running on the same machine — which is exactly why it's a good fit for
`ai-agent` (Python) publishing and `orchestrator` (Java) consuming.

---

## Part 2 — How PolyPilot actually uses this

### The queue: `ai.signals`

There's exactly one meaningful queue in this feature: **`ai.signals`**. `ai-agent` is the sole
producer; `orchestrator` is the sole consumer. One JSON message = one completed news+sentiment
analysis for one market.

### The exchange: the *default* exchange

Look at `ai-agent/app/services/publisher.py`:

```python
channel.queue_declare(queue=settings.rabbitmq_queue_ai_signals, durable=True)
channel.basic_publish(
    exchange="",
    routing_key=settings.rabbitmq_queue_ai_signals,
    ...
)
```

`exchange=""` is the default exchange described above. The routing key is literally the queue name
(`ai.signals`), which the default exchange's implicit binding maps directly to the queue of the
same name. There's no custom exchange, no fanout, no topic pattern — this is the simplest possible
routing setup RabbitMQ supports: one producer, one queue, direct delivery.

This is a deliberate simplicity choice, not a limitation of RabbitMQ — see "What you could change"
below for when you'd introduce a real exchange.

### Queue declaration — two places declare the same queue

Both `ai-agent` (`publisher.py`, shown above) and `orchestrator`
(`orchestrator/src/main/java/com/polypilot/ai/config/RabbitConfig.java`) declare `ai.signals` on
startup/first-use. This is normal and safe *as long as both declarations agree on the queue's
arguments* — RabbitMQ treats `queue_declare` as idempotent (declaring an already-existing queue
with identical arguments is a no-op), but declaring it with *different* arguments throws a
`PRECONDITION_FAILED` channel error.

**This is worth knowing about, because it's a real gotcha in this exact codebase**:
`orchestrator`'s declaration includes dead-letter arguments:

```java
QueueBuilder.durable(queueName)
    .withArgument("x-dead-letter-exchange", DLX_NAME)
    .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
    .build();
```

...but `ai-agent`'s `publisher.py` declares the same queue with **no arguments at all** (just
`durable=True`). Right now this works only because `orchestrator` always declares the queue first
(it does so as a `@Bean` at Spring context startup, before any `news-sync` tick could have run and
triggered `ai-agent` to publish). If that ordering ever flips — e.g. `ai-agent` starts handling
requests and publishes before `orchestrator` has finished booting — `ai-agent`'s bare
`queue_declare` would either succeed and silently create the queue *without* the DLX arguments (if
it runs first), or fail (if orchestrator's stricter declaration already won and ai-agent's laxer
one is considered compatible — RabbitMQ actually allows a "subset" re-declare without extra
arguments to pass, so in practice ai-agent's declaration is permissive enough not to conflict).
Either way, the dead-letter arguments are effectively **owned by `orchestrator`**; if you ever
change them, change them in `RabbitConfig.java` and don't add conflicting arguments to
`publisher.py`'s bare declaration.

### Retry + dead-lettering — what happens when `orchestrator` fails to process a message

From `RabbitConfig.java`:

1. `ai.signals` (the primary queue) is declared with `x-dead-letter-exchange: ai.signals.dlx` and
   `x-dead-letter-routing-key: ai.signals.dlq`.
2. `ai.signals.dlx` is a plain `DirectExchange`.
3. `ai.signals.dlq` is a separate durable queue, bound to `ai.signals.dlx` with routing key
   `ai.signals.dlq`.
4. The listener container factory has a retry advice chain:
   `RetryInterceptorBuilder.stateless().maxRetries(3).recoverer(new RejectAndDontRequeueRecoverer())`.

Put together: when `AiSignalListener` throws while handling a message, Spring's retry interceptor
catches it **in-process**, right there in the same consumer thread, and calls the listener method
again — up to 3 attempts total for that one message, with no RabbitMQ-level requeue happening in
between. If all 3 attempts throw, `RejectAndDontRequeueRecoverer` rejects the message with
`requeue=false`. A rejected-without-requeue message on a queue that has dead-letter arguments
configured is exactly the trigger condition for RabbitMQ to auto-forward it to the dead-letter
exchange — which is `ai.signals.dlx`, which routes it (via the binding) into `ai.signals.dlq`.

So: **a message that fails 3 times ends up sitting in `ai.signals.dlq`, untouched, forever** (or
until something reads/purges it). Nothing currently consumes `ai.signals.dlq` — there's no
`@RabbitListener` on it. It's a holding pen, visible and inspectable via the RabbitMQ management UI
at `localhost:15672`, but not currently acted on automatically.

### What you can change

**Yes — you can route dead-lettered messages somewhere else, or do something with them.** Nothing
about the current design locks that down; a DLQ is just a regular queue. Some concrete options,
roughly in order of how much they'd change:

1. **Add a consumer on `ai.signals.dlq`.** The simplest change: write another `@RabbitListener`
   (in `orchestrator`, or even a small separate script) that consumes from `ai.signals.dlq` and
   does something useful — log it somewhere more visible, page someone, write a row to a
   `failed_signals` table for manual review, or attempt a repair-and-republish. This requires no
   change to `RabbitConfig.java` at all.

2. **Route dead letters to a *different* queue than today**, by changing the `x-dead-letter-routing-key`
   argument on `ai.signals`'s declaration (or adding a second binding on `ai.signals.dlx` with a
   different routing key and having your producer/retry logic pick which one). For example, you
   could split into `ai.signals.dlq.retryable` (things worth trying again later, maybe after a
   delay) vs. `ai.signals.dlq.poison` (malformed messages, never going to succeed) by having the
   listener distinguish the failure type and nack with different routing, though that requires
   manual `channel.basicPublish`/`basicNack` control rather than the declarative dead-letter
   arguments alone.

3. **Delayed retry instead of dead-lettering immediately.** Right now, all 3 retries happen
   back-to-back in the same method invocation, with no delay — if the failure is transient (e.g. a
   momentary DB connection blip), a fixed delay between attempts would help. Spring's retry
   interceptor supports a `BackOffPolicy` (e.g. `ExponentialBackOffPolicy`) that could be added to
   `retryInterceptor()` without restructuring anything else. A more elaborate pattern (common in
   RabbitMQ specifically) is a "delay queue": a queue with a message TTL and *its own* dead-letter
   config pointing back at `ai.signals` — a message sitting there for N seconds "expires" and gets
   dead-lettered back into the real processing queue, effectively implementing a delayed retry
   without polling or a scheduler.

4. **Change the exchange type.** If a second consumer ever needs a copy of every `ai.signals`
   message (e.g. an analytics pipeline that wants to log all sentiment results, separate from
   `orchestrator`'s DB write), you'd introduce a real named exchange — likely a `fanout` exchange —
   and bind *two* queues to it (`ai.signals.orchestrator`, `ai.signals.analytics`), with the
   producer publishing once to the exchange instead of directly to a named queue. This is the
   natural next step if "one producer, one consumer" ever becomes "one producer, many consumers,"
   and it wouldn't require touching `ai-agent`'s publish call much — just the exchange/binding setup
   and the routing key/exchange name it publishes to.

5. **Increase/decrease `MAX_RETRY_ATTEMPTS`.** It's a single constant in `RabbitConfig.java`.
   Higher tolerates more transient flakiness before giving up; lower dead-letters faster. There's
   no strong reason today's value of 3 is correct beyond "a reasonable default" — it wasn't tuned
   against real failure data yet.

6. **Message TTL.** You could add an `x-message-ttl` argument to `ai.signals` itself, so a message
   that sits unconsumed for too long (e.g. `orchestrator` was down for hours) is dead-lettered
   automatically rather than being processed very late against possibly-stale context. Currently
   there's no TTL — a message will wait indefinitely for a consumer.

7. **Queue length limits / priority.** Not relevant at today's volume (at most one message per
   open market per 15 minutes), but if this ever became high-throughput, `x-max-length` (cap queue
   size, drop or dead-letter overflow) and priority queues (`x-max-priority`, letting some messages
   jump the queue) are both standard RabbitMQ features you could layer on without changing the
   overall architecture.

### What would need to change together

A few of the above interact with each other, worth keeping in mind if you touch this:

- If you add a second consumer via a fanout exchange (#4), the *producer* side (`publisher.py`)
  needs to start publishing to that named exchange instead of using the default exchange shortcut
  — right now it relies on the default exchange's implicit "routing key = queue name" behavior,
  which only works for the single-queue case.
- Any change to `ai.signals`'s declared arguments (TTL, new DLX routing key, max length) needs to
  happen in **both** places that declare it (`RabbitConfig.java` and `publisher.py`) — or, better,
  make `publisher.py` stop declaring the queue with conflicting semantics at all, since (per above)
  `orchestrator` is the actual owner of the queue's configuration. The cleanest fix, if this
  bothers you, is to have `ai-agent` not declare the queue at all and just publish — RabbitMQ
  doesn't require the publisher to declare a queue it's not consuming from, as long as something
  else guarantees the queue exists before messages arrive (which `orchestrator`'s `@Bean` already
  does, since the Spring context — and therefore the queue declaration — comes up as part of the
  Docker Compose service dependency graph before `ai-agent` would have any market to analyze).
- If you consume `ai.signals.dlq` (#1) and want to *retry into the real pipeline* rather than just
  logging, be careful not to recreate the same failure loop — re-publishing straight back onto
  `ai.signals` for a message that will deterministically fail the same way just moves the infinite
  loop from "in RabbitMQ" to "between two queues."
