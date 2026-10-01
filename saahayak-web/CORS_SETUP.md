# CORS Setup for SAHAYAK Frontend

The browser-based React app runs on the Vite dev origin:

- http://localhost:5173

The Spring Boot shelter-management-service must allow that origin in its CORS configuration.

## Required backend configuration

The service includes a CORS configuration for `localhost:5173` and includes the Authorization header.

Key settings:

- Allowed origins: http://localhost:5173
- Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Allowed headers: Authorization, Content-Type
- Exposed headers: Authorization

This is required because the frontend calls the backend directly from the browser and does not proxy through the server.

## Why this matters

Without explicit CORS support, the browser blocks requests such as:

- POST /api/auth/login
- POST /api/auth/register
- GET /api/shelters
- PATCH /api/shelters/{id}/checkin
- PATCH /api/shelters/{id}/checkout

## Live verification

The backend was confirmed to respond successfully when called from the browser origin after CORS was enabled.

## Operational note

For a local demo environment, the backend must be started first and the frontend must then run on the Vite port so the browser can access the live service.
