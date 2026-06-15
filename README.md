# Budget Tracker

A local-first personal budget tracker built as a monorepo.

## Stack

- Backend: Java 21, Spring Boot, Maven, SQLite, JPA/Hibernate
- Frontend: React, TypeScript, Vite
- Testing: JUnit, Spring MockMvc, Vitest, React Testing Library
- CI: GitHub Actions
- API testing: Postman

## Repository Layout

```text
budget-tracker/
  backend/              Spring Boot REST API
  frontend/             React + TypeScript app
  postman/              Postman API collection
  .github/workflows/    CI workflow
```

## Architecture Notes

This project starts as a single-user local application. The backend owns data access and exposes a REST API. The frontend is a separate Vite app so it can evolve independently while still living in the same repository.

SQLite is configured as the local database at `backend/data/budget-tracker.sqlite`. The `backend/data/` folder is ignored by Git so local budget data is not committed.

Database schema changes are managed with Flyway migrations in `backend/src/main/resources/db/migration`. Hibernate validates the schema at startup, but Flyway owns creating and changing tables.

The database foundation currently includes:

- Accounts
- Categories
- Tags
- Categorisation rules
- Import batches

Default income and expense categories are seeded by migration so a fresh local database starts with useful category options.

The first API slice is a health endpoint:

```http
GET /api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Run Locally

Requirements:

- Java 21
- Maven
- Node.js 22 or newer
- npm

Start the backend:

```powershell
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

Start the frontend in a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173`.

## Test and Build

Backend tests:

```powershell
cd backend
mvn test
```

Frontend tests:

```powershell
cd frontend
npm test
```

Frontend production build:

```powershell
cd frontend
npm run build
```

## API Testing

Import `postman/budget-tracker.postman_collection.json` into Postman. The collection includes a `baseUrl` variable set to `http://localhost:8080`.

Phase 2 adds database schema and seed data only. The public API surface is still the health endpoint.

## CI

GitHub Actions runs on pushes and pull requests to `main`.

The workflow checks:

- Backend tests
- Frontend tests
- Frontend production build
