# Transaction Aggregation API

A Spring Boot service that aggregates customer financial transactions from multiple SA bank mock providers, applies rule-based categorization, and exposes a REST analytics API.

---

## Tech Stack

| Layer        | Technology                                  |
|--------------|---------------------------------------------|
| Runtime      | Java 21, Spring Boot 3.5                    |
| Persistence  | PostgreSQL 16, Spring Data JPA, Flyway      |
| API          | OpenAPI 3 (contract-first), SpringDoc UI    |
| Security     | Spring Security, OAuth2 JWT Resource Server |
| Observability| Spring Actuator, Micrometer, Prometheus     |
| Build        | Maven 3.9                                   |

---

## Prerequisites

| Tool              | Minimum version |
|-------------------|-----------------|
| Java              | 21              |
| Maven             | 3.9             |
| Docker            | 24              |
| Docker Compose    | 2.x             |

---

## Build

### Compile and package (skip tests)

```bash
mvn clean package -DskipTests
```

### Compile, package, and run all tests

```bash
mvn clean install
```

Tests use an H2 in-memory database — no running PostgreSQL required.

---

## Run

### Option 1 — Docker Compose (recommended)

Starts PostgreSQL and the application together.

```bash
docker compose up --build
```

On first run Maven downloads dependencies inside the container (~2 min). Subsequent builds reuse the layer cache.

| Endpoint        | URL                                   |
|-----------------|---------------------------------------|
| Application     | http://localhost:8080                 |
| Swagger UI      | http://localhost:8080/swagger-ui.html |
| Health check    | http://localhost:8080/actuator/health |
| Prometheus      | http://localhost:8080/actuator/prometheus |

To stop and remove containers:

```bash
docker compose down
```

To also remove the database volume (wipes all data):

```bash
docker compose down -v
```

### Option 2 — Local (requires PostgreSQL running)

```bash
# 1. Start only the database
docker compose up postgres -d

# 2. Run the application
mvn spring-boot:run
```

---

## JWT Authentication

All API endpoints require a Bearer JWT token except `/actuator/health` and `/actuator/prometheus`.

### Local testing (docker-compose)

`docker compose up --build` starts a **mock OAuth2 server** on port 9000 alongside the app.
Get a token and call the API in three commands:

```bash
# 1. Fetch a token from the mock issuer
TOKEN=$(curl -s -X POST http://localhost:9000/default/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test&client_secret=test" \
  | jq -r .access_token)

# 2. Use it
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/customers/CUST-001/summary
```

> `jq` is used above to parse the JSON response. Install with `brew install jq` (macOS) or `apt install jq` (Linux). Alternatively paste the full JSON response into a JWT decoder and copy `access_token` manually.

The mock server endpoints:

| Purpose        | URL                                          |
|----------------|----------------------------------------------|
| Token endpoint | `POST http://localhost:9000/default/token`   |
| JWKS endpoint  | `GET  http://localhost:9000/default/jwks`    |

---

## Test

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CategorizationServiceTest

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

Test breakdown:

| Class                        | Type            | What it covers                                      |
|------------------------------|-----------------|-----------------------------------------------------|
| `CategorizationServiceTest`  | Unit            | 21 merchant → category mappings, null/edge cases    |
| `ConnectorTest`              | Unit            | FNB, ABSA, Capitec connectors return valid data     |
| `IngestionServiceTest`       | Unit            | Save, dedup, categorization, fault isolation        |
| `TransactionServiceTest`     | Unit            | Customer lookup, pagination, spend/income queries   |
| `CustomerRepositoryTest`     | `@DataJpaTest`  | Customer CRUD and unique constraint                 |
| `TransactionRepositoryTest`  | `@DataJpaTest`  | All 6 custom JPQL queries against H2                |
| `CustomerApiTest`            | `@WebMvcTest`   | All 4 endpoints — 200 / 401 / 404 / 400 responses  |

---

## API Reference

Full interactive docs at `http://localhost:8080/swagger-ui.html`.

### Seeded customers (loaded via Flyway V2)

| Customer Number | Name            |
|-----------------|-----------------|
| `CUST-001`      | Thabo Nkosi     |
| `CUST-002`      | Naledi Dlamini  |
| `CUST-003`      | Sipho Mthembu   |

### Endpoints

