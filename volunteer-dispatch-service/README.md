# volunteer-dispatch-service

Disaster relief volunteer dispatch microservice. Part of the **Sahayak** disaster-relief platform.

Track volunteer availability, skills and location, then coordinate assignments with shelter-management-service.

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

The service listens on port `8084`. All config is driven by environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SHELTER_SERVICE_URL`).

### Local dev without Docker

Use the `h2` profile for a zero-setup in-memory database (H2 console at `/h2-console`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8084` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sahayak` | JDBC connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sahayak` / `sahayak` | Database credentials |
| `JWT_SECRET` | dev placeholder | HMAC-SHA256 key (>= 32 bytes); same value as shelter-management-service is required to forward caller tokens |
| `SHELTER_SERVICE_URL` | `http://localhost:8082` | Base URL for shelter-management-service |
| `shelter-service.connect-timeout` | `2s` | Maximum time to establish the shelter-service connection |
| `shelter-service.response-timeout` | `3s` | Maximum wait for shelter-service response |
| `jwt.expiration-ms` | `86400000` (24h) | Token lifetime |

## Authentication

All `/api/volunteers/**` and `/api/dispatch/**` endpoints require a Bearer token. `POST /api/auth/register`, `POST /api/auth/login` and `GET /actuator/health` are public.

```bash
# Register a dispatcher
curl -X POST http://localhost:8084/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"dispatcher","password":"password123"}'

# Login -> returns { token, tokenType, expiresInSeconds, username, role }
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"dispatcher","password":"password123"}'

# Use the token for protected calls
curl http://localhost:8084/api/volunteers \
  -H "Authorization: Bearer <token>"
```

Dispatch requests forward the caller's Bearer token to shelter-management-service, preserving the authenticated user's context. The shelter service must use the same `JWT_SECRET` to validate the token.

## API

Base path: `http://localhost:8084/api/volunteers` (all volunteer and dispatch endpoints require `Authorization: Bearer <token>`)

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/volunteers` | Paginated, searchable volunteer listing |
| GET | `/api/volunteers/{id}` | Get a single volunteer |
| POST | `/api/volunteers` | Register a volunteer |
| PUT | `/api/volunteers/{id}` | Update a volunteer (full update) |
| DELETE | `/api/volunteers/{id}` | Delete a volunteer |
| POST | `/api/dispatch/assign` | Verify shelter status/capacity and assign a volunteer |
| GET | `/api/dispatch/suggest?shelterId=5&skill=MEDICAL` | Rank available volunteers by skill and distance |
| POST | `/api/auth/register` | Create an account (public) |
| POST | `/api/auth/login` | Obtain a JWT (public) |

### Querying the listing

`GET /api/volunteers` supports pagination, sorting, name search and availability filtering:

```
GET /api/volunteers?search=asha&availabilityStatus=AVAILABLE&page=0&size=25&sort=name,asc
```

| Param | Description |
|-------|-------------|
| `search` | Case-insensitive match on volunteer name |
| `availabilityStatus` | `AVAILABLE`, `DISPATCHED` or `OFF_DUTY` |
| `page`, `size`, `sort` | Spring Data pagination (`sort=field,direction`, repeatable) |

### Dispatch and suggestions

`POST /api/dispatch/assign` accepts `{ "volunteerId": 1, "shelterId": 5, "requiredSkill": "MEDICAL" }`. It verifies the shelter is `OPEN` and has positive available capacity before creating an assignment and changing the volunteer's status to `DISPATCHED`. Shelter-service connection errors, timeouts, or error responses return `503` without assigning the volunteer.

The suggestion endpoint scores exact skill matches with `+10` and adds a distance bonus of `1 / (1 + distanceKm)`. Results sort by score descending, then distance ascending.

## Errors

```json
{
  "timestamp": "2026-09-23T10:15:30",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Shelter service is unavailable or timed out",
  "path": "/api/dispatch/assign",
  "fieldErrors": null
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation failed (`fieldErrors` has per-field messages) |
| 401 | Missing/invalid token or wrong credentials |
| 404 | Volunteer not found |
| 409 | Volunteer is unavailable/unqualified, or shelter is closed/full |
| 503 | Shelter service unavailable, timed out or returned an error |

## Concurrency

`Volunteer` uses a JPA `@Version` column for optimistic locking. Dispatch locks the volunteer row while checking availability and saving the assignment, so concurrent dispatch requests cannot assign the same volunteer twice. Shelter capacity is checked by shelter-management-service, which remains the owner of shelter capacity state.

## Packaging / Containers

- `mvn package` produces a runnable Spring Boot jar
- `Dockerfile` multi-stage build (Maven + JDK 25 → JRE 25)
- `docker-compose.yml` runs PostgreSQL + the app
- `.github/workflows/ci.yml` runs `mvn clean verify` on Java 25 for the shelter, victim, and volunteer services

## Project Layout

```
src/main/java/com/drrcp/volunteerdispatch/
├── api/            ApiError response body
├── config/         SecurityConfig + WebClientConfig
├── controller/     REST endpoints (volunteers, dispatch + auth)
├── domain/         Volunteer, DispatchAssignment, User, and enums
├── dto/            Request/response objects
├── exception/      Custom exceptions + GlobalExceptionHandler
├── repository/     Spring Data JPA repositories
├── security/       JwtService (issue/validate)
└── service/        Business logic (VolunteerService)
```

## Running Tests

`mvn clean test` uses an in-memory H2 database (no PostgreSQL required) and covers:

- `VolunteerServiceTest` – CRUD, pagination/search, availability filters
- `VolunteerSecurityIntegrationTest` – JWT access, public health endpoint
- `DispatchIntegrationTest` – shelter lookup/token forwarding, status/capacity checks, timeout/503 behavior, suggestion scoring using MockWebServer