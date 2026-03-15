# Architecture

## 1) High-level component diagram

```mermaid
flowchart LR
    U[Browser User] --> F[Frontend Angular + Nginx]
    F -->|Session cookie BFF_SESSION_ID| B[Backend Spring Boot BFF + API]
    B -->|Authorization code exchange| K[Keycloak]
    B -->|Backchannel logout revocation| K
    B -->|Role + employee_id checks| B
    B --> P[(PostgreSQL)]
    B --> R[(Redis — BFF Sessions)]
    B -->|Admin provisioning APIs| K

    subgraph Infra
      P
      R
      K
      B
      F
    end
```

### Component responsibilities

- Frontend: user interface, route guards, session-based API calls (never handles tokens directly).
- Backend: OAuth2 confidential client (authorization code exchange), BFF session management, business rules, validation, authorization, termination enforcement, data persistence.
- PostgreSQL: source of truth for employee records.
- Redis: BFF session storage (access/refresh tokens, user info, 30-minute TTL); also short-lived employee read cache.
- Keycloak: identity provider, token issuer, role model, admin provisioning target.

## 2) API versioning strategy

The application exposes two API versions:

- **V1** (`/api/v1/employees`): Original employee management API without manager relationships. Remains fully functional for backward compatibility.
- **V2** (`/api/v2/employees`): Enhanced API with manager-employee relationships, subordinates endpoint, and organizational hierarchy support.

Both versions coexist. V1 endpoints continue to work with the enhanced entity model (manager relationships are simply not exposed in V1 responses). The frontend uses V2 for all operations.

## 3) API contract summary (V1)

Base path: `/api/v1/employees`

### Create employee (HR admin)

- `POST /api/v1/employees`
- Auth: `ROLE_HR_ADMIN`

Example request:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "jobTitle": "Engineer",
  "dateOfBirth": "1990-01-01",
  "gender": "Male",
  "dateOfHire": "2024-01-01",
  "dateOfTermination": null,
  "homeAddress": "{\"line1\":\"10 Main St\",\"city\":\"Austin\"}",
  "mailingAddress": "{\"line1\":\"PO Box 10\",\"city\":\"Austin\"}",
  "telephoneNumber": "+1-555-0100",
  "emailAddress": "john.doe@company.local"
}
```

Example response (`201`):

```json
{
  "id": "f5e4c0f6-3f77-4a5c-bcbf-333333333333",
  "employeeId": "EMP-900001",
  "firstName": "John",
  "lastName": "Doe",
  "jobTitle": "Engineer",
  "emailAddress": "john.doe@company.local",
  "dateOfHire": "2024-01-01",
  "dateOfTermination": null
}
```

Notes:

- Backend generates `employeeId` in `EMP-######` format.
- Backend provisions/updates Keycloak user by **email-based username** and syncs account enabled state from termination status.

### Get employee details

- `GET /api/v1/employees/{employeeId}`
- Auth: `ROLE_HR_ADMIN` or owning `ROLE_EMPLOYEE`

Example response (`200`):

```json
{
  "id": "f5e4c0f6-3f77-4a5c-bcbf-333333333333",
  "employeeId": "EMP-900001",
  "firstName": "John",
  "lastName": "Doe",
  "jobTitle": "Engineer",
  "dateOfBirth": "1990-01-01",
  "gender": "Male",
  "dateOfHire": "2024-01-01",
  "dateOfTermination": null,
  "homeAddress": "{\"line1\":\"10 Main St\",\"city\":\"Austin\"}",
  "mailingAddress": "{\"line1\":\"PO Box 10\",\"city\":\"Austin\"}",
  "telephoneNumber": "+1-555-0100",
  "emailAddress": "john.doe@company.local",
  "createdAt": "2026-02-28T19:00:00Z",
  "updatedAt": "2026-02-28T19:00:00Z"
}
```

### Update employee (full)

- `PUT /api/v1/employees/{employeeId}`
- Auth: `ROLE_HR_ADMIN`

### Patch contact data

- `PATCH /api/v1/employees/{employeeId}/contact`
- Auth: `ROLE_HR_ADMIN` or owning `ROLE_EMPLOYEE`

Example request:

