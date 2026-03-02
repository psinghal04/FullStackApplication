# Infra Stack

This folder provides the full local stack for development:

- PostgreSQL (`localhost:5432`)
- Redis (`localhost:6379`)
- Keycloak (`localhost:8080`)
- Backend API (`localhost:8081`)
- Frontend (`localhost:4200`)

## Start full stack

```bash
cd infra
docker compose up --build -d
```

From repo root, use:

```bash
docker compose -f infra/docker-compose.yml up --build -d
```

This builds and starts `postgres`, `redis`, `keycloak`, `backend`, and `frontend` on network `hr-network`.

JWT validation note (Docker): browser tokens are issued with issuer `http://localhost:8080/realms/hr`, while backend fetches JWKS from internal URL `http://keycloak:8080/.../certs`.

## Realm import options

### Option A: Auto-import via Docker Compose env/volume (default)

- File: `infra/keycloak/realm-export.json`
- Compose mounts file to `/opt/keycloak/data/import/realm-export.json`
- Keycloak starts with `start-dev --import-realm`

The imported realm includes:

- realm role `HR_ADMIN`
- user **Stacey Smith** (`stacey.smith@company.local`)
- password `ChangeMe123!`
- custom attribute `employee_id = HR-ADMIN-0001`
- `employee_id` protocol mapper on client `hr-frontend` (emits claim into access/id/userinfo tokens)
- confidential client `hr-admin-client` (secret: `hr-admin-secret`)
- `hr-admin-client` uses service account with `fullScopeAllowed=true` for admin API role mappings

### Option B: Manual import with `kc.sh import`

```bash
docker compose up -d postgres
docker run --rm \
  --network hr-network \
  -v "$PWD/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro" \
  quay.io/keycloak/keycloak:26.1 \
  import --file /opt/keycloak/data/import/realm-export.json
```

Note: run the above from `infra/` so `$PWD/keycloak/realm-export.json` resolves correctly.

Then start Keycloak normally:

```bash
docker compose up -d keycloak
```

## Optional: seed script for HR admin

If you need to re-seed the admin user/role after startup:

```bash
chmod +x scripts/seed-hr-admin.sh
./scripts/seed-hr-admin.sh
```

This script also ensures a matching employee row exists in the app database for `employee_id` (default `HR-ADMIN-0001`) so **My Profile** can load successfully.
It also grants `realm-management` roles (`query-users`, `view-users`, `manage-users`, `view-realm`) to `hr-admin-client` service account so backend employee provisioning works.

## Verify pre-seeded Stacey user

Run this after `docker compose up --build -d`:

```bash
chmod +x scripts/verify-keycloak-stacey.sh
./scripts/verify-keycloak-stacey.sh
```

Optional env overrides:

- `KEYCLOAK_URL` (default `http://localhost:8080`)
- `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (defaults `admin` / `admin123`)
- `REALM` (default `hr`)
- `EXPECTED_USERNAME` (default `stacey.smith@company.local`)
- `TIMEOUT_SECONDS` (default `60`)

## Smoke checks

```bash
docker compose ps
curl http://localhost:8081/actuator/health
curl http://localhost:8080/realms/hr/.well-known/openid-configuration
```

From repo root, use `docker compose -f infra/docker-compose.yml ps`.
