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

### API Versioning

The application exposes two API versions:

- **V1** (`/api/v1/employees`): Original employee management without manager relationships.
- **V2** (`/api/v2/employees`): Enhanced with manager-employee relationships and subordinates.

Both versions remain fully functional. The frontend uses V2 for all operations.

### Roles

The application uses two primary realm roles:

- `HR_ADMIN`
- `EMPLOYEE`

### Use cases by role

#### HR Admin

- Create employee records with optional manager assignment.
- Search employees by `employeeId` (exact, case-insensitive) or `lastName` (partial, case-insensitive).
- View employee details including assigned manager.
- Perform full employee updates including manager assignment/changes.
- Update employee contact information.
- View any employee's direct reports.

#### Employee

- View own profile including assigned manager and direct reports (read-only).
- Update own contact fields (home/mailing address, phone) for own record only.

### Manager Relationships (V2 API)

- Employees can optionally have a manager (another employee).
- Manager assignment is optional and can be set/cleared by HR admins.
- Employees can view their own manager and direct reports.
- HR admins can view and modify any manager assignments.
- Self-management is prevented (employees cannot be their own manager).

### Cross-cutting business rules

- `emailAddress` is immutable after employee creation.
- For newly added employees, Keycloak `username` is set to the employee `emailAddress`.
- New employee accounts are initialized with default password `ChangeMe123!`, which users can change on first login.
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
- API versioning with backward-compatible v1 and feature-enhanced v2 endpoints.
- Custom JPQL queries with `JOIN FETCH` for eager loading manager relationships (N+1 prevention).

### Java modernization checklist (Java 17)

- Prefer `record` for immutable DTOs and configuration models.
- Use sealed exception hierarchies for closed, type-safe error modeling.
- Use switch expressions (Java 17-safe forms) for small mapping logic.
- Favor `Optional`/`ifPresentOrElse` to flatten null-heavy control flow.
- Keep behavior-preserving refactors small and verify with focused tests first.

### Identity Layer (Keycloak)

- Externalized authentication and role model in Keycloak.
- Realm role mapping (`HR_ADMIN`, `EMPLOYEE`) to backend authorities.
- `employee_id` claim mapping for ownership enforcement.
- Admin client integration for user upsert and account enable/disable synchronization.

### Data Layer (PostgreSQL + Redis)

- PostgreSQL as source of truth for employee records.
- Flyway migrations for schema versioning (V1: base schema, V2: unique email constraint, V3: manager relationships).
- Redis-backed caching for selected read paths.
- `jsonb` storage for flexible address payloads.
- Self-referencing foreign key for manager-employee relationships with cascade rules.

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

Pre-build checklist (before `docker compose ... up --build`):

```bash
cd backend
./mvnw clean -DskipTests package
cd ..
```

- Backend JAR build is required because `backend/Dockerfile` copies `target/*.jar`.
- Frontend prebuild is not required; the frontend Docker image runs `npm run build` during image build.

1. Start all services:

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

If port `8081` is already in use on your machine, start with an override:

```bash
BACKEND_HOST_PORT=18081 docker compose -f infra/docker-compose.yml up --build -d
```

2. Verify container health:

```bash
docker compose -f infra/docker-compose.yml ps
curl http://localhost:${BACKEND_HOST_PORT:-8081}/actuator/health
curl http://localhost:8080/realms/hr/.well-known/openid-configuration
```

3. Seed Keycloak admin mappings (required for backend employee provisioning):

```bash
cd infra
./scripts/seed-hr-admin.sh
cd ..
```

4. Open the application:

- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:${BACKEND_HOST_PORT:-8081}`
- Keycloak: `http://localhost:8080`

5. Default seeded user (from realm import):

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
- Security: `docs/security.md` (employee credential bootstrap: `docs/security.md#employee-credential-bootstrap`)
- Performance: `docs/performance.md`
- Optional reference snippets: `docs/examples/README.md`
