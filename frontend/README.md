# ClaimFlow frontend

ClaimFlow is the responsive React frontend for the Enterprise Insurance Claim Processing System (ICPS). It is isolated from the Spring Boot backend so it can be installed and run without changing backend business logic or API contracts.

## Run locally

1. Open a terminal in `frontend`.
2. Copy `.env.example` to `.env`.
3. Set `VITE_API_BASE_URL` to the Spring Boot API root (default: `http://localhost:8080/api`).
4. Install and run:

```bash
npm install
npm run dev
```

Build the production bundle with `npm run build`.

## Features

- JWT login, registration, persistent session, automatic authorization header, and logout on `401`.
- Responsive sidebar/mobile drawer with role-based menu and route protection.
- Customer claim creation, draft edits/deletion, submission, details, documents, and timeline.
- Agent, investigator, and supervisor workflow queues mapped to the supplied backend workflow endpoints.
- Admin dashboard, user role/status administration, and read-only claims view.
- Typed Axios services, React Query caching/invalidation, form validation through React Hook Form and Zod, file upload validation, accessible UI primitives, status chips, and graceful empty/loading/error states.

## Backend contract used

All endpoint paths live in `src/constants/api.ts`. The interface follows the existing Spring Boot routes, including `/auth`, `/claims`, `/documents`, `/workflow/claims`, `/users/profile`, and `/admin`.

The current backend only exposes dashboard statistics for administrators; non-admin dashboards intentionally provide role-appropriate navigation instead of fabricating unavailable metrics. Server-side search, audit history, and richer role dashboards can be added when the planned Day 6/7 endpoints are available.

## Structure

```text
src/
  api/          Axios configuration
  components/   Reusable UI, pagination, uploads, status
  constants/    API paths and domain constants
  contexts/     Authentication session
  layouts/      Responsive application layout
  pages/        Authentication and operational screens
  routes/       Protected and role-based route guards
  services/     Typed backend integrations
  styles/       Tailwind entry styles
  types/        Backend DTO interfaces
  utils/        Display formatting helpers
```