```json
{
  "homeAddress": "{\"line1\":\"101 New Street\",\"city\":\"Austin\"}",
  "mailingAddress": "{\"line1\":\"PO Box 10\",\"city\":\"Austin\"}",
  "telephoneNumber": "+1-555-0101"
}
```

Note: `emailAddress` cannot be changed once created; including it in contact patch returns `400`.

### Search employees (paginated)

- `GET /api/v1/employees/search?employeeId=EMP-900001&page=0&size=25`
- `GET /api/v1/employees/search?lastName=Doe&page=0&size=25`
- Auth: `ROLE_HR_ADMIN`

Behavior:

- At least one of `employeeId` or `lastName` is required.
- If both are provided, `employeeId` takes precedence.
- `employeeId` is exact match, case-insensitive.
- `lastName` is partial match, case-insensitive.

Example response (`200`):

```json
{
  "content": [
    {
      "id": "f5e4c0f6-3f77-4a5c-bcbf-333333333333",
      "employeeId": "EMP-900001",
      "firstName": "John",
      "lastName": "Doe",
      "jobTitle": "Engineer",
      "emailAddress": "john.doe@company.local",
      "dateOfHire": "2024-01-01",
      "dateOfTermination": null
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 25 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 25,
  "number": 0
}
```

## 3.5) API contract summary (V2)

Base path: `//v2/employees`

V2 extends V1 with manager-employee relationship support. All V1 endpoints have V2 equivalents with `manager` field included in responses.

### Key differences from V1

- All response DTOs include `manager: { id, employeeId, firstName, lastName, jobTitle } | null`.
- Create and update requests accept optional `managerId` field.
- New endpoint: `GET /api/v2/employees/{employeeId}/subordinates` returns direct reports.

### Create employee with manager (HR admin)

- `POST /api/v2/employees`
- Auth: `ROLE_HR_ADMIN`

Example request with manager:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "jobTitle": "Engineer",
  "dateOfBirth": "1990-01-01",
  "gender": "Male",
  "dateOfHire": "2024-01-01",
  "dateOfTermination": null,
  "homeAddress": "{\"line1\":\"10 Main St\",\"city\":\"Austin\"}",
  "mailingAddress": "{\"line1\":\"PO Box 10\",\"city\":\"Austin\"}",
  "telephoneNumber": "+1-555-0100",
  "emailAddress": "john.doe@company.local",
  "managerId": "f1234567-89ab-cdef-0123-456789abcdef"
}
```

Example response (`201`):

```json
{
  "id": "f5e4c0f6-3f77-4a5c-bcbf-333333333333",
  "employeeId": "EMP-900001",
  "firstName": "John",
  "lastName": "Doe",
  "jobTitle": "Engineer",
  "emailAddress": "john.doe@company.local",
  "dateOfHire": "2024-01-01",
  "dateOfTermination": null,
  "manager": {
    "id": "f1234567-89ab-cdef-0123-456789abcdef",
    "employeeId": "EMP-900000",
    "firstName": "Jane",
    "lastName": "Smith",
    "jobTitle": "Senior Engineer"
  }
}
```

### Get employee details with manager

- `GET /api/v2/employees/{employeeId}`
- Auth: `ROLE_HR_ADMIN` or owning `ROLE_EMPLOYEE`

Response includes `manager` field (null if no manager assigned).

### Get employee's direct reports

- `GET /api/v2/employees/{employeeId}/subordinates`
- Auth: `ROLE_HR_ADMIN` or owning `ROLE_EMPLOYEE` (can view own subordinates)

Example response (`200`):

```json
[
  {
    "id": "f5e4c0f6-3f77-4a5c-bcbf-444444444444",
    "employeeId": "EMP-900002",
    "firstName": "Alice",
    "lastName": "Johnson",
    "jobTitle": "Junior Engineer",
    "emailAddress": "alice.johnson@company.local",
    "dateOfHire": "2024-06-01",
    "dateOfTermination": null,
    "manager": {
      "id": "f5e4c0f6-3f77-4a5c-bcbf-333333333333",
      "employeeId": "EMP-900001",
      "firstName": "John",
      "lastName": "Doe",
      "jobTitle": "Engineer"
    }
  }
]
```

Notes:

- Employees can view their own subordinates (ownership check: `#employeeId == authentication.principal.employee_id`).
- Manager assignment is optional; `managerId` can be null or omitted to clear/leave unassigned.
- Self-management is validated: cannot set an employee as their own manager (400 error).

