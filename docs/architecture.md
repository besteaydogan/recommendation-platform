# Recommendation Platform Architecture

## Modular monolith

Recommendation Platform is a modular monolith: one deployable Spring Boot application with package boundaries organized by business capability. A package may be extracted into a separate service later only when operational requirements justify the additional deployment and coordination cost.

## Feature areas

- `history` owns browsing-history storage, lookup, deletion, and history-specific rules.
- `recommendation` owns recommendation strategy selection, bestseller calculation, and recommendation constraints.
- `catalog` provides product and category concepts plus their persistence access.
- `messaging` owns Kafka event contracts, publishing, and consumption; it contains no history or recommendation business rules.
- `common` is limited to genuinely cross-cutting framework configuration and API error handling concerns.

## Dependency direction

Within a feature, dependencies point toward the domain:

```text
api
  ↓
application
  ↓
domain

infrastructure
  └── implements or supports application/domain requirements
```

Cross-feature collaboration follows explicit application or query boundaries:

```text
history.application
    ↓
catalog domain/query abstractions

recommendation.application
    ↓
history and catalog query abstractions

messaging.consumer
    ↓
history.application
```

Controllers must not access JPA repositories directly. Domain packages must not depend on Spring, Kafka, or database implementations. Recommendation code must not depend on REST controllers, and history code must not depend on recommendation infrastructure.

## Product-view ingestion

```text
product-views.json (JSON Lines, read-only)
    → asynchronous producer (user ID key)
    → configured product-views Kafka topic
    → validating consumer
    → transactional product_views insert
```

The database unique constraint on `message_id` is the final duplicate-event guard. The source file is never rewritten.

Kafka records use the event `userId` as their key. The producer preserves source message IDs, adds a UTC view timestamp, publishes records at the configured interval, and stops at end-of-file. Malformed source records and invalid consumed events are logged and skipped. Duplicate message IDs are treated as successful no-ops, including database-constraint races, so they do not cause endless listener failure.

## Recommendation flow

```text
product_views + products
    → top three categories in PostgreSQL

orders + order_items + products
    → distinct-buyer ranking over the provided monthly order dataset
    → general cache or category-filtered query
    → recommendation API
```

The cache stores only the general top ten. Personalized requests execute one combined, parameterized category query so cross-category ranking remains correct.

The assignment-provided `orders` table is the last-month data boundary and has no timestamp column. Recommendation Platform does not claim or fabricate a rolling SQL cutoff. A production source containing historical orders would need an indexed order timestamp and an explicit cutoff predicate.

## Scheduler and cache

The configuration-driven scheduler loads the general top ten at application startup and refreshes it periodically when enabled. Cache replacement is atomic, and a failed refresh keeps the previous immutable list. Personalized rankings are queried directly so multiple selected categories are ranked together by distinct buyer count.

## Failure behavior

HTTP path validation failures return a small 400 response with `timestamp`, `status`, `error`, `message`, and `path`. Unexpected HTTP exceptions are logged and return the same shape with status 500 and a generic message; stack traces and infrastructure details are never included in the response. Missing history, missing recommendation data, and idempotent history deletion remain normal 200/204 outcomes.

Kafka failures remain isolated from HTTP handling. Invalid Kafka events are skipped, duplicate messages are acknowledged as idempotent no-ops, and unknown product IDs are retained because the verified schema has no product foreign key.
