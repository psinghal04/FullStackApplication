# Examples Index

This directory contains **reference snippets** for optional observability/security integrations.
These files are not auto-wired into runtime unless you explicitly integrate them.

## Current runtime status

### Implemented in app
- Backend request correlation IDs via `X-Correlation-Id` are implemented in `backend/src/main/java/com/example/hrapp/logging/CorrelationIdFilter.java`.

### Optional / example-only
- Backend Sentry tracing example:
  - `docs/examples/backend/sentry/SentryTracingConfigExample.java`
  - `docs/examples/backend/sentry/application-sentry-example.yml`
  - `docs/examples/backend/sentry/pom-fragment.xml`
- Frontend Sentry + correlation examples:
  - `docs/examples/frontend/sentry/sentry.init.example.ts`
  - `docs/examples/frontend/sentry/correlation-id.interceptor.example.ts`

## How to use these examples safely

1. Copy the snippet into real app code/config (do not run examples directly).
2. Replace placeholders (DSN, release, hostnames, sampling values).
3. Validate behavior in local dev and staging before production rollout.
4. Keep correlation and tracing scoped to backend API calls (avoid identity-provider endpoints).

## Related docs

- `docs/security.md`
- `docs/performance.md`
- `docs/architecture.md`
