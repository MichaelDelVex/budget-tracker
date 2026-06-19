# Budget Tracker

A local-first personal budget tracker for importing bank CSV exports, reviewing transactions, categorising spending, and reporting on savings and investment property activity.

The app is designed as a single-user local application first. Data stays in a local SQLite database, with a Spring Boot REST API and a React frontend in the same repository.

## Tech Stack

- Backend: Java 21, Spring Boot, Maven, SQLite, JPA/Hibernate, Flyway
- Frontend: React, TypeScript, Vite
- Testing: JUnit, Spring MockMvc, Mockito, Vitest, React Testing Library
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

The backend owns persistence and exposes REST endpoints under `/api`. Flyway manages SQLite schema changes in `backend/src/main/resources/db/migration`, while Hibernate validates the schema at startup.

The frontend is a Vite app with hash-based routes. During local development, Vite proxies `/api` requests to the backend on port `8080`.

SQLite data is stored at `backend/data/budget-tracker.sqlite`. The `backend/data/` folder is ignored by Git so local budget data is not committed.

Default categories and property-report tags are seeded by migrations so a fresh database has useful starting data.

SQLite foreign key checks are enabled for every application database connection. SQLite requires this per connection, so the app sets `PRAGMA foreign_keys=ON` when connections are opened.

## Local Database Safety

The local SQLite database contains sensitive financial data. Do not commit it, upload it to issue trackers, or share it in logs/screenshots unless you have intentionally scrubbed it.

DELETE endpoints permanently remove local financial data. Back up the SQLite database before using destructive API requests directly.

Default database path:

```text
backend/data/budget-tracker.sqlite
```

Back up the database while the backend is stopped:

```powershell
Copy-Item backend/data/budget-tracker.sqlite backend/data/budget-tracker.backup.sqlite
```

Restore from a backup while the backend is stopped:

```powershell
Copy-Item backend/data/budget-tracker.backup.sqlite backend/data/budget-tracker.sqlite
```

Reset the app to a fresh database while the backend is stopped:

```powershell
Remove-Item backend/data/budget-tracker.sqlite
```

The next backend startup will recreate the schema with Flyway migrations and seed data.

To use a different database path, override the datasource URL when starting the backend:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:sqlite:C:/Users/YourName/budget-data/budget-tracker.sqlite"
mvn spring-boot:run
```

Keep backups somewhere private and included in your normal machine backup routine.

## Run the Backend

Requirements:

- Java 21
- Maven

```powershell
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

Useful health check:

```http
GET http://localhost:8080/api/health
```

## Run the Frontend

Requirements:

- Node.js 22 or newer
- npm

```powershell
cd frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173`.

Routes:

- `#/dashboard`
- `#/transactions`
- `#/import`
- `#/categories-tags`
- `#/rules`
- `#/property-report`

## Run Tests

Backend:

```powershell
cd backend
mvn test
```

Frontend:

```powershell
cd frontend
npm test
```

Frontend production build:

```powershell
cd frontend
npm run build
```

CI runs backend tests, frontend tests, and the frontend build on pushes and pull requests to `main`.

## CSV Imports

Import transactions from the app's Import page or the API:

```http
POST /api/imports/transactions
```

Multipart form fields:

```text
accountId
file
```

Current CSV support is NAB-style exports. The parser normalises signed CSV amounts into the app model:

- `INCOME` and `EXPENSE` are stored as separate directions.
- `amount` is stored as a positive value.

Duplicate detection skips rows with the same account, transaction date, description, and amount. The import summary includes duplicate details so the Import page can show the uploaded row and the existing transaction or earlier CSV row it matched.

When a CSV includes a `Category` column, imports first try to match that value to a saved CSV category mapping, then to an active app category with the same income/expense type. Unknown CSV categories are shown in the import summary with matched rows, an editable app category name, and an option to save the mapping for future imports. If no CSV category match exists, imports apply active categorisation rules by priority. If no rule matches, the active `Uncategorised` category is used when available.

## API Overview

Health:

```http
GET /api/health
```

Accounts:

```http
GET /api/accounts
GET /api/accounts/{id}
POST /api/accounts
PUT /api/accounts/{id}
DELETE /api/accounts/{id}
DELETE /api/accounts/{id}/with-transactions
```

`DELETE /api/accounts/{id}` is the safe delete path and will fail if the account is still referenced. `DELETE /api/accounts/{id}/with-transactions` permanently deletes the account, its transactions, and its import batches. Back up the SQLite database before using the destructive delete endpoint.

Transactions:

```http
GET /api/transactions
GET /api/transactions/{id}
POST /api/transactions
PUT /api/transactions/{id}
DELETE /api/transactions/{id}
```

Transaction filters:

```text
dateFrom, dateTo, accountId, categoryId, tagId, direction, search, page, size, sort
```

Categories:

```http
GET /api/categories
POST /api/categories
PUT /api/categories/{id}
DELETE /api/categories/{id}
```

Tags:

```http
GET /api/tags
POST /api/tags
PUT /api/tags/{id}
DELETE /api/tags/{id}
```

Categorisation rules:

```http
GET /api/categorisation-rules
POST /api/categorisation-rules
PUT /api/categorisation-rules/{id}
DELETE /api/categorisation-rules/{id}
```

Imports:

```http
POST /api/imports/transactions
POST /api/imports/csv-categories
```

Budgets:

```http
GET /api/budgets/profiles
POST /api/budgets/profiles
PUT /api/budgets/profiles/{id}
DELETE /api/budgets/profiles/{id}
GET /api/budgets/profiles/{profileId}/nodes
POST /api/budgets/profiles/{profileId}/nodes
PUT /api/budgets/nodes/{id}
DELETE /api/budgets/nodes/{id}
```

Budget profiles can be saved as drafts with incomplete node percentages. A profile can only be activated when each sibling group totals exactly `100%`. Draft node changes cannot push a sibling group above `100%`.

Reports:

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

`groupBy` supports `FORTNIGHT`, `MONTH`, `QUARTER`, and `YEAR`.

## Postman

Import `postman/budget-tracker.postman_collection.json` into Postman.

Collection variables:

- `baseUrl`: defaults to `http://localhost:8080`
- `accountId`: used by account-scoped requests
- `transactionId`: used by transaction detail/update/delete requests
- `categoryId`: used by category and rule examples
- `tagId`: used by tag and rule examples
- `categorisationRuleId`: used by rule update/delete requests
- `budgetProfileId`: used by budget node requests
- `budgetNodeId`: used by budget node update/delete requests

Start the backend before sending requests. Create or update variable values as you create local records.

## Error Handling

API errors use a consistent JSON shape:

```json
{
  "message": "Validation failed",
  "fields": {
    "name": "must not be blank"
  }
}
```

The frontend displays both the main message and any field-level validation details returned by the API.

## Known Limitations

- Single-user local application; there is no authentication or multi-user isolation yet.
- CSV import currently supports NAB-style exports only.
- Duplicate detection uses account, date, description, and amount; it does not use bank transaction IDs.
- Transaction creation is import-first; manual transaction creation is not implemented yet.
- Reporting is based on stored category/tag assignments and does not yet support saved report presets.
- SQLite is configured for local use, not concurrent multi-user hosting.

## Future Roadmap

- Manual transaction creation and split transactions
- More bank CSV parsers
- Saved filters and report presets
- Budget targets by category
- Recurring transaction detection
- Exportable reports
- Optional authentication for shared household use
- Backup and restore workflow for the SQLite database
