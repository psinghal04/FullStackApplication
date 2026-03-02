# Security Decisions

This document captures the security posture and implementation guidance for this stack.

Note: files under `docs/examples/**` are reference snippets and are not auto-wired into runtime unless you explicitly integrate them.

## 1) Why OIDC with Keycloak was chosen

OIDC + Keycloak was selected to separate authentication/identity from business logic.

Key benefits:

- Standards-based auth (`OAuth2` / `OIDC`) with JWT access tokens.
- Centralized identity management (users, roles, realm config, password policy, MFA options).
- Built-in federation support (SSO, external identity providers) if needed later.
- Role and claim mapping in tokens reduces custom auth code in backend services.
- Works well for both local development (realm import) and production hardening.

Architecture intent:

- Keycloak is the identity provider and token issuer.
- Backend is an OAuth2 resource server and validates bearer tokens.
- Frontend delegates login/logout to Keycloak.

## 2) Token storage tradeoffs and recommendation

### A) In-memory token storage

Pros:

- Strong protection against long-lived token theft from XSS (token is not persisted across reload).
- Simpler than cookie/BFF architecture.

Cons:

- User session is lost on full reload/browser restart unless refreshed via SSO.
- Requires careful handling of refresh and silent login.

Use when:

- SPA directly integrates with OIDC provider and you want lower complexity than BFF.

### B) HttpOnly secure cookie (recommended for production)

Pros:

- Access/refresh tokens are not readable by JavaScript (`HttpOnly`), reducing XSS exfiltration risk.
- Better session UX and centralized token handling.

Cons:

- Requires BFF or gateway pattern.
- Must defend CSRF (SameSite + CSRF token strategy).

Use when:

- Production environment with stricter browser threat model and larger attack surface.

### C) localStorage / sessionStorage

Pros:

- Easiest implementation.

Cons:

- High risk: tokens are readable by injected JS in XSS scenarios.
- Not recommended for sensitive systems.

Use when:

- Avoid if possible.

### Recommended approach

- Current acceptable approach for this project stage: in-memory token handling in SPA.
- Production recommendation: migrate to BFF with HttpOnly, Secure, SameSite cookies and short-lived access tokens.

Current implementation notes:

- Frontend uses `keycloak-js` and obtains bearer tokens at runtime (`AuthService#getAccessToken`).
- Frontend API service sets `Authorization: Bearer <token>` per request.
- No token persistence in `localStorage` or `sessionStorage` is implemented.

## 3) CSP, SRI, and secure headers (Nginx)

Current state:

- Frontend Nginx config currently focuses on SPA routing and cache headers.
- Strict security headers/CSP from the example below are **recommended** but not fully enforced in the current local `frontend/nginx/default.conf`.

Recommended hardening snippet:

Example `nginx` snippet:

```nginx
# Security headers
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
add_header Cross-Origin-Opener-Policy "same-origin" always;
add_header Cross-Origin-Resource-Policy "same-site" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

# CSP (adjust connect-src for your backend + keycloak hosts)
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self' https://auth.example.com https://api.example.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'" always;
```

Notes:

- HSTS only when TLS is enforced end-to-end.
- Keep `script-src` strict; avoid `unsafe-inline` and `unsafe-eval` in production.
- Include Keycloak and backend origins in `connect-src` only as needed.

### SRI guidance

- For third-party CDN assets, use Subresource Integrity (`integrity` + `crossorigin="anonymous"`).
- For Angular CLI-managed local bundles, hashing/caching is already handled by build output names.
- Prefer self-hosting critical scripts/styles when possible.

## 4) Recommended CORS policy values

Current state:

- Backend does not define a custom CORS configuration bean.
- Local deployment uses same-origin frontend-to-backend calls through Nginx (`/api` proxy), so broad CORS rules are not required for that path.

Recommended baseline:

- `allowedOrigins`: exact frontend origin(s), e.g. `https://app.example.com`.
- `allowedMethods`: `GET,POST,PUT,PATCH,DELETE,OPTIONS`.
- `allowedHeaders`: `Authorization,Content-Type,X-Correlation-ID`.
- `exposedHeaders`: minimal set only if required by client.
- `allowCredentials`: `false` for bearer-token auth; `true` only for cookie-based auth.
- `maxAge`: e.g. `3600` seconds.

Do not use wildcard origin (`*`) in production when credentials are used.

## 5) How backend verifies roles and checks termination

Current backend enforcement path:

1. **JWT validation**
   - Resource server validates bearer token signature/issuer via Spring Security OAuth2 Resource Server.

2. **Role extraction**
   - `backend/src/main/java/com/example/hrapp/security/KeycloakJwtAuthenticationConverter.java`
   - Reads `realm_access.roles` from Keycloak JWT and maps to `ROLE_*` authorities.
   - Extracts `employee_id` claim into custom principal.

3. **Authorization checks**
   - `backend/src/main/java/com/example/hrapp/employee/EmployeeController.java`
   - Uses `@PreAuthorize` with role checks and ownership checks, e.g. employee can access only own record via `authentication.principal.employee_id`.

4. **Termination enforcement**
   - `backend/src/main/java/com/example/hrapp/security/TerminatedEmployeeFilter.java`
   - Runs after bearer auth.
   - Looks up `employee_id` in DB and denies requests when `dateOfTermination <= today`.
   - Returns `403` with JSON payload including `reason: terminated`.
   - Attempts to disable Keycloak user (`setUserEnabledByEmail(email, false)`) when email claim is present.

5. **Filter wiring**
   - `backend/src/main/java/com/example/hrapp/security/SecurityConfig.java`
   - Adds `TerminatedEmployeeFilter` after bearer token authentication.

## Employee credential bootstrap

- For newly added employees, Keycloak `username` is set to the employee `emailAddress`.
- New employee accounts are initialized with default password `ChangeMe123!`, which users can change on first login.

## Action checklist

- [x] Keep Keycloak realm role model minimal (`HR_ADMIN`, `EMPLOYEE`) for current scope.
- [x] Protect employee endpoints with explicit authorization annotations.
- [x] Keep termination filter active for authenticated requests.
- [ ] Enforce explicit CORS policy for cross-origin production deployments.
- [ ] Add strict Nginx security headers + CSP in deployment config.
- [ ] Migrate to BFF + HttpOnly cookie strategy before production launch.
