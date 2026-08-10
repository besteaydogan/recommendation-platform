# RecoFlow

RecoFlow is a Spring Boot recommendation backend combining Kafka-based product-view ingestion with PostgreSQL bestseller queries and REST APIs.

## Assignment requirements covered

- Finite JSON Lines product-view producer
- Kafka stream reader with message-ID idempotency
- Browsing-history query API
- Browsing-history product deletion API
- Personalized category-based bestseller strategy
- General bestseller strategy
- Minimum-five recommendation rule
- Focused unit and PostgreSQL integration tests
- Containerized PostgreSQL, Kafka, and application runtime

## Architecture

```text
product-views.json
        ↓
View Producer
        ↓
Kafka: product-views
        ↓
Stream Reader
        ↓
PostgreSQL: product_views
        ↓
History and Recommendation APIs

orders + order_items + products
        ↓
Bestseller SQL
        ↓
Recommendation Service
```

RecoFlow is one deployable modular monolith. See [architecture details](docs/architecture.md) and [database details](docs/database.md).

## Technology choices

- **Java 21:** Provides the language and runtime baseline.
- **Spring Boot:** Hosts configuration, REST endpoints, validation, scheduling, persistence, and Kafka integration.
- **Kafka:** Carries product-view events keyed by user ID.
- **PostgreSQL:** Stores browsing history and executes category and bestseller aggregations.
- **Flyway:** Validates and applies versioned schema migrations.
- **Docker Compose:** Runs PostgreSQL, Kafka, and RecoFlow together.
- **JUnit 5, Mockito, and Testcontainers:** Cover unit, web-slice, and PostgreSQL repository behavior.

## Database assumptions

- `products`, `orders`, and `order_items` are assignment-owned source tables.
- `product_views` is owned by RecoFlow.
- The provided `orders` dataset represents the last-month purchase window.
- The verified source schema has no order timestamp, so runtime rolling-window filtering is not possible.
- A production dataset containing historical orders would require an indexed timestamp and an explicit cutoff predicate.
- Unknown product-view IDs are retained because `product_views.product_id` intentionally has no product foreign key.

## Configuration

Copy `.env.example` to `.env` for local overrides. Duration values accept Spring Boot formats such as `1s`, `30s`, and `1000ms`.

### Application

| Variable | Purpose | Default |
| --- | --- | --- |
| `APP_PORT` | RecoFlow HTTP port | `8080` |

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

## Run

From the repository root:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

Verify application health using the configured port:

```powershell
Invoke-RestMethod http://localhost:<APP_PORT>/actuator/health
```

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

## Testing

From the application directory:

```powershell
cd recoflow
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Repository tests use PostgreSQL through Testcontainers and therefore require Docker.

## Known limitations

- One deployable modular monolith
- In-memory general bestseller cache with no distributed cache
- No authentication
- No historical rolling-window filter because the source schema has no order timestamp
- Unknown product-view IDs are retained according to the documented schema decision
- Not production-ready
