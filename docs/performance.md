# Performance & UX Checklist

Use this checklist to improve perceived performance and runtime UX in a measurable way.

## 1) SSR for first paint of HR pages (optional)

- [ ] Evaluate Angular SSR for `/hr/*` routes where first contentful paint is currently slow.
- [ ] Run A/B measurement (CSR vs SSR) for `FCP`, `LCP`, and `TTI` on representative HR pages.
- [ ] If SSR is enabled, ensure auth-sensitive sections do not leak private data in server-rendered HTML.
- [ ] Add server-side cache policy only for non-user-specific shell content.

Current state:

- Frontend is deployed as static Angular build served by Nginx.
- SSR is not enabled.

Pros:

- Faster first paint and better SEO/share previews.
- Better perceived performance on cold loads and slower devices.

Cons:

- Higher deployment complexity (Node server/runtime, hydration issues).
- Extra work for auth-aware rendering and cache correctness.

Recommended decision rule:

- Enable SSR only if measured `LCP` improves materially (for example, >20%) on HR entry pages without adding auth/caching risk.

## 2) Route-level code splitting and lazy loading

- [x] Convert HR routes into lazy-loaded feature routes/modules.
- [x] Keep login/public route in primary bundle; defer HR admin and profile-heavy views.
- [x] Add preloading strategy only for likely next routes after login.
- [ ] Verify production network waterfall and chunk split using local production builds.

Implementation notes:

- Use route `loadComponent` / `loadChildren` so HR code is fetched on demand.
- App currently uses selective preloading (`data.preload = true`) for `employee/profile` and `hr/employees`.

## 3) Critical CSS inlining for login page

- [ ] Identify above-the-fold styles for login page shell.
- [ ] Inline only minimal critical CSS in `index.html` or server template.
- [ ] Defer non-critical stylesheets to avoid render-blocking.
- [ ] Validate no visual regressions in dark/light themes and mobile widths.

Current state:

- Critical CSS inlining is not configured.
- Production build has font inlining disabled (`optimization.fonts = false`) to avoid external font fetch issues during build/runtime.

Guardrails:

- Keep critical CSS small (target <10KB compressed).
- Avoid duplicating large style blocks that already exist in main bundles.

## 4) Bundle size budgets + Lighthouse checkpoints

- [x] Define Angular bundle budget for `initial` bundle in Angular build config.
- [ ] Add additional budgets (`anyComponentStyle`, lazy chunks) if needed.
- [ ] Fail local production build on budget overrun.
- [ ] Run Lighthouse manually (or on a team schedule) for key routes: login, HR search, employee profile.
- [ ] Track and document thresholds for `LCP`, `CLS`, `INP`, and total JS bytes.

Suggested initial targets:

- `LCP` < 2.5s on desktop baseline.
- `CLS` < 0.1.
- `INP` < 200ms.
- Keep initial JS transfer as low as practical (<250KB gz as a starting target).

## 5) Production RUM with web-vitals

- [ ] Add `web-vitals` package to frontend.
- [ ] Capture `LCP`, `CLS`, `INP`, `TTFB`, `FCP` in production only.
- [ ] Send metrics to observability backend/Sentry/DataDog with route + release tags.
- [ ] Include `X-Correlation-Id` (or session-level correlation tag) for backend trace alignment.
- [ ] Sample client metrics (for example 10-20%) to control ingestion volume.

Current state:

- RUM instrumentation is not implemented yet.

Example bootstrap snippet:

```ts
import { onCLS, onINP, onLCP, onTTFB, onFCP } from 'web-vitals';

const emit = (metric: { name: string; value: number; id: string }) => {
  if (window.location.hostname === 'localhost') return;
  navigator.sendBeacon(
    '/rum',
    JSON.stringify({
      ...metric,
      route: window.location.pathname,
      release: (window as { __APP_RELEASE__?: string }).__APP_RELEASE__ ?? 'unknown'
    })
  );
};

onCLS(emit);
onINP(emit);
onLCP(emit);
onTTFB(emit);
onFCP(emit);
```

## 6) Execution plan (recommended order)

- [ ] Baseline current metrics (Lighthouse + web-vitals dev capture).
- [x] Implement lazy loading first (lowest risk, high impact).
- [ ] Add bundle budgets and local enforcement.
- [ ] Add RUM instrumentation in production.
- [ ] Evaluate SSR based on measured gap after above optimizations.
- [ ] Add critical CSS only if login first paint remains a bottleneck.
