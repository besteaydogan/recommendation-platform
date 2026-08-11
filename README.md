# recommendation-platform

Recommendation Platform is a Java 21/Spring Boot backend that turns product-view events into browsing history and personalized product recommendations. It demonstrates reliable Kafka ingestion, database-level idempotency, PostgreSQL ranking queries, observability, and automated testing in one containerized project.

## Project at a glance

### What does it do?

The application streams product views from a JSONL file into Kafka, stores each event as browsing history, and exposes REST endpoints for history management and recommendations. Users with history receive category-based personalized bestsellers; users without history receive general bestsellers.

### Why Kafka?

Kafka separates event production from database processing and provides a durable buffer between them. This lets ingestion and persistence run independently while supporting delivery acknowledgements, consumer retries, dead-letter handling, lag measurement, and future horizontal scaling. Events are keyed by user ID to preserve per-user ordering within a partition.

### How are failures handled?

- The producer checks every asynchronous `KafkaTemplate.send()` result through its `CompletableFuture`. A failed delivery stops the publication run instead of reporting false success.
- The consumer retries persistence failures with configurable fixed backoff. After the configured attempts are exhausted—three by default—the event is published to `<topic>.DLT`.
- Micrometer counters expose consumed events, consumer failures, and DLT recoveries through the Prometheus endpoint.

### How is idempotency guaranteed?

Every event carries a UUID message ID. PostgreSQL enforces a unique constraint on `product_views.message_id`, while the repository writes with `INSERT ... ON CONFLICT (message_id) DO NOTHING`. Duplicate deliveries and concurrent consumers therefore cannot create duplicate history rows; the database is the final consistency boundary.

### How are recommendations produced?

The service selects up to three categories from a user's browsing history, then ranks products in those categories by distinct purchasing users. Without browsing history, it uses a periodically refreshed general-bestseller cache. Results are capped at ten products and returned as an empty list when fewer than five products qualify.

### Architecture

```text
Product View
     ↓
 Producer
     ↓
  Kafka
     ↓
 Consumer
     ↓
PostgreSQL
     ↓
Recommendation Service
     ↓
 REST API
```

### How do I run it?

Docker is the only runtime prerequisite. From the repository root:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

The finite file producer is disabled by default. Set `PRODUCT_VIEW_PRODUCER_ENABLED=true` in `.env` to ingest `product-views.json` when the application starts.

### How do I test it?

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

The suite contains unit, MVC, Kafka failure-handling, and PostgreSQL integration tests. Testcontainers-based tests require Docker. The verified suite currently contains 59 passing tests, and the included local load test processed 100,000 events with zero final lag, failures, or DLT records.

Recommendation Platform is one deployable modular monolith. See [architecture details](docs/architecture.md) and [database details](docs/database.md).

## Technology choices

- **Java 21:** Provides the language and runtime baseline.
- **Spring Boot:** Hosts configuration, REST endpoints, validation, scheduling, persistence, and Kafka integration.
- **Kafka:** Carries product-view events keyed by user ID.
- **PostgreSQL:** Stores browsing history and executes category and bestseller aggregations.
- **Flyway:** Validates and applies versioned schema migrations.
- **Docker Compose:** Runs PostgreSQL, Kafka, and the application together.
- **JUnit 5, Mockito, and Testcontainers:** Cover unit, web-slice, and PostgreSQL repository behavior.

## Database assumptions

- `products`, `orders`, and `order_items` are assignment-owned source tables.
- `product_views` is owned by Recommendation Platform.
- The provided `orders` dataset represents the last-month purchase window.
- The verified source schema has no order timestamp, so runtime rolling-window filtering is not possible.
- A production dataset containing historical orders would require an indexed timestamp and an explicit cutoff predicate.
- Unknown product-view IDs are retained because `product_views.product_id` intentionally has no product foreign key.

## Configuration

Copy `.env.example` to `.env` for local overrides. Duration values accept Spring Boot formats such as `1s`, `30s`, and `1000ms`.

### Application

| Variable | Purpose | Default |
| --- | --- | --- |
| `APP_PORT` | Application HTTP port | `8080` |

### Database

| Variable | Purpose | Default |
| --- | --- | --- |
| `POSTGRES_IMAGE` | PostgreSQL container image | `postgres:17.9-alpine` |
| `POSTGRES_PORT` | PostgreSQL host port | `5433` |
| `DB_HOST` | Database host outside Compose | `localhost` |
| `DB_PORT` | Database port outside Compose | `5433` |
| `DB_NAME` | Database name | `recoflow` |
| `DB_USERNAME` | Database user | `recoflow` |
| `DB_PASSWORD` | Local database password | `recoflow_dev_password` |

### Kafka

