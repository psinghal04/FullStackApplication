# Backend Notes

## Employee creation + Keycloak provisioning

When HR calls `POST /api/v1/employees`:

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
- `PATCH /api/v1/employees/{employeeId}/contact`:
  - rejects `emailAddress` updates

## Search behavior

`GET /api/v1/employees/search` supports `employeeId` and `lastName`:

- At least one of `employeeId` or `lastName` must be provided.
- If `employeeId` is present, it takes precedence.
- `employeeId` search is exact and case-insensitive.
- `lastName` search is partial and case-insensitive.

## Termination enforcement

- `TerminatedEmployeeFilter` blocks authenticated terminated employees (`dateOfTermination <= today`) with HTTP 403.
- Response payload:

```json
{
  "status": 403,
  "message": "Employee terminated",
  "reason": "terminated"
}
```

- On blocked requests, backend also attempts to disable the matching Keycloak user via email claim (`setUserEnabledByEmail(email, false)`).

## Keycloak config via env vars

- `KEYCLOAK_ADMIN_SERVER_URL`
- `KEYCLOAK_ADMIN_REALM`
- `KEYCLOAK_ADMIN_CLIENT_ID`
- `KEYCLOAK_ADMIN_CLIENT_SECRET`
- `KEYCLOAK_EMPLOYEE_ROLE`
- `KEYCLOAK_EMPLOYEE_DOMAIN`
- `KEYCLOAK_EMPLOYEE_TEMP_PASSWORD`
- `KEYCLOAK_EMPLOYEE_PASSWORD_TEMPORARY`

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
