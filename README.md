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
- Session-based authentication via BFF pattern — no tokens in the browser.
- CSRF protection via `CsrfTokenStore` and HTTP interceptor (`X-XSRF-TOKEN` header).
- `AuthService` initialises at app startup: fetches CSRF token and checks `/api/auth/me` to restore session state.
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
- `employee_id` claim mapping for ownership enforcement; fallback to DB email lookup for users where the claim is absent.
- Admin client integration for user upsert and account enable/disable synchronization.
- BFF OAuth2 client: backend handles authorization code exchange and stores tokens in Redis — never in the browser.
- Backchannel logout: backend calls Keycloak server-to-server to revoke the session silently, then clears the Redis session and cookie.

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

## Authentication Architecture

### Before: Direct Frontend OAuth2 (Keycloak.js)

The original implementation used `keycloak-js` in the Angular frontend to handle the entire OAuth2/OIDC flow:

- Frontend initiated the authorization code flow directly with Keycloak.
- On successful login, Keycloak returned JWT access and refresh tokens to the browser.
- Angular held tokens in-memory via the `keycloak-js` adapter.
- API calls included `Authorization: Bearer <token>` headers constructed client-side.

Security risks: JWT tokens resided in browser memory, readable by any JavaScript executing in the page. XSS attacks could exfiltrate tokens. No server-side session revocation was possible.

### After: Backend-for-Frontend (BFF) Pattern

The current implementation delegates the entire OAuth2 flow to the Spring Boot backend:

- The backend acts as a confidential OAuth2 client, performing the authorization code exchange server-side.
- On successful login, access and refresh tokens are stored in **Redis** — they never reach the browser.
- The backend creates a `BffSession` and sets an `HttpOnly` session cookie (`BFF_SESSION_ID`).
- The Angular frontend communicates using only the session cookie — it never sees or handles tokens.
- CSRF protection uses the double-submit cookie pattern (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` request header).
- Logout performs a backchannel revocation call to Keycloak server-to-server, then clears the Redis session and cookie.

| Security property | Before (Keycloak.js) | After (BFF) |
|---|---|---|
| Token location | Browser memory (JavaScript) | Server-side Redis |
| Token visible to JS | Yes — readable by any script | No — never leaves server |
| XSS token theft | Possible | Not applicable |
| Session revocation | Not possible (wait for expiry) | Immediate (delete Redis session) |
| CSRF protection | Not required (bearer token) | Enforced (double-submit cookie) |
| Cookie security | N/A | `HttpOnly`, `SameSite=Lax` |
| Logout | Front-channel Keycloak redirect | Backchannel revocation + redirect |

### BFF Authentication Flow

1. User navigates to the app. `AuthService.init()` calls `GET /api/auth/csrf` and `GET /api/auth/me`.
2. No active session → `GET /api/auth/me` returns `401`. Angular routes to `/login`.
3. `LoginPageComponent.ngOnInit()` immediately redirects to `GET /oauth2/authorization/keycloak`.
4. Spring Security issues the OAuth2 authorization redirect to Keycloak (with PKCE).
5. User authenticates with Keycloak. Keycloak redirects back with an authorization code to the nginx proxy.
6. Backend exchanges the code for tokens server-to-server. `BffOAuth2LoginSuccessHandler` stores tokens in Redis and sets the `BFF_SESSION_ID` cookie.
7. User is redirected back to the Angular app's home page.
8. Angular re-initializes: `GET /api/auth/me` returns the user profile from the Redis session.
9. Route guards allow access to protected routes.
10. On logout, `POST /api/auth/logout` performs backchannel revocation with Keycloak, deletes the Redis session, clears the cookie, and returns `{logoutUrl: "/login"}`.

### BFF Migration: Issues Encountered

#### 1. `ERR_TOO_MANY_REDIRECTS` on first login

**Root cause**: After OAuth2 login success, the backend redirected to `/` (relative path), which resolved to the backend root, not the Angular frontend. The Angular guard re-triggered login, creating a loop.

**Fix**: `BffOAuth2LoginSuccessHandler` now issues an absolute redirect to the frontend origin (e.g. `http://localhost:4200/`). Route guards no longer `await` the `login()` call since it is a browser redirect, not a promise.

#### 2. Logout silently ignored (CSRF ordering)

**Root cause**: `AuthService.logout()` called `csrfStore.clearToken()` before sending the `POST /api/auth/logout` request. The CSRF interceptor read from the store — already empty — so no header was attached and the request was rejected with `403`.

**Fix**: Logout sequence reordered: POST first (CSRF token still present in store), then clear in-memory state and redirect.

#### 3. Logout endpoint returning 401

**Root cause**: `/api/auth/logout` was not listed in Spring Security's `permitAll()` rules. The session cookie was cleared as part of logout, so the request arrived unauthenticated and was rejected before processing.

**Fix**: `/api/auth/logout` added to both `permitAll()` and the CSRF ignore list in `SecurityConfig`.

#### 4. Keycloak showing an intermediate confirmation page on logout

**Root cause**: The initial logout used RP-Initiated Logout with `id_token_hint`. Keycloak displays a manual confirmation page when the provided `id_token` is expired or unrecognised.

**Fix**: Replaced with **backchannel logout** — the backend POSTs the `refresh_token` directly to Keycloak's token revocation endpoint server-to-server, terminating the SSO session without any browser interaction.

#### 5. Backchannel logout failing with `Connection refused` (Docker networking)

**Root cause**: The `RestTemplate` backchannel call used the public Keycloak URL (`http://localhost:8080`). From inside the Docker container `localhost` resolves to the container itself, not the Keycloak service.

**Fix**: A separate `app.keycloak.internal-issuer-uri` property (defaulting to `http://keycloak:8080/realms/hr`) is used for server-to-server calls. The public URL is retained for browser redirects.

#### 6. Employee role: "Session expired" on profile load

**Root cause**: Keycloak was not configured to include the `employee_id` custom claim in JWT tokens for users holding only the `EMPLOYEE` role. `BffOAuth2LoginSuccessHandler.extractEmployeeId()` returned `null`, so the session was created without an `employeeId`.

**Fix**: Added a fallback in `extractEmployeeId()` — when the `employee_id` claim is absent, the handler queries the database by the user's email address to resolve the `Employee` record.

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
