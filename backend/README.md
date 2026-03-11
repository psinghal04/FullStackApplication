# Backend Notes

## API Versioning

The backend exposes two API versions:

- **V1** (`/api/v1/employees`): Original employee management API
- **V2** (`/api/v2/employees`): Enhanced API with manager-employee relationships

Both versions coexist and work with the same enhanced entity model. V1 simply omits manager data from responses.

## Employee creation + Keycloak provisioning

When HR calls `POST /api/v1/employees` or `POST /api/v2/employees`:

1. Backend generates a unique `employeeId` in `EMP-######` format.
2. Employee is saved in the DB (`EmployeeService#create`, transactional).
3. Keycloak Admin API upserts a user using:
   - `username`: normalized employee email (lowercase/trimmed)
   - `email`, `firstName`, `lastName`
   - `attributes.employee_id` as a list value
   - `enabled: true` in the upsert payload
4. Backend enforces effective account status with a second sync call:
   - `setUserEnabledByEmail(email, !isTerminated(employee))`

### Idempotency

- If user exists, backend updates the user representation.
- If user does not exist, backend creates it and then resolves the created user id.
- Realm roles are normalized to employee-only by removing current realm roles and assigning `KEYCLOAK_EMPLOYEE_ROLE`.

### Failure strategy

- Strategy: rollback DB create when Keycloak provisioning fails.
- `EmployeeService#create` is transactional and provisioning errors are surfaced as `KeycloakProvisioningException`.
- Retry (Resilience4j) is enabled for Keycloak calls:
  - `KEYCLOAK_ADMIN_RETRY_MAX_ATTEMPTS`
  - `KEYCLOAK_ADMIN_RETRY_WAIT_DURATION` (e.g. `300ms`)
  - `KEYCLOAK_ADMIN_RETRY_BACKOFF_MULTIPLIER`

## Update behavior

- `PUT /api/v1/employees/{employeeId}`:
  - enforces unique `employeeId`
  - rejects email changes (`emailAddress cannot be changed once created`)
  - syncs Keycloak enabled state based on termination date
- `PUT /api/v2/employees/{employeeId}`:
  - all V1 rules apply
  - accepts optional `managerId` for manager assignment
  - validates that managerId exists and prevents self-management
  - clearing manager: omit or set `managerId` to null
- `PATCH /api/v1/employees/{employeeId}/contact`:
  - rejects `emailAddress` updates

## Manager relationships (V2 API)

- Optional manager assignment via `managerId` field in create/update requests
- Self-management validation: returns 400 if employee tries to manage themselves
- Database: `manager_id` column with self-referencing FK, `ON DELETE SET NULL` cascade
- Entity: JPA `@ManyToOne` / `@OneToMany` bidirectional relationship with LAZY fetch
- Query optimization: custom JPQL with `LEFT JOIN FETCH` to prevent N+1 queries
- Authorization:
  - HR admins can assign/modify any manager relationships
  - Employees can view their own manager (read-only)
  - Employees can view their own direct reports via `/subordinates` endpoint

## Search behavior

Canonical API behavior and precedence rules:

- [Search employees (paginated)](../docs/architecture.md#search-employees-paginated)

## Termination enforcement

Canonical lifecycle/security flow details:

- [Termination handling](../docs/architecture.md#termination-handling)
- [How backend verifies roles and checks termination](../docs/security.md#5-how-backend-verifies-roles-and-checks-termination)

## Keycloak config via env vars

- `KEYCLOAK_ADMIN_SERVER_URL`
- `KEYCLOAK_ADMIN_REALM`
- `KEYCLOAK_ADMIN_CLIENT_ID`
- `KEYCLOAK_ADMIN_CLIENT_SECRET`
- `KEYCLOAK_EMPLOYEE_ROLE`
- `KEYCLOAK_EMPLOYEE_DOMAIN`
- `KEYCLOAK_EMPLOYEE_TEMP_PASSWORD`
- `KEYCLOAK_EMPLOYEE_PASSWORD_TEMPORARY`

## Java modernization checklist (Java 17)

Canonical checklist lives in the root docs:

- [Java modernization checklist](../README.md#java-modernization-checklist-java-17)

## Example Keycloak upsert payload

```json
{
  "username": "john.doe@company.local",
  "enabled": true,
  "email": "john.doe@company.local",
  "firstName": "John",
  "lastName": "Doe",
  "attributes": {
    "employee_id": ["EMP-000001"]
  }
}
```
