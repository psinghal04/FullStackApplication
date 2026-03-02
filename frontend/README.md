# Frontend Authentication Notes

## Keycloak SPA init

Keycloak is initialized at app bootstrap in `src/main.ts` via `APP_INITIALIZER`, then guards protect routes.

## Token storage tradeoffs

Current implementation uses `keycloak-js` default in-memory token handling in the browser runtime.

- Pros: no localStorage persistence by default, reduced long-term token exposure.
- Cons: token lost on full page reload/session restore unless re-established via SSO check.

For highest security in production, prefer backend-for-frontend (BFF) with HttpOnly secure cookies. A pure SPA cannot directly set HttpOnly cookies from JavaScript.
