# Sample Full-Stack HR Application

This is a sample full-stack application that I developed using Angular, Spring Boot, Keycloak, and PostgreSQL to illustrate practical architectural best practices for this stack.

The application models a simple HR domain with role-based access, employee lifecycle management, and identity integration via Keycloak.

## Technology Stack

- Frontend: Angular 19 + Angular Material
- Backend: Spring Boot 3 (Java 17)
- Identity and Access Management: Keycloak (OIDC/OAuth2)
- Database: PostgreSQL
- Cache: Redis
- Local orchestration: Docker Compose

## Repository Structure

- `frontend/` - Angular SPA
- `backend/` - Spring Boot REST API
- `infra/` - Docker Compose, Keycloak realm import, helper scripts
- `docs/` - architecture, security, performance notes and reference examples

## Functional Overview

### Roles

The application uses two primary realm roles:

- `HR_ADMIN`
- `EMPLOYEE`

### Use cases by role

#### HR Admin

- Create employee records.
- Search employees by `employeeId` (exact, case-insensitive) or `lastName` (partial, case-insensitive).
- View employee details.
- Perform full employee updates.
- Update employee contact information.

#### Employee

- View own profile (`/api/v1/employees/me` and corresponding UI route).
- Update own contact fields (home/mailing address, phone) for own record only.

### Cross-cutting business rules

- `emailAddress` is immutable after employee creation.
- Terminated employees are blocked from API access with `403` + `reason: terminated`.
- On terminated requests, backend attempts to disable the corresponding Keycloak account.

## Architectural Best Practices Demonstrated

### Frontend (Angular)

- Route-level lazy loading and selective preloading for key routes.
- Role-aware guards and app shell navigation.
- Token retrieval at runtime via `keycloak-js` integration (no localStorage/sessionStorage token persistence).
- Clear API boundary through a dedicated `EmployeeApiService`.

### Backend (Spring Boot)

- Layered design (controller/service/repository + DTO mapping).
- Method-level authorization (`@PreAuthorize`) with ownership checks.
- Explicit domain rules in service layer (immutable email, search precedence semantics).
- Transactional create flow with Keycloak provisioning and retry/fallback handling.
- Termination enforcement via dedicated security filter.
- Correlation ID filter (`X-Correlation-Id`) for traceability.

### Identity Layer (Keycloak)

- Externalized authentication and role model in Keycloak.
- Realm role mapping (`HR_ADMIN`, `EMPLOYEE`) to backend authorities.
- `employee_id` claim mapping for ownership enforcement.
- Admin client integration for user upsert and account enable/disable synchronization.

### Data Layer (PostgreSQL + Redis)

- PostgreSQL as source of truth for employee records.
- Flyway migrations for schema versioning.
- Redis-backed caching for selected read paths.
- `jsonb` storage for flexible address payloads.

### Infrastructure Layer (Docker Compose)

- Reproducible local stack with one compose file.
- Container health checks and dependency ordering.
- Keycloak realm auto-import for predictable local bootstrap.
- Runtime config through environment variables, not hard-coded secrets.

## Prerequisite Software

Install the following before running locally:

- Docker Desktop (or Docker Engine) with Docker Compose v2
- Git
- A modern browser (Chrome/Edge/Firefox)

Optional (only if running services outside Docker):

- Java 17+
- Maven 3.9+
- Node.js 20+
- npm 10+

## Run Locally
NOTE: The Docker Compose runtime environment is for local execution only. In a real production scenario, all credentials and secrets should be stored and sourced from a secure secrets storage solution such as Hashicorp Vault or AWS Secrets Manager. Credentials and secrets should never be committed to source control.

From the repository root:

1. Start all services:

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

2. Verify container health:

```bash
docker compose -f infra/docker-compose.yml ps
curl http://localhost:8081/actuator/health
curl http://localhost:8080/realms/hr/.well-known/openid-configuration
```

3. Open the application:

- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8081`
- Keycloak: `http://localhost:8080`

4. Default seeded user (from realm import):

- Username: `stacey.smith@company.local`
- Password: `ChangeMe123!`
- Role: `HR_ADMIN`

## Stop and Clean Up

Stop services:

```bash
docker compose -f infra/docker-compose.yml down
```

Stop and remove volumes (reset local data):

```bash
docker compose -f infra/docker-compose.yml down -v
```

## Documentation

- Architecture: `docs/architecture.md`
- Security: `docs/security.md`
- Performance: `docs/performance.md`
- Optional reference snippets: `docs/examples/README.md`
