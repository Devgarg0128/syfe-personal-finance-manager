# Personal Finance Manager — Syfe Backend Assignment

A Java 17 / Spring Boot 3 REST API for session-based authentication, personal transactions, categories, savings goals, and monthly/yearly reports. **PostgreSQL is the only supported database; H2 is not included.**

## Requirements

- JDK 17+
- Maven 3.9+
- PostgreSQL 14+ (tested target: PostgreSQL 18)

## 1. Create the database

In `psql` or pgAdmin, create a database named `syfe_finance`:

```sql
CREATE DATABASE syfe_finance;
```

The database user must have permission to connect and create/alter tables in this database. Hibernate creates/updates the tables on startup with `DDL_AUTO=update`.

## 2. Configure environment variables

Set these in the same terminal from which you run Maven. Do not commit credentials.

**PowerShell (Windows):**

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "YOUR_ACTUAL_POSTGRES_PASSWORD"
# Optional; defaults to the local database shown below:
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/syfe_finance"
$env:COOKIE_SECURE = "false"
```

**Bash (Linux/macOS):**

```bash
export DB_USERNAME=postgres
export DB_PASSWORD='YOUR_ACTUAL_POSTGRES_PASSWORD'
export DATABASE_URL='jdbc:postgresql://localhost:5432/syfe_finance'
export COOKIE_SECURE=false
```

`DATABASE_URL` must be a JDBC URL beginning with `jdbc:postgresql://`, not a provider-specific `postgres://` URL. `DB_PASSWORD` is intentionally not stored in the project. The PostgreSQL driver is explicitly set to `org.postgresql.Driver` in `application.properties`.

## 3. Build, test, and run

```bash
mvn clean test
mvn spring-boot:run
```

Base URL: `http://localhost:8080/api`. The unit tests use Mockito and do not need a running database. Running the application does require PostgreSQL to be reachable and the environment variables to be set.

## Authentication example

Registration and login are public. Login establishes a server-side HTTP session and returns a `JSESSIONID` cookie. Keep and send that cookie on subsequent requests.

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"user@example.com","password":"password123","fullName":"Example User","phoneNumber":"+919876543210"}'

curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user@example.com","password":"password123"}'

curl -i -b cookies.txt http://localhost:8080/api/categories
```

## API endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Create a session |
| POST | `/api/auth/logout` | Invalidate the session |
| GET/POST | `/api/categories` | List or create categories |
| DELETE | `/api/categories/{name}` | Delete an eligible custom category |
| GET/POST | `/api/transactions` | List or create transactions |
| PUT/DELETE | `/api/transactions/{id}` | Update or delete a transaction (date is immutable) |
| POST/GET | `/api/goals` | Create or list savings goals |
| GET/PUT/DELETE | `/api/goals/{id}` | Read, update, or delete a goal |
| GET | `/api/reports/monthly/{year}/{month}` | Monthly report |
| GET | `/api/reports/yearly/{year}` | Yearly report |
| GET | `/health` | Health check |

Transaction filters: `startDate`, `endDate`, `categoryId`, and `type` (`INCOME` or `EXPENSE`). Dates use ISO `YYYY-MM-DD`.

## Deployment

A Dockerfile and `render.yaml` are included. Configure a persistent PostgreSQL instance in your hosting provider and set `DATABASE_URL` to its JDBC URL, `DB_USERNAME`, `DB_PASSWORD`, and `COOKIE_SECURE=true`. Do not commit secrets. Configure a persistent disk/database; free web instances may sleep and cause cold starts. Verify the deployed API using the assignment's `financial_manager_tests.sh` script before submission.

## Testing and coverage

```bash
mvn clean test
```

JaCoCo writes a report to `target/site/jacoco/index.html`. The assignment requests at least 80% coverage; review the generated report and add tests for any uncovered branches before claiming the threshold. The assignment's external test script is not included in this repository, so compatibility with all 86 external checks must be verified against the actual script.

## Design

- Controller → service → repository layering
- DTOs and Jakarta Bean Validation for request validation
- Centralized API exception handling
- BCrypt password hashing and server-side session-cookie authentication
- User-owned records are queried using the authenticated username/owner ID
- `BigDecimal` for money and `LocalDate` for transaction dates
- Predefined categories are seeded as global categories; custom categories belong to one user

## Configuration notes

- `spring.datasource.driver-class-name` is fixed to `org.postgresql.Driver`; there is no H2 fallback.
- `DDL_AUTO` defaults to `update` for assignment convenience. For production, use Flyway/Liquibase migrations and set `DDL_AUTO=validate`.
- Session cookies use `HttpOnly` and `SameSite=Lax`. `COOKIE_SECURE=true` should be used behind HTTPS. CSRF protection should be enabled or replaced with an appropriate defense before exposing a browser-facing session-authenticated production API.
