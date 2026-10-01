# victim-registration-service

Disaster relief victim registration microservice. Part of the **Sahayak** disaster-relief platform.

Register affected people, track their needs and shelter assignments, and coordinate shelter check-in with the shelter-management service.

## Tech Stack

- Java 25
- Spring Boot 3.5.x (Web, WebFlux client, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator)
- PostgreSQL for persistence (H2 in-memory available for local dev / tests)
- JWT (HS256) authentication
- Lombok
- JUnit 5 and MockWebServer

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

The service listens on port `8081`. It does not seed personal victim records. Configuration is driven by environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SHELTER_SERVICE_URL`).

### Local dev without Docker

Use the `h2` profile for a zero-setup in-memory database (H2 console at `/h2-console`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sahayak` | JDBC connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sahayak` / `sahayak` | Database credentials |
| `JWT_SECRET` | dev placeholder | HMAC-SHA256 key (>= 32 bytes); use the same value as shelter-management-service to forward its users' tokens |
| `SHELTER_SERVICE_URL` | `http://localhost:8082` | Base URL for shelter-management-service |
| `shelter-service.connect-timeout` | `2s` | Maximum time to establish the shelter-service connection |
| `shelter-service.response-timeout` | `3s` | Maximum wait for the shelter-service response |
| `jwt.expiration-ms` | `86400000` (24h) | Token lifetime |

## Authentication

All `/api/victims/**` endpoints require a Bearer token. `POST /api/auth/register`, `POST /api/auth/login` and `GET /actuator/health` are public.

```bash
# Register a relief worker
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"reliefworker","password":"password123"}'

# Login -> returns { token, tokenType, expiresInSeconds, username, role }
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"reliefworker","password":"password123"}'

# Use the token for protected calls
curl http://localhost:8081/api/victims \
  -H "Authorization: Bearer <token>"
```

Passwords are stored hashed with BCrypt. Login failures return `401`, duplicate usernames `409`. The assignment endpoint forwards the caller's Bearer token to shelter-management-service, preserving the authenticated user's identity and authorization context; both services must use the same `JWT_SECRET` for the shelter service to validate it.

## API

Base path: `http://localhost:8081/api/victims` (all victim endpoints require `Authorization: Bearer <token>`)

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/victims` | Paginated victim listing, searchable by name |
| GET | `/api/victims/{id}` | Get a single victim |
| POST | `/api/victims` | Register a victim |
| PUT | `/api/victims/{id}` | Update a victim (full update) |
| DELETE | `/api/victims/{id}` | Delete a victim |
| PATCH | `/api/victims/{id}/assign-shelter/{shelterId}` | Check in to a shelter and record the assignment |
| POST | `/api/auth/register` | Create an account (public) |
| POST | `/api/auth/login` | Obtain a JWT (public) |

### Querying the listing

`GET /api/victims` supports pagination, sorting and name search:

```
GET /api/victims?search=ananya&page=0&size=25&sort=fullName,asc
```

| Param | Description |
|-------|-------------|
| `search` | Case-insensitive match on full name |
| `page`, `size`, `sort` | Spring Data pagination (`sort=field,direction`, repeatable) |

Response shape:

```json
{
  "content": [ { ...victim... } ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Shelter assignment

`PATCH /api/victims/{id}/assign-shelter/{shelterId}` calls `POST /api/shelters/{shelterId}/checkin` before updating the victim's shelter and status. Shelter errors retain the upstream conflict message. If shelter-management-service is unavailable or times out, the endpoint returns `503` without changing the victim record.

## Validation and Errors

Aadhaar numbers must contain exactly 12 digits and be unique. Phone numbers must match the 10-digit Indian mobile pattern. Age must be non-negative and family size must be at least 1.

```json
{
  "timestamp": "2026-09-23T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Shelter 2 is at full capacity",
  "path": "/api/victims/3/assign-shelter/2",
  "fieldErrors": null
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation failed (`fieldErrors` has per-field messages) |
| 401 | Missing/invalid token or wrong credentials |
| 404 | Victim not found |
| 409 | Duplicate Aadhaar or shelter check-in conflict |
| 502 | Unexpected shelter-service response |
| 503 | Shelter service unavailable or timed out |

## Concurrency

`Victim` uses a JPA `@Version` column for optimistic locking. Shelter assignment also obtains a row lock for the victim during the inter-service check-in, preventing concurrent requests from assigning the same victim twice. The shelter service independently protects capacity with its own versioned check-in. These are separate databases and HTTP is not a distributed transaction: if the shelter accepts check-in but the victim database cannot commit afterward, manual reconciliation may be required.

## Packaging / Containers

- `mvn package` produces a runnable Spring Boot jar
- `Dockerfile` multi-stage build (Maven + JDK 25 → JRE 25)
- `docker-compose.yml` runs PostgreSQL + the app
- `.github/workflows/ci.yml` runs `mvn clean verify` on Java 25 for shelter and victim services

## Project Layout

```
src/main/java/com/drrcp/victimregistration/
├── api/            ApiError response body
├── config/         SecurityConfig + WebClientConfig
├── controller/     REST endpoints (victims + auth)
├── domain/         Victim, User, and enums
├── dto/            Request/response objects
├── exception/      Custom exceptions + GlobalExceptionHandler
├── repository/     Spring Data JPA repositories
├── security/       JwtService (issue/validate)
└── service/        Business logic (VictimService)
```

## Running Tests

`mvn clean test` uses an in-memory H2 database (no PostgreSQL required) and covers:

- `VictimServiceTest` – CRUD, pagination/search, duplicate Aadhaar handling
- `VictimSecurityIntegrationTest` – JWT register/login flow, 401 handling, validation errors
- `ShelterAssignmentClientTest` – caller-token forwarding, shelter 409 propagation, timeout/503 handling using MockWebServer