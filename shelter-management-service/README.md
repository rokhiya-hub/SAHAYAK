# shelter-management-service

Disaster relief shelter management microservice. Part of the **Sahayak** disaster-relief platform.

Track shelters, their capacity and the resources they provide so relief workers can quickly find open shelters and register residents during a disaster.

## Tech Stack

- Java 25
- Spring Boot 3.5.x (Web, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator)
- PostgreSQL for persistence (H2 in-memory available for local dev / tests)
- JWT (HS256) authentication
- Lombok
- JUnit 5

## Getting Started

The default configuration expects PostgreSQL at `localhost:5432` (database/user/password `sahayak`). The easiest way is via Docker:

```bash
# Start PostgreSQL + the app together
docker compose up --build
```

Prefer running the app locally?

```bash
# 1. Start only PostgreSQL
docker compose up -d postgres

# 2. Build, test and start the app
mvn clean test
mvn spring-boot:run
```

On startup the service seeds 5 sample shelters (via `DataSeeder`, only when the table is empty). All config is driven by environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`), so the same image works in any environment.

### Local dev without Docker

Use the `h2` profile for a zero-setup in-memory database (H2 console at `/h2-console`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8082` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sahayak` | JDBC connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sahayak` / `sahayak` | Database credentials |
| `JWT_SECRET` | dev placeholder | HMAC-SHA256 key for signing tokens (>= 32 bytes; override in production) |
| `jwt.expiration-ms` | `86400000` (24h) | Token lifetime |

## Authentication

All `/api/shelters/**`, `/api/shelters/{id}/**` and check-in/check-out endpoints require a Bearer token. `POST /api/auth/register`, `POST /api/auth/login` and `GET /actuator/health` are public.

```bash
# Register a relief worker
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"reliefworker","password":"password123"}'

# Login -> returns { token, tokenType, expiresInSeconds, username, role }
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"reliefworker","password":"password123"}'

# Use the token for protected calls
curl http://localhost:8082/api/shelters \
  -H "Authorization: Bearer <token>"
```

Passwords are stored hashed with BCrypt. Login failures return `401`, duplicate usernames `409`.

## API

Base path: `http://localhost:8082/api/shelters` (all endpoints below require `Authorization: Bearer <token>`)

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/shelters` | Paginated, queryable shelter listing |
| GET | `/api/shelters/{id}` | Get a single shelter |
| POST | `/api/shelters` | Create a shelter |
| PUT | `/api/shelters/{id}` | Update a shelter (full update) |
| DELETE | `/api/shelters/{id}` | Delete a shelter |
| POST | `/api/shelters/{id}/checkin` | Register one resident |
| POST | `/api/shelters/{id}/checkout` | Release one spot |
| GET | `/api/shelters/available` | Open shelters with capacity (optionally by proximity) |
| POST | `/api/auth/register` | Create an account (public) |
| POST | `/api/auth/login` | Obtain a JWT (public) |

### Querying the listing

`GET /api/shelters` supports pagination, sorting and filtering:

```
GET /api/shelters?search=mg+road&status=OPEN&page=0&size=25&sort=name,asc
```

| Param | Description |
|-------|-------------|
| `search` | Case-insensitive match on name or address |
| `status` | `OPEN`, `FULL` or `CLOSED` |
| `page`, `size`, `sort` | Spring Data pagination (`sort=field,direction`, repeatable) |

Response shape:

```json
{
  "content": [ { ...shelter... } ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Proximity search

`GET /api/shelters/available` filters open shelters in the database using a Haversine distance query:

```bash
# At least 50 free spots, within 25 km of a location, nearest first
curl "http://localhost:8082/api/shelters/available?minCapacity=50&lat=12.9716&lng=77.5946&radiusKm=25"
```

Each result includes `distanceKm`. Without `lat`/`lng` it lists open shelters sorted by available capacity (descending).

### Shelter status

Status is derived automatically from capacity: `OPEN` (has free spots), `FULL` (occupancy == capacity), or `CLOSED` (manually closed, rejects check-ins).

### Error responses

```json
{
  "timestamp": "2026-09-23T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Shelter 2 is at full capacity",
  "path": "/api/shelters/2/checkin",
  "fieldErrors": null
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation failed (`fieldErrors` has per-field messages) |
| 401 | Missing/invalid token or wrong credentials |
| 404 | Shelter not found |
| 409 | Shelter full/closed/empty, duplicate username, or capacity rule violated |

## Concurrency

`Shelter` uses a JPA `@Version` column for optimistic locking. `checkin`/`checkout` flush immediately inside the transaction, so simultaneous requests against the same shelter are safely serialized: one succeeds, the rest see `ShelterFullException` (HTTP 409) or a stale-version conflict (also HTTP 409).

## Packaging / Containers

- `mvn package` produces a runnable Spring Boot jar
- `Dockerfile` multi-stage build (Maven + JDK 25 → JRE 25)
- `docker-compose.yml` runs PostgreSQL + the app
- `.github/workflows/ci.yml` runs `mvn clean verify` on Java 25 for every push/PR

## Project Layout

```
src/main/java/com/drrcp/sheltermanagement/
├── api/            ApiError response body
├── config/         SecurityConfig + DataSeeder
├── controller/     REST endpoints (shelters + auth)
├── domain/         Shelter, User entities + ShelterStatus
├── dto/            Request/response objects
├── exception/      Custom exceptions + GlobalExceptionHandler
├── repository/     Spring Data JPA repositories
├── security/       JwtService (issue/validate tokens)
└── service/        Business logic (ShelterService)
```

## Running Tests

`mvn clean test` uses an in-memory H2 database (no PostgreSQL required) and covers:

- `ShelterServiceTest` – CRUD, pagination/search/status filters, checkin/checkout, capacity rules, proximity search
- `ShelterControllerTest` – API layer, query-param forwarding, validation errors, error mapping
- `SecurityIntegrationTest` – JWT register/login flow, 401 handling
- `ShelterServiceConcurrencyTest` – concurrent checkins against a full shelter