#### Customer Summary
```
GET /customers/{customerId}/summary
```
Returns current-month spend, income, and account count.

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/customers/CUST-001/summary
```
```json
{
  "customerId": "CUST-001",
  "totalAccounts": 2,
  "monthlySpend": 2654.00,
  "monthlyIncome": 28500.00
}
```

#### Transaction Search
```
GET /customers/{customerId}/transactions?from=&to=&category=&page=&size=
```

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/customers/CUST-001/transactions?category=GROCERIES&page=0&size=10"
```
```json
{
  "content": [
    {
      "transactionId": "...",
      "merchant": "Shoprite Jabulani Mall",
      "amount": -320.00,
      "currency": "ZAR",
      "category": "GROCERIES",
      "transactionDate": "2026-06-09T08:00:00Z",
      "sourceSystem": "FNB"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

#### Category Breakdown
```
GET /customers/{customerId}/categories?from=&to=
```

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/customers/CUST-001/categories?from=2026-06-01&to=2026-06-30"
```
```json
{
  "customerId": "CUST-001",
  "categories": [
    { "category": "GROCERIES",     "amount": -1110.00, "transactionCount": 3 },
    { "category": "FUEL",          "amount":  -750.00, "transactionCount": 1 },
    { "category": "ENTERTAINMENT", "amount":  -199.00, "transactionCount": 1 }
  ]
}
```

#### Monthly Spend
```
GET /customers/{customerId}/monthly-spend?month=YYYY-MM
```

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/customers/CUST-001/monthly-spend?month=2026-06"
```
```json
{
  "customerId": "CUST-001",
  "month": "2026-06",
  "amount": 2059.00
}
```

---

## Transaction Ingestion

Ingestion runs automatically every 5 minutes (configurable via `ingestion.interval-ms`).

**Flow:** Fetch → Categorize → Upsert (idempotent, deduplicated by `external_id + source_system`)

### Mock Sources

| Connector        | Source name | Mock transactions                          |
|------------------|-------------|--------------------------------------------|
| `FnbConnector`   | `FNB`       | Shoprite, Shell, Nando's, salary, Uber     |
| `AbsaConnector`  | `ABSA`      | Checkers, Showmax, Bolt, Chicken Licken, Engen |
| `CapitecConnector` | `CAPITEC` | Pick n Pay, Engen, DStv, Steers, freelance |

### Categorization Rules

| Category        | Matched merchants                                        |
|-----------------|----------------------------------------------------------|
| `TRANSPORT`     | Uber, Bolt, Taxify                                       |
| `GROCERIES`     | Woolworths, Checkers, Pick n Pay, Shoprite, Spar         |
| `FUEL`          | Shell, Engen, Sasol, Caltex, BP                          |
| `ENTERTAINMENT` | Netflix, Spotify, DStv, Showmax, Ster-Kinekor            |
| `DINING`        | Nando's, Steers, Chicken Licken, KFC, McDonald           |
| `UTILITIES`     | Eskom, Rand Water, City Power, Joburg Water              |
| `UNCATEGORIZED` | everything else                                          |

**To add a rule:** add one entry to `CategorizationService.RULES` — no new files, no wiring.

---

## Configuration Reference

Key properties in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/transactions_db` | Database URL (overridden to `postgres` in docker profile) |
| `spring.security.oauth2.resourceserver.jwt.jwks-uri` | `http://localhost:9000/default/jwks` | JWKS endpoint for token validation |
| `ingestion.interval-ms` | `300000` | Ingestion interval in ms (5 min) |

Environment variables accepted by Docker Compose:

```
DB_HOST       postgres host       (default: postgres)
DB_PORT       postgres port       (default: 5432)
DB_NAME       database name       (default: transactions_db)
DB_USERNAME   database user       (default: txn_user)
DB_PASSWORD   database password   (default: txn_pass)
JWKS_URI      JWKS endpoint URL   (default: http://mock-oauth2:8080/default/jwks)
```

---


## Project Structure

```
src/main/java/za/co/samrepa/
├── TransactionAggregationApplication.java
├── categorization/        CategorizationService (Map-based rules)
├── config/                SecurityConfig (JWT resource server)
├── connector/             SourceConnector + FNB / ABSA / Capitec mocks
├── delegate/              CustomerApiDelegateImpl (OpenAPI delegate)
├── entity/                Customer, Account, Transaction (JPA)
├── exception/             GlobalExceptionHandler, CustomerNotFoundException
├── mapper/                TransactionMapper (plain @Component)
├── repository/            Spring Data JPA repositories
├── scheduler/             IngestionScheduler (@Scheduled)
└── service/               IngestionService, TransactionService
```