## 4) Data model and field tradeoffs

Primary employee fields were chosen to support:

- legal identity and staffing operations (`firstName`, `lastName`, `employeeId`),
- HR lifecycle (`dateOfHire`, `dateOfTermination`),
- communication (`telephoneNumber`, `emailAddress`),
- profile completeness (`dateOfBirth`, `gender`, addresses),
- auditability (`createdAt`, `updatedAt`).

### Address modeling tradeoff: text in app layer, JSONB in DB

Current implementation:

- DB columns are `jsonb` (`home_address`, `mailing_address`) for shape flexibility.
- Java entity currently maps them as `String` fields containing JSON text.

Why this is acceptable now:

- avoids tight coupling to an address object schema while API evolves,
- supports storing structured address payloads without schema churn.

Tradeoffs:

- validation of internal address shape is weaker in the backend,
- JSON query/filtering is less ergonomic when app layer treats values as text.

Upgrade option:

- introduce typed `Address` value object in backend and map via JSON converter,
- keep DB `jsonb`, add server-side address schema validation,
- version API carefully to preserve backward compatibility.

### Manager relationship modeling

Current implementation (V3 migration):

- `manager_id` column (UUID, nullable) with self-referencing foreign key to `employees.id`.
- Index on `manager_id` for efficient subordinates queries.
- `ON DELETE SET NULL` cascade: when a manager is deleted, their direct reports have `manager_id` cleared.

Entity mapping:

- JPA `@ManyToOne` for `manager` relationship (LAZY fetch by default).
- JPA `@OneToMany` for `subordinates` collection (LAZY fetch by default).
- Repository uses custom JPQL queries with `LEFT JOIN FETCH` to eagerly load manager relationships when needed, preventing N+1 query issues.

Business rules:

- Manager assignment is optional.
- Self-management is prevented at service layer validation.
- Manager can be any active employee (including HR admins).
- No validation enforcing organizational depth limits (flat hierarchies are supported).

Query optimization:

- `findByEmployeeIdWithManager()`: Eagerly fetches employee with manager in single query.
- `findByEmployeeIdWithSubordinates()`: Eagerly fetches subordinates and their managers to avoid N+1.
- Search queries include manager data to populate V2 DTOs efficiently.

## 5) Authentication and authorization flow

### Overview

The application uses the **Backend-for-Frontend (BFF) pattern** for authentication. The Angular frontend never handles OAuth2 tokens directly. All token exchange and storage is performed server-side. The browser communicates with the backend using an `HttpOnly` session cookie (`BFF_SESSION_ID`).

The backend has a dual security configuration:
- **OAuth2 Login Client**: handles the authorization code flow and creates BFF sessions.
- **OAuth2 Resource Server**: validates bearer tokens for direct API access (e.g. machine-to-machine or testing).

### Login flow

1. User opens the frontend. `AuthService.init()` calls `GET /api/auth/csrf` (anonymous) and `GET /api/auth/me`.
2. No active session → `GET /api/auth/me` returns `401`. Angular routes to `/login`.
3. `LoginPageComponent.ngOnInit()` immediately redirects to `GET /oauth2/authorization/keycloak`.
4. Spring Security issues the OAuth2 authorization redirect to Keycloak (PKCE, authorization code flow).
5. Keycloak authenticates the user and redirects back with an authorization code.
6. Nginx proxies the callback (`/login/oauth2/code/keycloak`) to the backend.
7. `BffOAuth2LoginSuccessHandler` exchanges the code for tokens server-to-server, creates a `BffSession` in Redis, and sets the `BFF_SESSION_ID` HttpOnly cookie. Access and refresh tokens never leave the server.
8. User is redirected to the Angular frontend home page (absolute URL to ensure the browser lands on the nginx port).
9. Angular re-initializes: `GET /api/auth/me` returns user info resolved from the Redis session.
10. Route guards allow access to protected routes.

### Logout flow

1. Angular calls `POST /api/auth/logout` (with `X-XSRF-TOKEN` header while CSRF store is still populated).
2. Backend retrieves the `BffSession` from Redis using the `BFF_SESSION_ID` cookie.
3. Backend POSTs the stored `refresh_token` to Keycloak's token endpoint (backchannel revocation, server-to-server using the Docker-internal hostname). This terminates the Keycloak SSO session silently.
4. Backend deletes the Redis session and clears the cookie.
5. Backend returns `{"logoutUrl": "/login"}`. Angular clears in-memory state and redirects.