| Variable | Purpose | Default |
| --- | --- | --- |
| `KAFKA_IMAGE` | Kafka container image | `apache/kafka:3.9.1` |
| `KAFKA_PORT` | Kafka host port | `9092` |
| `KAFKA_BOOTSTRAP_SERVERS` | Bootstrap address outside Compose | `localhost:9092` |
| `KAFKA_PRODUCT_VIEWS_TOPIC` | Product-view topic | `product-views` |
| `KAFKA_PRODUCT_VIEWS_CONSUMER_GROUP` | Product-view consumer group | `recoflow-product-views` |
| `KAFKA_CONSUMER_RETRY_MAX_ATTEMPTS` | Total consumer delivery attempts before DLT | `3` |
| `KAFKA_CONSUMER_RETRY_BACKOFF` | Fixed delay between consumer attempts | `1s` |

After the configured attempts are exhausted, failed product-view records are published to `<product-view-topic>.DLT`.

### Producer

| Variable | Purpose | Default |
| --- | --- | --- |
| `PRODUCT_VIEW_PRODUCER_ENABLED` | Enables the finite file producer | `false` |
| `PRODUCT_VIEWS_FILE_PATH` | JSON Lines input path | `/app/data/product-views.json` |
| `PRODUCT_VIEW_INTERVAL_MS` | Delay between records | `1s` |

### Bestseller scheduler

| Variable | Purpose | Default |
| --- | --- | --- |
| `BESTSELLER_REFRESH_ENABLED` | Enables general bestseller refresh | `true` |
| `BESTSELLER_REFRESH_INTERVAL_MS` | Refresh interval | `30s` |

## Operations and observability

Verify application health using the configured port:

```powershell
Invoke-RestMethod http://localhost:<APP_PORT>/actuator/health
```

Prometheus metrics are exposed at:

```text
http://localhost:<APP_PORT>/actuator/prometheus
```

Application metrics include Kafka consumer deliveries and failures, DLT recoveries,
recommendation request count, and recommendation latency.

## API examples

### Browsing history

```http
GET /users/user-120/history
```

```json
{
  "user-id": "user-120",
  "products": ["product-10", "product-20"],
  "type": "personalized"
}
```

Missing history returns HTTP 200 with an empty `products` array.

### Delete history product

```http
DELETE /users/user-120/history/product-10
```

The endpoint removes every matching occurrence and returns HTTP 204. Repeating the request also returns HTTP 204.

### Recommendations

```http
GET /users/user-120/recommendations
```

```json
{
  "user-id": "user-120",
  "products": ["product-170", "product-83", "product-158", "product-45", "product-116"],
  "type": "personalized"
}
```

Valid requests return HTTP 200. Validation failures and unexpected HTTP failures use the shared `timestamp`, `status`, `error`, `message`, and `path` response fields without exposing implementation details.

## Recommendation rules

- Select at most three categories from browsing history.
- Rank categories by view count, latest view time, then category ID.
- Rank products by distinct purchasing users in the provided monthly order dataset.
- Use only selected categories for personalized results; use all categories for general results.
- Order ties by product ID and return at most ten products.
- Return an empty array when fewer than five products qualify.
- Do not fall back to general results when a personalized result is insufficient.

## Local ingestion load test

The repository includes a small, repeatable end-to-end load test rather than a JMeter project. It starts an isolated Docker Compose project, streams the existing 100,000-line JSONL file through the application producer, waits until PostgreSQL contains all records and the Kafka consumer-group lag reaches zero, and saves the measurements to `target/load-test-result.json`.

Run it from the repository root:

```powershell
.\scripts\run-load-test.ps1
```

The load-test stack uses the separate ports and volumes in `load-test.env`; it does not remove the normal development stack's data. It is removed automatically after the run. Pass `-KeepRunning` to retain it for manual inspection.

### Recorded baseline

The following numbers are from a local Docker Desktop run on August 11, 2026, using one Kafka partition, Kafka 3.9.1, PostgreSQL 17.9, Java 21, and a `1ms` producer interval:

| Measurement | Result |
| --- | ---: |
| Input events | 100,000 |
| Persisted rows | 100,000 |
| End-to-end processing time | 173.219 s |
| Average throughput | 577.3 events/s |
| Final consumer lag | 0 |
| Consumer failures | 0 |
| DLT metric count | 0 |
| DLT topic records | 0 |

The timer starts at the producer's `Started streaming` log and stops only after both the database row count reaches 100,000 and consumer lag reaches zero. Image build and service health-check time are excluded. These are local baseline numbers, not a production capacity guarantee; the producer intentionally waits for each Kafka delivery result and the consumer persists each event in its own transaction.

## Known limitations

- One deployable modular monolith
- In-memory general bestseller cache with no distributed cache
- No authentication
- No historical rolling-window filter because the source schema has no order timestamp
- Unknown product-view IDs are retained according to the documented schema decision
- Not production-ready
