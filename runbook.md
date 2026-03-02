# Runbook

Short operational commands for local/dev environments.

> Compose file location: `infra/docker-compose.yml`.
>
> You can either:
> - run commands from repo root with `docker compose -f infra/docker-compose.yml ...`, or
> - `cd infra` and run `docker compose ...`.

## 1) Bring stack up / down

```bash
docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml ps
```

```bash
docker compose -f infra/docker-compose.yml down
```

## 2) Migrate database

### Preferred (app-driven Flyway migration)

Flyway runs automatically when backend starts:

```bash
docker compose -f infra/docker-compose.yml up -d --build backend
docker compose -f infra/docker-compose.yml logs -f backend
```

### Verify migration state

```bash
docker exec -it hr-postgres psql -U hr -d hrapp -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## 3) Re-seed Keycloak HR admin (Stacey)

```bash
chmod +x infra/scripts/seed-hr-admin.sh
./infra/scripts/seed-hr-admin.sh
```

Quick verify:

```bash
./infra/scripts/verify-keycloak-stacey.sh
```

## 4) Flush Redis cache

Flush all keys:

```bash
docker exec -it hr-redis redis-cli FLUSHALL
```

Flush current DB only:

```bash
docker exec -it hr-redis redis-cli FLUSHDB
```

## 5) Troubleshooting quick checks

Backend health:

```bash
curl -s http://localhost:8081/actuator/health
```

Keycloak OpenID config:

```bash
curl -s http://localhost:8080/realms/hr/.well-known/openid-configuration
```

If startup is inconsistent due to old local state, reset volumes:

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up --build -d
```