### CSRF protection

- `CookieCsrfTokenRepository.withHttpOnlyFalse()` writes a `XSRF-TOKEN` cookie readable by JavaScript.
- Angular's `CsrfInterceptor` reads the token from the `CsrfTokenStore` (populated at init time from `GET /api/auth/csrf`) and attaches an `X-XSRF-TOKEN` header on every state-changing request.
- `/api/auth/logout` and the OAuth2 callback paths are excluded from CSRF checks.

### Role verification in backend

- For session-based requests: `BffSessionAuthenticationFilter` validates the `BFF_SESSION_ID` cookie, loads the `BffSession` from Redis, and sets a `BffSessionPrincipal` as the Spring Security principal.
- For bearer token requests: Spring Security OAuth2 Resource Server validates the JWT; `KeycloakJwtAuthenticationConverter` maps `realm_access.roles` to `ROLE_*` authorities.
- Method-level authorization (`@PreAuthorize`) gates each endpoint using either principal type.

### employee_id claim to DB record mapping

- For bearer token auth, the `employee_id` JWT claim is extracted by `KeycloakJwtAuthenticationConverter`.
- For BFF session auth, `employee_id` is stored in the `BffSession` at login time.
- `BffOAuth2LoginSuccessHandler.extractEmployeeId()` reads the `employee_id` claim from the OIDC token first; if absent (common for users where the Keycloak mapper is not configured), it falls back to a database lookup by email address.
- Ownership rules compare the path `employeeId` with `authentication.principal.employee_id` in `@PreAuthorize`.

### Termination handling

- `TerminatedEmployeeFilter` runs after authentication for both principal types.
- It loads the employee by `employee_id` and checks `dateOfTermination`.
- If terminated (`<= today`), request is denied with `403` and `reason: terminated`.
- On terminated requests, the filter also attempts `setUserEnabledByEmail(email, false)` to disable the Keycloak account.

### Key new environment variables (BFF)

| Variable | Purpose |
|---|---|
| `OAUTH2_CLIENT_ID` | Keycloak client ID for the BFF OAuth2 client |
| `OAUTH2_CLIENT_SECRET` | Keycloak client secret |
| `OAUTH2_AUTH_URI` | Keycloak authorization endpoint (browser-facing) |
| `OAUTH2_TOKEN_URI` | Keycloak token endpoint (internal, for code exchange) |
| `OAUTH2_REDIRECT_URI` | Redirect URI registered in Keycloak (`http://localhost:4200/login/oauth2/code/keycloak`) |
| `KEYCLOAK_PUBLIC_ISSUER_URI` | Keycloak issuer URI as seen by the browser |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection for BFF session storage |

See `infra/docker-compose.yml` for the full set of values used in local development.

## 6) Deployment notes

### Key environment variables

Backend runtime:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_ADMIN_SERVER_URL`, `KEYCLOAK_ADMIN_REALM`, `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET`
- `REDIS_HOST`, `REDIS_PORT`
- `KEYCLOAK_ADMIN_RETRY_MAX_ATTEMPTS`, `KEYCLOAK_ADMIN_RETRY_WAIT_DURATION`, `KEYCLOAK_ADMIN_RETRY_BACKOFF_MULTIPLIER`

Infra runtime:

- Keycloak admin bootstrap (`KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`)
- Postgres credentials (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)

### DB migrations

- Flyway migrations are executed on backend startup.
- Migration scripts live under `backend/src/main/resources/db/migration`.
- Migrations must be forward-only and idempotent for repeated environment startup.

### Upgrade path and backward compatibility

Recommended approach:

- Prefer additive API changes first (new optional fields, new endpoints).
- Keep existing response fields stable; avoid breaking renames/removals.
- Introduce deprecation window before removing legacy fields.
- Sequence rollout:
  1. deploy schema migration (backward compatible),
  2. deploy backend reading/writing both old/new shape if needed,
  3. deploy frontend using new fields,
  4. clean up deprecated columns/fields in a later release.

This minimizes client breakage during rolling deployments.
