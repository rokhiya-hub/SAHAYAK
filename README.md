# Sahayak

Disaster-relief platform: five Spring Boot domain services behind a Spring Cloud Gateway, plus a React frontend. Each service owns its own database and enforces its own JWT authorization; the gateway only routes.

## Repository Layout

Every top-level directory is one deployable unit. There is no shared module — services communicate over HTTP only.

This project keeps a clean service-per-folder model so the runtime layout stays familiar and stable.

```text
Sahayak/
├── api-gateway/                   # Spring Cloud Gateway
├── victim-registration-service/    # Victim intake and auth
├── shelter-management-service/     # Shelter inventory and check-in flows
├── resource-inventory-service/     # Relief stock and allocation
├── volunteer-dispatch-service/     # Volunteer roster and dispatch
├── reporting-service/             # SOAP/XSD reporting and audit log
├── saahayak-web/                  # React + Vite frontend
├── docker/                        # Shared PostgreSQL bootstrap
│   └── postgres/
│       └── init/
├── .github/                       # CI and project automation
├── docs/                          # Project documentation
├── docker-compose.yml             # Full-stack Compose setup
├── README.md                      # Main project overview and instructions
├── .gitignore                     # Git exclusions
├── .vscode/                       # Editor configuration
└── .env files / local config     # Managed per service or per environment
```

### Service map

| Directory | Port | Purpose |
|---|---:|---|
| `api-gateway/` | 8080 | Path-preserving routes to the domain APIs and the reporting SOAP endpoint |
| `victim-registration-service/` | 8081 | Victim intake, auth, shelter assignment |
| `shelter-management-service/` | 8082 | Shelter inventory, occupancy, check-in/out |
| `resource-inventory-service/` | 8083 | Relief stock, allocations, fulfillment |
| `volunteer-dispatch-service/` | 8084 | Volunteer roster, skill matching, dispatch |
| `reporting-service/` | 8085 | SOAP/XSD aggregation reports (also owns the platform audit log) |
| `saahayak-web/` | 5173 | React 19 + Vite frontend |
| `docker/postgres/init/` | — | Init SQL creating the per-service databases |

Each service directory holds the same set of files:

```
<service>/
├── pom.xml                 Maven build
├── src/main/java/…         package root: api, config, controller, domain,
│                           dto, exception, repository, security, service
├── src/main/resources/     application.yml + application-h2.yml
├── src/test/               JUnit 5 tests (H2, no PostgreSQL needed)
├── Dockerfile              multi-stage: Maven + JDK 25 → JRE 25
├── .dockerignore
├── docker-compose.yml      standalone: this service + its own PostgreSQL
└── README.md               endpoints, config, error codes, layout
```

The per-service `docker-compose.yml` files each start their own PostgreSQL bound to a host port, so they cannot run side by side. Use the root compose for the full stack.

## Running the Whole Stack

```bash
docker compose up --build
```

This starts one shared PostgreSQL on `5432` (with a separate database per service) plus all six JVMs. The init script at `docker/postgres/init/01-create-databases.sql` only runs on an empty volume; to recreate the databases after changing it, run `docker compose down -v`.

Bring up a subset:

```bash
docker compose up -d postgres
docker compose up -d --build victim-registration-service
```

## Running Without Docker

PostgreSQL is optional — every service ships an `h2` profile with an in-memory database. One terminal per service, from the repository root:

```bash
mvn -f victim-registration-service/pom.xml "-Dspring-boot.run.profiles=h2" spring-boot:run
mvn -f shelter-management-service/pom.xml "-Dspring-boot.run.profiles=h2" spring-boot:run
mvn -f resource-inventory-service/pom.xml "-Dspring-boot.run.profiles=h2" spring-boot:run
mvn -f volunteer-dispatch-service/pom.xml "-Dspring-boot.run.profiles=h2" spring-boot:run
mvn -f reporting-service/pom.xml  "-Dspring-boot.run.profiles=h2" spring-boot:run
mvn -f api-gateway/pom.xml spring-boot:run
```

Then the frontend:

```bash
cd saahayak-web
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173.

## Cross-Service Configuration

Services call each other by container name under compose, and by `localhost` when run with Maven. Override these when deploying anywhere else.

| Variable | Used by | Points at |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | all domain services | their own database |
| `JWT_SECRET` | all domain services | shared HMAC key; must be identical across services so forwarded bearer tokens validate |
| `SHELTER_SERVICE_URL` | victim-registration, volunteer-dispatch | shelter-management-service |
| `VICTIM_SERVICE_URL` | reporting-service | victim-registration-service |
| `RESOURCE_SERVICE_URL` | reporting-service | resource-inventory-service |
| `VOLUNTEER_SERVICE_URL` | reporting-service | volunteer-dispatch-service |
| `FRONTEND_ALLOWED_ORIGINS` | CORS config | browser origins allowed to call the APIs |
| `SERVICES_*_URL` | api-gateway | per-service base URLs |

## Databases

One PostgreSQL instance, five databases:

| Service | Database |
|---|---|
| victim-registration-service | `sahayak_victim` |
| shelter-management-service | `sahayak_shelter` |
| resource-inventory-service | `sahayak_resource` |
| volunteer-dispatch-service | `sahayak_volunteer` |
| reporting-service | `sahayak_reporting` |

Schemas are managed by Hibernate `ddl-auto: update`. There are no migration scripts yet.

## Gateway Routing

| Path | Service |
|---|---|
| `/api/auth/**`, `/api/victims/**` | Victim Registration |
| `/api/shelters/**` | Shelter Management |
| `/api/resources/**` | Resource Inventory |
| `/api/volunteers/**`, `/api/dispatch/**` | Volunteer Dispatch |
| `/ws/**` | Reporting (SOAP) |
| `/api/audit-log` | Reporting by default; send `X-Audit-Service: shelter\|victim\|resource\|volunteer` to read that service's trail |

The gateway forwards bearer tokens unchanged and does not authenticate.

## Testing

```bash
mvn -f <service>/pom.xml clean test
```

Tests run against in-memory H2, so no PostgreSQL is required. `.github/workflows/ci.yml` runs `mvn clean verify` for all six services on Java 25.

```bash
cd saahayak-web && npm run lint && npm run build
```

## Known Limitations

Backend gaps are documented in depth in `saahayak-web/README.md`. The short version: self-registration only ever assigns `USER` and there are no `ADMIN` accounts without pre-provisioning, non-audit endpoints accept any authenticated caller, and there is no password reset, invitation, or role-management API. Do not treat UI visibility as an access control boundary.
