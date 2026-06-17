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

The frontend MVP includes dashboard, transaction review, CSV import, category/tag management, and categorisation rule management screens. During local development, Vite proxies `/api` requests to the Spring Boot backend on port `8080`.

SQLite is configured as the local database at `backend/data/budget-tracker.sqlite`. The `backend/data/` folder is ignored by Git so local budget data is not committed.

Database schema changes are managed with Flyway migrations in `backend/src/main/resources/db/migration`. Hibernate validates the schema at startup, but Flyway owns creating and changing tables.

The database foundation currently includes:

- Accounts
- Categories
- Tags
- Categorisation rules
- Import batches

Default income and expense categories are seeded by migration so a fresh local database starts with useful category options. Property reporting tags are also seeded for rental income and common property expense buckets.

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

Account CRUD endpoints:

```http
GET /api/accounts
GET /api/accounts/{id}
POST /api/accounts
PUT /api/accounts/{id}
DELETE /api/accounts/{id}
```

Create/update request body:

```json
{
  "name": "Everyday",
  "bank": "Example Bank",
  "accountType": "CHECKING"
}
```

Transaction endpoints:

```http
GET /api/transactions
GET /api/transactions/{id}
PUT /api/transactions/{id}
DELETE /api/transactions/{id}
```

Transaction list filters:

```text
dateFrom, dateTo, accountId, categoryId, tagId, direction, search, page, size, sort
```

Transaction update request body:

```json
{
  "accountId": 1,
  "transactionDate": "2026-01-10",
  "description": "Coffee",
  "rawDescription": "COFFEE SHOP",
  "amount": 4.5,
  "direction": "EXPENSE",
  "categoryId": 2,
  "tagId": 1,
  "importBatchId": null
}
```

Category endpoints:

```http
GET /api/categories
POST /api/categories
PUT /api/categories/{id}
DELETE /api/categories/{id}
```

Tag endpoints:

```http
GET /api/tags
POST /api/tags
PUT /api/tags/{id}
DELETE /api/tags/{id}
```

Categorisation rule endpoints:

```http
GET /api/categorisation-rules
POST /api/categorisation-rules
PUT /api/categorisation-rules/{id}
DELETE /api/categorisation-rules/{id}
```

Rules match imported transaction descriptions case-insensitively. Active rules are applied by priority, with lower numbers winning. If no rule matches, imports use the active `Uncategorised` category when it exists.

Report endpoints:

```http
GET /api/reports/summary
GET /api/reports/spending-by-category
GET /api/reports/income-vs-expenses
GET /api/reports/property
```

Report query parameters:

```text
dateFrom, dateTo, accountId, groupBy
```

`groupBy` supports `FORTNIGHT`, `MONTH`, `QUARTER`, and `YEAR`. Dashboard summary cards and charts use these report endpoints.

The property report uses transaction tags to total rental income, mortgage, insurance, rates, repairs, property management fees, and other property expenses.

CSV import endpoint:

```http
POST /api/imports/transactions
```

Multipart form fields:

```text
accountId
file
```

The initial parser supports NAB-style CSV exports. Imports skip duplicate transactions using the account, transaction date, description, and amount as the duplicate key.

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

Frontend routes are hash-based: `#/dashboard`, `#/transactions`, `#/import`, `#/categories-tags`, `#/rules`, and `#/property-report`.

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

The collection includes health, account, transaction, report, category, tag, rule, and CSV import requests.

## CI

GitHub Actions runs on pushes and pull requests to `main`.

The workflow checks:

- Backend tests
- Frontend tests
- Frontend production build
