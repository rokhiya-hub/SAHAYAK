# SAHAYAK Web

A polished React + Tailwind demo for a disaster-relief coordination dashboard connected to the live Spring Boot shelter service.

## Prerequisites

- Node.js 18+
- Java 25 (for the backend services)
- The shelter-management-service running locally at http://localhost:8082

## Start the backend first

From the workspace root:

```bash
cd shelter-management-service
$env:SPRING_PROFILES_ACTIVE='h2'
mvn spring-boot:run -DskipTests
```

This service is the live backend used by the dashboard. The app will also show the other services in the architecture view, but they are intentionally marked as planned/offline unless their backend is running.

## Start the frontend

```bash
cd saahayak-web
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

Then open http://localhost:5173

## Live vs planned features

### Live
- Shelter dashboard list and live occupancy stats
- Check-in and check-out actions against /api/shelters/{id}/checkin and /checkout
- JWT authentication flow against /api/auth/login and /api/auth/register
- Concurrency demo using real backend conflict behavior at 409 responses

### UI built, backend pending

These services are represented in the architecture and landing page, but they are intentionally not treated as live data sources because their backend endpoints are not yet active.

```text
Volunteer Dispatch: http://localhost:8081
Resource Inventory: http://localhost:8083
Victim Registry: http://localhost:8085
Reporting Service: http://localhost:8084
```

The frontend shows a clear “Offline” state for those services instead of fabricating fake data.

## Environment variables

Copy `.env.example` to `.env` and adjust as needed:

```bash
cp .env.example .env
```

## Notes

- The app keeps the JWT in memory rather than localStorage because localStorage exposes the token to XSS-based theft.
- CORS is required for the browser to access the shelter backend from the Vite dev origin: http://localhost:5173
