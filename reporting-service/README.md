# reporting-service

Contract-first SOAP reporting aggregator for the **Sahayak** disaster-relief platform.

Generate a district and date-range relief status report by querying victim registration, shelter management, resource inventory, and volunteer dispatch services in parallel.

## Tech Stack

- Java 25
- Spring Boot 3.5.x (Spring-WS, Web, WebFlux client, Data JPA, Security, OAuth2 Resource Server, Actuator)
- JAXB classes generated from `src/main/resources/xsd/relief-report.xsd`
- PostgreSQL for audit-log persistence (H2 in-memory available for local dev / tests)
- JWT (HS256) authentication
- Lombok, AspectJ, JUnit 5, MockWebServer

## Why SOAP

SOAP remains useful when integrating with legacy and government systems that depend on stable, formally described XML contracts. The WSDL/XSD give consumers a versionable schema and generated client types. NDMA's [SACHET public alert portal](https://sachet.ndma.gov.in/) is a relevant interoperability precedent: it disseminates structured disaster alerts using the Common Alerting Protocol (CAP). This is a precedent for standardized machine-readable public-sector messaging, not a claim that SACHET itself uses this service's SOAP transport.

## Getting Started

The default configuration expects PostgreSQL at `localhost:5432` (database/user/password `sahayak`). The easiest way to start the reporting service and its audit database is via Docker:

```bash
docker compose up --build
```

For local development:

```bash
# Run tests using the in-memory H2 profile
mvn clean verify

# Start with H2; upstream service URLs can be overridden with environment variables
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The reporting API listens on port `8085`. The service has no local copy of report data; it reads from the upstream services. The database is used only for the required audit log.

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8085` | HTTP/SOAP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sahayak` | Audit database JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sahayak` / `sahayak` | Audit database credentials |
| `JWT_SECRET` | dev placeholder | HMAC-SHA256 key (>= 32 bytes); use the same key as Sahayak services |
| `VICTIM_SERVICE_URL` | `http://localhost:8081` | Victim registration base URL |
| `SHELTER_SERVICE_URL` | `http://localhost:8082` | Shelter management base URL |
| `RESOURCE_SERVICE_URL` | `http://localhost:8083` | Resource inventory base URL |
| `VOLUNTEER_SERVICE_URL` | `http://localhost:8084` | Volunteer dispatch base URL |
| `reporting.connect-timeout` | `2s` | Upstream connection timeout |
| `reporting.response-timeout` | `5s` | Upstream response timeout |
| `reporting.page-size` | `500` | Page size used to fetch upstream reports |

## Authentication and Audit

The SOAP endpoint and WSDL require a Bearer JWT. The reporting service forwards that token to each upstream API so those services apply their normal validation and authorization rules. `GET /api/audit-log` additionally requires the `ADMIN` role claim. SOAP does not make an endpoint trusted by itself: callers still need authentication, authorization, transport protection, and an audit trail because government/partner networks and credentials can be misconfigured or compromised.

The audit aspect records public service methods whose names begin with `allocate`, `checkin`, `checkout`, `assign`, `register`, or `fulfill`. It records method/argument summaries, caller subject, timestamp, elapsed time, success, and outcome details; sensitive-looking values such as phone numbers and tokens are redacted. Audit writes use a separate transaction so failed business operations still leave an audit record.

## SOAP Contract

WSDL: `http://localhost:8085/ws/relief-report.wsdl`

SOAP endpoint: `http://localhost:8085/ws`

The operation accepts district name and inclusive `fromDate`/`toDate` dates. It aggregates active shelters, occupancy, registered victims associated with those shelters, non-rejected resource allocations, and currently dispatched volunteers. The four first-page requests run concurrently; additional pages and allocation histories are fetched concurrently as well. The upstream APIs do not expose a canonical district field, so shelter matching uses the shelter service's name/address search and other records are associated through shelter IDs.

Sample request:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:rep="http://drrcp.com/reporting">
  <soapenv:Header/>
  <soapenv:Body>
    <rep:ReliefStatusReportRequest>
      <rep:districtName>North district</rep:districtName>
      <rep:fromDate>2026-06-01</rep:fromDate>
      <rep:toDate>2026-06-30</rep:toDate>
    </rep:ReliefStatusReportRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Sample response:

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header/>
  <SOAP-ENV:Body>
    <ns2:ReliefStatusReportResponse xmlns:ns2="http://drrcp.com/reporting">
      <ns2:totalVictimsRegistered>42</ns2:totalVictimsRegistered>
      <ns2:totalSheltersActive>3</ns2:totalSheltersActive>
      <ns2:totalOccupancy>187</ns2:totalOccupancy>
      <ns2:resourceAllocations>
        <ns2:resourceName>Rice</ns2:resourceName>
        <ns2:quantityAllocated>40</ns2:quantityAllocated>
        <ns2:shelterId>7</ns2:shelterId>
      </ns2:resourceAllocations>
      <ns2:volunteersDispatched>6</ns2:volunteersDispatched>
      <ns2:generatedAt>2026-06-30T14:25:00Z</ns2:generatedAt>
    </ns2:ReliefStatusReportResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### Testing with SoapUI

1. Create a SOAP project from `http://localhost:8085/ws/relief-report.wsdl`.
2. Add an HTTP `Authorization: Bearer <JWT>` header to the request.
3. Select the generated `ReliefStatusReport` operation and fill in the district and date range.
4. Send the request to `http://localhost:8085/ws`.

## Audit API

`GET /api/audit-log?page=0&size=25&sort=timestamp,desc` returns paginated audit records and requires a JWT with role `ADMIN`.

## Packaging / Containers

- `mvn clean install` generates JAXB sources, tests the application, and produces a runnable Spring Boot jar
- `Dockerfile` multi-stage build (Maven + JDK 25 → JRE 25)
- `docker-compose.yml` runs PostgreSQL + the reporting service
- `.github/workflows/ci.yml` runs `mvn clean verify` on Java 25 for the reporting service and platform APIs

## Project Layout

```
src/main/java/com/drrcp/reporting/
├── api/            ApiError response body
├── aspect/         AuditLoggingAspect
├── config/         WebClient, security, and SOAP/WSDL configuration
├── controller/     Audit log REST endpoint
├── domain/         AuditLog entity
├── dto/            Upstream snapshots and audit/page responses
├── endpoint/       ReliefReportEndpoint
├── exception/      Aggregation errors + REST exception handling
├── repository/     Audit log repository
└── service/        Parallel report aggregation and audit writer
```

## Running Tests

`mvn clean verify` uses H2 and MockWebServer to cover secured WSDL/SOAP access, parallel upstream aggregation, admin-only audit reads, and audit persistence/redaction.